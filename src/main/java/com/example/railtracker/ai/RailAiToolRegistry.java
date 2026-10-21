package com.example.railtracker.ai;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Registry of all railway intelligence tools available to Gemini for function calling.
 * Each tool maps to an existing backend service method.
 */
@Component
public class RailAiToolRegistry {

    /**
     * Returns all tool definitions in Gemini function declaration format.
     */
    public List<Map<String, Object>> getToolDefinitions() {
        List<Map<String, Object>> tools = new ArrayList<>();

        tools.add(buildTool(
            "get_train_live_status",
            "Get the current live running status of a train including delay, position, speed, and station-by-station progress. Use this when the user asks about train delays, current position, or running status.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "train_number", Map.of(
                        "type", "string",
                        "description", "The train number (e.g., '12760', '22222')"
                    )
                ),
                "required", List.of("train_number")
            )
        ));

        tools.add(buildTool(
            "get_train_route",
            "Get the complete scheduled route of a train with all stopping stations, distances, and timings. Use this to understand the train's scheduled path, halts, and timetable.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "train_number", Map.of(
                        "type", "string",
                        "description", "The train number (e.g., '12760')"
                    )
                ),
                "required", List.of("train_number")
            )
        ));

        tools.add(buildTool(
            "get_station_board",
            "Get the list of trains arriving at or departing from a specific station. Use this to find trains at a station or check station activity.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "station_code", Map.of(
                        "type", "string",
                        "description", "The station code (e.g., 'NDLS' for New Delhi, 'MAS' for Chennai Central)"
                    ),
                    "type", Map.of(
                        "type", "string",
                        "description", "Filter type: 'arrival', 'departure', or 'all'",
                        "enum", List.of("arrival", "departure", "all")
                    )
                ),
                "required", List.of("station_code")
            )
        ));

        tools.add(buildTool(
            "get_trains_between_stations",
            "Find all trains running between two stations. Use this when the user asks about trains on a particular route or wants to find connecting trains.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "from_station", Map.of(
                        "type", "string",
                        "description", "Source station code (e.g., 'NDLS')"
                    ),
                    "to_station", Map.of(
                        "type", "string",
                        "description", "Destination station code (e.g., 'MAS')"
                    )
                ),
                "required", List.of("from_station", "to_station")
            )
        ));

        tools.add(buildTool(
            "search_trains",
            "Search for a train by its name or number. Use this when you need to find the train number from a name, or verify a train exists.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "query", Map.of(
                        "type", "string",
                        "description", "Train name or number to search for (e.g., 'Rajdhani', '12760', 'Tamil Nadu Express')"
                    )
                ),
                "required", List.of("query")
            )
        ));

        tools.add(buildTool(
            "search_stations",
            "Search for a station by name or code. Use this to find station codes when the user mentions a city or station name.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "query", Map.of(
                        "type", "string",
                        "description", "Station name or code to search for (e.g., 'New Delhi', 'NDLS', 'Mumbai')"
                    )
                ),
                "required", List.of("query")
            )
        ));

        tools.add(buildTool(
            "get_live_station_board",
            "Get the real-time live board for a station showing trains with actual arrival/departure times and delays. Use this for live station activity.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "station_code", Map.of(
                        "type", "string",
                        "description", "The station code (e.g., 'NDLS')"
                    ),
                    "hours_ahead", Map.of(
                        "type", "integer",
                        "description", "How many hours ahead to look (default: 4)"
                    ),
                    "type", Map.of(
                        "type", "string",
                        "description", "Filter type: 'arrival', 'departure', or 'all'",
                        "enum", List.of("arrival", "departure", "all")
                    )
                ),
                "required", List.of("station_code")
            )
        ));

        return tools;
    }

    private Map<String, Object> buildTool(String name, String description, Map<String, Object> parameters) {
        Map<String, Object> tool = new LinkedHashMap<>();
        tool.put("name", name);
        tool.put("description", description);
        tool.put("parameters", parameters);
        return tool;
    }
}
