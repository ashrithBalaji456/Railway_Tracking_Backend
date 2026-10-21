package com.example.railtracker.service.impl;

import com.example.railtracker.ai.GeminiClient;
import com.example.railtracker.ai.GeminiClient.GeminiConversationResult;
import com.example.railtracker.ai.RailAiToolExecutor;
import com.example.railtracker.ai.RailAiToolRegistry;
import com.example.railtracker.dto.RailAiRequest;
import com.example.railtracker.dto.RailAiResponse;
import com.example.railtracker.dto.TrainLiveStatusDto;
import com.example.railtracker.dto.TrainRouteDto;
import com.example.railtracker.service.RailAiService;
import com.example.railtracker.service.TrainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Enhanced RailAI service that uses Gemini function calling to gather evidence
 * from backend services before generating responses. Falls back to rule-based
 * parsing when Gemini API is unavailable.
 */
@Service
public class RailAiServiceImpl implements RailAiService {

    private static final Logger logger = LoggerFactory.getLogger(RailAiServiceImpl.class);

    private final TrainService trainService;
    private final GeminiClient geminiClient;
    private final RailAiToolRegistry toolRegistry;
    private final RailAiToolExecutor toolExecutor;

    public RailAiServiceImpl(
            TrainService trainService,
            GeminiClient geminiClient,
            RailAiToolRegistry toolRegistry,
            RailAiToolExecutor toolExecutor) {
        this.trainService = trainService;
        this.geminiClient = geminiClient;
        this.toolRegistry = toolRegistry;
        this.toolExecutor = toolExecutor;
    }

    @Override
    public RailAiResponse getAiResponse(String username, RailAiRequest request) {
        logger.debug("Generating RailAI response for user '{}', trainContext={}", username, request.trainNumber());

        // 1. Try Gemini function-calling intelligence path
        if (geminiClient.isConfigured()) {
            try {
                return executeIntelligenceMode(username, request);
            } catch (Exception e) {
                logger.error("Gemini intelligence mode failed, falling back to rules: {}", e.getMessage());
            }
        }

        // 2. Fallback: rule-based response
        logger.info("Executing rules-based parsing engine fallback.");
        return executeFallbackMode(request);
    }

    // ==================== Intelligence Mode (Gemini + Function Calling) ====================

    private RailAiResponse executeIntelligenceMode(String username, RailAiRequest request) {
        String systemPrompt = buildSystemPrompt(request);
        String userPrompt = buildUserPrompt(request);

        List<Map<String, Object>> toolDefinitions = toolRegistry.getToolDefinitions();

        logger.info("Submitting RailAI intelligence query with {} tool definitions", toolDefinitions.size());

        GeminiConversationResult result = geminiClient.generateWithTools(
            systemPrompt,
            userPrompt,
            toolDefinitions,
            toolExecutor
        );

        logger.info("RailAI intelligence response generated. Tools used: {}", result.toolsUsed());

        return new RailAiResponse(result.response(), result.toolsUsed());
    }

    private String buildSystemPrompt(RailAiRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
            You are RailAI, an intelligent Indian Railways assistant built into the RailTrack India platform.
            
            YOUR CAPABILITIES:
            - You have access to LIVE railway data through function calls (tools).
            - You can check train live status, routes, station boards, and find trains between stations.
            - You provide accurate, data-driven answers based on real backend evidence.
            
            CRITICAL RULES:
            1. NEVER invent or fabricate train numbers, schedules, or delay information.
            2. ALWAYS use the provided tools to fetch real data before answering railway-specific questions.
            3. If a tool returns an error, acknowledge the limitation honestly.
            4. For delay questions, check live status AND route to identify where delays occurred.
            5. Present delay information clearly: "Train X is running Y minutes late, last seen at Z station."
            6. When showing station-by-station delays, highlight where delay increased significantly.
            7. Use the search_trains tool if you need to find a train number from a name.
            8. Use the search_stations tool if you need to find a station code from a name.
            
            RESPONSE FORMAT:
            - Be conversational, professional, and concise.
            - Use clear formatting with bullet points and sections when presenting data.
            - Always mention the source of your data (e.g., "Based on live tracking data...").
            - For multi-language queries, respond in the same language as the user.
            - Include train numbers in parentheses for clarity.
            
            DELAY INVESTIGATION PROTOCOL:
            When a user asks "Why is my train delayed?":
            1. First, get the train's live status to see current delay.
            2. Examine station-by-station progress to find WHERE delay started increasing.
            3. Identify the exact section (between stations) where maximum delay was added.
            4. Report findings: delay amount, section, and any patterns you notice.
            """);

        return sb.toString();
    }

    private String buildUserPrompt(RailAiRequest request) {
        StringBuilder sb = new StringBuilder();

        // Add train context if available
        if (request.trainNumber() != null && !request.trainNumber().trim().isEmpty()) {
            sb.append(String.format("[Context: User is currently viewing/tracking train number %s]%n%n", 
                request.trainNumber()));
        }

        sb.append(request.prompt());
        return sb.toString();
    }

    // ==================== Fallback Mode (Rule-Based) ====================

    private RailAiResponse executeFallbackMode(RailAiRequest request) {
        TrainLiveStatusDto liveStatus = null;
        TrainRouteDto route = null;

        if (request.trainNumber() != null && !request.trainNumber().trim().isEmpty()) {
            try {
                liveStatus = trainService.getTrainLiveStatus(request.trainNumber());
                route = trainService.getTrainRoute(request.trainNumber());
            } catch (Exception e) {
                logger.warn("Could not retrieve context for fallback: {}", e.getMessage());
            }
        }

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
                "Currently, I am operating in fallback rules-based mode. To enable full AI-powered intelligence with function calling, please configure the GEMINI_API_KEY environment variable.\n\n" +
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
