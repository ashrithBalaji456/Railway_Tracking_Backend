package com.example.railtracker.controller;

import com.example.railtracker.dto.ApiResponse;
import com.example.railtracker.dto.TrainDto;
import com.example.railtracker.dto.TrainLiveStatusDto;
import com.example.railtracker.dto.TrainRouteDto;
import com.example.railtracker.service.TrainService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/trains")
public class TrainController {

    private final TrainService trainService;

    public TrainController(TrainService trainService) {
        this.trainService = trainService;
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
