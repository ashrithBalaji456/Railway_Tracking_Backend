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
    String lastUpdatedAt
) implements Serializable {
    public record ExceptionDetail(String type, String message) implements Serializable {}
}

