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
import java.util.HashMap;
import java.util.stream.Collectors;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.LocalDate;
import java.time.Duration;
import java.time.DayOfWeek;

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

    @Autowired(required = false)
    private org.springframework.cache.CacheManager cacheManager;

    public void setCacheManager(org.springframework.cache.CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

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
        return getStationTrains(stationCode, type, false);
    }

    @Override
    public List<StationBoardTrainDto> getStationTrains(String stationCode, String type, boolean forceRefresh) {
        logger.debug("Retrieving trains for station: {} with filter: {} (forceRefresh={})", stationCode, type, forceRefresh);
        String cacheKey = (stationCode != null ? stationCode.trim().toUpperCase() : "") + ":" + (type != null ? type.trim().toUpperCase() : "ALL");

        if (!forceRefresh && cacheManager != null) {
            try {
                var cache = cacheManager.getCache("station:trains");
                if (cache != null) {
                    var wrapper = cache.get(cacheKey);
                    if (wrapper != null && wrapper.get() instanceof List<?> cachedList) {
                        logger.debug("Cache hit for station:trains key: {}", cacheKey);
                        @SuppressWarnings("unchecked")
                        List<StationBoardTrainDto> typed = (List<StationBoardTrainDto>) cachedList;
                        return typed;
                    }
                }
            } catch (Exception e) {
                logger.warn("Cache read error for station:trains: {}", e.getMessage());
            }
        }

        List<StationBoardTrainDto> allTrains = null;
        try {
            try {
                getStationDetails(stationCode);
            } catch (ResourceNotFoundException e) {
                logger.warn("Station {} is not available in lookup data. Continuing with empty/remote board lookup.", stationCode, e);
            }

            allTrains = railwayDataProvider.getStationBoard(stationCode, true);
            if (allTrains != null) {
                allTrains = allTrains.stream()
                        .map(t -> ensureOriginDestHalts(t, stationCode))
                        .collect(Collectors.toList());
                saveStationTrainsIfMissing(allTrains);
            }
            allTrains = enrichArrivalDays(allTrains, stationCode);

            List<StationBoardTrainDto> result;
            if (type == null || type.equalsIgnoreCase("ALL")) {
                result = allTrains;
            } else if (type.equalsIgnoreCase("STOPPING")) {
                result = allTrains.stream()
                        .filter(StationBoardTrainDto::isHalt)
                        .collect(Collectors.toList());
            } else if (type.equalsIgnoreCase("NON_STOP")) {
                result = allTrains.stream()
                        .filter(t -> !t.isHalt())
                        .collect(Collectors.toList());
            } else {
                result = allTrains;
            }

            if (cacheManager != null && result != null && !result.isEmpty()) {
                try {
                    var cache = cacheManager.getCache("station:trains");
                    if (cache != null) {
                        cache.put(cacheKey, result);
                    }
                } catch (Exception e) {
                    logger.warn("Cache write error for station:trains: {}", e.getMessage());
                }
            }
            return result;
        } catch (Exception e) {
            if (forceRefresh && cacheManager != null) {
                try {
                    var cache = cacheManager.getCache("station:trains");
                    if (cache != null) {
                        var wrapper = cache.get(cacheKey);
                        if (wrapper != null && wrapper.get() instanceof List<?> cachedList) {
                            logger.warn("Preserving existing cached station:trains on error for key {}: {}", cacheKey, e.getMessage());
                            @SuppressWarnings("unchecked")
                            List<StationBoardTrainDto> typed = (List<StationBoardTrainDto>) cachedList;
                            return typed;
                        }
                    }
                } catch (Exception ignored) {}
            }
            throw e;
        }
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
    public LiveStationBoardResponse getLiveStationBoardDetails(String stationCode, int hours) {
        return getLiveStationBoardDetails(stationCode, hours, false);
    }

    @Override
    public LiveStationBoardResponse getLiveStationBoardDetails(String stationCode, int hours, boolean forceRefresh) {
        logger.debug("Retrieving live board details for station: {} for next {} hours (forceRefresh={})", stationCode, hours, forceRefresh);
        String cacheKey = (stationCode != null ? stationCode.trim().toUpperCase() : "") + ":" + hours;

        if (!forceRefresh && cacheManager != null) {
            try {
                var cache = cacheManager.getCache("live-station");
                if (cache != null) {
                    var wrapper = cache.get(cacheKey);
                    if (wrapper != null && wrapper.get() instanceof LiveStationBoardResponse cachedRes) {
                        logger.debug("Cache hit for live-station key: {}", cacheKey);
                        return cachedRes;
                    }
                }
            } catch (Exception e) {
                logger.warn("Cache read error for live-station: {}", e.getMessage());
            }
        }

        StationDto st = null;
        try {
            st = getStationDetails(stationCode);
        } catch (ResourceNotFoundException e) {
            logger.warn("Station {} is not available in lookup data. Continuing.", stationCode);
        }
        
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
        ZonedDateTime nowIst = ZonedDateTime.now(IST_ZONE);
        ZonedDateTime windowStart = nowIst;
        ZonedDateTime windowEnd = nowIst.plusHours(hours);

        String fromWindow = windowStart.format(timeFmt);
        String toWindow = windowEnd.format(timeFmt);
        LiveStationBoardResponse.WindowInfo windowInfo = new LiveStationBoardResponse.WindowInfo(
                fromWindow,
                toWindow,
                hours,
                windowStart.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                windowEnd.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        );

        LiveStationBoardResponse rawResponse = null;
        try {
            rawResponse = railwayDataProvider.getLiveStationBoardDetails(stationCode, hours);
        } catch (Exception e) {
            logger.debug("Live station endpoint /stations/{}/live not accessible on current tier: {}", stationCode, e.getMessage());
        }

        List<LiveStationBoardResponse.LiveStationBoardTrain> candidateTrains = new ArrayList<>();
        Set<String> presentTrainKeys = new HashSet<>();

        if (rawResponse != null && rawResponse.trains() != null && !rawResponse.trains().isEmpty()) {
            for (LiveStationBoardResponse.LiveStationBoardTrain t : rawResponse.trains()) {
                candidateTrains.add(t);
                if (t.trainNumber() != null) {
                    String normNum = t.trainNumber().trim().toUpperCase();
                    String jDate = t.journeyDate() != null && !t.journeyDate().isBlank() ? t.journeyDate().trim() : windowStart.toLocalDate().toString();
                    String passageTime = t.scheduledArrival() != null && !t.scheduledArrival().isBlank() ? t.scheduledArrival().trim() : (t.scheduledDeparture() != null ? t.scheduledDeparture().trim() : "");
                    presentTrainKeys.add(normNum + "_" + jDate + "_" + passageTime);
                    presentTrainKeys.add(normNum + "_" + jDate);
                }
            }
        }

        List<StationBoardTrainDto> scheduledTrains = null;
        try {
            scheduledTrains = getStationTrains(stationCode, "ALL", false);
        } catch (Exception e) {
            logger.debug("Scheduled timetable lookup for station {} failed: {}", stationCode, e.getMessage());
        }

        Map<String, Integer> arrivalDayMap = new HashMap<>();
        Map<String, Integer> departureDayMap = new HashMap<>();
        if (scheduledTrains != null) {
            for (StationBoardTrainDto sbt : scheduledTrains) {
                if (sbt.trainNumber() != null) {
                    String tn = sbt.trainNumber().trim().toUpperCase();
                    if (sbt.arrivalDay() != null) {
                        arrivalDayMap.put(tn, sbt.arrivalDay());
                    }
                    if (sbt.departureDay() != null) {
                        departureDayMap.put(tn, sbt.departureDay());
                    }
                }
            }
        }

        // Merge valid scheduled timetable trains into candidateTrains
        if (scheduledTrains != null && !scheduledTrains.isEmpty()) {
            final ZonedDateTime refStart = windowStart;
            final ZonedDateTime refEnd = windowEnd;
            LocalDate baseDate = refStart.toLocalDate();

            for (StationBoardTrainDto sbt : scheduledTrains) {
                if (sbt == null || sbt.trainNumber() == null) continue;

                // Independent passage time and day offset resolution:
                // Arrival uses arrivalDay (fallback to departureDay)
                // Departure uses departureDay (fallback to arrivalDay)
                // Non-stopping passage uses station passage day
                LocalTime schedTime = null;
                Integer passageDay = null;

                if (sbt.scheduledArrival() != null) {
                    schedTime = sbt.scheduledArrival();
                    passageDay = sbt.arrivalDay() != null && sbt.arrivalDay() > 0 ? sbt.arrivalDay() : sbt.departureDay();
                } else if (sbt.scheduledDeparture() != null) {
                    schedTime = sbt.scheduledDeparture();
                    passageDay = sbt.departureDay() != null && sbt.departureDay() > 0 ? sbt.departureDay() : sbt.arrivalDay();
                }
                if (schedTime == null) continue;

                // Candidate passage occurrences: today or tomorrow relative to windowStart
                ZonedDateTime dtToday = ZonedDateTime.of(baseDate, schedTime, IST_ZONE);
                ZonedDateTime dtTomorrow = ZonedDateTime.of(baseDate.plusDays(1), schedTime, IST_ZONE);

                // Check interval [refStart - 3h, refEnd] to allow delayed trains entering the window
                LocalDate stationDate = null;
                ZonedDateTime passageZoned = null;

                if (!dtToday.isBefore(refStart.minusHours(3)) && !dtToday.isAfter(refEnd)) {
                    stationDate = baseDate;
                    passageZoned = dtToday;
                } else if (!dtTomorrow.isBefore(refStart.minusHours(3)) && !dtTomorrow.isAfter(refEnd)) {
                    stationDate = baseDate.plusDays(1);
                    passageZoned = dtTomorrow;
                }

                if (stationDate == null || passageZoned == null) {
                    continue; // Excluded by time window
                }

                // Multi-day journey offset resolved independently
                int dayOffset = (passageDay != null && passageDay > 1) ? passageDay - 1 : 0;
                LocalDate journeyStartDate = stationDate.minusDays(dayOffset);

                // CRITICAL OPERATING DAY VALIDATION:
                // Operating days correspond to journey-start date
                if (!isOperatingOnDate(sbt.runningDays(), journeyStartDate)) {
                    continue; // Excluded by operating day validation
                }

                String normNum = sbt.trainNumber().trim().toUpperCase();
                String expectedJourneyDate = journeyStartDate.toString();
                String passageTimeStr = schedTime.format(timeFmt);
                String compKey = normNum + "_" + expectedJourneyDate + "_" + passageTimeStr;
                String jKey = normNum + "_" + expectedJourneyDate;

                if (presentTrainKeys.contains(compKey) || presentTrainKeys.contains(jKey)) {
                    continue; // Deduplicate live/scheduled duplicates
                }

                // Fast cache-only check for live delay without external API calls
                TrainLiveStatusDto cachedLive = getCachedTrainLiveStatus(normNum, expectedJourneyDate);
                Integer delay = null;
                String actArr = null;
                String actDep = null;
                String platform = sbt.platform();
                StationTrainLiveStatus status = StationTrainLiveStatus.SCHEDULED;

                if (cachedLive != null) {
                    TrainLiveStatusDto.StationStatusDto matchedStop = null;
                    if (cachedLive.stations() != null) {
                        for (TrainLiveStatusDto.StationStatusDto stop : cachedLive.stations()) {
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
                        } else if (cachedLive.delayMinutes() != null) {
                            delay = cachedLive.delayMinutes();
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
                        } else if (Boolean.TRUE.equals(cachedLive.isCancelled()) || (cachedLive.skippedStations() != null && cachedLive.skippedStations().contains(stationCode.toUpperCase()))) {
                            status = StationTrainLiveStatus.STATUS_UNAVAILABLE;
                        } else if (cachedLive.currentStationCode() != null && !cachedLive.currentStationCode().isBlank()) {
                            status = StationTrainLiveStatus.UPCOMING;
                        }
                    }
                }

                // Check expected passage time against window [refStart - 2m, refEnd]
                ZonedDateTime expPassage = (delay != null && delay > 0) ? passageZoned.plusMinutes(delay) : passageZoned;
                if (expPassage.isBefore(refStart.minusMinutes(2)) || expPassage.isAfter(refEnd)) {
                    continue; // Excluded: passage outside window
                }

                presentTrainKeys.add(compKey);
                presentTrainKeys.add(jKey);

                String trainNumber = sbt.trainNumber();
                String trainName = sbt.trainName();
                String trainType = sbt.trainType();
                String srcCode = sbt.sourceStation() != null ? sbt.sourceStation() : stationCode;
                String destCode = sbt.destinationStation() != null ? sbt.destinationStation() : stationCode;
                StationPassType passType = Boolean.TRUE.equals(sbt.isHalt()) ? StationPassType.STOPPING : StationPassType.PASS_THROUGH;

                String schArr = sbt.scheduledArrival() != null ? sbt.scheduledArrival().format(timeFmt) : null;
                String schDep = sbt.scheduledDeparture() != null ? sbt.scheduledDeparture().format(timeFmt) : null;

                String expArr = null;
                if (sbt.scheduledArrival() != null) {
                    ZonedDateTime arrDt = ZonedDateTime.of(stationDate, sbt.scheduledArrival(), IST_ZONE);
                    if (delay != null && delay > 0) {
                        arrDt = arrDt.plusMinutes(delay);
                    }
                    expArr = arrDt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                }

                String expDep = null;
                if (sbt.scheduledDeparture() != null) {
                    ZonedDateTime depDt = ZonedDateTime.of(stationDate, sbt.scheduledDeparture(), IST_ZONE);
                    if (delay != null && delay > 0) {
                        depDt = depDt.plusMinutes(delay);
                    }
                    expDep = depDt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                }

                if (actArr != null && !actArr.isBlank()) {
                    expArr = actArr;
                }
                if (actDep != null && !actDep.isBlank() && status == StationTrainLiveStatus.DEPARTED) {
                    expDep = actDep;
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
                        delay, // Preserve null for scheduled-only trains without confirmed live delay
                        platform,
                        status,
                        sbt.runningDays(),
                        actArr,
                        actDep,
                        expectedJourneyDate
                ));
            }
        }

        saveLiveTrainsIfMissing(candidateTrains);
        
        final ZonedDateTime finalWindowStart = windowStart;
        final ZonedDateTime finalWindowEnd = windowEnd;

        // Batch station name lookup in ONE query
        Set<String> codesToFetch = new HashSet<>();
        for (LiveStationBoardResponse.LiveStationBoardTrain t : candidateTrains) {
            if (t.source() != null && t.source().code() != null) codesToFetch.add(t.source().code().trim().toUpperCase());
            if (t.destination() != null && t.destination().code() != null) codesToFetch.add(t.destination().code().trim().toUpperCase());
        }
        Map<String, String> stationNameMap = codesToFetch.isEmpty() ? Map.of() :
                stationRepository.findByStationCodeIn(codesToFetch).stream()
                        .collect(Collectors.toMap(
                                s -> s.getStationCode().toUpperCase(),
                                com.example.railtracker.entity.Station::getStationName,
                                (existing, rep) -> existing
                        ));
        
        List<LiveStationBoardResponse.LiveStationBoardTrain> enrichedTrains = candidateTrains.stream()
                .map(t -> {
                    String srcCode = t.source().code();
                    String srcName = stationNameMap.getOrDefault(srcCode != null ? srcCode.toUpperCase() : "", srcCode);
                            
                    String destCode = t.destination().code();
                    String destName = stationNameMap.getOrDefault(destCode != null ? destCode.toUpperCase() : "", destCode);

                    boolean isOriginOrDest = stationCode.equalsIgnoreCase(srcCode) || stationCode.equalsIgnoreCase(destCode);
                    StationPassType resolvedPassType = isOriginOrDest ? StationPassType.STOPPING : t.passType();

                    StationTrainLiveStatus resolvedStatus = t.liveStatus() != null ? t.liveStatus() : StationTrainLiveStatus.SCHEDULED;
                    String actArr = t.actualArrival();
                    String actDep = t.actualDeparture();
                    String expArr = t.expectedArrival();
                    String expDep = t.expectedDeparture();
                    Integer delay = t.delayMinutes();
                    String platform = t.platform();
                    String effectiveJourneyDate = t.journeyDate();

                    // Check arrival/departure day maps if journeyDate needs to be corrected for multi-rake trains
                    if (effectiveJourneyDate == null || effectiveJourneyDate.isBlank()) {
                        LocalDate stDate = finalWindowStart.toLocalDate();
                        LocalTime pTime = parseLocalTime(t.expectedArrival() != null ? t.expectedArrival() : (t.expectedDeparture() != null ? t.expectedDeparture() : t.scheduledArrival()));
                        if (pTime != null) {
                            ZonedDateTime pToday = ZonedDateTime.of(stDate, pTime, IST_ZONE);
                            if (pToday.isBefore(finalWindowStart.minusMinutes(30))) {
                                stDate = stDate.plusDays(1);
                            }
                        }
                        int offset = 0;
                        if (t.trainNumber() != null) {
                            String tn = t.trainNumber().trim().toUpperCase();
                            Integer dbDay = (t.scheduledArrival() != null)
                                    ? (arrivalDayMap.get(tn) != null ? arrivalDayMap.get(tn) : departureDayMap.get(tn))
                                    : (departureDayMap.get(tn) != null ? departureDayMap.get(tn) : arrivalDayMap.get(tn));
                            if (dbDay != null && dbDay > 1) {
                                offset = dbDay - 1;
                            }
                        }
                        effectiveJourneyDate = stDate.minusDays(offset).toString();
                    }

                    // Precedence 1: Confirmed departure telemetry -> DEPARTED
                    if (actDep != null && !actDep.isBlank()) {
                        resolvedStatus = StationTrainLiveStatus.DEPARTED;
                    } 
                    // Precedence 2: Confirmed arrival telemetry without confirmed departure -> ARRIVED / AT_STATION
                    else if (actArr != null && !actArr.isBlank()) {
                        resolvedStatus = (resolvedStatus == StationTrainLiveStatus.ARRIVED)
                                ? StationTrainLiveStatus.ARRIVED
                                : StationTrainLiveStatus.AT_STATION;
                        actDep = null;
                    } 
                    // Precedence 3: Reliable station-specific live telemetry supporting arrival -> AT_STATION / ARRIVED
                    else if (resolvedStatus == StationTrainLiveStatus.AT_STATION || resolvedStatus == StationTrainLiveStatus.ARRIVED) {
                        actDep = null;
                    } 
                    // Precedence 4 & 5: When status is UPCOMING or SCHEDULED
                    else if (resolvedStatus == StationTrainLiveStatus.UPCOMING || resolvedStatus == StationTrainLiveStatus.SCHEDULED) {
                        actDep = null;
                        // Fast, zero-API cache-only check for live train tracking telemetry
                        TrainLiveStatusDto cachedLive = getCachedTrainLiveStatus(t.trainNumber(), effectiveJourneyDate);
                        if (cachedLive != null) {
                            boolean atStationTelemetry = false;
                            TrainLiveStatusDto.StationStatusDto matchedStop = null;
                            if (cachedLive.stations() != null) {
                                for (TrainLiveStatusDto.StationStatusDto stop : cachedLive.stations()) {
                                    if (stop.stationCode() != null && stop.stationCode().equalsIgnoreCase(stationCode)) {
                                        matchedStop = stop;
                                        break;
                                    }
                                }
                            }
                            if (matchedStop != null) {
                                String sStatus = matchedStop.status() != null ? matchedStop.status().toLowerCase() : "";
                                boolean isAtStationStop = sStatus.contains("at-station") || sStatus.contains("at_station") || sStatus.contains("arriv");

                                if (isAtStationStop) {
                                    actDep = null;
                                    atStationTelemetry = true;
                                    if (matchedStop.actualArrival() != null && !matchedStop.actualArrival().isBlank()) {
                                        actArr = matchedStop.actualArrival();
                                    }
                                } else if ((matchedStop.actualDeparture() != null && !matchedStop.actualDeparture().isBlank()) || sStatus.contains("depart")) {
                                    actDep = matchedStop.actualDeparture();
                                    resolvedStatus = StationTrainLiveStatus.DEPARTED;
                                } else if (matchedStop.actualArrival() != null && !matchedStop.actualArrival().isBlank()) {
                                    actArr = matchedStop.actualArrival();
                                    atStationTelemetry = true;
                                }
                                if (matchedStop.delayMinutes() != null) {
                                    delay = matchedStop.delayMinutes();
                                } else if (cachedLive.delayMinutes() != null) {
                                    delay = cachedLive.delayMinutes();
                                }
                                if (matchedStop.expectedArrival() != null && !matchedStop.expectedArrival().isBlank()) {
                                    expArr = matchedStop.expectedArrival();
                                }
                                if (matchedStop.expectedDeparture() != null && !matchedStop.expectedDeparture().isBlank()) {
                                    expDep = matchedStop.expectedDeparture();
                                }
                                if (matchedStop.platform() != null && !matchedStop.platform().isBlank()) {
                                    platform = matchedStop.platform();
                                }
                            } else {
                                if (cachedLive.currentStationCode() != null && cachedLive.currentStationCode().equalsIgnoreCase(stationCode)) {
                                    atStationTelemetry = true;
                                }
                                if (cachedLive.delayMinutes() != null) {
                                    delay = cachedLive.delayMinutes();
                                }
                            }
                            if (atStationTelemetry && resolvedStatus != StationTrainLiveStatus.DEPARTED) {
                                resolvedStatus = (resolvedStatus == StationTrainLiveStatus.ARRIVED)
                                        ? StationTrainLiveStatus.ARRIVED
                                        : StationTrainLiveStatus.AT_STATION;
                            }
                        }
                    } else if (resolvedStatus == StationTrainLiveStatus.NOT_STARTED) {
                        actDep = null;
                    }

                    LocalTime sArrTime = parseLocalTime(t.scheduledArrival());
                    LocalTime eArrTime = parseLocalTime(expArr);
                    if (expArr == null || expArr.isBlank() || expArr.equalsIgnoreCase(t.scheduledArrival()) || (sArrTime != null && sArrTime.equals(eArrTime))) {
                        if (actArr != null && !actArr.isBlank()) {
                            expArr = actArr;
                        } else if (delay != null && delay > 0 && sArrTime != null) {
                            expArr = formatDelayedIso(sArrTime, delay, finalWindowStart);
                        }
                    }
                    LocalTime sDepTime = parseLocalTime(t.scheduledDeparture());
                    LocalTime eDepTime = parseLocalTime(expDep);
                    if (expDep == null || expDep.isBlank() || expDep.equalsIgnoreCase(t.scheduledDeparture()) || (sDepTime != null && sDepTime.equals(eDepTime))) {
                        if (actDep != null && !actDep.isBlank() && resolvedStatus == StationTrainLiveStatus.DEPARTED) {
                            expDep = actDep;
                        } else if (delay != null && delay > 0 && sDepTime != null) {
                            expDep = formatDelayedIso(sDepTime, delay, finalWindowStart);
                        }
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
                            expArr,
                            expDep,
                            delay,
                            platform,
                            resolvedStatus,
                            t.runningDays(),
                            actArr,
                            actDep,
                            effectiveJourneyDate
                    );
                })
                .filter(t -> shouldIncludeInLiveBoard(t, finalWindowStart, finalWindowEnd))
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
                st != null && st.latitude() != null ? st.latitude() : 0.0,
                st != null && st.longitude() != null ? st.longitude() : 0.0
        );
        
        LiveStationBoardResponse result = new LiveStationBoardResponse(resolvedStation, windowInfo, enrichedTrains);
        if (cacheManager != null && result != null) {
            try {
                var cache = cacheManager.getCache("live-station");
                if (cache != null) {
                    cache.put(cacheKey, result);
                }
            } catch (Exception e) {
                logger.warn("Cache write error for live-station: {}", e.getMessage());
            }
        }
        return result;
    }
    
    private TrainLiveStatusDto getCachedTrainLiveStatus(String trainNumber, String journeyDate) {
        if (cacheManager == null || trainNumber == null || trainNumber.isBlank()) {
            return null;
        }
        try {
            var cache = cacheManager.getCache("train:live");
            if (cache == null) return null;
            String key = trainNumber.trim() + "_" + (journeyDate != null && !journeyDate.isBlank() ? journeyDate.trim() : "current");
            var wrapper = cache.get(key);
            if (wrapper != null && wrapper.get() instanceof TrainLiveStatusDto cachedDto) {
                if (isLiveCacheValid(cachedDto, journeyDate)) {
                    return cachedDto;
                }
            }
            if (journeyDate != null && !journeyDate.isBlank()) {
                var currentWrapper = cache.get(trainNumber.trim() + "_current");
                if (currentWrapper != null && currentWrapper.get() instanceof TrainLiveStatusDto currDto) {
                    if (isLiveCacheValid(currDto, journeyDate)) {
                        return currDto;
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Cache lookup for train:live {}_{} failed: {}", trainNumber, journeyDate, e.getMessage());
        }
        return null;
    }

    private boolean isLiveCacheValid(TrainLiveStatusDto dto, String requestedJourneyDate) {
        if (dto == null) return false;
        if (requestedJourneyDate != null && !requestedJourneyDate.isBlank()) {
            if (dto.startDate() != null && !dto.startDate().isBlank()) {
                String req = requestedJourneyDate.replaceAll("[^0-9]", "");
                String resp = dto.startDate().replaceAll("[^0-9]", "");
                if (!req.isEmpty() && !resp.isEmpty() && !req.equals(resp)) {
                    return false;
                }
            }
        }
        // Telemetry freshness validation: cached live telemetry older than 15 minutes is considered stale
        if (dto.lastUpdatedAt() != null) {
            long minutesOld = java.time.Duration.between(dto.lastUpdatedAt(), java.time.LocalDateTime.now()).toMinutes();
            if (minutesOld > 15 || minutesOld < -5) {
                return false;
            }
        }
        return true;
    }

    public boolean isOperatingOnDate(String runningDays, LocalDate journeyStartDate) {
        if (journeyStartDate == null) {
            return true;
        }
        if (runningDays == null || runningDays.isBlank()) {
            return true;
        }
        String days = runningDays.trim().toUpperCase();
        if (days.equals("DAILY") || days.equals("ALL") || days.equals("EVERYDAY") || days.contains("DAILY")) {
            return true;
        }

        DayOfWeek dow = journeyStartDate.getDayOfWeek();
        String dowName = dow.name(); // e.g. "SUNDAY", "MONDAY"
        String dowShort3 = dowName.substring(0, 3); // "SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"
        String dowShort2 = dowName.substring(0, 2); // "SU", "MO", "TU", "WE", "TH", "FR", "SA"

        String[] tokens = days.split("[,\\s/|]+");
        for (String token : tokens) {
            String t = token.trim();
            if (t.isEmpty()) continue;
            if (t.equals("DAILY") || t.equals("ALL")) return true;
            if (t.equals(dowName) || t.equals(dowShort3) || t.equals(dowShort2)) {
                return true;
            }
            // Strict ISO-8601 numeric weekday parsing: 1=Monday, ..., 7=Sunday
            if (t.equals(String.valueOf(dow.getValue()))) {
                return true;
            }
        }

        if (days.contains(dowShort3)) {
            return true;
        }

        return false;
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

    public boolean shouldIncludeInLiveBoard(LiveStationBoardResponse.LiveStationBoardTrain t, ZonedDateTime nowIst, ZonedDateTime windowEnd) {
        if (t == null) return false;
        StationTrainLiveStatus status = t.liveStatus() != null ? t.liveStatus() : StationTrainLiveStatus.SCHEDULED;

        switch (status) {
            case DEPARTED: {
                String actDep = t.actualDeparture();
                if (actDep == null || actDep.isBlank()) {
                    return false;
                }
                ZonedDateTime actualDepTime = parsePassageDateTime(actDep, nowIst);
                long elapsedMinutes = Duration.between(actualDepTime, nowIst).toMinutes();
                // Retain only if actual departure from this station occurred within the last 7 minutes
                // (with -2 minutes tolerance for clock skew)
                return elapsedMinutes >= -2 && elapsedMinutes <= 7;
            }

            case ARRIVED:
            case AT_STATION: {
                // Train is currently halted at station and has not departed
                if (t.actualDeparture() != null && !t.actualDeparture().isBlank()) {
                    ZonedDateTime actualDepTime = parsePassageDateTime(t.actualDeparture(), nowIst);
                    long elapsedMinutes = Duration.between(actualDepTime, nowIst).toMinutes();
                    return elapsedMinutes >= -2 && elapsedMinutes <= 7;
                }
                // Check departure time if train continues beyond this station
                ZonedDateTime depTime = resolveDepartureDateTime(t, nowIst);
                if (depTime != null) {
                    return !depTime.isBefore(nowIst.minusMinutes(30)) && !depTime.isAfter(windowEnd);
                }
                // For terminating trains without departure: check arrival was within recent 30 mins
                ZonedDateTime arrTime = resolveArrivalDateTime(t, nowIst);
                if (arrTime != null) {
                    long elapsed = Duration.between(arrTime, nowIst).toMinutes();
                    return elapsed >= -5 && elapsed <= 30;
                }
                return false;
            }

            case UPCOMING: {
                ZonedDateTime passageTime = resolvePassageDateTime(t, nowIst);
                // 5-minute pre-arrival tolerance buffer for approaching trains
                return !passageTime.isBefore(nowIst.minusMinutes(5)) && !passageTime.isAfter(windowEnd);
            }

            case SCHEDULED:
            case NOT_STARTED:
            case STATUS_UNAVAILABLE:
            case UNKNOWN:
            default: {
                ZonedDateTime passageTime = resolvePassageDateTime(t, nowIst);
                // Future scheduled train or untracked train: must fall in [now - 2m, windowEnd]
                return !passageTime.isBefore(nowIst.minusMinutes(2)) && !passageTime.isAfter(windowEnd);
            }
        }
    }

    ZonedDateTime resolveDepartureDateTime(LiveStationBoardResponse.LiveStationBoardTrain t, ZonedDateTime refStart) {
        if (t == null) return null;
        String timeStr = t.expectedDeparture() != null && !t.expectedDeparture().isBlank() ? t.expectedDeparture() :
                         t.scheduledDeparture() != null && !t.scheduledDeparture().isBlank() ? t.scheduledDeparture() : null;
        if (timeStr == null || timeStr.isBlank()) return null;
        return parsePassageDateTime(timeStr, refStart);
    }

    ZonedDateTime resolveArrivalDateTime(LiveStationBoardResponse.LiveStationBoardTrain t, ZonedDateTime refStart) {
        if (t == null) return null;
        String timeStr = t.actualArrival() != null && !t.actualArrival().isBlank() ? t.actualArrival() :
                         t.expectedArrival() != null && !t.expectedArrival().isBlank() ? t.expectedArrival() :
                         t.scheduledArrival() != null && !t.scheduledArrival().isBlank() ? t.scheduledArrival() : null;
        if (timeStr == null || timeStr.isBlank()) return null;
        return parsePassageDateTime(timeStr, refStart);
    }

    ZonedDateTime parsePassageDateTime(String timeStr, ZonedDateTime refStart) {
        if (timeStr == null || timeStr.isBlank()) {
            return refStart.plusYears(1);
        }
        String clean = timeStr.trim();
        if (clean.contains("T")) {
            try {
                return ZonedDateTime.parse(clean).withZoneSameInstant(IST_ZONE);
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
        LocalDate schDate = sch.isBefore(refStart.toLocalTime()) ? baseDate.plusDays(1) : baseDate;
        ZonedDateTime dt = ZonedDateTime.of(schDate, sch, IST_ZONE).plusMinutes(delayMinutes);
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
                    ? savedStations.stream().filter(ts -> ts.getTrain() != null && ts.getTrain().getTrainNumber() != null && ts.getArrivalDay() != null)
                            .collect(Collectors.toMap(
                                    ts -> ts.getTrain().getTrainNumber().trim().toUpperCase(),
                                    ts -> ts.getArrivalDay(),
                                    (existing, replacement) -> existing
                            ))
                    : Map.of();
            Map<String, Integer> departureDayMap = (savedStations != null && !savedStations.isEmpty())
                    ? savedStations.stream().filter(ts -> ts.getTrain() != null && ts.getTrain().getTrainNumber() != null && ts.getDepartureDay() != null)
                            .collect(Collectors.toMap(
                                    ts -> ts.getTrain().getTrainNumber().trim().toUpperCase(),
                                    ts -> ts.getDepartureDay(),
                                    (existing, replacement) -> existing
                            ))
                    : Map.of();

            return trains.stream()
                    .map(t -> {
                        String tn = t.trainNumber() != null ? t.trainNumber().trim().toUpperCase() : "";
                        Integer dbArrivalDay = arrivalDayMap.get(tn);
                        Integer finalArrivalDay = (dbArrivalDay != null) ? dbArrivalDay : t.arrivalDay();
                        Integer dbDepDay = departureDayMap.get(tn);
                        Integer finalDepDay = (dbDepDay != null) ? dbDepDay : (t.departureDay() != null ? t.departureDay() : finalArrivalDay);
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
                                finalArrivalDay,
                                finalDepDay,
                                t.platform()
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

    private void saveStationTrainsIfMissing(List<StationBoardTrainDto> trains) {
        if (trains == null || trains.isEmpty()) return;
        try {
            Set<String> numbers = trains.stream()
                    .map(StationBoardTrainDto::trainNumber)
                    .filter(n -> n != null && !n.isBlank())
                    .map(String::trim)
                    .collect(Collectors.toSet());
            if (numbers.isEmpty()) return;

            Set<String> existingNumbers = trainRepository.findByTrainNumberIn(numbers).stream()
                    .map(t -> t.getTrainNumber().trim())
                    .collect(Collectors.toSet());

            List<Train> newTrains = new ArrayList<>();
            for (StationBoardTrainDto t : trains) {
                String num = t.trainNumber() != null ? t.trainNumber().trim() : null;
                if (num != null && !existingNumbers.contains(num)) {
                    existingNumbers.add(num);
                    String name = t.trainName() != null ? t.trainName().trim() : "Express";
                    String type = t.trainType() != null ? t.trainType().trim() : "Express";
                    String cat = determineCategory(name, type);
                    newTrains.add(new Train(
                            num,
                            name,
                            type,
                            cat,
                            t.sourceStation() != null ? t.sourceStation().trim() : "",
                            t.destinationStation() != null ? t.destinationStation().trim() : "",
                            null,
                            null,
                            t.runningDays() != null ? t.runningDays() : "Daily"
                    ));
                }
            }
            if (!newTrains.isEmpty()) {
                trainRepository.saveAll(newTrains);
                logger.debug("Bulk saved {} new scheduled trains", newTrains.size());
            }
        } catch (Exception e) {
            logger.debug("Bulk save of scheduled trains skipped: {}", e.getMessage());
        }
    }

    private void saveLiveTrainsIfMissing(List<LiveStationBoardResponse.LiveStationBoardTrain> trains) {
        if (trains == null || trains.isEmpty()) return;
        try {
            Set<String> numbers = trains.stream()
                    .map(LiveStationBoardResponse.LiveStationBoardTrain::trainNumber)
                    .filter(n -> n != null && !n.isBlank())
                    .map(String::trim)
                    .collect(Collectors.toSet());
            if (numbers.isEmpty()) return;

            Set<String> existingNumbers = trainRepository.findByTrainNumberIn(numbers).stream()
                    .map(t -> t.getTrainNumber().trim())
                    .collect(Collectors.toSet());

            List<Train> newTrains = new ArrayList<>();
            for (LiveStationBoardResponse.LiveStationBoardTrain t : trains) {
                String num = t.trainNumber() != null ? t.trainNumber().trim() : null;
                if (num != null && !existingNumbers.contains(num)) {
                    existingNumbers.add(num);
                    String name = t.trainName() != null ? t.trainName().trim() : "Express";
                    String type = t.trainType() != null ? t.trainType().trim() : "Express";
                    String cat = determineCategory(name, type);
                    String src = t.source() != null && t.source().name() != null ? t.source().name().trim() : (t.source() != null ? t.source().code() : "");
                    String dest = t.destination() != null && t.destination().name() != null ? t.destination().name().trim() : (t.destination() != null ? t.destination().code() : "");
                    newTrains.add(new Train(
                            num,
                            name,
                            type,
                            cat,
                            src,
                            dest,
                            null,
                            null,
                            t.runningDays() != null ? t.runningDays() : "Daily"
                    ));
                }
            }
            if (!newTrains.isEmpty()) {
                trainRepository.saveAll(newTrains);
                logger.debug("Bulk saved {} new live board trains", newTrains.size());
            }
        } catch (Exception e) {
            logger.debug("Bulk save of live trains skipped: {}", e.getMessage());
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


