package com.example.railtracker.dto;

public enum StationTrainLiveStatus {
    ARRIVED,
    DEPARTED,
    UPCOMING,
    SCHEDULED,
    STATUS_UNAVAILABLE,
    // Backward-compatible aliases for legacy callers
    AT_STATION,
    NOT_STARTED,
    UNKNOWN
}

