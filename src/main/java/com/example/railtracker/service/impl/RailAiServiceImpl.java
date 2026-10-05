package com.example.railtracker.service.impl;

import com.example.railtracker.dto.RailAiRequest;
import com.example.railtracker.dto.RailAiResponse;
import com.example.railtracker.dto.TrainLiveStatusDto;
import com.example.railtracker.dto.TrainRouteDto;
import com.example.railtracker.service.RailAiService;
import com.example.railtracker.service.TrainService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Map;

@Service
public class RailAiServiceImpl implements RailAiService {

    private static final Logger logger = LoggerFactory.getLogger(RailAiServiceImpl.class);

    private final TrainService trainService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    public RailAiServiceImpl(TrainService trainService, ObjectMapper objectMapper) {
        this.trainService = trainService;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public RailAiResponse getAiResponse(String username, RailAiRequest request) {
        logger.debug("Generating RailAI response for user '{}', trainContext={}", username, request.trainNumber());

        // 1. Gather context
        StringBuilder contextBuilder = new StringBuilder();
        TrainLiveStatusDto liveStatus = null;
        TrainRouteDto route = null;

        if (request.trainNumber() != null && !request.trainNumber().trim().isEmpty()) {
            try {
                liveStatus = trainService.getTrainLiveStatus(request.trainNumber());
                route = trainService.getTrainRoute(request.trainNumber());

                contextBuilder.append(String.format(
                        "Context (Live Train Data):\n" +
                        "Train: %s (No. %s)\n" +
                        "Current status: %s\n" +
                        "Current live delay: %d minutes late\n" +
                        "Last known coordinates: Lat %f, Lng %f\n" +
                        "Last location: %s\n" +
                        "Next station stop: %s (%s)\n" +
                        "Route path: from %s to %s. Total distance: %d km.\n",
                        liveStatus.trainName(), liveStatus.trainNumber(),
                        liveStatus.runStatus(), liveStatus.delayMinutes(),
                        liveStatus.latitude(), liveStatus.longitude(),
                        liveStatus.lastLocationInfo(), liveStatus.nextStationName(), liveStatus.nextStationCode(),
                        route != null ? route.sourceStation() : "Unknown", route != null ? route.destinationStation() : "Unknown",
                        route != null ? route.distance() : 0
                ));
            } catch (Exception e) {
                logger.warn("Could not retrieve live context for RailAI query on train {}: {}", request.trainNumber(), e.getMessage());
            }
        }

        String context = contextBuilder.toString();
        String systemPrompt = "You are RailAI, a helpful, professional, and context-aware virtual assistant for Indian Railways. " +
                "Assist passengers by providing clear information. Keep answers polite and concise.";

        // 2. Determine execution pathway: GenAI vs Rule-based fallback
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            try {
                String promptText = String.format("%s\n\n%s\n\nUser Question: %s", systemPrompt, context, request.prompt());
                
                // Formulate Gemini API generateContent payload
                Map<String, Object> textPart = Map.of("text", promptText);
                Map<String, Object> partsObj = Map.of("parts", List.of(textPart));
                Map<String, Object> payloadMap = Map.of("contents", List.of(partsObj));
                String jsonPayload = objectMapper.writeValueAsString(payloadMap);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);

                String endpoint = String.format("%s?key=%s", apiUrl, apiKey);
                logger.info("Submitting context-rich prompt to Gemini GenAI REST endpoint.");
                
                String rawResponse = restTemplate.postForObject(endpoint, entity, String.class);
                JsonNode root = objectMapper.readTree(rawResponse);
                
                String generatedText = root.path("candidates")
                        .get(0)
                        .path("content")
                        .path("parts")
                        .get(0)
                        .path("text")
                        .asText();

                return new RailAiResponse(generatedText.trim());
            } catch (Exception e) {
                logger.error("Gemini API call failed, reverting to intelligent rules-based parsing: {}", e.getMessage());
            }
        }

        // 3. Rules-Based Fallback Engine
        logger.info("Executing rules-based parsing engine fallback.");
        String reply = executeFallbackRules(request.prompt(), request.trainNumber(), liveStatus, route);
        return new RailAiResponse(reply);
    }

    private String executeFallbackRules(String prompt, String trainNumber, TrainLiveStatusDto liveStatus, TrainRouteDto route) {
        String cleaned = prompt.toLowerCase();
        boolean hasLive = (liveStatus != null);

        if (cleaned.contains("delay") || cleaned.contains("late") || cleaned.contains("status")) {
            if (hasLive) {
                return String.format(
                        "Based on live telemetry, the %s (No. %s) is currently running. Its status is '%s' and it is delayed by %d minutes. The last recorded location was: %s.",
                        liveStatus.trainName(), liveStatus.trainNumber(), liveStatus.runStatus(),
                        liveStatus.delayMinutes(), liveStatus.lastLocationInfo()
                );
            }
            return "To check delay status, please provide a valid train number or view a train details page first.";
        }

        if (cleaned.contains("route") || cleaned.contains("halts") || cleaned.contains("schedule") || cleaned.contains("stop")) {
            if (hasLive && route != null) {
                return String.format(
                        "The %s runs from %s to %s, covering a distance of %d km. The route contains %d halts.",
                        liveStatus.trainName(), route.sourceStation(), route.destinationStation(),
                        route.distance(), route.stations().size()
                );
            }
            return "Please select a train first to show its scheduled route halts.";
        }

        if (cleaned.contains("alert") || cleaned.contains("alarm") || cleaned.contains("notification")) {
            return "You can easily set up delay alert thresholds by clicking the Bell icon next to any train timetable. You will receive warning badges in your dashboard if delays exceed your threshold.";
        }

        // Default chatbot response
        String welcome = "Hello! I am RailAI, your Indian Railways virtual assistant. " +
                "Currently, I am operating in fallback rules-based mode. To enable full GenAI conversational intelligence, please configure the GEMINI_API_KEY environment variable in application.yml.\n\n" +
                "For now, I can assist you with:\n" +
                "• Checking train live delay running status\n" +
                "• Detailing route stops mapping\n" +
                "• Configuring customized delay alerts settings";
        
        if (hasLive) {
            welcome += String.format("\n\n(Context: You are currently viewing train No. %s - %s)", trainNumber, liveStatus.trainName());
        }
        
        return welcome;
    }
}
