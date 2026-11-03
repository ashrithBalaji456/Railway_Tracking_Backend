package com.example.railtracker.dto;

import java.time.LocalDateTime;

public record NotificationPreferencesResponse(
    Long id,
    String email,
    String username,
    String whatsappPhone,
    boolean weeklyDigestEnabled,
    int delayAlertThresholdMinutes,
    boolean travelReminderEnabled,
    boolean stationWakeUpAlarmEnabled,
    LocalDateTime updatedAt
) {}
