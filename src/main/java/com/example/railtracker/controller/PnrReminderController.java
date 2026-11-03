package com.example.railtracker.controller;

import com.example.railtracker.dto.ApiResponse;
import com.example.railtracker.dto.TravelReminderRequest;
import com.example.railtracker.dto.TravelReminderResponse;
import com.example.railtracker.entity.TravelReminder;
import com.example.railtracker.entity.User;
import com.example.railtracker.repository.TravelReminderRepository;
import com.example.railtracker.repository.UserRepository;
import com.example.railtracker.service.TravelReminderScheduler;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/pnr")
public class PnrReminderController {

    private final TravelReminderRepository reminderRepository;
    private final TravelReminderScheduler reminderScheduler;
    private final UserRepository userRepository;

    public PnrReminderController(
            TravelReminderRepository reminderRepository,
            TravelReminderScheduler reminderScheduler,
            UserRepository userRepository) {
        this.reminderRepository = reminderRepository;
        this.reminderScheduler = reminderScheduler;
        this.userRepository = userRepository;
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
}
