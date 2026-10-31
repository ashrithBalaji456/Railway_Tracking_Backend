package com.example.railtracker.service.impl;

import com.example.railtracker.client.RailwayDataProvider;
import com.example.railtracker.dto.LiveStationBoardResponse;
import com.example.railtracker.dto.StationBoardTrainDto;
import com.example.railtracker.dto.StationDto;
import com.example.railtracker.dto.StationPassType;
import com.example.railtracker.dto.StationTrainLiveStatus;
import com.example.railtracker.dto.TrainLiveStatusDto;
import com.example.railtracker.entity.Station;
import com.example.railtracker.exception.ResourceNotFoundException;
import com.example.railtracker.repository.StationRepository;
import com.example.railtracker.repository.TrainStationRepository;
import com.example.railtracker.entity.TrainStation;
import com.example.railtracker.service.StationService;
import com.example.railtracker.service.TrainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.LocalDate;
import java.time.Duration;

import com.example.railtracker.entity.Train;
import com.example.railtracker.repository.TrainRepository;

@Service
public class StationServiceImpl implements StationService {
    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");

    private static final Logger logger = LoggerFactory.getLogger(StationServiceImpl.class);

    private final StationRepository stationRepository;
    private final RailwayDataProvider railwayDataProvider;
    private final TrainStationRepository trainStationRepository;
    private final TrainRepository trainRepository;
    private final TrainService trainService;

    @Autowired
    public StationServiceImpl(
            StationRepository stationRepository,
            RailwayDataProvider railwayDataProvider,
            TrainStationRepository trainStationRepository,
            TrainRepository trainRepository,
            @Lazy TrainService trainService) {
        this.stationRepository = stationRepository;
        this.railwayDataProvider = railwayDataProvider;
        this.trainStationRepository = trainStationRepository;
        this.trainRepository = trainRepository;
        this.trainService = trainService;
    }

    // Overloaded constructor for tests or callers without TrainService
    public StationServiceImpl(
            StationRepository stationRepository,
            RailwayDataProvider railwayDataProvider,
            TrainStationRepository trainStationRepository,
            TrainRepository trainRepository) {
        this(stationRepository, railwayDataProvider, trainStationRepository, trainRepository, null);
    }

    @Override
    @Transactional
    @Cacheable(value = "station:lookup", key = "#query")
    public List<StationDto> searchStations(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        String cleanQuery = query.trim();
        logger.debug("Searching stations locally with query: {}", cleanQuery);
        List<Station> localStations = stationRepository.searchStations(cleanQuery, PageRequest.of(0, 25));

        if (!localStations.isEmpty()) {
            return localStations.stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());
        }


        logger.info("No local stations found. Fetching from external provider for query: {}", query);
        try {
            List<StationDto> externalStations = railwayDataProvider.searchStations(query);
            for (StationDto dto : externalStations) {
                if (stationRepository.findByStationCode(dto.stationCode()).isEmpty()) {
                    Station station = new Station(
                            dto.stationCode(),
                            dto.stationName(),
                            dto.city(),
                            dto.state(),
                            dto.latitude(),
                            dto.longitude()
                    );
                    stationRepository.save(station);
                    logger.debug("Saved station {} locally", dto.stationCode());
                }
            }
            return externalStations;
        } catch (Exception e) {
            logger.error("Failed to fetch stations externally", e);
            return List.of();
        }
    }

    @Override
    @Transactional
    public StationDto getStationDetails(String stationCode) {
        logger.debug("Retrieving details for station: {}", stationCode);
        return stationRepository.findByStationCode(stationCode)
                .map(this::mapToDto)
                .orElseGet(() -> {
                    logger.info("Station {} not found locally. Searching externally.", stationCode);
                    List<StationDto> searchResults = railwayDataProvider.searchStations(stationCode);
                    StationDto foundDto = searchResults.stream()
                            .filter(s -> s.stationCode().equalsIgnoreCase(stationCode))
                            .findFirst()
                            .orElseThrow(() -> new ResourceNotFoundException("Station not found: " + stationCode));

                    Station station = new Station(
                            foundDto.stationCode(),
                            foundDto.stationName(),
                            foundDto.city(),
                            foundDto.state(),
                            foundDto.latitude(),
                            foundDto.longitude()
                    );
                    stationRepository.save(station);
                    return foundDto;
                });
    }

    @Override
    public List<StationBoardTrainDto> getStationTrains(String stationCode, String type) {
        logger.debug("Retrieving trains for station: {} with filter: {}", stationCode, type);
        try {
            getStationDetails(stationCode);
        } catch (ResourceNotFoundException e) {
            logger.warn("Station {} is not available in lookup data. Continuing with empty/remote board lookup.", stationCode, e);
        }

        List<StationBoardTrainDto> allTrains = railwayDataProvider.getStationBoard(stationCode, true);
        if (allTrains != null) {
            allTrains = allTrains.stream()
                    .map(t -> ensureOriginDestHalts(t, stationCode))
                    .collect(Collectors.toList());
            for (StationBoardTrainDto t : allTrains) {
                saveTrainIfMissing(t);
            }
        }
        allTrains = enrichArrivalDays(allTrains, stationCode);

        if (type == null || type.equalsIgnoreCase("ALL")) {
            return allTrains;
        } else if (type.equalsIgnoreCase("STOPPING")) {
            return allTrains.stream()
                    .filter(StationBoardTrainDto::isHalt)
                    .collect(Collectors.toList());
        } else if (type.equalsIgnoreCase("NON_STOP")) {
            return allTrains.stream()
                    .filter(t -> !t.isHalt())
                    .collect(Collectors.toList());
        }
        return allTrains;
    }

    @Override
    @Cacheable(value = "station:live", key = "#stationCode + '-' + #hoursAhead + '-' + #type")
    public List<StationBoardTrainDto> getStationLiveBoard(String stationCode, int hoursAhead, String type) {
        logger.debug("Retrieving live board for station: {} with hoursAhead: {} and filter: {}", stationCode, hoursAhead, type);
        try {
            getStationDetails(stationCode);
        } catch (ResourceNotFoundException e) {
            logger.warn("Station {} is not available in lookup data. Continuing with empty/remote live board lookup.", stationCode, e);
        }

        List<StationBoardTrainDto> liveBoard = railwayDataProvider.getStationLiveBoard(stationCode, hoursAhead, type);
        if (liveBoard != null) {
            liveBoard = liveBoard.stream()
                    .map(t -> ensureOriginDestHalts(t, stationCode))
                    .collect(Collectors.toList());
            for (StationBoardTrainDto t : liveBoard) {
                saveTrainIfMissing(t);
            }
        }
        return enrichArrivalDays(liveBoard, stationCode);
    }

    @Override
    @Cacheable(value = "live-station", key = "#stationCode + ':' + #hours")
    public LiveStationBoardResponse getLiveStationBoardDetails(String stationCode, int hours) {
        logger.debug("Retrieving live board details for station: {} for next {} hours", stationCode, hours);
        
        StationDto st = null;
        try {
            st = getStationDetails(stationCode);
        } catch (ResourceNotFoundException e) {
            logger.warn("Station {} is not available in lookup data. Continuing.", stationCode);
        }
        
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
        ZonedDateTime nowIst = ZonedDateTime.now(IST_ZONE);
        ZonedDateTime windowStart = nowIst.minusMinutes(120);
        ZonedDateTime windowEnd = nowIst.plusHours(hours);

        String fromWindow = windowStart.format(timeFmt);
        String toWindow = windowEnd.format(timeFmt);
        LiveStationBoardResponse.WindowInfo windowInfo = new LiveStationBoardResponse.WindowInfo(fromWindow, toWindow, hours);

        LiveStationBoardResponse rawResponse = null;
        try {
            rawResponse = railwayDataProvider.getLiveStationBoardDetails(stationCode, hours);
        } catch (Exception e) {
            logger.debug("Live station endpoint /stations/{}/live not accessible on current tier: {}", stationCode, e.getMessage());
        }

        if (rawResponse != null && rawResponse.window() != null && rawResponse.window().from() != null) {
            LocalTime rawFrom = parseLocalTime(rawResponse.window().from());
            LocalTime rawTo = parseLocalTime(rawResponse.window().to());
            if (rawFrom != null && rawTo != null) {
                LocalDate baseDate = nowIst.toLocalDate();
                windowStart = ZonedDateTime.of(baseDate, rawFrom, IST_ZONE);
                windowEnd = ZonedDateTime.of(baseDate, rawTo, IST_ZONE);
                if (windowEnd.isBefore(windowStart)) {
                    windowEnd = windowEnd.plusDays(1);
                }
                fromWindow = windowStart.format(timeFmt);
                toWindow = windowEnd.format(timeFmt);
                windowInfo = new LiveStationBoardResponse.WindowInfo(fromWindow, toWindow, hours);
            }
        }

        List<LiveStationBoardResponse.LiveStationBoardTrain> candidateTrains = new ArrayList<>();
        Set<String> presentTrainNumbers = new HashSet<>();

        if (rawResponse != null && rawResponse.trains() != null && !rawResponse.trains().isEmpty()) {
            for (LiveStationBoardResponse.LiveStationBoardTrain t : rawResponse.trains()) {
                candidateTrains.add(t);
                if (t.trainNumber() != null) {
                    presentTrainNumbers.add(t.trainNumber().trim().toUpperCase());
                }
            }
        }

        // Merge scheduled trains in window if rawResponse missed any (e.g. delayed trains)
        List<StationBoardTrainDto> scheduledTrains = getStationTrains(stationCode, "ALL");
        if (scheduledTrains != null && !scheduledTrains.isEmpty()) {
            LocalTime winStartTime = windowStart.toLocalTime();
            LocalTime winEndTime = windowEnd.toLocalTime();
            
            List<StationBoardTrainDto> inWindow = scheduledTrains.stream()
                    .filter(t -> isTimeInWindow(t.scheduledArrival(), t.scheduledDeparture(), winStartTime, winEndTime))
                    .filter(t -> t.trainNumber() == null || !presentTrainNumbers.contains(t.trainNumber().trim().toUpperCase()))
                    .collect(Collectors.toList());
            
            List<StationBoardTrainDto> targetTrains = inWindow;
            final ZonedDateTime refStart = windowStart;
            targetTrains.sort(Comparator.comparingInt(t -> getMinutesFromZoned(t.scheduledArrival(), t.scheduledDeparture(), refStart)));
            
            // Free-plan budget protection: cap live lookups (up to 8 if candidateTrains was empty, or up to 4 if merging missing trains)
            final int MAX_LIVE_ENRICH_TRAINS = candidateTrains.isEmpty() ? 8 : 4;
            int enrichCount = Math.min(targetTrains.size(), MAX_LIVE_ENRICH_TRAINS);
            
            for (int i = 0; i < targetTrains.size(); i++) {
                StationBoardTrainDto sbt = targetTrains.get(i);
                boolean shouldEnrich = i < enrichCount;
                
                String trainNumber = sbt.trainNumber();
                String trainName = sbt.trainName();
                String trainType = sbt.trainType();
                String srcCode = sbt.sourceStation() != null ? sbt.sourceStation() : stationCode;
                String destCode = sbt.destinationStation() != null ? sbt.destinationStation() : stationCode;
                StationPassType passType = Boolean.TRUE.equals(sbt.isHalt()) ? StationPassType.STOPPING : StationPassType.PASS_THROUGH;
                
                String schArr = sbt.scheduledArrival() != null ? sbt.scheduledArrival().format(timeFmt) : null;
                String schDep = sbt.scheduledDeparture() != null ? sbt.scheduledDeparture().format(timeFmt) : null;
                String expArr = schArr;
                String expDep = schDep;
                String actArr = null;
                String actDep = null;
                Integer delay = sbt.delayMinutes() != null ? sbt.delayMinutes() : 0;
                String platform = sbt.platform();
                StationTrainLiveStatus status = StationTrainLiveStatus.SCHEDULED;
                
                if (shouldEnrich && trainService != null) {
                    try {
                        TrainLiveStatusDto liveStatus = trainService.getTrainLiveStatus(trainNumber);
                        if (liveStatus != null) {
                            TrainLiveStatusDto.StationStatusDto matchedStop = null;
                            if (liveStatus.stations() != null) {
                                for (TrainLiveStatusDto.StationStatusDto stop : liveStatus.stations()) {
                                    if (stop.stationCode() != null && stop.stationCode().equalsIgnoreCase(stationCode)) {
                                        matchedStop = stop;
                                        break;
                                    }
                                }
                            }
                            
                            if (matchedStop != null) {
                                actArr = matchedStop.actualArrival();
                                actDep = matchedStop.actualDeparture();
                                if (matchedStop.delayMinutes() != null) {
                                    delay = matchedStop.delayMinutes();
                                } else if (liveStatus.delayMinutes() != null) {
                                    delay = liveStatus.delayMinutes();
                                }
                                if (matchedStop.platform() != null && !matchedStop.platform().isBlank()) {
                                    platform = matchedStop.platform();
                                }
                                
                                String sStatus = matchedStop.status() != null ? matchedStop.status().toLowerCase() : "";
                                boolean hasActualDep = actDep != null && !actDep.isBlank();
                                boolean hasActualArr = actArr != null && !actArr.isBlank();
                                
                                if (sStatus.contains("not-start") || sStatus.contains("not_start")) {
                                    status = StationTrainLiveStatus.NOT_STARTED;
                                } else if (sStatus.contains("at-station") || sStatus.contains("at_station")) {
                                    status = StationTrainLiveStatus.AT_STATION;
                                } else if (sStatus.contains("depart") || (hasActualDep && !sStatus.contains("arriv") && !sStatus.contains("upcoming"))) {
                                    status = StationTrainLiveStatus.DEPARTED;
                                } else if (sStatus.contains("arriv") || (hasActualArr && !hasActualDep)) {
                                    status = StationTrainLiveStatus.ARRIVED;
                                } else if (sStatus.contains("upcoming") || sStatus.contains("approach")) {
                                    status = StationTrainLiveStatus.UPCOMING;
                                } else if (Boolean.TRUE.equals(liveStatus.isCancelled()) || (liveStatus.skippedStations() != null && liveStatus.skippedStations().contains(stationCode.toUpperCase()))) {
                                    status = StationTrainLiveStatus.STATUS_UNAVAILABLE;
                                } else if (liveStatus.currentStationCode() != null && !liveStatus.currentStationCode().isBlank()) {
                                    status = StationTrainLiveStatus.UPCOMING;
                                } else {
                                    status = StationTrainLiveStatus.SCHEDULED;
                                }
                            } else {
                                if (Boolean.TRUE.equals(liveStatus.isDiverted()) || Boolean.TRUE.equals(liveStatus.isCancelled())) {
                                    status = StationTrainLiveStatus.STATUS_UNAVAILABLE;
                                } else {
                                    status = StationTrainLiveStatus.SCHEDULED;
                                }
                            }
                        } else {
                            status = StationTrainLiveStatus.STATUS_UNAVAILABLE;
                        }
                    } catch (Exception e) {
                        logger.debug("Failed to enrich live status for train {}: {}", trainNumber, e.getMessage());
                        status = StationTrainLiveStatus.STATUS_UNAVAILABLE;
                    }
                }
                
                if (actArr != null && !actArr.isBlank()) {
                    expArr = actArr;
                } else if (delay != null && delay > 0 && sbt.scheduledArrival() != null) {
                    expArr = formatDelayedIso(sbt.scheduledArrival(), delay, windowStart);
                }
                
                if (actDep != null && !actDep.isBlank() && status == StationTrainLiveStatus.DEPARTED) {
                    expDep = actDep;
                } else if (delay != null && delay > 0 && sbt.scheduledDeparture() != null) {
                    expDep = formatDelayedIso(sbt.scheduledDeparture(), delay, windowStart);
                }
                
                if (status != StationTrainLiveStatus.DEPARTED) {
                    actDep = null;
                }
                
                candidateTrains.add(new LiveStationBoardResponse.LiveStationBoardTrain(
                        trainNumber,
                        trainName,
                        trainType,
                        new LiveStationBoardResponse.StationCodeName(srcCode, srcCode),
                        new LiveStationBoardResponse.StationCodeName(destCode, destCode),
                        passType,
                        schArr,
                        schDep,
                        expArr,
                        expDep,
                        delay,
                        platform,
                        status,
                        sbt.runningDays(),
                        actArr,
                        actDep
                ));
            }
        }

        for (LiveStationBoardResponse.LiveStationBoardTrain t : candidateTrains) {
            saveTrainIfMissing(t);
        }
        
        final ZonedDateTime finalWindowStart = windowStart;
        final ZonedDateTime finalWindowEnd = windowEnd;
        
        List<LiveStationBoardResponse.LiveStationBoardTrain> enrichedTrains = candidateTrains.stream()
                .map(t -> {
                    String srcCode = t.source().code();
                    String srcName = stationRepository.findByStationCode(srcCode)
                            .map(com.example.railtracker.entity.Station::getStationName)
                            .orElse(srcCode);
                            
                    String destCode = t.destination().code();
                    String destName = stationRepository.findByStationCode(destCode)
                            .map(com.example.railtracker.entity.Station::getStationName)
                            .orElse(destCode);

                    boolean isOriginOrDest = stationCode.equalsIgnoreCase(srcCode) || stationCode.equalsIgnoreCase(destCode);
                    StationPassType resolvedPassType = isOriginOrDest ? StationPassType.STOPPING : t.passType();

                    StationTrainLiveStatus resolvedStatus = t.liveStatus();
                    String actArr = t.actualArrival();
                    String actDep = t.actualDeparture();

                    if (resolvedStatus == StationTrainLiveStatus.NOT_STARTED || resolvedStatus == StationTrainLiveStatus.AT_STATION) {
                        actDep = null;
                    } else if (resolvedStatus == StationTrainLiveStatus.UPCOMING) {
                        actDep = null;
                    } else if (actDep != null && !actDep.isBlank()) {
                        resolvedStatus = StationTrainLiveStatus.DEPARTED;
                    } else if (actArr != null && !actArr.isBlank()) {
                        resolvedStatus = StationTrainLiveStatus.ARRIVED;
                    }
                            
                    return new LiveStationBoardResponse.LiveStationBoardTrain(
                            t.trainNumber(),
                            t.trainName(),
                            t.trainType(),
                            new LiveStationBoardResponse.StationCodeName(srcCode, srcName),
                            new LiveStationBoardResponse.StationCodeName(destCode, destName),
                            resolvedPassType,
                            t.scheduledArrival(),
                            t.scheduledDeparture(),
                            t.expectedArrival(),
                            t.expectedDeparture(),
                            t.delayMinutes(),
                            t.platform(),
                            resolvedStatus,
                            t.runningDays(),
                            actArr,
                            actDep
                    );
                })
                .filter(t -> {
                    ZonedDateTime passageTime = resolvePassageDateTime(t, finalWindowStart);
                    return !passageTime.isBefore(finalWindowStart) && !passageTime.isAfter(finalWindowEnd);
                })
                .sorted((t1, t2) -> {
                    int p1 = getLiveStatusPriority(t1.liveStatus());
                    int p2 = getLiveStatusPriority(t2.liveStatus());
                    if (p1 != p2) {
                        return Integer.compare(p1, p2);
                    }
                    
                    ZonedDateTime dt1 = resolvePassageDateTime(t1, finalWindowStart);
                    ZonedDateTime dt2 = resolvePassageDateTime(t2, finalWindowStart);
                    
                    return dt1.compareTo(dt2);
                })
                .collect(Collectors.toList());
                
        LiveStationBoardResponse.StationInfo resolvedStation = new LiveStationBoardResponse.StationInfo(
                st != null ? st.stationCode() : stationCode,
                st != null ? st.stationName() : stationCode,
                st != null ? st.city() : null,
                st != null ? st.latitude() : 0.0,
                st != null ? st.longitude() : 0.0
        );
        
        return new LiveStationBoardResponse(resolvedStation, windowInfo, enrichedTrains);
    }
    
    private boolean isTimeInWindow(LocalTime arr, LocalTime dep, LocalTime start, LocalTime end) {
        LocalTime t = arr != null ? arr : dep;
        if (t == null) return true;
        if (start.isBefore(end)) {
            return !t.isBefore(start) && !t.isAfter(end);
        } else {
            return !t.isBefore(start) || !t.isAfter(end);
        }
    }

    private int getMinutesFromNow(LocalTime arr, LocalTime dep, LocalTime now) {
        LocalTime t = arr != null ? arr : dep;
        if (t == null) return 1440;
        int diff = (int) java.time.Duration.between(now, t).toMinutes();
        return diff < 0 ? diff + 1440 : diff;
    }

    private LocalTime parseLocalTime(String timeStr) {
        if (timeStr == null || timeStr.isBlank()) return null;
        try {
            String clean = timeStr.trim();
            if (clean.contains("T")) {
                int tIdx = clean.indexOf('T');
                if (clean.length() >= tIdx + 6) {
                    clean = clean.substring(tIdx + 1, tIdx + 6);
                }
            } else if (clean.length() > 5) {
                clean = clean.substring(0, 5);
            }
            return LocalTime.parse(clean);
        } catch (Exception e) {
            return null;
        }
    }
    
    private ZonedDateTime resolvePassageDateTime(LiveStationBoardResponse.LiveStationBoardTrain t, ZonedDateTime refStart) {
        if (t == null) return refStart.plusYears(1);
        String timeStr = t.expectedArrival() != null && !t.expectedArrival().isBlank() ? t.expectedArrival() :
                         t.expectedDeparture() != null && !t.expectedDeparture().isBlank() ? t.expectedDeparture() :
                         t.actualArrival() != null && !t.actualArrival().isBlank() ? t.actualArrival() :
                         t.actualDeparture() != null && !t.actualDeparture().isBlank() ? t.actualDeparture() :
                         t.scheduledArrival() != null && !t.scheduledArrival().isBlank() ? t.scheduledArrival() :
                         t.scheduledDeparture();
        return parsePassageDateTime(timeStr, refStart);
    }

    private ZonedDateTime parsePassageDateTime(String timeStr, ZonedDateTime refStart) {
        if (timeStr == null || timeStr.isBlank()) {
            return refStart.plusYears(1);
        }
        String clean = timeStr.trim();
        if (clean.contains("T")) {
            try {
                return ZonedDateTime.parse(clean);
            } catch (Exception e) {
                try {
                    return java.time.OffsetDateTime.parse(clean).atZoneSameInstant(IST_ZONE);
                } catch (Exception ex) {
                    try {
                        return java.time.LocalDateTime.parse(clean).atZone(IST_ZONE);
                    } catch (Exception exc) {
                        int tIdx = clean.indexOf('T');
                        if (clean.length() >= tIdx + 6) {
                            clean = clean.substring(tIdx + 1, tIdx + 6);
                        }
                    }
                }
            }
        }
        if (clean.length() > 5) {
            clean = clean.substring(0, 5);
        }
        try {
            LocalTime lt = LocalTime.parse(clean);
            LocalDate baseDate = refStart.toLocalDate();
            ZonedDateTime dtToday = ZonedDateTime.of(baseDate, lt, IST_ZONE);
            long diffToday = Duration.between(refStart, dtToday).toMinutes();
            if (diffToday < -180) {
                ZonedDateTime dtTomorrow = dtToday.plusDays(1);
                long diffTomorrow = Duration.between(refStart, dtTomorrow).toMinutes();
                if (Math.abs(diffTomorrow) < Math.abs(diffToday)) {
                    return dtTomorrow;
                }
            } else if (diffToday > 720) {
                ZonedDateTime dtYesterday = dtToday.minusDays(1);
                long diffYesterday = Duration.between(refStart, dtYesterday).toMinutes();
                if (Math.abs(diffYesterday) < Math.abs(diffToday)) {
                    return dtYesterday;
                }
            }
            return dtToday;
        } catch (Exception e) {
            return refStart.plusYears(1);
        }
    }

    private String formatDelayedIso(LocalTime sch, int delayMinutes, ZonedDateTime refStart) {
        if (sch == null) return null;
        LocalDate baseDate = refStart.toLocalDate();
        ZonedDateTime dt = ZonedDateTime.of(baseDate, sch, IST_ZONE).plusMinutes(delayMinutes);
        return dt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private int getMinutesFromZoned(LocalTime arr, LocalTime dep, ZonedDateTime refStart) {
        LocalTime t = arr != null ? arr : dep;
        if (t == null) return 1440;
        int diff = (int) Duration.between(refStart.toLocalTime(), t).toMinutes();
        return diff < 0 ? diff + 1440 : diff;
    }

    private int getLiveStatusPriority(com.example.railtracker.dto.StationTrainLiveStatus status) {
        if (status == null) return 6;
        switch (status) {
            case ARRIVED:
            case AT_STATION: return 1;
            case UPCOMING: return 2;
            case SCHEDULED:
            case NOT_STARTED: return 3;
            case DEPARTED: return 4;
            case STATUS_UNAVAILABLE: return 5;
            default: return 6;
        }
    }
    
    private String getSortTime(LiveStationBoardResponse.LiveStationBoardTrain t) {
        String exp = t.expectedArrival() != null ? t.expectedArrival() : t.expectedDeparture();
        if (exp != null && !exp.trim().isEmpty()) {
            return exp;
        }
        String sch = t.scheduledArrival() != null ? t.scheduledArrival() : t.scheduledDeparture();
        return sch;
    }

    private StationBoardTrainDto ensureOriginDestHalts(StationBoardTrainDto t, String stationCode) {
        if (t == null) return null;
        boolean isOriginOrDest = stationCode != null && (
                stationCode.equalsIgnoreCase(t.sourceStation()) ||
                stationCode.equalsIgnoreCase(t.destinationStation())
        );
        if (isOriginOrDest && !t.isHalt()) {
            return new StationBoardTrainDto(
                    t.trainNumber(),
                    t.trainName(),
                    t.trainType(),
                    t.runningDays(),
                    t.sourceStation(),
                    t.destinationStation(),
                    t.scheduledArrival(),
                    t.scheduledDeparture(),
                    t.actualArrival(),
                    t.actualDeparture(),
                    t.delayMinutes(),
                    "STOPPING",
                    true,
                    t.currentStatus(),
                    t.arrivalDay()
            );
        }
        return t;
    }

    private List<StationBoardTrainDto> enrichArrivalDays(List<StationBoardTrainDto> trains, String stationCode) {
        if (trains == null) return List.of();
        try {
            List<TrainStation> savedStations = trainStationRepository.findByStationCodeWithDetails(stationCode);
            Map<String, Integer> arrivalDayMap = (savedStations != null && !savedStations.isEmpty())
                    ? savedStations.stream().collect(Collectors.toMap(
                            ts -> ts.getTrain().getTrainNumber(),
                            ts -> ts.getArrivalDay(),
                            (existing, replacement) -> existing
                    ))
                    : Map.of();

            return trains.stream()
                    .map(t -> {
                        Integer dbArrivalDay = arrivalDayMap.get(t.trainNumber());
                        Integer finalArrivalDay = (dbArrivalDay != null) ? dbArrivalDay : t.arrivalDay();
                        return new StationBoardTrainDto(
                                t.trainNumber(),
                                t.trainName(),
                                t.trainType(),
                                t.runningDays(),
                                resolveStationName(t.sourceStation()),
                                resolveStationName(t.destinationStation()),
                                t.scheduledArrival(),
                                t.scheduledDeparture(),
                                t.actualArrival(),
                                t.actualDeparture(),
                                t.delayMinutes(),
                                t.stopType(),
                                t.isHalt(),
                                t.currentStatus(),
                                finalArrivalDay
                        );
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.warn("Failed to enrich arrival days from database for station: {}", stationCode, e);
            return trains.stream()
                    .map(t -> new StationBoardTrainDto(
                            t.trainNumber(),
                            t.trainName(),
                            t.trainType(),
                            t.runningDays(),
                            resolveStationName(t.sourceStation()),
                            resolveStationName(t.destinationStation()),
                            t.scheduledArrival(),
                            t.scheduledDeparture(),
                            t.actualArrival(),
                            t.actualDeparture(),
                            t.delayMinutes(),
                            t.stopType(),
                            t.isHalt(),
                            t.currentStatus(),
                            t.arrivalDay()
                    ))
                    .collect(Collectors.toList());
        }
    }

    private void saveTrainIfMissing(StationBoardTrainDto t) {
        if (t == null || t.trainNumber() == null || t.trainNumber().isBlank()) return;
        try {
            String num = t.trainNumber().trim();
            var existingOpt = trainRepository.findByTrainNumber(num);
            if (existingOpt.isEmpty()) {
                String name = t.trainName() != null ? t.trainName().trim() : "Express";
                String type = t.trainType() != null ? t.trainType().trim() : "Express";
                String cat = determineCategory(name, type);
                Train train = new Train(
                        num,
                        name,
                        type,
                        cat,
                        t.sourceStation() != null ? t.sourceStation().trim() : "",
                        t.destinationStation() != null ? t.destinationStation().trim() : "",
                        null,
                        null,
                        t.runningDays() != null ? t.runningDays() : "Daily"
                );
                trainRepository.save(train);
                logger.info("Auto-populated new train {} ({}) from station board into database", num, name);
            } else {
                Train train = existingOpt.get();
                boolean changed = false;

                if (t.trainName() != null && !t.trainName().isBlank()
                        && !t.trainName().trim().equalsIgnoreCase(train.getTrainName())) {
                    logger.info("Auto-updating train name for {} from station board: '{}' -> '{}'",
                            num, train.getTrainName(), t.trainName().trim());
                    train.setTrainName(t.trainName().trim());
                    changed = true;
                }
                if (t.trainType() != null && !t.trainType().isBlank()
                        && !t.trainType().trim().equalsIgnoreCase(train.getTrainType())) {
                    train.setTrainType(t.trainType().trim());
                    train.setCategory(determineCategory(train.getTrainName(), train.getTrainType()));
                    changed = true;
                }

                if (t.sourceStation() != null && !t.sourceStation().isBlank()
                        && !t.sourceStation().trim().equalsIgnoreCase(train.getSourceStation())) {
                    logger.info("Auto-updating source station for train {} from station board: '{}' -> '{}'",
                            num, train.getSourceStation(), t.sourceStation().trim());
                    train.setSourceStation(t.sourceStation().trim());
                    changed = true;
                }
                if (t.destinationStation() != null && !t.destinationStation().isBlank()
                        && !t.destinationStation().trim().equalsIgnoreCase(train.getDestinationStation())) {
                    logger.info("Auto-updating destination station for train {} from station board: '{}' -> '{}'",
                            num, train.getDestinationStation(), t.destinationStation().trim());
                    train.setDestinationStation(t.destinationStation().trim());
                    changed = true;
                }
                if (changed) {
                    trainRepository.save(train);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to auto-populate train {}: {}", t.trainNumber(), e.getMessage());
        }
    }

    private String resolveStationName(String codeOrName) {
        if (codeOrName == null || codeOrName.isBlank()) {
            return codeOrName;
        }
        String clean = codeOrName.trim();
        if (clean.matches("^[A-Za-z0-9]{1,6}$")) {
            return stationRepository.findByStationCode(clean.toUpperCase())
                    .map(s -> formatStationDisplayName(s.getStationName(), s.getStationCode()))
                    .orElse(clean);
        }
        return clean;
    }

    private String formatStationDisplayName(String rawName, String code) {
        if (rawName == null || rawName.isBlank()) return code;
        String titleCased = toTitleCase(rawName.trim());
        if (titleCased.toUpperCase().endsWith("(" + code.toUpperCase() + ")")) {
            return titleCased;
        }
        return titleCased + " (" + code.toUpperCase() + ")";
    }

    private String toTitleCase(String text) {
        if (text == null || text.isBlank()) return text;
        String[] words = text.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (word.isEmpty()) continue;
            if (word.equalsIgnoreCase("JN") || word.equalsIgnoreCase("JN.")) {
                sb.append("Jn");
            } else if (word.equalsIgnoreCase("CANTT") || word.equalsIgnoreCase("CANTT.")) {
                sb.append("Cantt");
            } else if (word.equalsIgnoreCase("SF") || word.equalsIgnoreCase("EXP")) {
                sb.append(word.toUpperCase());
            } else {
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    sb.append(word.substring(1).toLowerCase());
                }
            }
            if (i < words.length - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    private void saveTrainIfMissing(LiveStationBoardResponse.LiveStationBoardTrain t) {
        if (t == null || t.trainNumber() == null || t.trainNumber().isBlank()) return;
        try {
            String num = t.trainNumber().trim();
            String src = t.source() != null && t.source().name() != null ? t.source().name().trim() : (t.source() != null ? t.source().code() : "");
            String dest = t.destination() != null && t.destination().name() != null ? t.destination().name().trim() : (t.destination() != null ? t.destination().code() : "");
            var existingOpt = trainRepository.findByTrainNumber(num);
            if (existingOpt.isEmpty()) {
                String name = t.trainName() != null ? t.trainName().trim() : "Express";
                String type = t.trainType() != null ? t.trainType().trim() : "Express";
                String cat = determineCategory(name, type);
                Train train = new Train(
                        num,
                        name,
                        type,
                        cat,
                        src,
                        dest,
                        null,
                        null,
                        t.runningDays() != null ? t.runningDays() : "Daily"
                );
                trainRepository.save(train);
                logger.info("Auto-populated new train {} ({}) from live station board into database", num, name);
            } else {
                Train train = existingOpt.get();
                boolean changed = false;
                if (!src.isBlank() && !src.equalsIgnoreCase(train.getSourceStation())) {
                    logger.info("Auto-updating source station for train {} from live board: '{}' -> '{}'",
                            num, train.getSourceStation(), src);
                    train.setSourceStation(src);
                    changed = true;
                }
                if (!dest.isBlank() && !dest.equalsIgnoreCase(train.getDestinationStation())) {
                    logger.info("Auto-updating destination station for train {} from live board: '{}' -> '{}'",
                            num, train.getDestinationStation(), dest);
                    train.setDestinationStation(dest);
                    changed = true;
                }
                if (changed) {
                    trainRepository.save(train);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to auto-populate train {}: {}", t.trainNumber(), e.getMessage());
        }
    }

    private String determineCategory(String trainName, String trainType) {
        String text = ((trainName != null ? trainName : "") + " " + (trainType != null ? trainType : "")).toUpperCase();
        if (text.contains("VANDE BHARAT")) return "VANDE_BHARAT";
        if (text.contains("RAJDHANI")) return "RAJDHANI";
        if (text.contains("SHATABDI")) return "SHATABDI";
        if (text.contains("DURONTO")) return "DURONTO";
        if (text.contains("GARIB RATH")) return "GARIB_RATH";
        if (text.contains("HUMSAFAR")) return "HUMSAFAR";
        if (text.contains("TEJAS")) return "TEJAS";
        if (text.contains("SUPERFAST") || text.contains("SF")) return "SUPERFAST";
        if (text.contains("PASSENGER")) return "PASSENGER";
        if (text.contains("MEMU") || text.contains("DEMU") || text.contains("EMU")) return "SUBURBAN";
        if (text.contains("MAIL")) return "MAIL";
        return "EXPRESS";
    }

    private StationDto mapToDto(Station station) {
        return new StationDto(
                station.getStationCode(),
                station.getStationName(),
                station.getCity() != null ? station.getCity() : station.getDistrict(),
                station.getState(),
                station.getLatitude(),
                station.getLongitude(),
                station.getActive(),
                station.getSerialNumber(),
                station.getOldCategory(),
                station.getNewCategory(),
                station.getDivision(),
                station.getZone(),
                station.getDistrict()
        );
    }
}


