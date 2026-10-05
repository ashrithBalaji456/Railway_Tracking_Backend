package com.example.railtracker.dto;

public record AlertRequest(
    String trainNumber,
    String trainName,
    Integer thresholdMinutes
) {}
