package com.example.railtracker.controller;

import com.example.railtracker.dto.ApiResponse;
import com.example.railtracker.dto.ChartAlertRequest;
import com.example.railtracker.dto.TravelReminderRequest;
import com.example.railtracker.dto.TravelReminderResponse;
import com.example.railtracker.entity.TravelReminder;
import com.example.railtracker.entity.User;
import com.example.railtracker.repository.TravelReminderRepository;
import com.example.railtracker.repository.UserRepository;
import com.example.railtracker.service.BrevoEmailService;
import com.example.railtracker.service.TravelReminderScheduler;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/pnr")
public class PnrReminderController {

    private static final Logger log = LoggerFactory.getLogger(PnrReminderController.class);

    private final TravelReminderRepository reminderRepository;
    private final TravelReminderScheduler reminderScheduler;
    private final UserRepository userRepository;
    private final BrevoEmailService brevoEmailService;

    public PnrReminderController(
            TravelReminderRepository reminderRepository,
            TravelReminderScheduler reminderScheduler,
            UserRepository userRepository,
            BrevoEmailService brevoEmailService) {
        this.reminderRepository = reminderRepository;
        this.reminderScheduler = reminderScheduler;
        this.userRepository = userRepository;
        this.brevoEmailService = brevoEmailService;
    }

    @PostMapping("/reminder")
    public ResponseEntity<ApiResponse<TravelReminderResponse>> scheduleReminder(
            @Valid @RequestBody TravelReminderRequest request,
            Principal principal) {

        String username = request.username();
        if ((username == null || username.isBlank()) && principal != null) {
            username = principal.getName();
        }

        // Avoid duplicate active reminders for same PNR and email
        TravelReminder reminder = reminderRepository.findByPnrAndEmail(request.pnr(), request.email().trim().toLowerCase())
                .orElse(new TravelReminder());

        reminder.setPnr(request.pnr());
        reminder.setTrainNumber(request.trainNumber());
        reminder.setTrainName(request.trainName());
        reminder.setTravelDate(request.travelDate());
        reminder.setEmail(request.email().trim().toLowerCase());
        reminder.setUsername(username);
        reminder.setReminderSent(false);

        TravelReminder saved = reminderRepository.save(reminder);

        TravelReminderResponse response = new TravelReminderResponse(
                saved.getId(),
                saved.getPnr(),
                saved.getTrainNumber(),
                saved.getTrainName(),
                saved.getTravelDate(),
                saved.getEmail(),
                saved.getReminderSent(),
                saved.getCreatedAt()
        );

        return ResponseEntity.ok(ApiResponse.success(
                "Travel reminder scheduled for morning of " + saved.getTravelDate(),
                response
        ));
    }

    @PostMapping("/reminder/test")
    public ResponseEntity<ApiResponse<String>> testReminder(
            @Valid @RequestBody TravelReminderRequest request) {

        boolean sent = reminderScheduler.sendTestReminder(request);
        return ResponseEntity.ok(ApiResponse.success(
                sent ? "Sample travel reminder email dispatched to " + request.email() : "Sample reminder processed (mock/dev)",
                request.email()
        ));
    }

    @GetMapping("/reminders")
    public ResponseEntity<ApiResponse<List<TravelReminderResponse>>> getUserReminders(
            @RequestParam(required = false) String email,
            Principal principal) {

        String targetEmail = email;
        if ((targetEmail == null || targetEmail.isBlank()) && principal != null) {
            targetEmail = userRepository.findByUsername(principal.getName())
                    .map(User::getEmail)
                    .orElse(null);
        }

        if (targetEmail == null || targetEmail.isBlank()) {
            return ResponseEntity.ok(ApiResponse.success(List.of()));
        }

        List<TravelReminderResponse> list = reminderRepository.findByEmailOrderByTravelDateDesc(targetEmail.trim().toLowerCase())
                .stream()
                .map(r -> new TravelReminderResponse(
                        r.getId(),
                        r.getPnr(),
                        r.getTrainNumber(),
                        r.getTrainName(),
                        r.getTravelDate(),
                        r.getEmail(),
                        r.getReminderSent(),
                        r.getCreatedAt()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PostMapping("/chart-alert")
    public ResponseEntity<ApiResponse<Map<String, Object>>> scheduleChartAlert(
            @Valid @RequestBody ChartAlertRequest request,
            Principal principal) {
        log.info("Registered IRCTC chart preparation alert for PNR={} Train={} email={}",
                request.pnr(), request.trainNumber(), request.email());

        // Also ensure a travel reminder record is persisted if travelDate is present
        try {
            TravelReminder reminder = reminderRepository.findByPnrAndEmail(request.pnr(), request.email().trim().toLowerCase())
                    .orElse(new TravelReminder());
            reminder.setPnr(request.pnr());
            reminder.setTrainNumber(request.trainNumber());
            if (request.trainName() != null && !request.trainName().isBlank()) {
                reminder.setTrainName(request.trainName().trim());
            }
            if (request.travelDate() != null && !request.travelDate().isBlank()) {
                reminder.setTravelDate(java.time.LocalDate.parse(request.travelDate()));
            }
            reminder.setEmail(request.email().trim().toLowerCase());
            reminder.setUsername(principal != null ? principal.getName() : "Passenger");
            reminderRepository.save(reminder);
        } catch (Exception e) {
            log.warn("Could not upsert reminder alongside chart alert: {}", e.getMessage());
        }

        return ResponseEntity.ok(ApiResponse.success(
                "Chart alert registered! You will receive an automated notification as soon as IRCTC finalizes charting.",
                Map.of("pnr", request.pnr(), "email", request.email(), "scheduled", true)
        ));
    }

    @PostMapping("/chart-alert/test")
    public ResponseEntity<ApiResponse<Boolean>> testChartAlert(
            @Valid @RequestBody ChartAlertRequest request,
            Principal principal) {
        String username = (principal != null) ? principal.getName() : "Passenger";
        boolean sent = brevoEmailService.sendChartPreparationEmail(
                request.email(),
                username,
                request.pnr(),
                request.trainNumber(),
                request.trainName(),
                request.chartType() != null ? request.chartType() : "Chart 1 (First Reservation Chart)",
                request.departureTime(),
                request.travelDate(),
                request.confirmationProbability()
        );

        if (sent) {
            return ResponseEntity.ok(ApiResponse.success(
                    "Sample IRCTC Chart Preparation Notification sent to " + request.email() + "!",
                    true
            ));
        } else {
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to dispatch chart alert via Brevo. Please check your Brevo configuration or recipient email."
            ));
        }
    }
}

