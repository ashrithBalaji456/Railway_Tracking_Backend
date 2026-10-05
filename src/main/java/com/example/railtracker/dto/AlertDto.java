package com.example.railtracker.dto;

public record AlertDto(
    Long id,
    String trainNumber,
    String trainName,
    Integer thresholdMinutes,
    Boolean active,
    Boolean triggered,
    Integer currentDelay
) {}
