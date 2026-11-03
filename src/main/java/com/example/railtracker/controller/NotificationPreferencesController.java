package com.example.railtracker.controller;

import com.example.railtracker.dto.NotificationPreferencesRequest;
import com.example.railtracker.dto.NotificationPreferencesResponse;
import com.example.railtracker.entity.TravelReminder;
import com.example.railtracker.entity.UserNotificationPreferences;
import com.example.railtracker.repository.TravelReminderRepository;
import com.example.railtracker.repository.UserNotificationPreferencesRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationPreferencesController {

    private static final Logger log = LoggerFactory.getLogger(NotificationPreferencesController.class);

    private final UserNotificationPreferencesRepository preferencesRepository;
    private final TravelReminderRepository travelReminderRepository;

    public NotificationPreferencesController(
            UserNotificationPreferencesRepository preferencesRepository,
            TravelReminderRepository travelReminderRepository) {
        this.preferencesRepository = preferencesRepository;
        this.travelReminderRepository = travelReminderRepository;
    }

    @GetMapping("/preferences")
    public ResponseEntity<NotificationPreferencesResponse> getPreferences(
            @RequestParam(required = false) String email,
            Principal principal) {
        String effectiveEmail = (email != null && !email.isBlank())
                ? email.trim()
                : (principal != null ? principal.getName() : null);

        if (effectiveEmail == null || effectiveEmail.isBlank()) {
            return ResponseEntity.ok(new NotificationPreferencesResponse(
                    null,
                    "",
                    null,
                    "",
                    true,
                    15,
                    true,
                    true,
                    LocalDateTime.now()
            ));
        }

        Optional<UserNotificationPreferences> existing = preferencesRepository.findByEmailIgnoreCase(effectiveEmail);
        if (existing.isPresent()) {
            UserNotificationPreferences p = existing.get();
            return ResponseEntity.ok(toResponse(p));
        }

        // Return default settings for new email
        return ResponseEntity.ok(new NotificationPreferencesResponse(
                null,
                effectiveEmail,
                principal != null ? principal.getName() : null,
                "",
                true,
                15,
                true,
                true,
                LocalDateTime.now()
        ));
    }

    @PutMapping("/preferences")
    public ResponseEntity<NotificationPreferencesResponse> updatePreferences(
            @Valid @RequestBody NotificationPreferencesRequest req,
            Principal principal) {
        String effectiveEmail = (req.email() != null && !req.email().isBlank())
                ? req.email().trim().toLowerCase()
                : (principal != null ? principal.getName() : null);

        if (effectiveEmail == null || effectiveEmail.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        UserNotificationPreferences prefs = preferencesRepository.findByEmailIgnoreCase(effectiveEmail)
                .orElseGet(() -> {
                    UserNotificationPreferences np = new UserNotificationPreferences();
                    np.setEmail(effectiveEmail);
                    return np;
                });

        if (req.username() != null && !req.username().isBlank()) {
            prefs.setUsername(req.username().trim());
        } else if (principal != null && prefs.getUsername() == null) {
            prefs.setUsername(principal.getName());
        }

        if (req.whatsappPhone() != null) {
            prefs.setWhatsappPhone(req.whatsappPhone().trim());
        }
        if (req.weeklyDigestEnabled() != null) {
            prefs.setWeeklyDigestEnabled(req.weeklyDigestEnabled());
        }
        if (req.delayAlertThresholdMinutes() != null) {
            prefs.setDelayAlertThresholdMinutes(req.delayAlertThresholdMinutes());
        }
        if (req.travelReminderEnabled() != null) {
            prefs.setTravelReminderEnabled(req.travelReminderEnabled());
        }
        if (req.stationWakeUpAlarmEnabled() != null) {
            prefs.setStationWakeUpAlarmEnabled(req.stationWakeUpAlarmEnabled());
        }

        UserNotificationPreferences saved = preferencesRepository.save(prefs);
        log.info("Saved notification preferences for email={}", saved.getEmail());
        return ResponseEntity.ok(toResponse(saved));
    }

    @GetMapping("/active-reminders")
    public ResponseEntity<List<TravelReminder>> getActiveReminders(
            @RequestParam(required = false) String email,
            Principal principal) {
        String effectiveEmail = (email != null && !email.isBlank())
                ? email.trim()
                : (principal != null ? principal.getName() : null);

        if (effectiveEmail == null || effectiveEmail.isBlank()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<TravelReminder> list = travelReminderRepository.findByEmailOrderByTravelDateDesc(effectiveEmail);
        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/reminders/{id}")
    public ResponseEntity<Map<String, Object>> cancelReminder(
            @PathVariable Long id,
            @RequestParam(required = false) String email) {
        Optional<TravelReminder> reminderOpt = travelReminderRepository.findById(id);
        if (reminderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        TravelReminder reminder = reminderOpt.get();
        if (email != null && !email.isBlank() && !reminder.getEmail().equalsIgnoreCase(email.trim())) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Unauthorized to cancel this reminder"));
        }

        travelReminderRepository.delete(reminder);
        log.info("Cancelled travel reminder id={} for pnr={}", id, reminder.getPnr());
        return ResponseEntity.ok(Map.of("success", true, "message", "Reminder cancelled successfully"));
    }

    private NotificationPreferencesResponse toResponse(UserNotificationPreferences p) {
        return new NotificationPreferencesResponse(
                p.getId(),
                p.getEmail(),
                p.getUsername(),
                p.getWhatsappPhone() != null ? p.getWhatsappPhone() : "",
                p.getWeeklyDigestEnabled(),
                p.getDelayAlertThresholdMinutes(),
                p.getTravelReminderEnabled(),
                p.getStationWakeUpAlarmEnabled(),
                p.getUpdatedAt()
        );
    }
}

