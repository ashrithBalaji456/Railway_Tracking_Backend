package com.example.railtracker.controller;

import com.example.railtracker.dto.ApiResponse;
import com.example.railtracker.dto.TrainExceptionDTO;
import com.example.railtracker.dto.TrainExceptionSummaryDTO;
import com.example.railtracker.service.TrainDisruptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/disruptions")
public class TrainDisruptionController {

    private final TrainDisruptionService disruptionService;

    public TrainDisruptionController(TrainDisruptionService disruptionService) {
        this.disruptionService = disruptionService;
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
}

