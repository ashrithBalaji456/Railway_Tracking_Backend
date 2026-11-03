package com.example.railtracker.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record StationBoardEmailRequest(
    @NotBlank(message = "Recipient email is required")
    @Email(message = "Invalid email format")
    String email,

    String username,

    Integer hours
) {}

