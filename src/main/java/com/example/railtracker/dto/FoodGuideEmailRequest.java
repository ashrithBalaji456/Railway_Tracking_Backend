package com.example.railtracker.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record FoodGuideEmailRequest(
    @NotBlank(message = "Train number is required")
    String trainNumber,

    String trainName,

    @NotBlank(message = "Recipient email is required")
    @Email(message = "Invalid email format")
    String email,

    List<FoodStopDto> stops
) {
    public record FoodStopDto(
        String stationCode,
        String stationName,
        String arrivalTime,
        Integer haltMinutes,
        String mealPhase,
        String delicacyTitle,
        String badgeEmoji,
        String bestPlatformOrStall,
        String mustTryItems
    ) {}
}
