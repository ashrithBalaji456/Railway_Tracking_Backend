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
import java.util.List;
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
        logger.debug("Searching trains locally with query: {}", query);
        List<Train> localTrains = trainRepository.searchTrains(query);

        if (!localTrains.isEmpty()) {
            return localTrains.stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());
        }

        logger.info("No local trains found. Searching RailRadar lookup for query: {}", query);
        try {
            List<TrainDto> externalTrains = railwayDataProvider.searchTrains(query);
            for (TrainDto externalTrain : externalTrains) {
                saveTrainIfMissing(externalTrain);
            }
            if (!externalTrains.isEmpty()) {
                return externalTrains;
            }
        } catch (Exception e) {
            logger.warn("RailRadar train lookup failed for query: {}", query, e);
        }

        try {
            TrainDto externalTrain = railwayDataProvider.getTrainDetails(query);
            saveTrainIfMissing(externalTrain);
            return List.of(externalTrain);
        } catch (Exception e) {
            logger.warn("Could not find train externally or invalid query: {}", query);
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




