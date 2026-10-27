package com.example.railtracker.controller;

import com.example.railtracker.dto.*;
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

    @org.springframework.beans.factory.annotation.Autowired
    public TrainController(
            TrainService trainService,
            TrainRepository trainRepository,
            StationRepository stationRepository,
            @org.springframework.beans.factory.annotation.Autowired(required = false) DataInitializer dataInitializer) {
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
        if (dataInitializer != null) {
            dataInitializer.seedStations();
            dataInitializer.seedTrains();
            dataInitializer.seedSampleTrains();
        }
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

    @GetMapping("/between")
    public ResponseEntity<ApiResponse<List<TrainBetweenStationsDto>>> getTrainsBetween(
            @RequestParam("from") String from,
            @RequestParam("to") String to,
            @RequestParam(value = "date", required = false) String date) {
        List<TrainBetweenStationsDto> trains = trainService.getTrainsBetween(from, to, date);
        return ResponseEntity.ok(ApiResponse.success("Trains between stations retrieved successfully", trains));
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

    @GetMapping("/{trainNumber}/delay")
    public ResponseEntity<ApiResponse<TrainDelayReportDTO>> getTrainDelayReport(
            @PathVariable("trainNumber") String trainNumber) {
        TrainDelayReportDTO delayReport = trainService.getTrainDelayReport(trainNumber);
        return ResponseEntity.ok(ApiResponse.success("Train delay report retrieved successfully", delayReport));
    }

    @GetMapping("/{trainNumber}/fare")
    public ResponseEntity<ApiResponse<TrainFareDTO>> getTrainFare(
            @PathVariable("trainNumber") String trainNumber,
            @RequestParam("source") String source,
            @RequestParam("destination") String destination) {
        TrainFareDTO fare = trainService.getTrainFare(trainNumber, source, destination);
        return ResponseEntity.ok(ApiResponse.success("Train fare retrieved successfully", fare));
    }
}
