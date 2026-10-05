package com.example.railtracker.controller;

import com.example.railtracker.dto.ApiResponse;
import com.example.railtracker.dto.JourneyOptionDto;
import com.example.railtracker.service.JourneyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/journey")
public class JourneyController {

    private final JourneyService journeyService;

    public JourneyController(JourneyService journeyService) {
        this.journeyService = journeyService;
    }

    @GetMapping("/plan")
    public ResponseEntity<ApiResponse<List<JourneyOptionDto>>> planJourney(
            @RequestParam String source,
            @RequestParam String destination,
            @RequestParam(required = false) String date) {
        List<JourneyOptionDto> plan = journeyService.planJourney(source, destination, date);
        return ResponseEntity.ok(ApiResponse.success(plan));
    }
}
