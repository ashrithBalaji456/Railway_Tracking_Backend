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
    LocalDateTime createdAt,
    String boardingStation,
    Integer advanceMinutes,
    String departureTime,
    String platform
) {
    public TravelReminderResponse(
        Long id,
        String pnr,
        String trainNumber,
        String trainName,
        LocalDate travelDate,
        String email,
        Boolean reminderSent,
        LocalDateTime createdAt
    ) {
        this(id, pnr, trainNumber, trainName, travelDate, email, reminderSent, createdAt, null, 150, null, null);
    }
}
