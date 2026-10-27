package com.example.railtracker.dto;

import java.io.Serializable;
import java.util.List;

public record TrainFareDTO(
    String trainNumber,
    String trainName,
    String sourceStation,
    String destinationStation,
    Integer distanceKm,
    List<ClassFare> classFares
) implements Serializable {
    public List<ClassFare> fares() {
        return classFares;
    }

    public record ClassFare(
        String classCode,
        String className,
        Integer baseFare,
        Integer totalFare
    ) implements Serializable {}
}

