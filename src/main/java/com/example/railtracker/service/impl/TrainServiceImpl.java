package com.example.railtracker.service.impl;

import com.example.railtracker.client.RailwayDataProvider;
import com.example.railtracker.dto.*;
import com.example.railtracker.entity.Station;
import com.example.railtracker.entity.Train;
import com.example.railtracker.entity.TrainStation;
import com.example.railtracker.exception.ResourceNotFoundException;
import com.example.railtracker.repository.StationRepository;
import com.example.railtracker.repository.TrainRepository;
import com.example.railtracker.repository.TrainStationRepository;
import com.example.railtracker.service.TrainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TrainServiceImpl implements TrainService {

    private static final Logger logger = LoggerFactory.getLogger(TrainServiceImpl.class);

    private final TrainRepository trainRepository;
    private final StationRepository stationRepository;
    private final TrainStationRepository trainStationRepository;
    private final RailwayDataProvider railwayDataProvider;

    public TrainServiceImpl(
            TrainRepository trainRepository,
            StationRepository stationRepository,
            TrainStationRepository trainStationRepository,
            RailwayDataProvider railwayDataProvider) {
        this.trainRepository = trainRepository;
        this.stationRepository = stationRepository;
        this.trainStationRepository = trainStationRepository;
        this.railwayDataProvider = railwayDataProvider;
    }

    @Override
    @Transactional
    public List<TrainDto> searchTrains(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        String cleanQuery = query.trim();
        logger.debug("Searching trains with query: {}", cleanQuery);

        boolean isNumeric = cleanQuery.matches("\\d+");

        if (isNumeric) {
            // Train Number search: suggest 5 trains matching format/prefix (e.g. "12" -> 12343, 12367, 12546...)
            List<Train> prefixMatches = trainRepository.findByTrainNumberStartingWith(cleanQuery);
            if (!prefixMatches.isEmpty()) {
                // Check if exact full train number was entered
                Train exactMatch = prefixMatches.stream()
                        .filter(t -> t.getTrainNumber().equals(cleanQuery))
                        .findFirst()
                        .orElse(null);

                if (exactMatch != null && cleanQuery.length() >= 5) {
                    return List.of(mapToDto(exactMatch));
                }

                List<Train> selected = new ArrayList<>();
                if (exactMatch != null) {
                    selected.add(exactMatch);
                }

                // If searching by prefix like "12", prioritize popular trains (12343 Darjeeling Mail, 12367 Vikramshila, 12546 Karmabhoomi, etc.)
                List<String> priorityOrder = List.of("12343", "12367", "12546", "12001", "12951", "12760", "12423", "12626");
                Map<String, Train> mapByNumber = prefixMatches.stream()
                        .collect(Collectors.toMap(Train::getTrainNumber, t -> t, (a, b) -> a));

                for (String pNum : priorityOrder) {
                    if (pNum.startsWith(cleanQuery) && mapByNumber.containsKey(pNum)) {
                        Train t = mapByNumber.get(pNum);
                        if (!selected.contains(t) && selected.size() < 5) {
                            selected.add(t);
                        }
                    }
                }

                // Fill up to 5 recommendations with well-distributed trains matching that prefix
                if (selected.size() < 5) {
                    List<Train> remaining = prefixMatches.stream()
                            .filter(t -> !selected.contains(t))
                            .toList();

                    int needed = 5 - selected.size();
                    if (remaining.size() <= needed) {
                        selected.addAll(remaining);
                    } else {
                        int step = Math.max(1, remaining.size() / needed);
                        for (int i = 0; i < remaining.size() && selected.size() < 5; i += step) {
                            selected.add(remaining.get(i));
                        }
                        for (Train t : remaining) {
                            if (selected.size() >= 5) break;
                            if (!selected.contains(t)) selected.add(t);
                        }
                    }
                }

                return selected.stream().map(this::mapToDto).collect(Collectors.toList());
            }
        } else {
            // Train Name search: suggest by train name (prefix matches prioritized, then contains)
            List<Train> nameMatches = trainRepository.searchByTrainName(
                    cleanQuery,
                    org.springframework.data.domain.PageRequest.of(0, 15)
            );
            if (!nameMatches.isEmpty()) {
                return nameMatches.stream().map(this::mapToDto).collect(Collectors.toList());
            }
        }

        // Fallback search across local trains
        List<Train> localTrains = trainRepository.searchTrains(cleanQuery);
        if (!localTrains.isEmpty()) {
            return localTrains.stream().limit(10).map(this::mapToDto).collect(Collectors.toList());
        }

        logger.info("No local trains found. Searching RailRadar lookup for query: {}", cleanQuery);
        try {
            List<TrainDto> externalTrains = railwayDataProvider.searchTrains(cleanQuery);
            for (TrainDto externalTrain : externalTrains) {
                saveTrainIfMissing(externalTrain);
            }
            if (!externalTrains.isEmpty()) {
                return externalTrains;
            }
        } catch (Exception e) {
            logger.warn("RailRadar train lookup failed for query: {}", cleanQuery, e);
        }

        try {
            TrainDto externalTrain = railwayDataProvider.getTrainDetails(cleanQuery);
            saveTrainIfMissing(externalTrain);
            return List.of(externalTrain);
        } catch (Exception e) {
            logger.warn("Could not find train externally or invalid query: {}", cleanQuery);
            return List.of();
        }
    }

    private void saveTrainIfMissing(TrainDto externalTrain) {
        if (externalTrain == null || externalTrain.trainNumber() == null) return;
        var existingOpt = trainRepository.findByTrainNumber(externalTrain.trainNumber());
        if (existingOpt.isEmpty()) {
            Train train = new Train(
                    externalTrain.trainNumber(),
                    externalTrain.trainName(),
                    externalTrain.trainType(),
                    externalTrain.category(),
                    externalTrain.sourceStation(),
                    externalTrain.destinationStation(),
                    externalTrain.distance(),
                    externalTrain.duration(),
                    externalTrain.runningDays(),
                    externalTrain.coachPosition()
            );
            trainRepository.save(train);
            logger.debug("Saved train {} locally with coachPosition", externalTrain.trainNumber());
        } else {
            Train train = existingOpt.get();
            if (externalTrain.coachPosition() != null && !externalTrain.coachPosition().equals(train.getCoachPosition())) {
                train.setCoachPosition(externalTrain.coachPosition());
                trainRepository.save(train);
            }
        }
    }
    @Override
    public TrainDto getTrainDetails(String trainNumber, String journeyDate) {
        if (journeyDate == null || journeyDate.isBlank()) {
            return getTrainDetails(trainNumber);
        }
        logger.info("Retrieving details for train: {} with journeyDate: {}", trainNumber, journeyDate);
        return railwayDataProvider.getTrainDetails(trainNumber, journeyDate);
    }

    @Override
    @Transactional
    @Cacheable(value = "train:details", key = "#trainNumber")
    public TrainDto getTrainDetails(String trainNumber) {
        logger.debug("Retrieving details for train: {}", trainNumber);
        return trainRepository.findByTrainNumber(trainNumber)
                .map(this::mapToDto)
                .orElseGet(() -> {
                    logger.info("Train {} not found locally. Searching externally.", trainNumber);
                    TrainDto foundDto = railwayDataProvider.getTrainDetails(trainNumber);
                    Train train = new Train(
                            foundDto.trainNumber(),
                            foundDto.trainName(),
                            foundDto.trainType(),
                            foundDto.category(),
                            foundDto.sourceStation(),
                            foundDto.destinationStation(),
                            foundDto.distance(),
                            foundDto.duration(),
                            foundDto.runningDays()
                    );
                    trainRepository.save(train);
                    return foundDto;
                });
    }

    @Override
    public TrainRouteDto getTrainRoute(String trainNumber, String journeyDate) {
        if (journeyDate == null || journeyDate.isBlank()) {
            return getTrainRoute(trainNumber);
        }
        logger.info("Retrieving dynamic route for train: {} on date: {}", trainNumber, journeyDate);
        TrainRouteDto routeDto = railwayDataProvider.getTrainRoute(trainNumber, journeyDate);

        // Enrich halts and stop types from the local database to prevent losing halt classification
        if (routeDto != null && routeDto.stations() != null) {
            List<TrainStation> localStations = trainStationRepository.findByTrainTrainNumberOrderBySequenceNumberAsc(trainNumber);
            if (localStations != null && !localStations.isEmpty()) {
                List<RouteStationDto> enrichedStations = routeDto.stations().stream()
                        .map(s -> {
                            TrainStation localHalt = localStations.stream()
                                    .filter(lh -> lh.getStation() != null && lh.getStation().getStationCode() != null &&
                                            lh.getStation().getStationCode().equalsIgnoreCase(s.stationCode()))
                                    .findFirst()
                                    .orElse(null);

                            if (localHalt != null) {
                                boolean isHalt = Boolean.TRUE.equals(localHalt.getIsHalt());
                                String stopType = isHalt ? "STOPPING" : "PASS_THROUGH";
                                return new RouteStationDto(
                                        s.stationCode(),
                                        s.stationName(),
                                        s.sequenceNumber(),
                                        s.arrivalTime(),
                                        s.departureTime(),
                                        s.arrivalDay(),
                                        s.departureDay(),
                                        s.haltMinutes(),
                                        stopType,
                                        isHalt,
                                        s.latitude(),
                                        s.longitude(),
                                        s.distance(),
                                        s.speedToNextStationKmph()
                                );
                            }
                            return s;
                        })
                        .collect(Collectors.toList());

                return new TrainRouteDto(
                        routeDto.trainNumber(),
                        routeDto.trainName(),
                        routeDto.trainType(),
                        routeDto.sourceStation(),
                        routeDto.destinationStation(),
                        routeDto.distance(),
                        routeDto.duration(),
                        routeDto.runningDays(),
                        routeDto.coachPosition(),
                        enrichedStations
                );
            }
        }
        return routeDto;
    }

    @Override
    @Transactional
    @Cacheable(value = "train:route", key = "#trainNumber")
    public TrainRouteDto getTrainRoute(String trainNumber) {
        logger.debug("Retrieving route for train: {}", trainNumber);
        
        // Ensure the Train entity exists locally first
        Train train = trainRepository.findByTrainNumber(trainNumber)
                .orElseGet(() -> {
                    TrainDto details = getTrainDetails(trainNumber);
                    return trainRepository.findByTrainNumber(details.trainNumber())
                            .orElseThrow(() -> new ResourceNotFoundException("Train not found: " + trainNumber));
                });

        List<TrainStation> cachedRoute = trainStationRepository.findByTrainTrainNumberOrderBySequenceNumberAsc(trainNumber);

        if (!cachedRoute.isEmpty()) {
            logger.debug("Serving cached route for train {}", trainNumber);
            List<RouteStationDto> stations = cachedRoute.stream()
                    .map(ts -> new RouteStationDto(
                            ts.getStation().getStationCode(),
                            ts.getStation().getStationName(),
                            ts.getSequenceNumber(),
                            ts.getArrivalTime(),
                            ts.getDepartureTime(),
                            ts.getArrivalDay(),
                            ts.getDepartureDay(),
                            ts.getHaltMinutes(),
                            ts.getStopType(),
                            ts.getIsHalt(),
                            ts.getStation().getLatitude(),
                            ts.getStation().getLongitude(),
                            ts.getDistance(),
                            ts.getSpeedToNextStationKmph()
                    ))
                    .collect(Collectors.toList());

            return new TrainRouteDto(
                    train.getTrainNumber(),
                    train.getTrainName(),
                    train.getTrainType(),
                    train.getSourceStation(),
                    train.getDestinationStation(),
                    train.getDistance(),
                    train.getDuration(),
                    train.getRunningDays(),
                    train.getCoachPosition(),
                    stations
            );
        }

        logger.info("Route cache miss for train {}. Loading route externally.", trainNumber);
        TrainRouteDto externalRoute = railwayDataProvider.getTrainRoute(trainNumber);

        for (RouteStationDto stationDto : externalRoute.stations()) {
            Station station = stationRepository.findByStationCode(stationDto.stationCode())
                    .orElseGet(() -> {
                        // Dynamically create station stub if missing locally
                        Station newStation = new Station(
                                stationDto.stationCode(),
                                stationDto.stationName(),
                                "", "", stationDto.latitude(), stationDto.longitude()
                        );
                        return stationRepository.save(newStation);
                    });

            if (hasRealCoordinates(stationDto.latitude(), stationDto.longitude())
                    && !hasRealCoordinates(station.getLatitude(), station.getLongitude())) {
                station.setLatitude(stationDto.latitude());
                station.setLongitude(stationDto.longitude());
                stationRepository.save(station);
            }

            TrainStation trainStation = new TrainStation(
                    train,
                    station,
                    stationDto.sequenceNumber(),
                    stationDto.arrivalTime(),
                    stationDto.departureTime(),
                    stationDto.arrivalDay(),
                    stationDto.departureDay(),
                    stationDto.haltMinutes(),
                    stationDto.stopType(),
                    stationDto.isHalt(),
                    stationDto.distance(),
                    stationDto.speedToNextStationKmph()
            );
            trainStationRepository.save(trainStation);
        }

        return externalRoute;
    }

    @Override
    @Cacheable(value = "train:live", key = "#trainNumber + '_' + (#journeyDate != null ? #journeyDate : 'current')")
    public TrainLiveStatusDto getTrainLiveStatus(String trainNumber, String journeyDate) {
        if (journeyDate == null || journeyDate.isBlank()) {
            return getTrainLiveStatus(trainNumber);
        }
        logger.info("Retrieving dynamic live status for train: {} on date: {}", trainNumber, journeyDate);
        return railwayDataProvider.getTrainLiveStatus(trainNumber, journeyDate);
    }

    @Override
    @Cacheable(value = "train:live", key = "#trainNumber + '_current'")
    public TrainLiveStatusDto getTrainLiveStatus(String trainNumber) {
        logger.debug("Retrieving live status for train: {}", trainNumber);
        return railwayDataProvider.getTrainLiveStatus(trainNumber);
    }

    private boolean hasRealCoordinates(Double latitude, Double longitude) {
        return latitude != null && longitude != null && (latitude != 0.0 || longitude != 0.0);
    }
    private TrainDto mapToDto(Train train) {
        return new TrainDto(
                train.getTrainNumber(),
                train.getTrainName(),
                train.getTrainType(),
                train.getCategory(),
                train.getSourceStation(),
                train.getDestinationStation(),
                train.getDistance(),
                train.getDuration(),
                train.getRunningDays(),
                train.getActive(),
                train.getCoachPosition()
        );
    }
}




