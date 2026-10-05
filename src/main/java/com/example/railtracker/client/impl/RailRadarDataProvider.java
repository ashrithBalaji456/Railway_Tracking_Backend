package com.example.railtracker.client.impl;

import com.example.railtracker.client.RailwayDataProvider;
import com.example.railtracker.client.RailRadarClient;
import com.example.railtracker.client.RailRadarClient.*;
import com.example.railtracker.dto.*;
import org.springframework.stereotype.Service;
import com.example.railtracker.exception.ResourceNotFoundException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RailRadarDataProvider implements RailwayDataProvider {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RailRadarDataProvider.class);
    private final RailRadarClient client;

    public RailRadarDataProvider(RailRadarClient client) {
        this.client = client;
    }

    private LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty() || timeStr.equals("--") || timeStr.equals("-")) {
            return null;
        }
        try {
            // Support HH:mm:ss or HH:mm formats
            if (timeStr.length() == 5) {
                return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"));
            }
            return LocalTime.parse(timeStr);
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception ex) {
                return LocalDateTime.now();
            }
        }
    }

    private Integer calculateHaltMinutes(String arrival, String departure) {
        LocalTime arr = parseTime(arrival);
        LocalTime dep = parseTime(departure);
        if (arr != null && dep != null) {
            java.time.Duration duration = java.time.Duration.between(arr, dep);
            return (int) Math.abs(duration.toMinutes());
        }
        return 0;
    }

    @Override
    public List<StationDto> searchStations(String query) {
        try {
            return client.searchStations(query).stream()
                    .map(s -> new StationDto(
                            s.stationCode(),
                            s.stationName(),
                            s.city(),
                            s.state(),
                            s.latitude(),
                            s.longitude(),
                            s.active()
                    ))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.warn("Failed to search stations externally for query: {}. Returning empty list fallback.", query, e);
            return List.of();
        }
    }

    @Override
    public List<TrainDto> searchTrains(String query) {
        try {
            return client.searchTrains(query).stream()
                    .filter(t -> t.trainNumber() != null)
                    .map(t -> new TrainDto(
                            t.trainNumber(),
                            t.trainName() != null && !t.trainName().isBlank() ? t.trainName() : "Train " + t.trainNumber(),
                            t.trainType(),
                            t.category(),
                            t.sourceStation(),
                            t.destinationStation(),
                            t.distance(),
                            t.duration(),
                            t.runningDays(),
                            t.active() == null || t.active(),
                            t.coachPosition()
                    ))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.warn("Failed to search trains externally for query: {}. Returning empty list fallback.", query, e);
            return List.of();
        }
    }
    @Override
    public TrainDto getTrainDetails(String trainNumber, String journeyDate) {
        if (journeyDate == null || journeyDate.isBlank()) {
            return getTrainDetails(trainNumber);
        }
        try {
            RailRadarTrain t = client.getTrainDetails(trainNumber, journeyDate);
            if (t == null || t.trainNumber() == null || t.trainName() == null || t.trainName().isBlank()) {
                throw new ResourceNotFoundException("RailRadar did not return train details for " + trainNumber + " on date " + journeyDate);
            }
            return new TrainDto(
                    t.trainNumber(),
                    t.trainName(),
                    t.trainType(),
                    t.category(),
                    t.sourceStation(),
                    t.destinationStation(),
                    t.distance(),
                    t.duration(),
                    t.runningDays(),
                    t.active(),
                    t.coachPosition()
            );
        } catch (Exception e) {
            logger.error("Failed to fetch RailRadar train details for {} on {}", trainNumber, journeyDate, e);
            throw new RuntimeException("RailRadar details unavailable for " + trainNumber + " on date " + journeyDate, e);
        }
    }

    @Override
    public TrainDto getTrainDetails(String trainNumber) {
        try {
            RailRadarTrain t = client.getTrainDetails(trainNumber);
            if (t == null || t.trainNumber() == null || t.trainName() == null || t.trainName().isBlank()) {
                throw new ResourceNotFoundException("RailRadar did not return train details for " + trainNumber);
            }
            return new TrainDto(
                    t.trainNumber(),
                    t.trainName(),
                    t.trainType(),
                    t.category(),
                    t.sourceStation(),
                    t.destinationStation(),
                    t.distance(),
                    t.duration(),
                    t.runningDays(),
                    t.active(),
                    t.coachPosition()
            );
        } catch (Exception detailsError) {
            logger.warn("RailRadar details endpoint failed for {}. Trying live endpoint as fallback.", trainNumber, detailsError);
            try {
                RailRadarTrainLiveStatus live = client.getTrainLiveStatus(trainNumber);
                if (live == null || live.trainNumber() == null) {
                    throw new ResourceNotFoundException("RailRadar did not return live train identity for " + trainNumber);
                }
                return new TrainDto(
                        live.trainNumber(),
                        live.trainName() != null && !live.trainName().isBlank() ? live.trainName() : "Train " + trainNumber,
                        "Live",
                        "LIVE",
                        live.currentStationCode(),
                        live.nextStationCode(),
                        null,
                        null,
                        null,
                        true,
                        null
                );
            } catch (Exception liveError) {
                logger.warn("Failed to fetch train identity from RailRadar live endpoint for {}.", trainNumber, liveError);
                throw new ResourceNotFoundException("RailRadar train details unavailable for " + trainNumber);
            }
        }
    }

    @Override
    public TrainRouteDto getTrainRoute(String trainNumber, String journeyDate) {
        if (journeyDate == null || journeyDate.isBlank()) {
            return getTrainRoute(trainNumber);
        }
        try {
            RailRadarTrain trainDetails = null;
            try {
                trainDetails = client.getTrainDetails(trainNumber, journeyDate);
                if (trainDetails != null && trainDetails.stations() != null && !trainDetails.stations().isEmpty()) {
                    return buildRouteFromDetails(trainNumber, trainDetails);
                }
            } catch (Exception detailsEx) {
                logger.warn("Could not fetch station route from RailRadar train details with journeyDate: {}. Trying route endpoint.", journeyDate, detailsEx);
            }

            RailRadarTrainRoute r = client.getTrainRoute(trainNumber, journeyDate);
            if (r == null || r.stops() == null || r.stops().isEmpty()) {
                throw new ResourceNotFoundException("RailRadar did not return a route for " + trainNumber + " on date " + journeyDate);
            }

            final RailRadarTrain details = trainDetails;

            List<RouteStationDto> stations = r.stops().stream()
                    .map(s -> {
                        RailRadarTrainStation scheduledHalt = null;
                        if (details != null && details.stations() != null) {
                            scheduledHalt = details.stations().stream()
                                    .filter(h -> h.station() != null && h.station().code() != null && h.station().code().equalsIgnoreCase(s.code()))
                                    .findFirst()
                                    .orElse(null);
                        }
                        
                        LocalTime arr = null;
                        LocalTime dep = null;
                        Integer arrDay = 0;
                        Integer depDay = 0;
                        Integer haltMin = 0;
                        boolean isHalt = false;
                        String stopType = "PASS_THROUGH";
                        
                        if (scheduledHalt != null) {
                            arr = parseTime(scheduledHalt.arrival());
                            dep = parseTime(scheduledHalt.departure());
                            arrDay = scheduledHalt.arrivalDay() != null ? scheduledHalt.arrivalDay() : 0;
                            depDay = scheduledHalt.departureDay() != null ? scheduledHalt.departureDay() : 0;
                            haltMin = calculateHaltMinutes(scheduledHalt.arrival(), scheduledHalt.departure());
                            isHalt = Boolean.TRUE.equals(scheduledHalt.isHalt());
                            stopType = isHalt ? "STOPPING" : "PASS_THROUGH";
                        }
                        
                        Double distanceVal = (scheduledHalt != null && scheduledHalt.distance() != null) ? scheduledHalt.distance() : 0.0;
                        Double speedVal = (scheduledHalt != null && scheduledHalt.speedToNextStationKmph() != null) ? scheduledHalt.speedToNextStationKmph() : 0.0;

                        return new RouteStationDto(
                                s.code(),
                                s.name(),
                                s.sequence(),
                                arr,
                                dep,
                                arrDay,
                                depDay,
                                haltMin,
                                stopType,
                                isHalt,
                                s.lat(),
                                s.lng(),
                                distanceVal,
                                speedVal
                        );
                    })
                    .collect(Collectors.toList());

            String trainName = details != null && details.trainName() != null ? details.trainName() : trainNumber;
            String trainType = details != null ? details.trainType() : null;
            String src = details != null ? details.sourceStation() : null;
            String dest = details != null ? details.destinationStation() : null;
            Integer dist = details != null ? details.distance() : null;
            Integer dur = details != null ? details.duration() : null;
            String runningDays = details != null ? details.runningDays() : null;

            return new TrainRouteDto(
                    trainNumber,
                    trainName,
                    trainType,
                    src,
                    dest,
                    dist,
                    dur,
                    runningDays,
                    details != null ? details.coachPosition() : null,
                    stations
            );
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.warn("Failed to fetch train route externally for {} on date {}.", trainNumber, journeyDate, e);
            throw new ResourceNotFoundException("RailRadar route unavailable for " + trainNumber + " on date " + journeyDate);
        }
    }

    @Override
    public TrainRouteDto getTrainRoute(String trainNumber) {
        try {
            RailRadarTrain trainDetails = null;
            try {
                trainDetails = client.getTrainDetails(trainNumber);
                if (trainDetails != null && trainDetails.stations() != null && !trainDetails.stations().isEmpty()) {
                    return buildRouteFromDetails(trainNumber, trainDetails);
                }
            } catch (Exception detailsEx) {
                logger.warn("Could not fetch station route from RailRadar train details. Trying route endpoint.", detailsEx);
            }

            RailRadarTrainRoute r = client.getTrainRoute(trainNumber);
            if (r == null || r.stops() == null || r.stops().isEmpty()) {
                throw new ResourceNotFoundException("RailRadar did not return a route for " + trainNumber);
            }

            final RailRadarTrain details = trainDetails;

            List<RouteStationDto> stations = r.stops().stream()
                    .map(s -> {
                        RailRadarTrainStation scheduledHalt = null;
                        if (details != null && details.stations() != null) {
                            scheduledHalt = details.stations().stream()
                                    .filter(h -> h.station() != null && h.station().code() != null && h.station().code().equalsIgnoreCase(s.code()))
                                    .findFirst()
                                    .orElse(null);
                        }
                        
                        LocalTime arr = null;
                        LocalTime dep = null;
                        Integer arrDay = 0;
                        Integer depDay = 0;
                        Integer haltMin = 0;
                        boolean isHalt = false;
                        String stopType = "PASS_THROUGH";
                        
                        if (scheduledHalt != null) {
                            arr = parseTime(scheduledHalt.arrival());
                            dep = parseTime(scheduledHalt.departure());
                            arrDay = scheduledHalt.arrivalDay() != null ? scheduledHalt.arrivalDay() : 0;
                            depDay = scheduledHalt.departureDay() != null ? scheduledHalt.departureDay() : 0;
                            haltMin = calculateHaltMinutes(scheduledHalt.arrival(), scheduledHalt.departure());
                            isHalt = Boolean.TRUE.equals(scheduledHalt.isHalt());
                            stopType = isHalt ? "STOPPING" : "PASS_THROUGH";
                        }
                        
                        Double distanceVal = (scheduledHalt != null && scheduledHalt.distance() != null) ? scheduledHalt.distance() : 0.0;
                        Double speedVal = (scheduledHalt != null && scheduledHalt.speedToNextStationKmph() != null) ? scheduledHalt.speedToNextStationKmph() : 0.0;

                        return new RouteStationDto(
                                s.code(),
                                s.name(),
                                s.sequence(),
                                arr,
                                dep,
                                arrDay,
                                depDay,
                                haltMin,
                                stopType,
                                isHalt,
                                s.lat(),
                                s.lng(),
                                distanceVal,
                                speedVal
                        );
                    })
                    .collect(Collectors.toList());

            String trainName = details != null && details.trainName() != null ? details.trainName() : trainNumber;
            String trainType = details != null ? details.trainType() : null;
            String src = details != null ? details.sourceStation() : null;
            String dest = details != null ? details.destinationStation() : null;
            Integer dist = details != null ? details.distance() : null;
            Integer dur = details != null ? details.duration() : null;
            String runningDays = details != null ? details.runningDays() : null;

            return new TrainRouteDto(
                    trainNumber,
                    trainName,
                    trainType,
                    src,
                    dest,
                    dist,
                    dur,
                    runningDays,
                    details != null ? details.coachPosition() : null,
                    stations
            );
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.warn("Failed to fetch train route externally for {}.", trainNumber, e);
            throw new ResourceNotFoundException("RailRadar route unavailable for " + trainNumber);
        }
    }

    private TrainRouteDto buildRouteFromDetails(String trainNumber, RailRadarTrain details) {
        List<RouteStationDto> stations = details.stations().stream()
                .filter(s -> s.station() != null)
                .map(s -> {
                    LocalTime arr = parseTime(s.arrival());
                    LocalTime dep = parseTime(s.departure());
                    Integer arrDay = s.arrivalDay() != null ? s.arrivalDay() : 0;
                    Integer depDay = s.departureDay() != null ? s.departureDay() : 0;
                    Integer haltMin = calculateHaltMinutes(s.arrival(), s.departure());
                    boolean isHalt = Boolean.TRUE.equals(s.isHalt());
                    String stopType = isHalt ? "STOPPING" : "PASS_THROUGH";

                    return new RouteStationDto(
                            s.station().code(),
                            s.station().name(),
                            s.sequence(),
                            arr,
                            dep,
                            arrDay,
                            depDay,
                            haltMin,
                            stopType,
                            isHalt,
                            s.station().lat(),
                            s.station().lng(),
                            s.distance() != null ? s.distance() : 0.0,
                            s.speedToNextStationKmph() != null ? s.speedToNextStationKmph() : 0.0
                    );
                })
                .collect(Collectors.toList());

        return new TrainRouteDto(
                details.trainNumber() != null ? details.trainNumber() : trainNumber,
                details.trainName(),
                details.trainType(),
                details.sourceStation(),
                details.destinationStation(),
                details.distance(),
                details.duration(),
                details.runningDays(),
                details.coachPosition(),
                stations
        );
    }
    @Override
    public TrainLiveStatusDto getTrainLiveStatus(String trainNumber, String journeyDate) {
        if (journeyDate == null || journeyDate.isBlank()) {
            return getTrainLiveStatus(trainNumber);
        }
        try {
            RailRadarTrainLiveStatus l = client.getTrainLiveStatus(trainNumber, journeyDate);
            if (l == null) {
                throw new ResourceNotFoundException("RailRadar did not return live status for " + trainNumber + " on date " + journeyDate);
            }
            List<TrainLiveStatusDto.StationStatusDto> stations = l.stations() == null ? List.of() : l.stations().stream()
                    .map(s -> new TrainLiveStatusDto.StationStatusDto(
                            s.station() != null ? s.station().code() : null,
                            s.station() != null ? s.station().name() : null,
                            s.sequence(),
                            s.scheduledArrival(),
                            s.scheduledDeparture(),
                            s.actualArrival(),
                            s.actualDeparture(),
                            s.delayMinutes(),
                            s.status(),
                            Boolean.TRUE.equals(s.isHalt())
                    ))
                    .collect(Collectors.toList());

            return new TrainLiveStatusDto(
                    l.trainNumber(),
                    l.trainName(),
                    l.runStatus(),
                    l.currentStationCode(),
                    l.currentStationName(),
                    l.nextStationCode(),
                    l.nextStationName(),
                    l.delayMinutes(),
                    l.latitude(),
                    l.longitude(),
                    l.speed(),
                    l.bearing(),
                    l.routeProgress(),
                    l.lastLocationInfo(),
                    parseDateTime(l.lastUpdatedAt()),
                    stations
            );
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.warn("Failed to fetch train live status externally for {} on date {}.", trainNumber, journeyDate, e);
            throw new ResourceNotFoundException("RailRadar live status unavailable for " + trainNumber + " on date " + journeyDate);
        }
    }

    @Override
    public TrainLiveStatusDto getTrainLiveStatus(String trainNumber) {
        try {
            RailRadarTrainLiveStatus l = client.getTrainLiveStatus(trainNumber);
            if (l == null) {
                throw new ResourceNotFoundException("RailRadar did not return live status for " + trainNumber);
            }
            List<TrainLiveStatusDto.StationStatusDto> stations = l.stations() == null ? List.of() : l.stations().stream()
                    .map(s -> new TrainLiveStatusDto.StationStatusDto(
                            s.station() != null ? s.station().code() : null,
                            s.station() != null ? s.station().name() : null,
                            s.sequence(),
                            s.scheduledArrival(),
                            s.scheduledDeparture(),
                            s.actualArrival(),
                            s.actualDeparture(),
                            s.delayMinutes(),
                            s.status(),
                            Boolean.TRUE.equals(s.isHalt())
                    ))
                    .collect(Collectors.toList());

            return new TrainLiveStatusDto(
                    l.trainNumber(),
                    l.trainName(),
                    l.runStatus(),
                    l.currentStationCode(),
                    l.currentStationName(),
                    l.nextStationCode(),
                    l.nextStationName(),
                    l.delayMinutes(),
                    l.latitude(),
                    l.longitude(),
                    l.speed(),
                    l.bearing(),
                    l.routeProgress(),
                    l.lastLocationInfo(),
                    parseDateTime(l.lastUpdatedAt()),
                    stations
            );
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.warn("Failed to fetch train live status externally for {}.", trainNumber, e);
            throw new ResourceNotFoundException("RailRadar live status unavailable for " + trainNumber);
        }
    }

    @Override
    public List<StationBoardTrainDto> getStationBoard(String stationCode, boolean includeIntermediate) {
        try {
            return client.getStationBoard(stationCode, includeIntermediate).stream()
                    .map(this::mapStationBoardTrain)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.warn("RailRadar station board unavailable for {}. Returning empty train board.", stationCode, e);
            return List.of();
        }
    }

    @Override
    public LiveStationBoardResponse getLiveStationBoardDetails(String stationCode, int hours) {
        try {
            return client.getLiveStationBoardDetails(stationCode, hours);
        } catch (Exception e) {
            logger.warn("RailRadar live station board details unavailable for {}. Throwing ApiException.", stationCode, e);
            throw new com.example.railtracker.exception.ApiException("Live station board unavailable. " + e.getMessage(), e);
        }
    }

    @Override
    public List<StationBoardTrainDto> getStationLiveBoard(String stationCode, int hoursAhead, String type) {
        List<StationBoardTrainDto> allTrains;
        try {
            allTrains = client.getStationLiveBoard(stationCode, hoursAhead).stream()
                    .map(this::mapStationBoardTrain)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.warn("RailRadar live station board unavailable for {}. Returning empty train board.", stationCode, e);
            allTrains = List.of();
        }

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

    private StationBoardTrainDto mapStationBoardTrain(RailRadarStationBoardTrain t) {
        return new StationBoardTrainDto(
                t.trainNumber(),
                t.trainName(),
                t.trainType(),
                t.runningDays(),
                t.sourceStation(),
                t.destinationStation(),
                parseTime(t.scheduledArrival()),
                parseTime(t.scheduledDeparture()),
                parseTime(t.actualArrival()),
                parseTime(t.actualDeparture()),
                t.delayMinutes(),
                Boolean.TRUE.equals(t.isHalt()) ? "STOPPING" : "PASS_THROUGH",
                Boolean.TRUE.equals(t.isHalt()),
                t.currentStatus(),
                t.arrivalDay() != null ? t.arrivalDay() : 0
        );
    }
    @Override
    public List<TrainBetweenStationsDto> getTrainsBetweenStations(String fromStationCode, String toStationCode, String date) {
        return client.getTrainsBetweenStations(fromStationCode, toStationCode, date).stream()
                .map(t -> new TrainBetweenStationsDto(
                        t.trainNumber(),
                        t.trainName(),
                        t.trainType(),
                        t.fromStationCode(),
                        t.toStationCode(),
                        parseTime(t.departureTime()),
                        parseTime(t.arrivalTime()),
                        t.durationMinutes(),
                        t.runningDays(),
                        t.fromSequence(),
                        t.toSequence()
                ))
                .collect(Collectors.toList());
    }

}
