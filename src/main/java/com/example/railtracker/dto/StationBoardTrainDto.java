package com.example.railtracker.dto;

import java.time.LocalTime;

public record StationBoardTrainDto(
    String trainNumber,
    String trainName,
    String trainType,
    String runningDays,
    String sourceStation,
    String destinationStation,
    LocalTime scheduledArrival,
    LocalTime scheduledDeparture,
    LocalTime actualArrival,
    LocalTime actualDeparture,
    Integer delayMinutes,
    String stopType, // STOPPING or PASS_THROUGH
    Boolean isHalt,
    String currentStatus,
    Integer arrivalDay,
    String platform
) {
    public StationBoardTrainDto(
        String trainNumber,
        String trainName,
        String trainType,
        String runningDays,
        String sourceStation,
        String destinationStation,
        LocalTime scheduledArrival,
        LocalTime scheduledDeparture,
        LocalTime actualArrival,
        LocalTime actualDeparture,
        Integer delayMinutes,
        String stopType,
        Boolean isHalt,
        String currentStatus,
        Integer arrivalDay
    ) {
        this(trainNumber, trainName, trainType, sourceDays(runningDays), sourceStation, destinationStation,
             scheduledArrival, scheduledDeparture, actualArrival, actualDeparture, delayMinutes,
             stopType, isHalt, currentStatus, arrivalDay, null);
    }

    private static String sourceDays(String runningDays) {
        return runningDays;
    }
}
