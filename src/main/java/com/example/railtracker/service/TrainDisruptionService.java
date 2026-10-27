package com.example.railtracker.service;

import com.example.railtracker.dto.TrainExceptionDTO;
import com.example.railtracker.dto.TrainExceptionSummaryDTO;
import java.util.List;

public interface TrainDisruptionService {
    List<TrainExceptionDTO> getExceptions(String type);
    TrainExceptionSummaryDTO getExceptionsSummary();
}

