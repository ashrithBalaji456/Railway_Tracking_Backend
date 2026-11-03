package com.example.railtracker.dto;

import jakarta.validation.constraints.NotNull;

public record EmailItineraryRequest(
    String recipientEmail,
    String recipientName,
    String travelDate,
    @NotNull(message = "Journey details cannot be null")
    JourneyOptionDto journey
) {}

