package com.example.railtracker.service;

import com.example.railtracker.client.RailwayDataProvider;
import com.example.railtracker.dto.TrainLiveStatusDto;
import com.example.railtracker.dto.TravelReminderRequest;
import com.example.railtracker.entity.TravelReminder;
import com.example.railtracker.repository.TravelReminderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class TravelReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(TravelReminderScheduler.class);

    private final TravelReminderRepository reminderRepository;
    private final RailwayDataProvider railwayDataProvider;
    private final BrevoEmailService brevoEmailService;

    public TravelReminderScheduler(
            TravelReminderRepository reminderRepository,
            RailwayDataProvider railwayDataProvider,
            BrevoEmailService brevoEmailService) {
        this.reminderRepository = reminderRepository;
        this.railwayDataProvider = railwayDataProvider;
        this.brevoEmailService = brevoEmailService;
    }

    /**
     * Runs daily at 06:00 AM IST to notify passengers traveling today.
     */
    @Scheduled(cron = "${app.reminder.cron:0 0 6 * * *}", zone = "Asia/Kolkata")
    @Transactional
    public void dispatchMorningTravelReminders() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        List<TravelReminder> todaysReminders = reminderRepository.findByTravelDateAndReminderSentFalse(today);

        if (todaysReminders.isEmpty()) {
            return;
        }

        log.info("[TravelReminderScheduler] Found {} travel reminders scheduled for today ({})",
                todaysReminders.size(), today);

        for (TravelReminder reminder : todaysReminders) {
            try {
                sendSingleReminder(reminder);
            } catch (Exception e) {
                log.warn("[TravelReminderScheduler] Failed dispatching reminder for PNR {}: {}",
                        reminder.getPnr(), e.getMessage());
            }
        }
    }

    private void sendSingleReminder(TravelReminder reminder) {
        String liveLocation = "Live tracking available on RailTrack";
        Integer delay = 0;

        try {
            TrainLiveStatusDto liveStatus = railwayDataProvider.getTrainLiveStatus(reminder.getTrainNumber());
            if (liveStatus != null) {
                delay = liveStatus.delayMinutes();
                if (liveStatus.currentStationName() != null) {
                    liveLocation = "Currently at " + liveStatus.currentStationName();
                } else if (liveStatus.lastLocationInfo() != null) {
                    liveLocation = liveStatus.lastLocationInfo();
                }
            }
        } catch (Exception ignored) {}

        boolean sent = brevoEmailService.sendTravelDayReminderEmail(
                reminder.getEmail(),
                reminder.getUsername(),
                reminder.getPnr(),
                reminder.getTrainNumber(),
                reminder.getTrainName(),
                reminder.getTravelDate(),
                liveLocation,
                delay
        );

        if (sent) {
            reminder.setReminderSent(true);
            reminderRepository.save(reminder);
            log.info("[TravelReminderScheduler] Dispatched travel reminder for PNR {} to {}",
                    reminder.getPnr(), reminder.getEmail());
        }
    }

    /**
     * Dispatches an instant test reminder for preview.
     */
    public boolean sendTestReminder(TravelReminderRequest request) {
        String liveLocation = "En Route (Live tracking active)";
        Integer delay = 0;

        try {
            TrainLiveStatusDto liveStatus = railwayDataProvider.getTrainLiveStatus(request.trainNumber());
            if (liveStatus != null) {
                delay = liveStatus.delayMinutes();
                if (liveStatus.currentStationName() != null) {
                    liveLocation = "Near " + liveStatus.currentStationName();
                }
            }
        } catch (Exception ignored) {}

        return brevoEmailService.sendTravelDayReminderEmail(
                request.email(),
                request.username() != null ? request.username() : "Passenger",
                request.pnr(),
                request.trainNumber(),
                request.trainName(),
                request.travelDate(),
                liveLocation,
                delay
        );
    }
}
