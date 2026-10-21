package com.example.railtracker.dto;

import java.util.List;

/**
 * Enhanced RailAI response with structured evidence chain and tool usage transparency.
 */
public record RailAiResponse(
    String response,                    // The AI-generated text response
    List<String> toolsUsed,             // Which backend tools Gemini invoked
    String confidence,                  // HIGH, MEDIUM, LOW, or FALLBACK
    String responseType                 // INTELLIGENCE, GENERAL, FALLBACK
) {
    // Backward-compatible constructor for existing callers and fallback mode
    public RailAiResponse(String response) {
        this(response, List.of(), "FALLBACK", "FALLBACK");
    }

    // Constructor for tool-powered responses
    public RailAiResponse(String response, List<String> toolsUsed) {
        this(response, toolsUsed, toolsUsed.isEmpty() ? "MEDIUM" : "HIGH", "INTELLIGENCE");
    }
}
