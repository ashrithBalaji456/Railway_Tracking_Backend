package com.example.railtracker.dto;

import java.time.LocalTime;

public record RouteStationDto(
    String stationCode,
    String stationName,
    Integer sequenceNumber,
    LocalTime arrivalTime,
    LocalTime departureTime,
    Integer arrivalDay,
    Integer departureDay,
    Integer haltMinutes,
    String stopType, // STOPPING or PASS_THROUGH
    Boolean isHalt,
    Double latitude,
    Double longitude,
    Double distance,
    Double speedToNextStationKmph
) {}
