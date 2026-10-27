package com.example.railtracker.dto;

import java.io.Serializable;

public record TrainDelayReportDTO(
    String trainNumber,
    String trainName,
    Integer avgArrivalDelayMinutes,
    Integer avgDepartureDelayMinutes,
    Double ontimePercentage,
    Integer totalRunsSampled,
    String punctualityGrade,
    String remarks
) implements Serializable {
    public Double onTimePercentage() {
        return ontimePercentage;
    }

    public Integer historicalAverageDelayMinutes() {
        return avgArrivalDelayMinutes;
    }
}

