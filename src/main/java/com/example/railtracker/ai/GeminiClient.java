package com.example.railtracker.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.*;

/**
 * Encapsulates all communication with the Gemini API.
 * Supports:
 *   - Standard text generation
 *   - Function calling (tool declarations + function responses)
 *   - Retry with exponential backoff
 *   - Structured response parsing
 */
@Component
public class GeminiClient {

    private static final Logger logger = LoggerFactory.getLogger(GeminiClient.class);
    private static final int MAX_FUNCTION_CALL_ROUNDS = 5;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    public GeminiClient(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    /**
     * Check if Gemini API is configured with a valid key.
     */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    /**
     * Simple text generation without function calling.
     */
    public String generateText(String systemPrompt, String userPrompt) {
        if (!isConfigured()) {
            throw new GeminiException("Gemini API key not configured");
        }

        ObjectNode payload = buildTextPayload(systemPrompt, userPrompt);
        return executeRequest(payload);
    }

    /**
     * Function-calling conversation loop.
     * 
     * @param systemPrompt System instructions for Gemini
     * @param userPrompt User's question
     * @param toolDefinitions List of tool definitions for function calling
     * @param toolExecutor Callback to execute tool calls
     * @return The final text response from Gemini
     */
    public GeminiConversationResult generateWithTools(
            String systemPrompt,
            String userPrompt,
            List<Map<String, Object>> toolDefinitions,
            ToolExecutorCallback toolExecutor) {

        if (!isConfigured()) {
            throw new GeminiException("Gemini API key not configured");
        }

        List<Map<String, Object>> contents = new ArrayList<>();
        List<String> toolsUsed = new ArrayList<>();

        // Initial user message
        contents.add(Map.of(
            "role", "user",
            "parts", List.of(Map.of("text", userPrompt))
        ));

        for (int round = 0; round < MAX_FUNCTION_CALL_ROUNDS; round++) {
            ObjectNode payload = buildFunctionCallingPayload(systemPrompt, contents, toolDefinitions);
            JsonNode response = executeRequestRaw(payload);

            // Extract candidate
            JsonNode candidate = response.path("candidates").path(0);
            JsonNode content = candidate.path("content");
            JsonNode parts = content.path("parts");

            if (parts.isEmpty()) {
                logger.warn("Gemini returned empty parts in round {}", round);
                break;
            }

            // Check if the model wants to call functions
            List<Map<String, Object>> functionCalls = extractFunctionCalls(parts);

            if (functionCalls.isEmpty()) {
                // No function calls — the model has produced a final text response
                String text = extractText(parts);
                return new GeminiConversationResult(text, toolsUsed);
            }

            // Add model's response (with function calls) to conversation
            contents.add(Map.of(
                "role", "model",
                "parts", convertPartsToList(parts)
            ));

            // Execute each function call and build function responses
            List<Map<String, Object>> functionResponseParts = new ArrayList<>();
            for (Map<String, Object> fc : functionCalls) {
                String functionName = (String) fc.get("name");
                @SuppressWarnings("unchecked")
                Map<String, Object> args = (Map<String, Object>) fc.get("args");

                logger.info("Gemini requested tool call: {}({})", functionName, args);
                toolsUsed.add(functionName);

                try {
                    Map<String, Object> result = toolExecutor.execute(functionName, args);
                    functionResponseParts.add(Map.of(
                        "functionResponse", Map.of(
                            "name", functionName,
                            "response", Map.of("result", result)
                        )
                    ));
                } catch (Exception e) {
                    logger.error("Tool execution failed for {}: {}", functionName, e.getMessage());
                    functionResponseParts.add(Map.of(
                        "functionResponse", Map.of(
                            "name", functionName,
                            "response", Map.of("error", e.getMessage())
                        )
                    ));
                }
            }

            // Add function responses to conversation
            contents.add(Map.of(
                "role", "user",
                "parts", functionResponseParts
            ));
        }

        // Safety fallback if we exceed max rounds
        return new GeminiConversationResult(
            "I've gathered the available information but reached my analysis limit. Please try a more specific question.",
            toolsUsed
        );
    }

    // ==================== Internal Methods ====================

    private ObjectNode buildTextPayload(String systemPrompt, String userPrompt) {
        ObjectNode payload = objectMapper.createObjectNode();

        // System instruction
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            ObjectNode systemInstruction = objectMapper.createObjectNode();
            ArrayNode systemParts = objectMapper.createArrayNode();
            systemParts.add(objectMapper.createObjectNode().put("text", systemPrompt));
            systemInstruction.set("parts", systemParts);
            payload.set("system_instruction", systemInstruction);
        }

        // User content
        ArrayNode contents = objectMapper.createArrayNode();
        ObjectNode userContent = objectMapper.createObjectNode();
        userContent.put("role", "user");
        ArrayNode userParts = objectMapper.createArrayNode();
        userParts.add(objectMapper.createObjectNode().put("text", userPrompt));
        userContent.set("parts", userParts);
        contents.add(userContent);
        payload.set("contents", contents);

        return payload;
    }

    private ObjectNode buildFunctionCallingPayload(
            String systemPrompt,
            List<Map<String, Object>> contents,
            List<Map<String, Object>> toolDefinitions) {

        ObjectNode payload = objectMapper.createObjectNode();

        // System instruction
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            ObjectNode systemInstruction = objectMapper.createObjectNode();
            ArrayNode systemParts = objectMapper.createArrayNode();
            systemParts.add(objectMapper.createObjectNode().put("text", systemPrompt));
            systemInstruction.set("parts", systemParts);
            payload.set("system_instruction", systemInstruction);
        }

        // Contents (conversation history)
        payload.set("contents", objectMapper.valueToTree(contents));

        // Tools (function declarations)
        if (toolDefinitions != null && !toolDefinitions.isEmpty()) {
            ArrayNode tools = objectMapper.createArrayNode();
            ObjectNode toolObj = objectMapper.createObjectNode();
            toolObj.set("function_declarations", objectMapper.valueToTree(toolDefinitions));
            tools.add(toolObj);
            payload.set("tools", tools);
        }

        // Generation config
        ObjectNode genConfig = objectMapper.createObjectNode();
        genConfig.put("temperature", 0.3);
        genConfig.put("maxOutputTokens", 4096);
        payload.set("generationConfig", genConfig);

        return payload;
    }

    private String executeRequest(ObjectNode payload) {
        JsonNode response = executeRequestRaw(payload);
        return response.path("candidates")
                .path(0)
                .path("content")
                .path("parts")
                .path(0)
                .path("text")
                .asText("");
    }

    private JsonNode executeRequestRaw(ObjectNode payload) {
        String endpoint = apiUrl + "?key=" + apiKey;

        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            logger.debug("Gemini request payload size: {} bytes", payloadJson.length());

            String rawResponse = webClient.post()
                    .uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payloadJson)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(), response ->
                        response.bodyToMono(String.class)
                            .map(body -> new GeminiException("Gemini API 4xx error: " + body))
                    )
                    .bodyToMono(String.class)
                    .retryWhen(Retry.backoff(2, Duration.ofSeconds(2))
                            .maxBackoff(Duration.ofSeconds(15))
                            .filter(throwable -> {
                                // Only retry on 5xx/network errors, not 4xx client errors
                                if (throwable instanceof GeminiException) {
                                    return false; // Don't retry client errors
                                }
                                logger.warn("Gemini API call failed, retrying: {}", 
                                    throwable.getMessage() != null ? throwable.getMessage() : throwable.getClass().getSimpleName());
                                return true;
                            }))
                    .block(Duration.ofSeconds(90));

            if (rawResponse == null) {
                throw new GeminiException("Gemini API returned null response");
            }

            JsonNode response = objectMapper.readTree(rawResponse);

            // Check for API-level errors
            if (response.has("error")) {
                String errorMsg = response.path("error").path("message").asText("Unknown error");
                throw new GeminiException("Gemini API error: " + errorMsg);
            }

            return response;
        } catch (GeminiException e) {
            throw e;
        } catch (Exception e) {
            throw new GeminiException("Failed to communicate with Gemini API: " + e.getMessage(), e);
        }
    }

    private List<Map<String, Object>> extractFunctionCalls(JsonNode parts) {
        List<Map<String, Object>> calls = new ArrayList<>();
        for (JsonNode part : parts) {
            if (part.has("functionCall")) {
                JsonNode fc = part.get("functionCall");
                Map<String, Object> call = new HashMap<>();
                call.put("name", fc.path("name").asText());
                call.put("args", objectMapper.convertValue(fc.path("args"), Map.class));
                calls.add(call);
            }
        }
        return calls;
    }

    private String extractText(JsonNode parts) {
        StringBuilder sb = new StringBuilder();
        for (JsonNode part : parts) {
            if (part.has("text")) {
                sb.append(part.get("text").asText());
            }
        }
        return sb.toString().trim();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> convertPartsToList(JsonNode parts) {
        return objectMapper.convertValue(parts, List.class);
    }

    // ==================== Inner Types ====================

    /**
     * Callback interface for executing tool calls.
     */
    @FunctionalInterface
    public interface ToolExecutorCallback {
        Map<String, Object> execute(String functionName, Map<String, Object> args);
    }

    /**
     * Result of a function-calling conversation.
     */
    public record GeminiConversationResult(
        String response,
        List<String> toolsUsed
    ) {}

    /**
     * Custom exception for Gemini API errors.
     */
    public static class GeminiException extends RuntimeException {
        public GeminiException(String message) {
            super(message);
        }
        public GeminiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
