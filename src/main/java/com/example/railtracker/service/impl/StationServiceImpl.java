package com.example.railtracker.service.impl;

import com.example.railtracker.client.RailwayDataProvider;
import com.example.railtracker.dto.LiveStationBoardResponse;
import com.example.railtracker.dto.StationBoardTrainDto;
import com.example.railtracker.dto.StationDto;
import com.example.railtracker.entity.Station;
import com.example.railtracker.exception.ResourceNotFoundException;
import com.example.railtracker.repository.StationRepository;
import com.example.railtracker.repository.TrainStationRepository;
import com.example.railtracker.entity.TrainStation;
import com.example.railtracker.service.StationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StationServiceImpl implements StationService {

    private static final Logger logger = LoggerFactory.getLogger(StationServiceImpl.class);

    private final StationRepository stationRepository;
    private final RailwayDataProvider railwayDataProvider;
    private final TrainStationRepository trainStationRepository;

    public StationServiceImpl(StationRepository stationRepository, RailwayDataProvider railwayDataProvider, TrainStationRepository trainStationRepository) {
        this.stationRepository = stationRepository;
        this.railwayDataProvider = railwayDataProvider;
        this.trainStationRepository = trainStationRepository;
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
        return enrichArrivalDays(liveBoard, stationCode);
    }

    @Override
    @Cacheable(value = "live-station", key = "#stationCode + ':' + #hours")
    public LiveStationBoardResponse getLiveStationBoardDetails(String stationCode, int hours) {
        logger.debug("Retrieving live board details for station: {} for next {} hours", stationCode, hours);
        
        try {
            getStationDetails(stationCode);
        } catch (ResourceNotFoundException e) {
            logger.warn("Station {} is not available in lookup data. Continuing.", stationCode);
        }
        
        LiveStationBoardResponse rawResponse = railwayDataProvider.getLiveStationBoardDetails(stationCode, hours);
        if (rawResponse == null) {
            return null;
        }
        
        List<LiveStationBoardResponse.LiveStationBoardTrain> enrichedTrains = rawResponse.trains().stream()
                .map(t -> {
                    String srcCode = t.source().code();
                    String srcName = stationRepository.findByStationCode(srcCode)
                            .map(com.example.railtracker.entity.Station::getStationName)
                            .orElse(srcCode);
                            
                    String destCode = t.destination().code();
                    String destName = stationRepository.findByStationCode(destCode)
                            .map(com.example.railtracker.entity.Station::getStationName)
                            .orElse(destCode);
                            
                    return new LiveStationBoardResponse.LiveStationBoardTrain(
                            t.trainNumber(),
                            t.trainName(),
                            t.trainType(),
                            new LiveStationBoardResponse.StationCodeName(srcCode, srcName),
                            new LiveStationBoardResponse.StationCodeName(destCode, destName),
                            t.passType(),
                            t.scheduledArrival(),
                            t.scheduledDeparture(),
                            t.expectedArrival(),
                            t.expectedDeparture(),
                            t.delayMinutes(),
                            t.platform(),
                            t.liveStatus(),
                            t.runningDays()
                    );
                })
                .sorted((t1, t2) -> {
                    int p1 = getLiveStatusPriority(t1.liveStatus());
                    int p2 = getLiveStatusPriority(t2.liveStatus());
                    if (p1 != p2) {
                        return Integer.compare(p1, p2);
                    }
                    
                    String time1 = getSortTime(t1);
                    String time2 = getSortTime(t2);
                    
                    if (time1 == null && time2 == null) return 0;
                    if (time1 == null) return 1;
                    if (time2 == null) return -1;
                    
                    return time1.compareTo(time2);
                })
                .collect(Collectors.toList());
                
        StationDto st = getStationDetails(stationCode);
        LiveStationBoardResponse.StationInfo resolvedStation = new LiveStationBoardResponse.StationInfo(
                st.stationCode(),
                st.stationName(),
                st.city(),
                st.latitude(),
                st.longitude()
        );
        
        return new LiveStationBoardResponse(resolvedStation, rawResponse.window(), enrichedTrains);
    }
    
    private int getLiveStatusPriority(com.example.railtracker.dto.StationTrainLiveStatus status) {
        if (status == null) return 5;
        switch (status) {
            case AT_STATION: return 1;
            case UPCOMING: return 2;
            case NOT_STARTED: return 3;
            case DEPARTED: return 4;
            default: return 5;
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

    private List<StationBoardTrainDto> enrichArrivalDays(List<StationBoardTrainDto> trains, String stationCode) {
        try {
            List<TrainStation> savedStations = trainStationRepository.findByStationCodeWithDetails(stationCode);
            if (savedStations == null || savedStations.isEmpty()) {
                return trains;
            }
            Map<String, Integer> arrivalDayMap = savedStations.stream()
                    .collect(Collectors.toMap(
                            ts -> ts.getTrain().getTrainNumber(),
                            ts -> ts.getArrivalDay(),
                            (existing, replacement) -> existing
                    ));

            return trains.stream()
                    .map(t -> {
                        Integer dbArrivalDay = arrivalDayMap.get(t.trainNumber());
                        if (dbArrivalDay != null && !dbArrivalDay.equals(t.arrivalDay())) {
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
                                    t.stopType(),
                                    t.isHalt(),
                                    t.currentStatus(),
                                    dbArrivalDay
                            );
                        }
                        return t;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.warn("Failed to enrich arrival days from database for station: {}", stationCode, e);
            return trains;
        }
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


