package com.example.railtracker.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record TravelReminderRequest(
    @NotBlank(message = "PNR is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "PNR must be exactly 10 numeric digits")
    String pnr,

    @NotBlank(message = "Train number is required")
    String trainNumber,

    String trainName,

    @NotNull(message = "Travel date is required")
    LocalDate travelDate,

    @NotBlank(message = "Email address is required")
    @Email(message = "Invalid email format")
    String email,

    String username
) {}
