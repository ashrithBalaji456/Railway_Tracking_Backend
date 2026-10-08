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
    @Transactional(readOnly = true)
    @Cacheable(value = "trains:search", key = "#query")
    public List<TrainDto> searchTrains(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        String cleanQuery = query.trim();
        logger.debug("Searching trains locally with query: {}", cleanQuery);

        boolean isNumeric = cleanQuery.matches("\\d+");

        if (isNumeric) {
            // Train Number search: suggest 5 trains matching format/prefix (e.g. 12, 20, 22, 01, 14...)
            List<Train> prefixMatches = trainRepository.findByTrainNumberStartingWith(cleanQuery);
            if (!prefixMatches.isEmpty()) {
                // If user entered a full 5-digit number, return exact match
                Train exactMatch = prefixMatches.stream()
                        .filter(t -> t.getTrainNumber().equals(cleanQuery))
                        .findFirst()
                        .orElse(null);

                if (exactMatch != null && cleanQuery.length() >= 5) {
                    return List.of(mapToDto(exactMatch));
                }

                // If 5 or fewer matches exist, return all
                if (prefixMatches.size() <= 5) {
                    return prefixMatches.stream().map(this::mapToDto).collect(Collectors.toList());
                }

                List<Train> selected = new ArrayList<>();
                if (exactMatch != null) {
                    selected.add(exactMatch);
                }

                // Evenly distribute 5 train numbers across the entire format series
                double step = (double) prefixMatches.size() / 5.0;
                for (int i = 0; i < 5 && selected.size() < 5; i++) {
                    int idx = (int) (i * step);
                    Train candidate = prefixMatches.get(idx);
                    if (!selected.contains(candidate)) {
                        selected.add(candidate);
                    }
                }

                // Fill up to 5 if needed
                for (Train t : prefixMatches) {
                    if (selected.size() >= 5) break;
                    if (!selected.contains(t)) {
                        selected.add(t);
                    }
                }

                return selected.stream().map(this::mapToDto).collect(Collectors.toList());
            }

            // Fallback: match train numbers containing the digits locally
            List<Train> containsMatches = trainRepository.searchTrains(cleanQuery);
            if (!containsMatches.isEmpty()) {
                return containsMatches.stream().limit(5).map(this::mapToDto).collect(Collectors.toList());
            }
            return List.of();
        } else {
            // Train Name search: suggest by train name (prefix matches prioritized, then contains)
            List<Train> nameMatches = trainRepository.searchByTrainName(
                    cleanQuery,
                    org.springframework.data.domain.PageRequest.of(0, 15)
            );
            if (!nameMatches.isEmpty()) {
                return nameMatches.stream().map(this::mapToDto).collect(Collectors.toList());
            }

            // Fallback search across local trains
            List<Train> localTrains = trainRepository.searchTrains(cleanQuery);
            if (!localTrains.isEmpty()) {
                return localTrains.stream().limit(10).map(this::mapToDto).collect(Collectors.toList());
            }
            return List.of();
        }
    }

    private void saveTrainIfMissing(TrainDto externalTrain) {
        if (externalTrain == null || externalTrain.trainNumber() == null || externalTrain.trainNumber().isBlank()) return;
        try {
            var existingOpt = trainRepository.findByTrainNumber(externalTrain.trainNumber());
            if (existingOpt.isEmpty()) {
                String name = externalTrain.trainName() != null ? externalTrain.trainName().trim() : "Express";
                String type = externalTrain.trainType() != null ? externalTrain.trainType().trim() : "Express";
                String cat = externalTrain.category() != null ? externalTrain.category() : determineCategory(name, type);
                Train train = new Train(
                        externalTrain.trainNumber().trim(),
                        name,
                        type,
                        cat,
                        externalTrain.sourceStation(),
                        externalTrain.destinationStation(),
                        externalTrain.distance(),
                        externalTrain.duration(),
                        externalTrain.runningDays() != null ? externalTrain.runningDays() : "Daily",
                        externalTrain.coachPosition()
                );
                trainRepository.save(train);
                logger.info("Saved train {} ({}) locally with coachPosition", externalTrain.trainNumber(), name);
            } else {
                Train train = existingOpt.get();
                boolean changed = false;
                if (externalTrain.coachPosition() != null && !externalTrain.coachPosition().equals(train.getCoachPosition())) {
                    train.setCoachPosition(externalTrain.coachPosition());
                    changed = true;
                }
                if ((train.getSourceStation() == null || train.getSourceStation().isBlank()) && externalTrain.sourceStation() != null) {
                    train.setSourceStation(externalTrain.sourceStation());
                    changed = true;
                }
                if ((train.getDestinationStation() == null || train.getDestinationStation().isBlank()) && externalTrain.destinationStation() != null) {
                    train.setDestinationStation(externalTrain.destinationStation());
                    changed = true;
                }
                if (changed) {
                    trainRepository.save(train);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to auto-save train {}: {}", externalTrain.trainNumber(), e.getMessage());
        }
    }

    private void saveTrainIfMissing(TrainRouteDto routeDto) {
        if (routeDto == null || routeDto.trainNumber() == null || routeDto.trainNumber().isBlank()) return;
        try {
            String num = routeDto.trainNumber().trim();
            if (!trainRepository.existsByTrainNumber(num)) {
                String name = routeDto.trainName() != null ? routeDto.trainName().trim() : "Express";
                String type = routeDto.trainType() != null ? routeDto.trainType().trim() : "Express";
                Train train = new Train(
                        num,
                        name,
                        type,
                        determineCategory(name, type),
                        routeDto.sourceStation(),
                        routeDto.destinationStation(),
                        routeDto.distance(),
                        routeDto.duration(),
                        routeDto.runningDays() != null ? routeDto.runningDays() : "Daily",
                        routeDto.coachPosition()
                );
                trainRepository.save(train);
                logger.info("Auto-populated new train {} ({}) locally from route schedule", num, name);
            }
        } catch (Exception e) {
            logger.warn("Failed to auto-populate train from route: {}", e.getMessage());
        }
    }

    private void saveTrainIfMissingFromLive(TrainLiveStatusDto live) {
        if (live == null || live.trainNumber() == null || live.trainNumber().isBlank()) return;
        try {
            String num = live.trainNumber().trim();
            if (!trainRepository.existsByTrainNumber(num)) {
                String name = live.trainName() != null ? live.trainName().trim() : "Express";
                Train train = new Train(
                        num,
                        name,
                        "Express",
                        determineCategory(name, "Express"),
                        "",
                        "",
                        null,
                        null,
                        "Daily"
                );
                trainRepository.save(train);
                logger.info("Auto-populated new train {} ({}) locally from live status", num, name);
            }
        } catch (Exception e) {
            logger.warn("Failed to auto-populate train from live status: {}", e.getMessage());
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

    @Override
    public TrainDto getTrainDetails(String trainNumber, String journeyDate) {
        if (journeyDate == null || journeyDate.isBlank()) {
            return getTrainDetails(trainNumber);
        }
        logger.info("Retrieving details for train: {} with journeyDate: {}", trainNumber, journeyDate);
        TrainDto dto = railwayDataProvider.getTrainDetails(trainNumber, journeyDate);
        saveTrainIfMissing(dto);
        return dto;
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
                    saveTrainIfMissing(foundDto);
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
        saveTrainIfMissing(routeDto);

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
        TrainLiveStatusDto status = railwayDataProvider.getTrainLiveStatus(trainNumber, journeyDate);
        saveTrainIfMissingFromLive(status);
        return status;
    }

    @Override
    @Cacheable(value = "train:live", key = "#trainNumber + '_current'")
    public TrainLiveStatusDto getTrainLiveStatus(String trainNumber) {
        logger.debug("Retrieving live status for train: {}", trainNumber);
        TrainLiveStatusDto status = railwayDataProvider.getTrainLiveStatus(trainNumber);
        saveTrainIfMissingFromLive(status);
        return status;
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




