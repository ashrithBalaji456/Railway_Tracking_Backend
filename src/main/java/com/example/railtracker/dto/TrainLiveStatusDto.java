package com.example.railtracker.dto;

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
    List<StationStatusDto> stations
) {
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
        Boolean isHalt
    ) {}
}
