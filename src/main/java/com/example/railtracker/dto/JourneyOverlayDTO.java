package com.example.railtracker.dto;

import java.io.Serializable;
import java.util.List;

public record JourneyOverlayDTO(
    List<LegDelay> delays
) implements Serializable {
    public record LegDelay(
        String train,
        String station,
        Integer dayOfWeek,
        Integer arrivalDelayMinutes,
        Integer departureDelayMinutes,
        Integer samples,
        Integer maxDelayMinutes,
        Integer ontimeCount
    ) implements Serializable {}
}

