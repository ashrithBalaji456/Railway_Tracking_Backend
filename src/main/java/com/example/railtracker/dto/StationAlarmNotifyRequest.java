package com.example.railtracker.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record StationAlarmNotifyRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,

    String username,

    @NotBlank(message = "Train number is required")
    String trainNumber,

    String trainName,

    @NotBlank(message = "Station code is required")
    String stationCode,

    String stationName,

    Integer etaMinutes,

    Integer distanceKm,

    String platform
) {}

