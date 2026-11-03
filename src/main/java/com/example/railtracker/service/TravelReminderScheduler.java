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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import java.util.Locale;

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
                sendDepartureReminder(reminder);
            } catch (Exception e) {
                log.warn("[TravelReminderScheduler] Failed dispatching reminder for PNR {}: {}",
                        reminder.getPnr(), e.getMessage());
            }
        }
    }

    /**
     * Periodic dynamic check (every 15 minutes) to evaluate dynamic departure
     * wake-up alarms
     * based on: (Scheduled Departure + Live Delay) - Advance Warning Minutes.
     */
    @Scheduled(cron = "${app.reminder.dynamic-cron:0 */15 * * * *}", zone = "Asia/Kolkata")
    @Transactional
    public void dispatchDynamicDepartureAlarms() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        List<TravelReminder> todaysReminders = reminderRepository.findByTravelDateAndReminderSentFalse(today);

        if (todaysReminders.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));

        for (TravelReminder reminder : todaysReminders) {
            try {
                if (shouldTriggerAlarmNow(reminder, now)) {
                    sendDepartureReminder(reminder);
                }
            } catch (Exception e) {
                log.warn("[TravelReminderScheduler] Dynamic check error for PNR {}: {}",
                        reminder.getPnr(), e.getMessage());
            }
        }
    }

    private boolean shouldTriggerAlarmNow(TravelReminder reminder, LocalDateTime now) {
        if (reminder.getDepartureTime() == null || reminder.getDepartureTime().isBlank()) {
            // No explicit departure time configured; will be handled by the 06:00 AM job
            return false;
        }

        LocalTime parsedTime = parseTimeSafe(reminder.getDepartureTime());
        if (parsedTime == null) {
            return false;
        }

        LocalDateTime scheduledDeparture = reminder.getTravelDate().atTime(parsedTime);

        // Fetch live train delay if possible
        int delayMinutes = 0;
        try {
            TrainLiveStatusDto liveStatus = railwayDataProvider.getTrainLiveStatus(reminder.getTrainNumber());
            if (liveStatus != null && liveStatus.delayMinutes() != null) {
                delayMinutes = Math.max(0, liveStatus.delayMinutes());
                if (liveStatus.platform() != null && !liveStatus.platform().isBlank()) {
                    reminder.setPlatform(liveStatus.platform());
                }
            }
        } catch (Exception ignored) {
        }

        LocalDateTime expectedDeparture = scheduledDeparture.plusMinutes(delayMinutes);
        int advanceMinutes = reminder.getAdvanceMinutes() != null ? reminder.getAdvanceMinutes() : 150;
        LocalDateTime alertTargetTime = expectedDeparture.minusMinutes(advanceMinutes);

        // Trigger if current time has reached or passed alertTargetTime (with a 20-min
        // tolerance window)
        return (now.isEqual(alertTargetTime) || now.isAfter(alertTargetTime.minusMinutes(5)))
                && now.isBefore(expectedDeparture);
    }

    private void sendDepartureReminder(TravelReminder reminder) {
        String liveLocation = "Live tracking available on RailTrack";
        Integer delay = 0;
        String platform = reminder.getPlatform() != null ? reminder.getPlatform() : "TBA";

        try {
            TrainLiveStatusDto liveStatus = railwayDataProvider.getTrainLiveStatus(reminder.getTrainNumber());
            if (liveStatus != null) {
                delay = liveStatus.delayMinutes() != null ? liveStatus.delayMinutes() : 0;
                if (liveStatus.platform() != null && !liveStatus.platform().isBlank()) {
                    platform = liveStatus.platform();
                    reminder.setPlatform(platform);
                }
                if (liveStatus.currentStationName() != null) {
                    liveLocation = "Currently at " + liveStatus.currentStationName();
                } else if (liveStatus.lastLocationInfo() != null) {
                    liveLocation = liveStatus.lastLocationInfo();
                }
            }
        } catch (Exception ignored) {
        }

        String scheduledTime = reminder.getDepartureTime() != null ? reminder.getDepartureTime() : "Morning";
        String expectedTime = scheduledTime;

        LocalTime parsed = parseTimeSafe(reminder.getDepartureTime());
        if (parsed != null && delay > 0) {
            expectedTime = parsed.plusMinutes(delay).format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH));
        }

        boolean sent;
        if (reminder.getBoardingStation() != null && !reminder.getBoardingStation().isBlank()) {
            sent = brevoEmailService.sendDepartureWakeUpEmail(
                    reminder.getEmail(),
                    reminder.getUsername(),
                    reminder.getPnr(),
                    reminder.getTrainNumber(),
                    reminder.getTrainName(),
                    reminder.getTravelDate(),
                    reminder.getBoardingStation(),
                    platform,
                    scheduledTime,
                    expectedTime,
                    delay,
                    reminder.getAdvanceMinutes() != null ? reminder.getAdvanceMinutes() : 150);
        } else {
            sent = brevoEmailService.sendTravelDayReminderEmail(
                    reminder.getEmail(),
                    reminder.getUsername(),
                    reminder.getPnr(),
                    reminder.getTrainNumber(),
                    reminder.getTrainName(),
                    reminder.getTravelDate(),
                    liveLocation,
                    delay);
        }

        if (sent) {
            reminder.setReminderSent(true);
            reminderRepository.save(reminder);
            log.info("[TravelReminderScheduler] Dispatched departure wake-up reminder for PNR {} to {}",
                    reminder.getPnr(), reminder.getEmail());
        }
    }

    /**
     * Dispatches an instant test reminder for preview.
     */
    public boolean sendTestReminder(TravelReminderRequest request) {
        String liveLocation = "En Route (Live tracking active)";
        Integer delay = 0;
        String platform = "3";

        try {
            TrainLiveStatusDto liveStatus = railwayDataProvider.getTrainLiveStatus(request.trainNumber());
            if (liveStatus != null) {
                delay = liveStatus.delayMinutes() != null ? liveStatus.delayMinutes() : 0;
                if (liveStatus.platform() != null && !liveStatus.platform().isBlank()) {
                    platform = liveStatus.platform();
                }
                if (liveStatus.currentStationName() != null) {
                    liveLocation = "Near " + liveStatus.currentStationName();
                }
            }
        } catch (Exception ignored) {
        }

        String boardingStation = request.boardingStation() != null && !request.boardingStation().isBlank()
                ? request.boardingStation().trim()
                : "New Delhi (NDLS)";
        String scheduledTime = request.departureTime() != null && !request.departureTime().isBlank()
                ? request.departureTime().trim()
                : "05:15 AM";
        int advanceMinutes = request.advanceMinutes() != null ? request.advanceMinutes() : 150;

        LocalTime parsed = parseTimeSafe(scheduledTime);
        String expectedTime = scheduledTime;
        if (parsed != null && delay > 0) {
            expectedTime = parsed.plusMinutes(delay).format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH));
        } else if (delay > 0) {
            expectedTime = "Delayed by " + delay + " mins";
        }

        return brevoEmailService.sendDepartureWakeUpEmail(
                request.email(),
                request.username() != null ? request.username() : "Passenger",
                request.pnr(),
                request.trainNumber(),
                request.trainName(),
                request.travelDate(),
                boardingStation,
                platform,
                scheduledTime,
                expectedTime,
                delay,
                advanceMinutes);
    }

    private LocalTime parseTimeSafe(String timeStr) {
        if (timeStr == null || timeStr.isBlank())
            return null;
        String clean = timeStr.trim();
        try {
            return LocalTime.parse(clean, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e1) {
            try {
                return LocalTime.parse(clean.toUpperCase(), new DateTimeFormatterBuilder()
                        .parseCaseInsensitive()
                        .appendPattern("h:mm a")
                        .toFormatter(Locale.ENGLISH));
            } catch (Exception e2) {
                try {
                    return LocalTime.parse(clean.toUpperCase(), new DateTimeFormatterBuilder()
                            .parseCaseInsensitive()
                            .appendPattern("hh:mm a")
                            .toFormatter(Locale.ENGLISH));
                } catch (Exception e3) {
                    return null;
                }
            }
        }
    }
}
