package com.example.railtracker.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record DisruptionAlertRequest(
    @NotBlank(message = "Train number is required")
    String trainNumber,

    String trainName,

    @NotBlank(message = "Disruption type is required")
    String disruptionType,

    String message,

    String sourceStation,

    String destStation,

    String alternativeRoute,

    String effectiveDate,

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email
) {}
