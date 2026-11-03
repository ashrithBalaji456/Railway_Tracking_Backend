package com.example.railtracker.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ChartAlertRequest(
    @NotBlank(message = "PNR is required")
    String pnr,

    @NotBlank(message = "Train number is required")
    String trainNumber,

    String trainName,

    @NotBlank(message = "Travel date is required")
    String travelDate,

    String departureTime,

    String chartType,

    String confirmationProbability,

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email
) {}
