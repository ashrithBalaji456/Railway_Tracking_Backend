package com.example.railtracker.service.impl;

import com.example.railtracker.client.RailwayDataProvider;
import com.example.railtracker.dto.AlertDto;
import com.example.railtracker.dto.AlertRequest;
import com.example.railtracker.dto.TrainLiveStatusDto;
import com.example.railtracker.entity.Alert;
import com.example.railtracker.entity.User;
import com.example.railtracker.exception.BadRequestException;
import com.example.railtracker.exception.ResourceNotFoundException;
import com.example.railtracker.repository.AlertRepository;
import com.example.railtracker.repository.UserRepository;
import com.example.railtracker.service.AlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AlertServiceImpl implements AlertService {

    private static final Logger logger = LoggerFactory.getLogger(AlertServiceImpl.class);

    private final AlertRepository alertRepository;
    private final UserRepository userRepository;
    private final RailwayDataProvider railwayDataProvider;

    public AlertServiceImpl(AlertRepository alertRepository, UserRepository userRepository, RailwayDataProvider railwayDataProvider) {
        this.alertRepository = alertRepository;
        this.userRepository = userRepository;
        this.railwayDataProvider = railwayDataProvider;
    }

    @Override
    @Transactional
    public AlertDto setAlertThreshold(String username, AlertRequest request) {
        logger.debug("User '{}' setting alert threshold: {} min for train {}", username, request.thresholdMinutes(), request.trainNumber());

        if (request.thresholdMinutes() <= 0) {
            throw new BadRequestException("Threshold minutes must be greater than 0");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // Check if an alert config already exists for this train
        Optional<Alert> existingOpt = alertRepository.findByUserUsernameAndTrainNumber(username, request.trainNumber());
        Alert alert;
        if (existingOpt.isPresent()) {
            alert = existingOpt.get();
            alert.setThresholdMinutes(request.thresholdMinutes());
            alert.setActive(true);
        } else {
            alert = new Alert(user, request.trainNumber(), request.trainName(), request.thresholdMinutes());
        }

        Alert saved = alertRepository.save(alert);
        
        // Fetch current live metrics to provide immediate response feedback
        int currentDelay = 0;
        try {
            TrainLiveStatusDto liveStatus = railwayDataProvider.getTrainLiveStatus(request.trainNumber());
            currentDelay = liveStatus.delayMinutes();
        } catch (Exception e) {
            logger.warn("Could not retrieve live delay for train {} during alert setup. Defaulting to 0.", request.trainNumber());
        }

        boolean triggered = currentDelay >= saved.getThresholdMinutes();
        return new AlertDto(
                saved.getId(),
                saved.getTrainNumber(),
                saved.getTrainName(),
                saved.getThresholdMinutes(),
                saved.getActive(),
                triggered,
                currentDelay
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertDto> getActiveAlerts(String username) {
        logger.debug("Retrieving active alerts for user '{}'", username);
        List<Alert> alerts = alertRepository.findByUserUsername(username);

        return alerts.stream()
                .map(alert -> {
                    int currentDelay = 0;
                    try {
                        TrainLiveStatusDto liveStatus = railwayDataProvider.getTrainLiveStatus(alert.getTrainNumber());
                        currentDelay = liveStatus.delayMinutes();
                    } catch (Exception e) {
                        logger.warn("Could not retrieve live delay for train {} during list. Defaulting to 0.", alert.getTrainNumber());
                    }
                    boolean triggered = currentDelay >= alert.getThresholdMinutes();
                    return new AlertDto(
                            alert.getId(),
                            alert.getTrainNumber(),
                            alert.getTrainName(),
                            alert.getThresholdMinutes(),
                            alert.getActive(),
                            triggered,
                            currentDelay
                    );
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void removeAlert(String username, Long alertId) {
        logger.debug("User '{}' removing alert: {}", username, alertId);

        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert setting not found"));

        // Security ownership validation
        if (!alert.getUser().getUsername().equalsIgnoreCase(username)) {
            throw new BadRequestException("You do not have permission to delete this alert setting");
        }

        alertRepository.delete(alert);
    }
}
