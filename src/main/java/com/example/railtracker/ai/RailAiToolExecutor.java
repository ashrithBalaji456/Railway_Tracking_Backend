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
        boolean isSpecial = isSpecialTrain(status.trainNumber(), status.trainName(), null);
        result.put("is_special_train", isSpecial);
        if (isSpecial) {
            result.put("train_priority", "LOW (Special Train)");
            result.put("priority_explanation", "This is an Indian Railways Special train (starts with 0 or marked Special). Special trains have very low operational track priority compared to regular scheduled express/superfast/Rajdhani trains. Rail traffic controllers frequently hold special trains in loop lines to allow regular scheduled trains to pass/overtake, which causes delays to accumulate gradually along the route.");
        }
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

        // Include compact station progress — only stations with meaningful data
        // to keep payload small for Gemini (full list was causing 90KB+ payloads)
        if (status.stations() != null && !status.stations().isEmpty()) {
            List<Map<String, Object>> stationProgress = new ArrayList<>();
            int total = status.stations().size();
            for (int i = 0; i < total; i++) {
                var st = status.stations().get(i);
                boolean isFirst = (i == 0);
                boolean isLast = (i == total - 1);
                boolean hasDelay = (st.delayMinutes() != null && st.delayMinutes() > 0);
                boolean isDepartedOrArrived = "DEPARTED".equals(st.status()) || "ARRIVED".equals(st.status());
                // Include: first, last, any with delay, departed/arrived stations
                // Skip upcoming stations with no delay info (they add bulk without value)
                if (isFirst || isLast || hasDelay || isDepartedOrArrived) {
                    Map<String, Object> stMap = new LinkedHashMap<>();
                    stMap.put("station", st.stationCode() + " - " + st.stationName());
                    stMap.put("seq", st.sequenceNumber());
                    if (st.scheduledArrival() != null) stMap.put("sched_arr", st.scheduledArrival());
                    if (st.actualArrival() != null) stMap.put("actual_arr", st.actualArrival());
                    if (st.delayMinutes() != null) stMap.put("delay_min", st.delayMinutes());
                    stMap.put("status", st.status());
                    stationProgress.add(stMap);
                }
            }
            result.put("station_count", total);
            result.put("key_stations", stationProgress);
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
        boolean isSpecial = isSpecialTrain(route.trainNumber(), route.trainName(), route.trainType());
        result.put("is_special_train", isSpecial);
        if (isSpecial) {
            result.put("train_priority", "LOW (Special Train)");
            result.put("priority_note", "Special train with lower operational track priority compared to regular scheduled trains.");
        }
        result.put("source_station", route.sourceStation());
        result.put("destination_station", route.destinationStation());
        result.put("total_distance_km", route.distance());
        result.put("total_duration_minutes", route.duration());
        result.put("running_days", route.runningDays());
        result.put("total_stops", route.stations() != null ? route.stations().size() : 0);

        if (route.stations() != null) {
            List<Map<String, Object>> stations = new ArrayList<>();
            for (RouteStationDto st : route.stations()) {
                // Only include stopping stations to keep payload compact
                if (st.isHalt() == null || st.isHalt()) {
                    Map<String, Object> stMap = new LinkedHashMap<>();
                    stMap.put("station", st.stationCode() + " - " + st.stationName());
                    stMap.put("seq", st.sequenceNumber());
                    stMap.put("arr", formatTime(st.arrivalTime()));
                    stMap.put("dep", formatTime(st.departureTime()));
                    if (st.haltMinutes() != null && st.haltMinutes() > 0) stMap.put("halt", st.haltMinutes());
                    if (st.distance() != null) stMap.put("dist_km", st.distance());
                    stations.add(stMap);
                }
            }
            result.put("stopping_stations", stations);
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

    /**
     * Determines whether a train is an Indian Railways Special train.
     * Special trains (numbered starting with 0, or named/typed Special/SPL/TOD)
     * are assigned lower operational priority, leading to gradual delay accumulation.
     */
    public static boolean isSpecialTrain(String trainNumber, String trainName, String trainType) {
        if (trainNumber != null && !trainNumber.isBlank()) {
            String num = trainNumber.trim();
            if (num.startsWith("0")) {
                return true;
            }
        }
        if (trainName != null && !trainName.isBlank()) {
            String upper = trainName.toUpperCase();
            if (upper.contains("SPECIAL") || upper.contains(" SPL") || upper.contains("SPL ") || upper.contains("TOD") || upper.contains("SUVIDHA") || upper.contains("CLONE")) {
                return true;
            }
        }
        if (trainType != null && !trainType.isBlank()) {
            String upper = trainType.toUpperCase();
            if (upper.contains("SPECIAL") || upper.contains("SPL")) {
                return true;
            }
        }
        return false;
    }
}
