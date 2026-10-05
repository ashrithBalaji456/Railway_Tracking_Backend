package com.example.railtracker.controller;

import com.example.railtracker.dto.ApiResponse;
import com.example.railtracker.dto.LiveStationBoardResponse;
import com.example.railtracker.service.StationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stations")
public class LiveStationBoardController {

    private final StationService stationService;

    public LiveStationBoardController(StationService stationService) {
        this.stationService = stationService;
    }

    @GetMapping("/{stationCode}/live")
    public ResponseEntity<ApiResponse<LiveStationBoardResponse>> getLiveStationBoard(
            @PathVariable("stationCode") String stationCode,
            @RequestParam(name = "hours", defaultValue = "4") int hours) {
        
        // 1. Validate stationCode is not blank
        if (stationCode == null || stationCode.trim().isEmpty()) {
            throw new com.example.railtracker.exception.BadRequestException("Station code must not be blank.");
        }
        
        // 2. Validate hours is 2, 4, 6, or 8
        if (hours != 2 && hours != 4 && hours != 6 && hours != 8) {
            throw new com.example.railtracker.exception.BadRequestException("Supported time windows are 2, 4, 6, or 8 hours.");
        }
        
        LiveStationBoardResponse response = stationService.getLiveStationBoardDetails(stationCode, hours);
        return ResponseEntity.ok(ApiResponse.success("Live station board retrieved successfully", response));
    }
}
