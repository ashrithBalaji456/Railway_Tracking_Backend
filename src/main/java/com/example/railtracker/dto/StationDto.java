package com.example.railtracker.dto;

public record StationDto(
    String stationCode,
    String stationName,
    String city,
    String state,
    Double latitude,
    Double longitude,
    Boolean active,
    Integer serialNumber,
    String oldCategory,
    String newCategory,
    String division,
    String zone,
    String district
) {
    public StationDto(String stationCode, String stationName, String city, String state, Double latitude, Double longitude, Boolean active) {
        this(stationCode, stationName, city, state, latitude, longitude, active, null, null, null, null, null, null);
    }
}

