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
    List<String> skippedStations,
    String startDate // Journey-start date (e.g. "2026-10-31")
) implements Serializable {

    // Backward-compatible constructor for 20-arg calls
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
        List<StationStatusDto> stations,
        Boolean isCancelled,
        Boolean isDiverted,
        Boolean isRescheduled,
        String disruptionMessage,
        List<String> skippedStations
    ) {
        this(trainNumber, trainName, runStatus, currentStationCode, currentStationName,
             nextStationCode, nextStationName, delayMinutes, latitude, longitude,
             speed, bearing, routeProgress, lastLocationInfo, lastUpdatedAt, stations,
             isCancelled, isDiverted, isRescheduled, disruptionMessage, skippedStations, null);
    }

    // Backward-compatible constructor for existing callers and unit tests (16 args)
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
             false, false, false, null, List.of(), null);
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
        String platform,
        String expectedArrival,
        String expectedDeparture,
        Boolean isPredicted
    ) implements Serializable {
        // Backward-compatible constructor for 11 args (with platform)
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
            Boolean isHalt,
            String platform
        ) {
            this(stationCode, stationName, sequenceNumber, scheduledArrival, scheduledDeparture,
                 actualArrival, actualDeparture, delayMinutes, status, isHalt, platform, null, null, false);
        }

        // Backward-compatible constructor for 10 args
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
                 actualArrival, actualDeparture, delayMinutes, status, isHalt, null, null, null, false);
        }
    }
}
