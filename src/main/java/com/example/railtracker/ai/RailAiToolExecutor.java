package com.example.railtracker.ai;

import com.example.railtracker.dto.*;
import com.example.railtracker.service.JourneyService;
import com.example.railtracker.service.StationService;
import com.example.railtracker.service.TrainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Executes RailAI tool calls by delegating to existing backend services.
 * Converts service results into structured Maps that Gemini can reason over.
 */
@Component
public class RailAiToolExecutor implements GeminiClient.ToolExecutorCallback {

    private static final Logger logger = LoggerFactory.getLogger(RailAiToolExecutor.class);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final TrainService trainService;
    private final StationService stationService;
    private final JourneyService journeyService;

    public RailAiToolExecutor(TrainService trainService, StationService stationService, JourneyService journeyService) {
        this.trainService = trainService;
        this.stationService = stationService;
        this.journeyService = journeyService;
    }

    @Override
    public Map<String, Object> execute(String functionName, Map<String, Object> args) {
        logger.info("Executing RailAI tool: {} with args: {}", functionName, args);

        try {
            return switch (functionName) {
                case "get_train_live_status" -> executeGetTrainLiveStatus(args);
                case "get_train_route" -> executeGetTrainRoute(args);
                case "get_station_board" -> executeGetStationBoard(args);
                case "get_trains_between_stations" -> executeGetTrainsBetweenStations(args);
                case "search_trains" -> executeSearchTrains(args);
                case "search_stations" -> executeSearchStations(args);
                case "get_live_station_board" -> executeGetLiveStationBoard(args);
                default -> Map.of("error", "Unknown tool: " + functionName);
            };
        } catch (Exception e) {
            logger.error("Tool execution failed for {}: {}", functionName, e.getMessage(), e);
            return Map.of("error", "Failed to execute " + functionName + ": " + e.getMessage());
        }
    }

    // ==================== Tool Implementations ====================

    private Map<String, Object> executeGetTrainLiveStatus(Map<String, Object> args) {
        String trainNumber = getStringArg(args, "train_number");
        TrainLiveStatusDto status = trainService.getTrainLiveStatus(trainNumber);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("train_number", status.trainNumber());
        result.put("train_name", status.trainName());
        result.put("run_status", status.runStatus());
        result.put("delay_minutes", status.delayMinutes());
        result.put("current_station_code", status.currentStationCode());
        result.put("current_station_name", status.currentStationName());
        result.put("next_station_code", status.nextStationCode());
        result.put("next_station_name", status.nextStationName());
        result.put("latitude", status.latitude());
        result.put("longitude", status.longitude());
        result.put("speed_kmph", status.speed());
        result.put("route_progress_percent", status.routeProgress());
        result.put("last_location_info", status.lastLocationInfo());
        result.put("last_updated_at", status.lastUpdatedAt() != null ? status.lastUpdatedAt().toString() : null);

        // Include station-by-station progress (compact summary)
        if (status.stations() != null && !status.stations().isEmpty()) {
            List<Map<String, Object>> stationProgress = new ArrayList<>();
            for (var st : status.stations()) {
                Map<String, Object> stMap = new LinkedHashMap<>();
                stMap.put("station_code", st.stationCode());
                stMap.put("station_name", st.stationName());
                stMap.put("sequence", st.sequenceNumber());
                stMap.put("scheduled_arrival", st.scheduledArrival());
                stMap.put("scheduled_departure", st.scheduledDeparture());
                stMap.put("actual_arrival", st.actualArrival());
                stMap.put("actual_departure", st.actualDeparture());
                stMap.put("delay_minutes", st.delayMinutes());
                stMap.put("status", st.status());
                stMap.put("is_halt", st.isHalt());
                stationProgress.add(stMap);
            }
            result.put("station_progress", stationProgress);
        }

        return result;
    }

    private Map<String, Object> executeGetTrainRoute(Map<String, Object> args) {
        String trainNumber = getStringArg(args, "train_number");
        TrainRouteDto route = trainService.getTrainRoute(trainNumber);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("train_number", route.trainNumber());
        result.put("train_name", route.trainName());
        result.put("train_type", route.trainType());
        result.put("source_station", route.sourceStation());
        result.put("destination_station", route.destinationStation());
        result.put("total_distance_km", route.distance());
        result.put("total_duration_minutes", route.duration());
        result.put("running_days", route.runningDays());
        result.put("total_stops", route.stations() != null ? route.stations().size() : 0);

        if (route.stations() != null) {
            List<Map<String, Object>> stations = new ArrayList<>();
            for (RouteStationDto st : route.stations()) {
                Map<String, Object> stMap = new LinkedHashMap<>();
                stMap.put("station_code", st.stationCode());
                stMap.put("station_name", st.stationName());
                stMap.put("sequence", st.sequenceNumber());
                stMap.put("arrival", formatTime(st.arrivalTime()));
                stMap.put("departure", formatTime(st.departureTime()));
                stMap.put("halt_minutes", st.haltMinutes());
                stMap.put("stop_type", st.stopType());
                stMap.put("distance_km", st.distance());
                stations.add(stMap);
            }
            result.put("stations", stations);
        }

        return result;
    }

    private Map<String, Object> executeGetStationBoard(Map<String, Object> args) {
        String stationCode = getStringArg(args, "station_code").toUpperCase();
        String type = getStringArgOrDefault(args, "type", "all");

        List<StationBoardTrainDto> trains = stationService.getStationTrains(stationCode, type);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("station_code", stationCode);
        result.put("filter_type", type);
        result.put("total_trains", trains.size());

        List<Map<String, Object>> trainList = new ArrayList<>();
        for (StationBoardTrainDto t : trains) {
            Map<String, Object> tMap = new LinkedHashMap<>();
            tMap.put("train_number", t.trainNumber());
            tMap.put("train_name", t.trainName());
            tMap.put("train_type", t.trainType());
            tMap.put("source", t.sourceStation());
            tMap.put("destination", t.destinationStation());
            tMap.put("scheduled_arrival", formatTime(t.scheduledArrival()));
            tMap.put("scheduled_departure", formatTime(t.scheduledDeparture()));
            tMap.put("delay_minutes", t.delayMinutes());
            tMap.put("current_status", t.currentStatus());
            trainList.add(tMap);
        }
        result.put("trains", trainList);

        return result;
    }

    private Map<String, Object> executeGetLiveStationBoard(Map<String, Object> args) {
        String stationCode = getStringArg(args, "station_code").toUpperCase();
        int hoursAhead = getIntArgOrDefault(args, "hours_ahead", 4);
        String type = getStringArgOrDefault(args, "type", "all");

        List<StationBoardTrainDto> trains = stationService.getStationLiveBoard(stationCode, hoursAhead, type);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("station_code", stationCode);
        result.put("hours_ahead", hoursAhead);
        result.put("filter_type", type);
        result.put("total_trains", trains.size());

        List<Map<String, Object>> trainList = new ArrayList<>();
        for (StationBoardTrainDto t : trains) {
            Map<String, Object> tMap = new LinkedHashMap<>();
            tMap.put("train_number", t.trainNumber());
            tMap.put("train_name", t.trainName());
            tMap.put("source", t.sourceStation());
            tMap.put("destination", t.destinationStation());
            tMap.put("scheduled_arrival", formatTime(t.scheduledArrival()));
            tMap.put("actual_arrival", formatTime(t.actualArrival()));
            tMap.put("scheduled_departure", formatTime(t.scheduledDeparture()));
            tMap.put("actual_departure", formatTime(t.actualDeparture()));
            tMap.put("delay_minutes", t.delayMinutes());
            tMap.put("current_status", t.currentStatus());
            trainList.add(tMap);
        }
        result.put("trains", trainList);

        return result;
    }

    private Map<String, Object> executeGetTrainsBetweenStations(Map<String, Object> args) {
        String from = getStringArg(args, "from_station").toUpperCase();
        String to = getStringArg(args, "to_station").toUpperCase();

        List<JourneyOptionDto> options = journeyService.planJourney(from, to, null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("from_station", from);
        result.put("to_station", to);
        result.put("total_options", options.size());

        List<Map<String, Object>> trainList = new ArrayList<>();
        for (JourneyOptionDto opt : options) {
            Map<String, Object> tMap = new LinkedHashMap<>();
            tMap.put("is_direct", opt.direct());
            tMap.put("train_number", opt.firstTrainNumber());
            tMap.put("train_name", opt.firstTrainName());
            tMap.put("origin_station", opt.originStationCode());
            tMap.put("destination_station", opt.destinationStationCode());
            tMap.put("departure_time", formatTime(opt.originDepartureTime()));
            tMap.put("arrival_time", formatTime(opt.destinationArrivalTime()));
            tMap.put("total_duration_minutes", opt.totalDurationMinutes());
            tMap.put("running_days", opt.firstTrainRunDays());
            if (!opt.direct() && opt.secondTrainNumber() != null) {
                tMap.put("second_train_number", opt.secondTrainNumber());
                tMap.put("second_train_name", opt.secondTrainName());
                tMap.put("transfer_station", opt.transferStationCode());
                tMap.put("layover_minutes", opt.layoverMinutes());
            }
            trainList.add(tMap);
        }
        result.put("trains", trainList);

        return result;
    }

    private Map<String, Object> executeSearchTrains(Map<String, Object> args) {
        String query = getStringArg(args, "query");
        List<TrainDto> trains = trainService.searchTrains(query);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("query", query);
        result.put("total_results", trains.size());

        List<Map<String, Object>> trainList = new ArrayList<>();
        for (TrainDto t : trains) {
            Map<String, Object> tMap = new LinkedHashMap<>();
            tMap.put("train_number", t.trainNumber());
            tMap.put("train_name", t.trainName());
            tMap.put("train_type", t.trainType());
            tMap.put("source", t.sourceStation());
            tMap.put("destination", t.destinationStation());
            tMap.put("running_days", t.runningDays());
            trainList.add(tMap);
        }
        result.put("trains", trainList);

        return result;
    }

    private Map<String, Object> executeSearchStations(Map<String, Object> args) {
        String query = getStringArg(args, "query");
        List<StationDto> stations = stationService.searchStations(query);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("query", query);
        result.put("total_results", stations.size());

        List<Map<String, Object>> stationList = new ArrayList<>();
        for (StationDto s : stations) {
            Map<String, Object> sMap = new LinkedHashMap<>();
            sMap.put("station_code", s.stationCode());
            sMap.put("station_name", s.stationName());
            sMap.put("city", s.city());
            sMap.put("state", s.state());
            sMap.put("zone", s.zone());
            stationList.add(sMap);
        }
        result.put("stations", stationList);

        return result;
    }

    // ==================== Utility Methods ====================

    private String getStringArg(Map<String, Object> args, String key) {
        Object val = args.get(key);
        if (val == null) {
            throw new IllegalArgumentException("Missing required argument: " + key);
        }
        return val.toString().trim();
    }

    private String getStringArgOrDefault(Map<String, Object> args, String key, String defaultVal) {
        Object val = args.get(key);
        return val != null ? val.toString().trim() : defaultVal;
    }

    private int getIntArgOrDefault(Map<String, Object> args, String key, int defaultVal) {
        Object val = args.get(key);
        if (val == null) return defaultVal;
        if (val instanceof Number) return ((Number) val).intValue();
        try {
            return Integer.parseInt(val.toString().trim());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private String formatTime(LocalTime time) {
        return time != null ? time.format(TIME_FMT) : null;
    }
}
