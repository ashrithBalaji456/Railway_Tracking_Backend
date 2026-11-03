package com.example.railtracker.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TravelReminderResponse(
    Long id,
    String pnr,
    String trainNumber,
    String trainName,
    LocalDate travelDate,
    String email,
    Boolean reminderSent,
    LocalDateTime createdAt
) {}

