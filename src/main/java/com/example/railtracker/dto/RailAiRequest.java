package com.example.railtracker.dto;

/**
 * Enhanced RailAI request supporting conversation context and train context.
 */
public record RailAiRequest(
    String prompt,
    String trainNumber,
    String conversationId  // Optional: for maintaining conversation context
) {
    // Backward-compatible constructor for existing callers
    public RailAiRequest(String prompt, String trainNumber) {
        this(prompt, trainNumber, null);
    }
}
