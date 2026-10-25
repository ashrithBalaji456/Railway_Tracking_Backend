package com.example.railtracker.controller;

import com.example.railtracker.dto.ApiResponse;
import com.example.railtracker.dto.TrainDto;
import com.example.railtracker.dto.TrainLiveStatusDto;
import com.example.railtracker.dto.TrainRouteDto;
import com.example.railtracker.service.TrainService;
import com.example.railtracker.config.DataInitializer;
import com.example.railtracker.repository.StationRepository;
import com.example.railtracker.repository.TrainRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/trains")
public class TrainController {

    private final TrainService trainService;
    private final TrainRepository trainRepository;
    private final StationRepository stationRepository;
    private final DataInitializer dataInitializer;

    public TrainController(
            TrainService trainService,
            TrainRepository trainRepository,
            StationRepository stationRepository,
            DataInitializer dataInitializer) {
        this.trainService = trainService;
        this.trainRepository = trainRepository;
        this.stationRepository = stationRepository;
        this.dataInitializer = dataInitializer;
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCounts() {
        long trainCount = trainRepository.count();
        long stationCount = stationRepository.count();
        Map<String, Object> data = Map.of(
                "trains", trainCount,
                "stations", stationCount
        );
        return ResponseEntity.ok(ApiResponse.success("Database record counts retrieved successfully", data));
    }

    @GetMapping("/seed")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerSeedGet() {
        return triggerSeed();
    }

    @PostMapping("/seed")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerSeed() {
        dataInitializer.seedStations();
        dataInitializer.seedTrains();
        dataInitializer.seedSampleTrains();
        long trainCount = trainRepository.count();
        long stationCount = stationRepository.count();
        Map<String, Object> data = Map.of(
                "message", "Database seeding completed",
                "trains", trainCount,
                "stations", stationCount
        );
        return ResponseEntity.ok(ApiResponse.success("Database seeding executed successfully", data));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TrainDto>>> searchTrains(@RequestParam("query") String query) {
        List<TrainDto> trains = trainService.searchTrains(query);
        return ResponseEntity.ok(ApiResponse.success("Trains retrieved successfully", trains));
    }

    @GetMapping("/{trainNumber}")
    public ResponseEntity<ApiResponse<TrainDto>> getTrainDetails(
            @PathVariable("trainNumber") String trainNumber,
            @RequestParam(value = "journeyDate", required = false) String journeyDate) {
        TrainDto train = trainService.getTrainDetails(trainNumber, journeyDate);
        return ResponseEntity.ok(ApiResponse.success("Train details retrieved successfully", train));
    }

    @GetMapping("/{trainNumber}/route")
    public ResponseEntity<ApiResponse<TrainRouteDto>> getTrainRoute(
            @PathVariable("trainNumber") String trainNumber,
            @RequestParam(value = "journeyDate", required = false) String journeyDate) {
        TrainRouteDto route = trainService.getTrainRoute(trainNumber, journeyDate);
        return ResponseEntity.ok(ApiResponse.success("Train route schedule retrieved successfully", route));
    }

    @GetMapping("/{trainNumber}/live")
    public ResponseEntity<ApiResponse<TrainLiveStatusDto>> getTrainLiveStatus(
            @PathVariable("trainNumber") String trainNumber,
            @RequestParam(value = "journeyDate", required = false) String journeyDate) {
        TrainLiveStatusDto liveStatus = trainService.getTrainLiveStatus(trainNumber, journeyDate);
        return ResponseEntity.ok(ApiResponse.success("Live train status retrieved successfully", liveStatus));
    }
}
