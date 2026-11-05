package com.example.railtracker.dto;

import java.time.LocalTime;

public record JourneyOptionDto(
    boolean direct,
    String firstTrainNumber,
    String firstTrainName,
    String originStationCode,
    String originStationName,
    LocalTime originDepartureTime,
    String destinationStationCode,
    String destinationStationName,
    LocalTime destinationArrivalTime,
    
    // Connection fields (null if direct)
    String secondTrainNumber,
    String secondTrainName,
    String transferStationCode,
    String transferStationName,
    LocalTime transferArrivalTime,
    LocalTime transferDepartureTime,
    Integer layoverMinutes,
    String layoverWarning, // TIGHT_CONNECTION, LONG_LAYOVER, or null
    
    Integer totalDurationMinutes,
    String firstTrainRunDays,
    String secondTrainRunDays,

    // Distance fields
    Double firstLegDistanceKm,
    Double secondLegDistanceKm,
    Double totalDistanceKm
) {
    public JourneyOptionDto(
        boolean direct,
        String firstTrainNumber,
        String firstTrainName,
        String originStationCode,
        String originStationName,
        LocalTime originDepartureTime,
        String destinationStationCode,
        String destinationStationName,
        LocalTime destinationArrivalTime,
        String secondTrainNumber,
        String secondTrainName,
        String transferStationCode,
        String transferStationName,
        LocalTime transferArrivalTime,
        LocalTime transferDepartureTime,
        Integer layoverMinutes,
        String layoverWarning,
        Integer totalDurationMinutes,
        String firstTrainRunDays,
        String secondTrainRunDays
    ) {
        this(direct, firstTrainNumber, firstTrainName, originStationCode, originStationName, originDepartureTime,
             destinationStationCode, destinationStationName, destinationArrivalTime, secondTrainNumber, secondTrainName,
             transferStationCode, transferStationName, transferArrivalTime, transferDepartureTime, layoverMinutes,
             layoverWarning, totalDurationMinutes, firstTrainRunDays, secondTrainRunDays, null, null, null);
    }
}
