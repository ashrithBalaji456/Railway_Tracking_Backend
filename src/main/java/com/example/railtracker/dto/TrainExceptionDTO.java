package com.example.railtracker.dto;

import java.io.Serializable;
import java.util.List;

public record TrainExceptionDTO(
    String trainNumber,
    String trainName,
    String trainType,
    String startDate,
    String sourceStationCode,
    String sourceStationName,
    String destinationStationCode,
    String destinationStationName,
    String status,
    String primaryType,
    String message,
    List<ExceptionDetail> exceptions,
    String lastUpdatedAt,
    String runningDays,
    Integer distance,
    Integer duration
) implements Serializable {
    public record ExceptionDetail(String type, String message) implements Serializable {}

    // Backward-compatible constructor
    public TrainExceptionDTO(
        String trainNumber,
        String trainName,
        String trainType,
        String startDate,
        String sourceStationCode,
        String sourceStationName,
        String destinationStationCode,
        String destinationStationName,
        String status,
        String primaryType,
        String message,
        List<ExceptionDetail> exceptions,
        String lastUpdatedAt
    ) {
        this(trainNumber, trainName, trainType, startDate, sourceStationCode, sourceStationName,
             destinationStationCode, destinationStationName, status, primaryType, message, exceptions,
             lastUpdatedAt, null, null, null);
    }
}

