package com.example.railtracker.dto;

public record TrainDto(
    String trainNumber,
    String trainName,
    String trainType,
    String category,
    String sourceStation,
    String destinationStation,
    Integer distance,
    Integer duration,
    String runningDays,
    Boolean active,
    String coachPosition
) {}
