package com.example.railtracker.dto;

import java.time.LocalTime;

public record TrainBetweenStationsDto(
    String trainNumber,
    String trainName,
    String trainType,
    String fromStationCode,
    String toStationCode,
    LocalTime departureTime,
    LocalTime arrivalTime,
    Integer durationMinutes,
    String runningDays,
    Integer fromSequence,
    Integer toSequence
) {}
