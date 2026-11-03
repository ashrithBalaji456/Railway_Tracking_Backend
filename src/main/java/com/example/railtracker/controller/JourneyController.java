package com.example.railtracker.controller;

import com.example.railtracker.dto.ApiResponse;
import com.example.railtracker.dto.EmailItineraryRequest;
import com.example.railtracker.dto.JourneyOptionDto;
import com.example.railtracker.dto.JourneyOverlayDTO;
import com.example.railtracker.entity.User;
import com.example.railtracker.exception.BadRequestException;
import com.example.railtracker.repository.UserRepository;
import com.example.railtracker.service.BrevoEmailService;
import com.example.railtracker.service.JourneyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/journey")
public class JourneyController {

    private final JourneyService journeyService;
    private final UserRepository userRepository;
    private final BrevoEmailService brevoEmailService;

    public JourneyController(
            JourneyService journeyService,
            UserRepository userRepository,
            BrevoEmailService brevoEmailService) {
        this.journeyService = journeyService;
        this.userRepository = userRepository;
        this.brevoEmailService = brevoEmailService;
    }

    @GetMapping("/plan")
    public ResponseEntity<ApiResponse<List<JourneyOptionDto>>> planJourney(
            @RequestParam String source,
            @RequestParam String destination,
            @RequestParam(required = false) String date) {
        List<JourneyOptionDto> plan = journeyService.planJourney(source, destination, date);
        return ResponseEntity.ok(ApiResponse.success(plan));
    }

    @PostMapping("/overlay")
    public ResponseEntity<ApiResponse<JourneyOverlayDTO>> getJourneyOverlay(@RequestBody Object requestBody) {
        JourneyOverlayDTO overlay = journeyService.getJourneyOverlay(requestBody);
        return ResponseEntity.ok(ApiResponse.success("Journey delay overlay retrieved successfully", overlay));
    }

    @PostMapping("/email-itinerary")
    public ResponseEntity<ApiResponse<String>> emailJourneyItinerary(
            @Valid @RequestBody EmailItineraryRequest request,
            Principal principal) {

        String targetEmail = request.recipientEmail();
        String targetName = request.recipientName();

        // If recipient email is not provided, resolve from authenticated user session
        if ((targetEmail == null || targetEmail.isBlank()) && principal != null) {
            targetEmail = userRepository.findByUsername(principal.getName())
                    .map(User::getEmail)
                    .orElse(null);
            if (targetName == null || targetName.isBlank()) {
                targetName = principal.getName();
            }
        }

        if (targetEmail == null || targetEmail.isBlank()) {
            throw new BadRequestException("Recipient email address is required");
        }

        boolean sent = brevoEmailService.sendJourneyItineraryEmail(
                targetEmail.trim().toLowerCase(),
                targetName,
                request.travelDate(),
                request.journey()
        );

        return ResponseEntity.ok(ApiResponse.success(
                sent ? "Itinerary has been sent to " + targetEmail : "Itinerary processed (simulated in development mode)",
                targetEmail
        ));
    }
}
