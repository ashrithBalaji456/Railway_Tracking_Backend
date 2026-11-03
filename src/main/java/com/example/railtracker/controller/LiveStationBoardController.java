package com.example.railtracker.controller;

import com.example.railtracker.dto.ApiResponse;
import com.example.railtracker.dto.LiveStationBoardResponse;
import com.example.railtracker.dto.StationBoardEmailRequest;
import com.example.railtracker.service.BrevoEmailService;
import com.example.railtracker.service.StationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/stations")
public class LiveStationBoardController {

    private final StationService stationService;
    private final BrevoEmailService emailService;

    public LiveStationBoardController(StationService stationService, BrevoEmailService emailService) {
        this.stationService = stationService;
        this.emailService = emailService;
    }

    @GetMapping("/{stationCode}/live")
    public ResponseEntity<ApiResponse<LiveStationBoardResponse>> getLiveStationBoard(
            @PathVariable("stationCode") String stationCode,
            @RequestParam(name = "hours", defaultValue = "4") int hours,
            @RequestParam(name = "refresh", defaultValue = "false") boolean refresh) {
        
        // 1. Validate stationCode is not blank
        if (stationCode == null || stationCode.trim().isEmpty()) {
            throw new com.example.railtracker.exception.BadRequestException("Station code must not be blank.");
        }
        
        // 2. Validate hours is 2, 4, 6, or 8
        if (hours != 2 && hours != 4 && hours != 6 && hours != 8) {
            throw new com.example.railtracker.exception.BadRequestException("Supported time windows are 2, 4, 6, or 8 hours.");
        }
        
        LiveStationBoardResponse response = stationService.getLiveStationBoardDetails(stationCode, hours, refresh);
        return ResponseEntity.ok(ApiResponse.success("Live station board retrieved successfully", response));
    }

    @PostMapping("/{stationCode}/live/email")
    public ResponseEntity<ApiResponse<String>> emailStationBoard(
            @PathVariable("stationCode") String stationCode,
            @Valid @RequestBody StationBoardEmailRequest request,
            Principal principal) {

        if (stationCode == null || stationCode.trim().isEmpty()) {
            throw new com.example.railtracker.exception.BadRequestException("Station code must not be blank.");
        }

        int hours = (request.hours() != null && (request.hours() == 2 || request.hours() == 4 || request.hours() == 6 || request.hours() == 8))
                ? request.hours()
                : 4;

        LiveStationBoardResponse boardData = stationService.getLiveStationBoardDetails(stationCode, hours, false);

        String username = request.username();
        if ((username == null || username.isBlank()) && principal != null) {
            username = principal.getName();
        }

        boolean sent = emailService.sendStationBoardSnapshotEmail(request.email().trim(), username, boardData);

        return ResponseEntity.ok(ApiResponse.success(
                sent ? "Live station timetable snapshot dispatched to " + request.email() : "Snapshot generated",
                request.email()
        ));
    }
}
