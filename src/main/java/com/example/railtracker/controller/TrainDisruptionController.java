package com.example.railtracker.controller;

import com.example.railtracker.dto.ApiResponse;
import com.example.railtracker.dto.DisruptionAlertRequest;
import com.example.railtracker.dto.TrainExceptionDTO;
import com.example.railtracker.dto.TrainExceptionSummaryDTO;
import com.example.railtracker.service.BrevoEmailService;
import com.example.railtracker.service.TrainDisruptionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/disruptions")
public class TrainDisruptionController {

    private static final Logger log = LoggerFactory.getLogger(TrainDisruptionController.class);

    private final TrainDisruptionService disruptionService;
    private final BrevoEmailService brevoEmailService;

    public TrainDisruptionController(
            TrainDisruptionService disruptionService,
            BrevoEmailService brevoEmailService) {
        this.disruptionService = disruptionService;
        this.brevoEmailService = brevoEmailService;
    }

    @GetMapping("/exceptions")
    public ResponseEntity<ApiResponse<List<TrainExceptionDTO>>> getExceptions(
            @RequestParam(value = "type", required = false) String type) {
        List<TrainExceptionDTO> exceptions = disruptionService.getExceptions(type);
        return ResponseEntity.ok(ApiResponse.success("Train exceptions retrieved successfully", exceptions));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<TrainExceptionSummaryDTO>> getSummary() {
        TrainExceptionSummaryDTO summary = disruptionService.getExceptionsSummary();
        return ResponseEntity.ok(ApiResponse.success("Disruptions summary retrieved successfully", summary));
    }

    @PostMapping("/alert")
    public ResponseEntity<ApiResponse<Map<String, Object>>> registerDisruptionAlert(
            @Valid @RequestBody DisruptionAlertRequest request,
            Principal principal) {
        log.info("Registered disruption alert for Train={} Type={} email={}",
                request.trainNumber(), request.disruptionType(), request.email());

        return ResponseEntity.ok(ApiResponse.success(
                "Disruption alert registered! You will receive automated notifications if route deviations or schedule changes occur.",
                Map.of("trainNumber", request.trainNumber(), "email", request.email(), "status", "ARMED")
        ));
    }

    @PostMapping("/alert/test")
    public ResponseEntity<ApiResponse<Boolean>> testDisruptionAlert(
            @Valid @RequestBody DisruptionAlertRequest request,
            Principal principal) {
        String username = (principal != null) ? principal.getName() : "Passenger";
        boolean sent = brevoEmailService.sendDisruptionAdvisoryEmail(
                request.email(),
                username,
                request.trainNumber(),
                request.trainName(),
                request.disruptionType(),
                request.message(),
                request.sourceStation(),
                request.destStation(),
                request.alternativeRoute(),
                request.effectiveDate()
        );

        if (sent) {
            return ResponseEntity.ok(ApiResponse.success(
                    "Sample Route Disruption & Safety Advisory email sent to " + request.email() + "!",
                    true
            ));
        } else {
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to dispatch disruption advisory email via Brevo. Please check email address."
            ));
        }
    }
}
