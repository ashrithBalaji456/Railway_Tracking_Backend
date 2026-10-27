package com.example.railtracker.service.impl;

import com.example.railtracker.client.RailRadarClient;
import com.example.railtracker.dto.TrainExceptionDTO;
import com.example.railtracker.dto.TrainExceptionSummaryDTO;
import com.example.railtracker.service.TrainDisruptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class TrainDisruptionServiceImpl implements TrainDisruptionService {

    private static final Logger logger = LoggerFactory.getLogger(TrainDisruptionServiceImpl.class);

    private final RailRadarClient railRadarClient;

    public TrainDisruptionServiceImpl(RailRadarClient railRadarClient) {
        this.railRadarClient = railRadarClient;
    }

    @Override
    @Cacheable(value = "train:exceptions", key = "#type != null ? #type : 'ALL'")
    public List<TrainExceptionDTO> getExceptions(String type) {
        logger.debug("Retrieving train exceptions for type: {}", type);
        return railRadarClient.getTrainExceptions(type);
    }

    @Override
    @Cacheable(value = "train:exceptions:summary")
    public TrainExceptionSummaryDTO getExceptionsSummary() {
        logger.debug("Retrieving train exceptions summary");
        return railRadarClient.getTrainExceptionsSummary();
    }
}

