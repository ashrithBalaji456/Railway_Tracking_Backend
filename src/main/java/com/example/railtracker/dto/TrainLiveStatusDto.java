package com.example.railtracker.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public record TrainLiveStatusDto(
    String trainNumber,
    String trainName,
    String runStatus, // RUNNING, AT_STATION, NOT_STARTED, COMPLETED, etc.
    String currentStationCode,
    String currentStationName,
    String nextStationCode,
    String nextStationName,
    Integer delayMinutes,
    Double latitude,
    Double longitude,
    Double speed,
    Double bearing,
    Double routeProgress,
    String lastLocationInfo,
    LocalDateTime lastUpdatedAt,
    List<StationStatusDto> stations,
    Boolean isCancelled,
    Boolean isDiverted,
    Boolean isRescheduled,
    String disruptionMessage,
    List<String> skippedStations
) implements Serializable {

    // Backward-compatible constructor for existing callers and unit tests
    public TrainLiveStatusDto(
        String trainNumber,
        String trainName,
        String runStatus,
        String currentStationCode,
        String currentStationName,
        String nextStationCode,
        String nextStationName,
        Integer delayMinutes,
        Double latitude,
        Double longitude,
        Double speed,
        Double bearing,
        Double routeProgress,
        String lastLocationInfo,
        LocalDateTime lastUpdatedAt,
        List<StationStatusDto> stations
    ) {
        this(trainNumber, trainName, runStatus, currentStationCode, currentStationName,
             nextStationCode, nextStationName, delayMinutes, latitude, longitude,
             speed, bearing, routeProgress, lastLocationInfo, lastUpdatedAt, stations,
             false, false, false, null, List.of());
    }

    public record StationStatusDto(
        String stationCode,
        String stationName,
        Integer sequenceNumber,
        String scheduledArrival,
        String scheduledDeparture,
        String actualArrival,
        String actualDeparture,
        Integer delayMinutes,
        String status, // ARRIVED, DEPARTED, SKIPPED, UPCOMING
        Boolean isHalt,
        String platform
    ) implements Serializable {
        // Backward-compatible constructor for existing callers and tests
        public StationStatusDto(
            String stationCode,
            String stationName,
            Integer sequenceNumber,
            String scheduledArrival,
            String scheduledDeparture,
            String actualArrival,
            String actualDeparture,
            Integer delayMinutes,
            String status,
            Boolean isHalt
        ) {
            this(stationCode, stationName, sequenceNumber, scheduledArrival, scheduledDeparture,
                 actualArrival, actualDeparture, delayMinutes, status, isHalt, null);
        }
    }
}
