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
    private final com.example.railtracker.service.BrevoEmailService brevoEmailService;

    @org.springframework.beans.factory.annotation.Autowired
    public TrainController(
            TrainService trainService,
            TrainRepository trainRepository,
            StationRepository stationRepository,
            @org.springframework.beans.factory.annotation.Autowired(required = false) DataInitializer dataInitializer,
            com.example.railtracker.service.BrevoEmailService brevoEmailService) {
        this.trainService = trainService;
        this.trainRepository = trainRepository;
        this.stationRepository = stationRepository;
        this.dataInitializer = dataInitializer;
        this.brevoEmailService = brevoEmailService;
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
            @RequestParam(value = "journeyDate", required = false) String journeyDate,
            @RequestParam(value = "date", required = false) String date) {
        String effectiveDate = (journeyDate != null && !journeyDate.isBlank()) ? journeyDate : date;
        TrainRouteDto route = trainService.getTrainRoute(trainNumber, effectiveDate);
        return ResponseEntity.ok(ApiResponse.success("Train route schedule retrieved successfully", route));
    }

    @GetMapping("/{trainNumber}/live")
    public ResponseEntity<ApiResponse<TrainLiveStatusDto>> getTrainLiveStatus(
            @PathVariable("trainNumber") String trainNumber,
            @RequestParam(value = "journeyDate", required = false) String journeyDate,
            @RequestParam(value = "date", required = false) String date) {
        String effectiveDate = (journeyDate != null && !journeyDate.isBlank()) ? journeyDate : date;
        TrainLiveStatusDto liveStatus = trainService.getTrainLiveStatus(trainNumber, effectiveDate);
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

    @PostMapping("/{trainNumber}/food-guide/email")
    public ResponseEntity<ApiResponse<Boolean>> emailFoodGuide(
            @PathVariable("trainNumber") String trainNumber,
            @jakarta.validation.Valid @RequestBody com.example.railtracker.dto.FoodGuideEmailRequest request,
            java.security.Principal principal) {
        String username = (principal != null) ? principal.getName() : "Traveler";
        boolean sent = brevoEmailService.sendRouteFoodGuideEmail(
                request.email(),
                username,
                trainNumber,
                request.trainName(),
                request.stops()
        );

        if (sent) {
            return ResponseEntity.ok(ApiResponse.success(
                    "Food on Track journey meal guide sent to " + request.email() + "!",
                    true
            ));
        } else {
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to dispatch food guide email. Please check your Brevo configuration or recipient email."
            ));
        }
    }
}
