package com.example.railtracker.controller;

import com.example.railtracker.dto.ApiResponse;
import com.example.railtracker.dto.RailAiRequest;
import com.example.railtracker.dto.RailAiResponse;
import com.example.railtracker.service.RailAiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
@RequestMapping("/api/v1/railai")
public class RailAiController {

    private final RailAiService railAiService;

    public RailAiController(RailAiService railAiService) {
        this.railAiService = railAiService;
    }

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<RailAiResponse>> getAiResponse(
            Principal principal,
            @RequestBody RailAiRequest request) {
        RailAiResponse response = railAiService.getAiResponse(principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
