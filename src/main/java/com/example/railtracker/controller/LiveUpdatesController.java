package com.example.railtracker.controller;

import com.example.railtracker.dto.TrainLiveStatusDto;
import com.example.railtracker.service.TrainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/live-updates")
public class LiveUpdatesController {

    private static final Logger logger = LoggerFactory.getLogger(LiveUpdatesController.class);
    private final TrainService trainService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public LiveUpdatesController(TrainService trainService) {
        this.trainService = trainService;
    }

    @GetMapping(value = "/{trainNumber}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamLiveUpdates(
            @PathVariable String trainNumber,
            @RequestParam(value = "journeyDate", required = false) String journeyDate) {
        logger.info("Client connected to stream live updates for train: {} with journeyDate: {}", trainNumber, journeyDate);
        
        // Timeout set to 5 minutes (300,000 ms)
        SseEmitter emitter = new SseEmitter(300000L);
        
        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> {
            try {
                TrainLiveStatusDto status = trainService.getTrainLiveStatus(trainNumber, journeyDate);
                emitter.send(SseEmitter.event()
                        .name("live-update")
                        .data(status)
                );
            } catch (IOException e) {
                logger.warn("SSE Emitter send failed, client probably disconnected: {}", e.getMessage());
                emitter.complete();
            } catch (Exception e) {
                logger.error("Error generating live telemetry for stream: {}", e.getMessage());
                try {
                    emitter.send(SseEmitter.event().name("error").data("Telemetry error: " + e.getMessage()));
                } catch (IOException ignored) {}
            }
        }, 0, 10, TimeUnit.SECONDS);

        // Cancel task on completion or timeout
        Runnable cleanup = () -> {
            logger.info("Closing SSE stream for train: {}", trainNumber);
            task.cancel(true);
        };
        
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(ex -> cleanup.run());

        return emitter;
    }
}
