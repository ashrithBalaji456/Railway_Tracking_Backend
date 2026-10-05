package com.example.railtracker.dto;

import java.util.List;

public record LiveStationBoardResponse(
    StationInfo station,
    WindowInfo window,
    List<LiveStationBoardTrain> trains
) {
    public record StationInfo(
        String code,
        String name,
        String city,
        Double lat,
        Double lng
    ) {}

    public record WindowInfo(
        String from,
        String to,
        Integer hours
    ) {}

    public record LiveStationBoardTrain(
        String trainNumber,
        String trainName,
        String trainType,
        StationCodeName source,
        StationCodeName destination,
        StationPassType passType,
        String scheduledArrival,
        String scheduledDeparture,
        String expectedArrival,
        String expectedDeparture,
        Integer delayMinutes,
        String platform,
        StationTrainLiveStatus liveStatus,
        String runningDays
    ) {}

    public record StationCodeName(
        String code,
        String name
    ) {}
}
