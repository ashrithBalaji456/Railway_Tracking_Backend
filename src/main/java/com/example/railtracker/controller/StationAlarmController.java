package com.example.railtracker.controller;

import com.example.railtracker.dto.ApiResponse;
import com.example.railtracker.dto.StationAlarmNotifyRequest;
import com.example.railtracker.entity.User;
import com.example.railtracker.repository.UserRepository;
import com.example.railtracker.service.BrevoEmailService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/alarm")
public class StationAlarmController {

    private final BrevoEmailService emailService;
    private final UserRepository userRepository;

    public StationAlarmController(BrevoEmailService emailService, UserRepository userRepository) {
        this.emailService = emailService;
        this.userRepository = userRepository;
    }

    @PostMapping("/approaching")
    public ResponseEntity<ApiResponse<String>> notifyApproaching(
            @Valid @RequestBody StationAlarmNotifyRequest request,
            Principal principal) {

        String username = request.username();
        if ((username == null || username.isBlank()) && principal != null) {
            username = principal.getName();
        }

        boolean sent = emailService.sendStationApproachingAlarmEmail(
                request.email().trim(),
                username,
                request.trainNumber(),
                request.trainName(),
                request.stationCode(),
                request.stationName(),
                request.etaMinutes(),
                request.distanceKm(),
                request.platform()
        );

        return ResponseEntity.ok(ApiResponse.success(
                sent ? "Emergency station wake-up email dispatched to " + request.email() : "Alarm alert recorded",
                request.email()
        ));
    }
}
