package com.example.railtracker.dto;

import java.io.Serializable;

public record TrainExceptionSummaryDTO(
    Integer total,
    Integer cancelled,
    Integer partiallyCancelled,
    Integer partialSource,
    Integer partialDestination,
    Integer diverted,
    Integer rescheduled,
    String updatedAt
) implements Serializable {}

