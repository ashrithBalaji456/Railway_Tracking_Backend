package com.example.railtracker.dto;

public record RailAiRequest(
    String prompt,
    String trainNumber
) {}
