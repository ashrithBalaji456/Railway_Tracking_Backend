package com.example.railtracker.controller;

import com.example.railtracker.dto.ApiResponse;
import com.example.railtracker.dto.StationBoardTrainDto;
import com.example.railtracker.dto.StationDto;
import com.example.railtracker.service.StationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/stations")
public class StationController {

    private final StationService stationService;

    public StationController(StationService stationService) {
        this.stationService = stationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<StationDto>>> searchStations(@RequestParam("query") String query) {
        List<StationDto> stations = stationService.searchStations(query);
        return ResponseEntity.ok(ApiResponse.success("Stations retrieved successfully", stations));
    }

    @GetMapping("/{stationCode}")
    public ResponseEntity<ApiResponse<StationDto>> getStationDetails(@PathVariable("stationCode") String stationCode) {
        StationDto station = stationService.getStationDetails(stationCode);
        return ResponseEntity.ok(ApiResponse.success("Station details retrieved successfully", station));
    }

    @GetMapping("/{stationCode}/trains")
    public ResponseEntity<ApiResponse<List<StationBoardTrainDto>>> getStationTrains(
            @PathVariable("stationCode") String stationCode,
            @RequestParam(value = "type", defaultValue = "ALL") String type) {
        List<StationBoardTrainDto> trains = stationService.getStationTrains(stationCode, type);
        return ResponseEntity.ok(ApiResponse.success("Station board trains retrieved successfully", trains));
    }

    @GetMapping("/{stationCode}/live")
    public ResponseEntity<ApiResponse<List<StationBoardTrainDto>>> getStationLiveBoard(
            @PathVariable("stationCode") String stationCode,
            @RequestParam(value = "hoursAhead", defaultValue = "2") int hoursAhead,
            @RequestParam(value = "type", defaultValue = "ALL") String type) {
        List<StationBoardTrainDto> trains = stationService.getStationLiveBoard(stationCode, hoursAhead, type);
        return ResponseEntity.ok(ApiResponse.success("Live station board retrieved successfully", trains));
    }
}
