package com.example.railtracker.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record NotificationPreferencesRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,

    String username,

    String whatsappPhone,

    Boolean weeklyDigestEnabled,

    Integer delayAlertThresholdMinutes,

    Boolean travelReminderEnabled,

    Boolean stationWakeUpAlarmEnabled
) {}

