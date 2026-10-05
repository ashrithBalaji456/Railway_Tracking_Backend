package com.example.railtracker.dto;

import java.util.List;

public record TrainRouteDto(
    String trainNumber,
    String trainName,
    String trainType,
    String sourceStation,
    String destinationStation,
    Integer distance,
    Integer duration,
    String runningDays,
    String coachPosition,
    List<RouteStationDto> stations
) {}
