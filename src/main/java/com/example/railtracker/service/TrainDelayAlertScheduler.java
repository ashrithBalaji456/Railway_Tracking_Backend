package com.example.railtracker.service;

import com.example.railtracker.client.RailwayDataProvider;
import com.example.railtracker.dto.TrainLiveStatusDto;
import com.example.railtracker.entity.Alert;
import com.example.railtracker.repository.AlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TrainDelayAlertScheduler {

    private static final Logger log = LoggerFactory.getLogger(TrainDelayAlertScheduler.class);

    private final AlertRepository alertRepository;
    private final RailwayDataProvider railwayDataProvider;
    private final BrevoEmailService brevoEmailService;

    public TrainDelayAlertScheduler(
            AlertRepository alertRepository,
            RailwayDataProvider railwayDataProvider,
            BrevoEmailService brevoEmailService) {
        this.alertRepository = alertRepository;
        this.railwayDataProvider = railwayDataProvider;
        this.brevoEmailService = brevoEmailService;
    }

    /**
     * Checks all active delay alert subscriptions every 5 minutes.
     * First execution starts 30 seconds after application startup.
     */
    @Scheduled(initialDelay = 30000, fixedRate = 300000)
    @Transactional
    public void checkAndDispatchDelayAlerts() {
        List<Alert> activeAlerts = alertRepository.findAllActiveWithUser();
        if (activeAlerts.isEmpty()) {
            return;
        }

        log.info("[TrainDelayAlertScheduler] Checking {} active delay alert subscriptions...", activeAlerts.size());

        for (Alert alert : activeAlerts) {
            try {
                processSingleAlert(alert);
            } catch (Exception e) {
                log.warn("[TrainDelayAlertScheduler] Failed evaluating alert ID {} for train {}: {}",
                        alert.getId(), alert.getTrainNumber(), e.getMessage());
            }
        }
    }

    private void processSingleAlert(Alert alert) {
        if (alert.getUser() == null || alert.getUser().getEmail() == null || alert.getUser().getEmail().isBlank()) {
            return;
        }

        TrainLiveStatusDto liveStatus = railwayDataProvider.getTrainLiveStatus(alert.getTrainNumber());
        if (liveStatus == null) {
            return;
        }

        int currentDelay = liveStatus.delayMinutes() != null ? liveStatus.delayMinutes() : 0;
        int threshold = alert.getThresholdMinutes() != null ? alert.getThresholdMinutes() : 15;

        // If delay is strictly below threshold, do not alert
        if (currentDelay < threshold) {
            return;
        }

        // Check alert cooldown:
        // Do not re-send an email if we alerted recently (< 60 minutes ago),
        // UNLESS the train delay increased by 15+ more minutes since last alert.
        LocalDateTime lastSent = alert.getLastAlertSentAt();
        Integer lastDelay = alert.getLastAlertedDelay();

        boolean shouldAlert = false;
        if (lastSent == null) {
            shouldAlert = true;
        } else {
            long minutesSinceLast = Duration.between(lastSent, LocalDateTime.now()).toMinutes();
            if (minutesSinceLast >= 60) {
                shouldAlert = true;
            } else if (lastDelay != null && (currentDelay - lastDelay) >= 15) {
                shouldAlert = true; // Delay worsened significantly
            }
        }

        if (shouldAlert) {
            log.info("[TrainDelayAlertScheduler] Train {} is delayed by {}m (threshold: {}m). Sending alert to {}",
                    alert.getTrainNumber(), currentDelay, threshold, alert.getUser().getEmail());

            boolean sent = brevoEmailService.sendTrainDelayAlertEmail(
                    alert.getUser().getEmail(),
                    alert.getUser().getUsername(),
                    alert.getTrainNumber(),
                    alert.getTrainName(),
                    currentDelay,
                    threshold,
                    liveStatus.currentStationName(),
                    liveStatus.nextStationName(),
                    liveStatus.runStatus(),
                    liveStatus.lastLocationInfo()
            );

            if (sent) {
                alert.setLastAlertSentAt(LocalDateTime.now());
                alert.setLastAlertedDelay(currentDelay);
                alertRepository.save(alert);
            }
        }
    }
}
