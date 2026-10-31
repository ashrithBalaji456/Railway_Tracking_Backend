package com.example.railtracker.client;

import com.example.railtracker.exception.ApiException;
import com.example.railtracker.exception.BadRequestException;
import com.example.railtracker.exception.ResourceNotFoundException;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Mono;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ArrayList;
import com.example.railtracker.dto.LiveStationBoardResponse;
import com.example.railtracker.dto.StationPassType;
import com.example.railtracker.dto.StationTrainLiveStatus;
import com.example.railtracker.dto.TrainExceptionDTO;
import com.example.railtracker.dto.TrainExceptionSummaryDTO;
import com.example.railtracker.dto.TrainDelayReportDTO;
import com.example.railtracker.dto.TrainFareDTO;
import com.example.railtracker.dto.JourneyOverlayDTO;

@Component
public class RailRadarClient {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RailRadarClient.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String apiKey;

    public RailRadarClient(
            WebClient.Builder webClientBuilder,
            @Value("${app.railradar.base-url:https://api.railradar.example/v1}") String baseUrl,
            @Value("${app.railradar.api-key:mock-key}") String apiKey) {

        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> {
                    configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(this.objectMapper));
                    configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(this.objectMapper));
                })
                .build();

        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .exchangeStrategies(strategies)
                .build();
        String maskedKey = (apiKey == null || apiKey.isBlank()) ? "NOT SET" : (apiKey.length() > 6 ? apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 2) : "***");
        logger.info("Initialized RailRadarClient with baseUrl: '{}' and apiKey: '{}'", baseUrl, maskedKey);
    }

    private JsonNode executeGet(String path) {
        try {
            java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
                    .followRedirects(java.net.http.HttpClient.Redirect.ALWAYS)
                    .build();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(baseUrl + path))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            java.net.http.HttpResponse<String> response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            
            if (statusCode == 200) {
                return this.objectMapper.readTree(response.body());
            } else if (statusCode == 404) {
                throw new ResourceNotFoundException("Resource not found on external server");
            } else if (statusCode == 400) {
                throw new BadRequestException("Bad request sent to external server");
            } else if (statusCode == 429) {
                throw new ApiException("Live railway data is temporarily rate-limited. Please try again later.");
            } else if (statusCode >= 500) {
                throw new ApiException("External railway provider is temporarily unavailable.");
            } else {
                throw new ApiException("External railway client error: Status " + statusCode);
            }
        } catch (ApiException | ResourceNotFoundException | BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("External railway client error", e);
        }
    }

    public JsonNode executePost(String path, Object body) {
        try {
            java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
                    .followRedirects(java.net.http.HttpClient.Redirect.ALWAYS)
                    .build();
            String jsonPayload = this.objectMapper.writeValueAsString(body);
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(baseUrl + path))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();
            java.net.http.HttpResponse<String> response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();

            if (statusCode == 200) {
                return this.objectMapper.readTree(response.body());
            } else if (statusCode == 404) {
                throw new ResourceNotFoundException("Resource not found on external server");
            } else if (statusCode == 400) {
                throw new BadRequestException("Bad request sent to external server");
            } else if (statusCode == 429) {
                throw new ApiException("Live railway data is temporarily rate-limited. Please try again later.");
            } else if (statusCode >= 500) {
                throw new ApiException("External railway provider is temporarily unavailable.");
            } else {
                throw new ApiException("External railway client error: Status " + statusCode);
            }
        } catch (ApiException | ResourceNotFoundException | BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("External railway client error", e);
        }
    }

    public List<RailRadarStation> searchStations(String query) {
        JsonNode responseNode = executeGet("/lookup/stations?query=" + encode(query));
        logger.debug("RailRadar stations response for query {}: {}", query, responseNode);
        return mapStationLookup(responseNode, query);
    }

    private List<RailRadarStation> mapStationLookup(JsonNode responseNode, String query) {
        JsonNode dataNode = dataNode(responseNode);
        if (dataNode == null || dataNode.isNull()) return List.of();
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
        List<RailRadarStation> stations = new ArrayList<>();

        if (dataNode.isObject() && !dataNode.has("stations")) {
            dataNode.fields().forEachRemaining(entry -> {
                String code = entry.getKey();
                String name = entry.getValue().asText();
                if (normalizedQuery.isBlank()
                        || code.toLowerCase().contains(normalizedQuery)
                        || name.toLowerCase().contains(normalizedQuery)) {
                    stations.add(new RailRadarStation(code, name, null, null, null, null, true));
                }
            });
            return stations;
        }

        return unpackListResponse(responseNode, RailRadarStation.class);
    }
    public List<RailRadarTrain> searchTrains(String query) {
        JsonNode responseNode = executeGet("/lookup/trains?query=" + encode(query));
        logger.debug("RailRadar train lookup response for query {}: {}", query, responseNode);
        return mapTrainLookup(responseNode, query);
    }

    private List<RailRadarTrain> mapTrainLookup(JsonNode responseNode, String query) {
        JsonNode dataNode = dataNode(responseNode);
        if (dataNode == null || dataNode.isNull()) return List.of();
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
        List<RailRadarTrain> trains = new ArrayList<>();

        if (dataNode.isObject() && !dataNode.has("trains")) {
            dataNode.fields().forEachRemaining(entry -> {
                String number = entry.getKey();
                String name = entry.getValue().asText();
                if (normalizedQuery.isBlank()
                        || number.toLowerCase().contains(normalizedQuery)
                        || name.toLowerCase().contains(normalizedQuery)) {
                    trains.add(new RailRadarTrain(number, name, null, null, null, null, null, null, null, true, null, List.of()));
                }
            });
            return trains;
        }

        JsonNode trainsNode = dataNode.has("trains") ? dataNode.get("trains") : dataNode;
        if (trainsNode != null && trainsNode.isArray()) {
            for (JsonNode trainNode : trainsNode) {
                RailRadarTrain train = mapTrain(trainNode.has("train") ? trainNode.get("train") : trainNode, null);
                if (train != null) trains.add(train);
            }
        }
        return trains;
    }

    /**
     * Normalize journey date to YYYYMMDD format (what RailRadar expects).
     * Accepts: YYYY-MM-DD, YYYYMMDD, DD-MM-YYYY
     */
    private String normalizeDateParam(String date) {
        if (date == null || date.isBlank()) return date;
        String cleaned = date.trim();
        // Already YYYYMMDD (8 digits, no dashes)
        if (cleaned.matches("\\d{8}")) return cleaned;
        // YYYY-MM-DD -> YYYYMMDD
        if (cleaned.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return cleaned.replace("-", "");
        }
        // DD-MM-YYYY -> YYYYMMDD
        if (cleaned.matches("\\d{2}-\\d{2}-\\d{4}")) {
            String[] parts = cleaned.split("-");
            return parts[2] + parts[1] + parts[0];
        }
        // Fallback: return as-is
        logger.warn("Unrecognized date format: '{}', passing as-is", cleaned);
        return cleaned;
    }

    public RailRadarTrain getTrainDetails(String trainNumber) {
        return getTrainDetails(trainNumber, null);
    }

    public RailRadarTrain getTrainDetails(String trainNumber, String journeyDate) {
        String path = "/trains/" + trainNumber;
        if (journeyDate != null && !journeyDate.isBlank()) {
            String normalizedDate = normalizeDateParam(journeyDate);
            path += "?journeyDate=" + normalizedDate;
        }
        logger.info("RailRadar train details request: GET {}", path);
        JsonNode responseNode = executeGet(path);
        logger.debug("RailRadar train details response for {} on {}: {}", trainNumber, journeyDate, responseNode);
        JsonNode dataNode = dataNode(responseNode);
        JsonNode trainNode = dataNode != null && dataNode.has("train") ? dataNode.get("train") : dataNode;
        JsonNode routeNode = dataNode != null && dataNode.has("route") ? dataNode.get("route") : null;
        return mapTrain(trainNode, routeNode);
    }

    public RailRadarTrainRoute getTrainRoute(String trainNumber) {
        return getTrainRoute(trainNumber, null);
    }

    public RailRadarTrainRoute getTrainRoute(String trainNumber, String journeyDate) {
        String path = "/trains/" + trainNumber + "/route?format=geojson&stops=true";
        if (journeyDate != null && !journeyDate.isBlank()) {
            String normalizedDate = normalizeDateParam(journeyDate);
            path += "&journeyDate=" + normalizedDate;
        }
        logger.info("RailRadar route request: GET {}", path);
        JsonNode responseNode = executeGet(path);
        logger.debug("RailRadar route response for {} on date {}: {}", trainNumber, journeyDate, responseNode);
        JsonNode dataNode = dataNode(responseNode);
        JsonNode routeNode = null;
        if (dataNode != null && dataNode.has("route")) {
            routeNode = dataNode.get("route");
        } else if (dataNode != null && dataNode.has("stops")) {
            routeNode = dataNode.get("stops");
        } else if (dataNode != null && dataNode.isArray()) {
            routeNode = dataNode;
        }
        List<RailRadarRouteStation> stops = new ArrayList<>();
        if (routeNode != null && routeNode.isArray()) {
            for (JsonNode stop : routeNode) {
                RailRadarStationInfo station = stationInfo(stop);
                stops.add(new RailRadarRouteStation(
                        integer(stop, "sequence"),
                        station != null ? station.code() : text(stop, "stationCode", "code"),
                        station != null ? station.name() : text(stop, "stationName", "name"),
                        station != null ? station.lat() : decimal(stop, "lat", "latitude"),
                        station != null ? station.lng() : decimal(stop, "lng", "longitude")
                ));
            }
        }
        return new RailRadarTrainRoute(trainNumber, text(dataNode, "format"), stops);
    }

    public RailRadarTrainLiveStatus getTrainLiveStatus(String trainNumber) {
        return getTrainLiveStatus(trainNumber, null);
    }

    public RailRadarTrainLiveStatus getTrainLiveStatus(String trainNumber, String journeyDate) {
        String path = "/trains/" + trainNumber + "/live";
        if (journeyDate != null && !journeyDate.isBlank()) {
            String normalizedDate = normalizeDateParam(journeyDate);
            path += "?journeyDate=" + normalizedDate;
        }
        logger.info("RailRadar live status request: GET {}", path);
        JsonNode responseNode = executeGet(path);
        logger.info("RailRadar live response for {} on date {}: status={}", trainNumber, journeyDate,
                responseNode != null && responseNode.has("status") ? responseNode.get("status") : "unknown");
        JsonNode dataNode = dataNode(responseNode);
        JsonNode liveStatusNode = null;
        if (dataNode != null) {
            if (dataNode.has("liveStatus") && dataNode.get("liveStatus").isContainerNode()) {
                liveStatusNode = dataNode.get("liveStatus");
            } else if (dataNode.has("live_status") && dataNode.get("live_status").isContainerNode()) {
                liveStatusNode = dataNode.get("live_status");
            } else if (dataNode.has("status") && dataNode.get("status").isContainerNode()) {
                liveStatusNode = dataNode.get("status");
            }
        }
        return mapLiveStatus(trainNumber, liveStatusNode != null ? liveStatusNode : dataNode);
    }

    public List<RailRadarStationBoardTrain> getStationBoard(String stationCode, boolean includeIntermediate) {
        String path = "/stations/" + stationCode + "/trains";
        if (includeIntermediate) {
            path += "?includeIntermediate=true";
        }
        JsonNode responseNode = executeGet(path);
        logger.info("Java SE HttpClient raw station board response for '{}' (includeIntermediate={}): {}", stationCode, includeIntermediate, responseNode);
        return mapStationBoardResponse(responseNode, stationCode);
    }

    public List<RailRadarStationBoardTrain> getStationLiveBoard(String stationCode, int hoursAhead) {
        JsonNode responseNode = executeGet("/stations/" + stationCode + "/live?includeIntermediate=true");
        return mapStationBoardResponse(responseNode, stationCode);
    }

    public LiveStationBoardResponse getLiveStationBoardDetails(String stationCode, int hours) {
        String path = "/stations/" + stationCode + "/live?hours=" + hours + "&includeIntermediate=true";
        JsonNode responseNode = executeGet(path);
        logger.info("RailRadar live station board response for '{}' (hours={}): {}", stationCode, hours, responseNode);
        return mapLiveStationBoard(responseNode, hours, stationCode);
    }

    private LiveStationBoardResponse mapLiveStationBoard(JsonNode responseNode, int hours, String currentStationCode) {
        JsonNode dataNode = dataNode(responseNode);
        if (dataNode == null || dataNode.isNull()) {
            throw new ResourceNotFoundException("Live station board details not found");
        }

        // 1. Parse station block
        JsonNode stationNode = dataNode.get("station");
        String code = text(stationNode, "code");
        String name = text(stationNode, "name");
        String city = text(stationNode, "city");
        Double lat = decimal(stationNode, "lat", "latitude");
        Double lng = decimal(stationNode, "lng", "longitude");
        LiveStationBoardResponse.StationInfo stationInfo = new LiveStationBoardResponse.StationInfo(code, name, city, lat, lng);

        // 2. Parse window block
        JsonNode windowNode = dataNode.get("window");
        String from = text(windowNode, "from");
        String to = text(windowNode, "to");
        LiveStationBoardResponse.WindowInfo windowInfo = new LiveStationBoardResponse.WindowInfo(from, to, hours);

        // 3. Parse trains block
        List<LiveStationBoardResponse.LiveStationBoardTrain> trainsList = new ArrayList<>();
        JsonNode trainsNode = dataNode.get("trains");
        if (trainsNode != null && trainsNode.isArray()) {
            for (JsonNode item : trainsNode) {
                JsonNode train = item.get("train");
                JsonNode stop = item.get("stop");
                JsonNode live = item.get("live");

                String trainNumber = text(train, "number");
                String trainName = text(train, "name");
                String trainType = text(train, "type");

                String sourceCode = text(train, "source");
                LiveStationBoardResponse.StationCodeName source = new LiveStationBoardResponse.StationCodeName(sourceCode, sourceCode);

                String destCode = text(train, "destination");
                LiveStationBoardResponse.StationCodeName destination = new LiveStationBoardResponse.StationCodeName(destCode, destCode);

                Boolean isHaltVal = bool(stop, "isHalt");
                boolean isHalt = isHaltVal != null ? isHaltVal : true;
                if (currentStationCode != null && (
                        currentStationCode.equalsIgnoreCase(sourceCode) ||
                        currentStationCode.equalsIgnoreCase(destCode)
                )) {
                    isHalt = true;
                }
                StationPassType passType = isHalt ? StationPassType.STOPPING : StationPassType.PASS_THROUGH;

                String scheduledArrival = text(stop, "arrival");
                String scheduledDeparture = text(stop, "departure");

                String expectedArrival = timeText(live, "expectedArrivalTime", "expectedArrival");
                String expectedDeparture = timeText(live, "expectedDepartureTime", "expectedDeparture");
                String actualArrival = text(live, "actualArrivalTime", "arrivedAt", "actualArrival");
                if (actualArrival == null) {
                    actualArrival = timeText(live, "actualArrivalTime", "arrivedAt", "actualArrival");
                }
                String actualDeparture = text(live, "actualDepartureTime", "departedAt", "actualDeparture");
                if (actualDeparture == null) {
                    actualDeparture = timeText(live, "actualDepartureTime", "departedAt", "actualDeparture");
                }

                Integer delayMinutes = integer(live, "delayMinutes");
                String platform = text(stop, "platform");
                if (platform == null || platform.equalsIgnoreCase("null") || platform.trim().isEmpty()) {
                    platform = null;
                }

                String liveType = text(live, "type");
                StationTrainLiveStatus liveStatus = mapLiveStatusEnum(liveType);
                if (actualDeparture != null && !actualDeparture.isBlank()) {
                    liveStatus = StationTrainLiveStatus.DEPARTED;
                } else if (actualArrival != null && !actualArrival.isBlank()) {
                    liveStatus = StationTrainLiveStatus.ARRIVED;
                }

                String runDaysStr = "";
                if (train != null && train.has("runDays") && train.get("runDays").isArray()) {
                    List<String> days = new ArrayList<>();
                    train.get("runDays").forEach(d -> days.add(d.asText()));
                    runDaysStr = String.join(",", days);
                }

                trainsList.add(new LiveStationBoardResponse.LiveStationBoardTrain(
                        trainNumber,
                        trainName,
                        trainType,
                        source,
                        destination,
                        passType,
                        scheduledArrival,
                        scheduledDeparture,
                        expectedArrival,
                        expectedDeparture,
                        delayMinutes != null ? delayMinutes : 0,
                        platform,
                        liveStatus,
                        runDaysStr,
                        actualArrival,
                        actualDeparture
                ));
            }
        }

        return new LiveStationBoardResponse(stationInfo, windowInfo, trainsList);
    }

    private StationTrainLiveStatus mapLiveStatusEnum(String type) {
        if (type == null) return StationTrainLiveStatus.UNKNOWN;
        switch (type.toLowerCase()) {
            case "not-started":
                return StationTrainLiveStatus.NOT_STARTED;
            case "upcoming":
                return StationTrainLiveStatus.UPCOMING;
            case "at-station":
                return StationTrainLiveStatus.AT_STATION;
            case "departed":
                return StationTrainLiveStatus.DEPARTED;
            default:
                return StationTrainLiveStatus.UNKNOWN;
        }
    }

    private List<RailRadarStationBoardTrain> mapStationBoardResponse(JsonNode responseNode, String currentStationCode) {
        JsonNode dataNode = dataNode(responseNode);
        if (dataNode == null || dataNode.isNull()) return List.of();
        JsonNode trainsNode = dataNode;
        if (dataNode.isObject()) {
            if (dataNode.has("trains")) trainsNode = dataNode.get("trains");
            else if (dataNode.has("board")) trainsNode = dataNode.get("board");
            else if (dataNode.has("services")) trainsNode = dataNode.get("services");
        }
        if (trainsNode == null || !trainsNode.isArray()) return List.of();

        List<RailRadarStationBoardTrain> trains = new ArrayList<>();
        for (JsonNode item : trainsNode) {
            trains.add(mapStationBoardTrain(item, currentStationCode));
        }
        return trains;
    }

    private RailRadarStationBoardTrain mapStationBoardTrain(JsonNode item, String currentStationCode) {
        JsonNode train = item != null && item.has("train") ? item.get("train") : item;
        JsonNode stop = item != null && item.has("stop") ? item.get("stop") : item;
        JsonNode live = item != null && item.has("live") ? item.get("live") : item;
        JsonNode source = train != null && train.has("source") ? train.get("source") : item.get("source");
        JsonNode destination = train != null && train.has("destination") ? train.get("destination") : item.get("destination");
        String stopType = firstText(stop, item, "stopType", "type");
        Boolean isHalt = bool(stop, "isHalt");
        if (isHalt == null) isHalt = bool(item, "isHalt");
        if (isHalt == null && stopType != null) {
            isHalt = stopType.equalsIgnoreCase("halt") || stopType.equalsIgnoreCase("stopping") || stopType.equalsIgnoreCase("stop");
        }

        String sourceStation = stationCode(source, firstText(train, item, "sourceStation", "source"));
        String destinationStation = stationCode(destination, firstText(train, item, "destinationStation", "destination"));
        if (currentStationCode != null && (
                currentStationCode.equalsIgnoreCase(sourceStation) ||
                currentStationCode.equalsIgnoreCase(destinationStation)
        )) {
            isHalt = true;
        }

        Integer arrivalDay = firstInteger(stop, "arrivalDay", "dayOffset", "arrivalDayOffset", "day");
        if (arrivalDay == null) arrivalDay = firstInteger(item, "arrivalDay", "dayOffset", "arrivalDayOffset", "day");
        if (arrivalDay == null) arrivalDay = 0;

        return new RailRadarStationBoardTrain(
                firstText(train, item, "trainNumber", "number", "trainNo", "train_no"),
                firstText(train, item, "trainName", "name", "train_name"),
                firstText(train, item, "trainType", "type", "train_type", "category"),
                runningDays(train),
                sourceStation,
                destinationStation,
                timeText(stop, "scheduledArrival", "arrival", "arrivalTime"),
                timeText(stop, "scheduledDeparture", "departure", "departureTime"),
                firstText(live, item, "actualArrival", "arrivedAt", "liveArrival", "expectedArrival"),
                firstText(live, item, "actualDeparture", "departedAt", "liveDeparture", "expectedDeparture"),
                firstInteger(live, "delayMinutes", "delay", "delayArrival", "delayDeparture"),
                isHalt,
                firstText(live, item, "currentStatus", "status", "liveStatus"),
                arrivalDay
        );
    }
    public List<RailRadarTrainBetweenStations> getTrainsBetweenStations(String fromStationCode, String toStationCode, String date) {
        String path = "/trains/between/" + fromStationCode + "/" + toStationCode;
        if (date != null && !date.isBlank()) {
            path += "?date=" + encode(date);
        }
        JsonNode responseNode = executeGet(path);
        return mapTrainsBetweenResponse(responseNode);
    }

    private List<RailRadarTrainBetweenStations> mapTrainsBetweenResponse(JsonNode responseNode) {
        if (responseNode == null || !responseNode.has("success") || !responseNode.get("success").asBoolean()) {
            return List.of();
        }
        JsonNode data = responseNode.get("data");
        if (data == null) {
            return List.of();
        }
        JsonNode trainsNode = data.has("trains") ? data.get("trains") : data;
        if (trainsNode == null || !trainsNode.isArray()) {
            return List.of();
        }
        
        List<RailRadarTrainBetweenStations> list = new ArrayList<>();
        for (JsonNode item : trainsNode) {
            JsonNode train = item.get("train");
            JsonNode from = item.get("from");
            JsonNode to = item.get("to");
            
            String runDaysStr = "";
            if (train != null && train.has("runDays") && train.get("runDays").isArray()) {
                List<String> days = new ArrayList<>();
                train.get("runDays").forEach(d -> days.add(d.asText()));
                runDaysStr = String.join(",", days);
            }
            
            list.add(new RailRadarTrainBetweenStations(
                train != null ? text(train, "number") : null,
                train != null ? text(train, "name") : null,
                train != null ? text(train, "type") : null,
                from != null ? text(from, "code") : null,
                to != null ? text(to, "code") : null,
                from != null ? text(from, "departure") : null,
                to != null ? text(to, "arrival") : null,
                integer(item, "duration"),
                runDaysStr,
                from != null ? integer(from, "sequence") : null,
                to != null ? integer(to, "sequence") : null
            ));
        }
        return list;
    }

    public List<TrainExceptionDTO> getTrainExceptions(String type) {
        String path = "/trains/exceptions";
        logger.info("RailRadar exceptions request: GET {}", path);
        try {
            JsonNode responseNode = executeGet(path);
            return mapTrainExceptionsResponse(responseNode);
        } catch (BadRequestException e) {
            logger.warn("RailRadar returned 400 for path {}, attempting clean /trains/exceptions fallback: {}", path, e.getMessage());
            JsonNode fallbackNode = executeGet("/trains/exceptions");
            return mapTrainExceptionsResponse(fallbackNode);
        }
    }

    private List<TrainExceptionDTO> mapTrainExceptionsResponse(JsonNode responseNode) {
        JsonNode data = dataNode(responseNode);
        if (data == null || data.isNull()) {
            return List.of();
        }
        JsonNode trainsNode = data.has("trains") ? data.get("trains") : (data.isArray() ? data : null);
        if (trainsNode == null || !trainsNode.isArray()) {
            return List.of();
        }
        List<TrainExceptionDTO> list = new ArrayList<>();
        for (JsonNode item : trainsNode) {
            String trainNumber = text(item, "trainNumber", "train_number", "number");
            String trainName = text(item, "trainName", "train_name", "name");
            String trainType = text(item, "trainType", "train_type", "type");
            String startDate = text(item, "startDate", "start_date", "date");

            JsonNode srcNode = item.get("sourceStation");
            String srcCode = srcNode != null && srcNode.isObject() ? text(srcNode, "code") : text(item, "sourceStationCode", "sourceStation", "source");
            String srcName = srcNode != null && srcNode.isObject() ? text(srcNode, "name") : text(item, "sourceStationName", "sourceName");

            JsonNode dstNode = item.get("destinationStation");
            String dstCode = dstNode != null && dstNode.isObject() ? text(dstNode, "code") : text(item, "destinationStationCode", "destinationStation", "destination");
            String dstName = dstNode != null && dstNode.isObject() ? text(dstNode, "name") : text(item, "destinationStationName", "destinationName");

            String status = text(item, "status");
            String primaryType = text(item, "primaryType", "primary_type", "type");
            String message = text(item, "message");
            String lastUpdatedAt = text(item, "lastUpdatedAt", "last_updated_at", "updatedAt");
            String runningDays = text(item, "runningDays", "running_days", "days");
            Integer distance = firstInteger(item, "distance", "distanceKm");
            Integer duration = firstInteger(item, "duration", "durationMinutes");

            List<TrainExceptionDTO.ExceptionDetail> details = new ArrayList<>();
            JsonNode excArray = item.get("exceptions");
            if (excArray != null && excArray.isArray()) {
                for (JsonNode exc : excArray) {
                    details.add(new TrainExceptionDTO.ExceptionDetail(
                        text(exc, "type"),
                        text(exc, "message")
                    ));
                }
            }

            if ((primaryType == null || primaryType.isBlank()) && !details.isEmpty() && details.get(0).type() != null) {
                primaryType = details.get(0).type();
            }
            if (primaryType == null || primaryType.isBlank()) {
                String fullText = ((status != null ? status : "") + " " + (message != null ? message : "")).toUpperCase();
                if (fullText.contains("CANCEL")) {
                    primaryType = "CANCELLED";
                } else if (fullText.contains("DIVERT")) {
                    primaryType = "DIVERTED";
                } else if (fullText.contains("RESCHED")) {
                    primaryType = "RESCHEDULED";
                } else {
                    primaryType = status != null && !status.isBlank() ? status : "DISRUPTED";
                }
            }

            list.add(new TrainExceptionDTO(
                trainNumber,
                trainName,
                trainType,
                startDate,
                srcCode,
                srcName,
                dstCode,
                dstName,
                status,
                primaryType,
                message,
                details,
                lastUpdatedAt,
                runningDays,
                distance,
                duration
            ));
        }
        return list;
    }

    public TrainExceptionSummaryDTO getTrainExceptionsSummary() {
        String path = "/trains/exceptions/summary";
        logger.info("RailRadar exceptions summary request: GET {}", path);
        try {
            JsonNode responseNode = executeGet(path);
            JsonNode data = dataNode(responseNode);
            if (data == null || data.isNull()) {
                return new TrainExceptionSummaryDTO(0, 0, 0, 0, 0, 0, 0, null);
            }
            return new TrainExceptionSummaryDTO(
                integer(data, "total"),
                integer(data, "cancelled"),
                firstInteger(data, "partiallyCancelled", "partially_cancelled"),
                firstInteger(data, "partialSource", "partial_source"),
                firstInteger(data, "partialDestination", "partial_destination"),
                integer(data, "diverted"),
                integer(data, "rescheduled"),
                text(data, "updatedAt", "updated_at")
            );
        } catch (Exception e) {
            logger.warn("RailRadar exceptions summary call failed: {}", e.getMessage());
            return new TrainExceptionSummaryDTO(0, 0, 0, 0, 0, 0, 0, null);
        }
    }

    public TrainDelayReportDTO getTrainDelayReport(String trainNumber) {
        String path = "/trains/" + trainNumber + "/delay";
        logger.info("RailRadar train delay request: GET {}", path);
        JsonNode responseNode = executeGet(path);
        JsonNode data = dataNode(responseNode);
        if (data == null || data.isNull()) {
            return new TrainDelayReportDTO(trainNumber, null, 0, 0, 100.0, 0, "A", "Normal operations");
        }
        return new TrainDelayReportDTO(
            text(data, "trainNumber", "number") != null ? text(data, "trainNumber", "number") : trainNumber,
            text(data, "trainName", "name"),
            firstInteger(data, "avgArrivalDelayMinutes", "avgDelayMinutes", "arrivalDelayMinutes", "arrivalDelay"),
            firstInteger(data, "avgDepartureDelayMinutes", "departureDelayMinutes", "departureDelay"),
            data.has("ontimePercentage") ? data.get("ontimePercentage").asDouble() : (data.has("ontime_percentage") ? data.get("ontime_percentage").asDouble() : 85.0),
            firstInteger(data, "totalRunsSampled", "samples", "runsSampled"),
            text(data, "punctualityGrade", "grade") != null ? text(data, "punctualityGrade", "grade") : "A",
            text(data, "remarks", "summary")
        );
    }

    public TrainFareDTO getTrainFare(String trainNumber, String source, String destination) {
        String path = "/trains/" + trainNumber + "/fare";
        if (source != null && !source.isBlank() && destination != null && !destination.isBlank()) {
            path += "?source=" + encode(source) + "&destination=" + encode(destination);
        }
        logger.info("RailRadar train fare request: GET {}", path);
        JsonNode responseNode = executeGet(path);
        JsonNode data = dataNode(responseNode);
        if (data == null || data.isNull()) {
            return null;
        }
        List<TrainFareDTO.ClassFare> fares = new ArrayList<>();
        JsonNode faresNode = data.has("fares") ? data.get("fares") 
            : (data.has("classes") ? data.get("classes") 
            : (data.has("classFares") ? data.get("classFares") 
            : (data.has("fareDetails") ? data.get("fareDetails") : null)));
        if (faresNode != null && faresNode.isArray()) {
            for (JsonNode f : faresNode) {
                fares.add(new TrainFareDTO.ClassFare(
                    text(f, "classCode", "code", "class"),
                    text(f, "className", "name"),
                    firstInteger(f, "baseFare", "base_fare"),
                    firstInteger(f, "totalFare", "total_fare", "fare")
                ));
            }
        }
        Integer distVal = firstInteger(data, "distance", "distanceKm", "distance_km");
        int dist = (distVal != null && distVal > 0) ? distVal : 350;
        if (fares.isEmpty()) {
            fares.add(new TrainFareDTO.ClassFare("2S", "Second Sitting", Math.max(45, (int)(dist * 0.22) + 30), Math.max(55, (int)(dist * 0.22) + 45)));
            fares.add(new TrainFareDTO.ClassFare("SL", "Sleeper Class", Math.max(140, (int)(dist * 0.42) + 80), Math.max(165, (int)(dist * 0.42) + 105)));
            fares.add(new TrainFareDTO.ClassFare("3A", "AC 3 Tier", Math.max(450, (int)(dist * 1.15) + 200), Math.max(500, (int)(dist * 1.15) + 260)));
            fares.add(new TrainFareDTO.ClassFare("2A", "AC 2 Tier", Math.max(680, (int)(dist * 1.65) + 320), Math.max(750, (int)(dist * 1.65) + 400)));
            fares.add(new TrainFareDTO.ClassFare("1A", "AC First Class", Math.max(1150, (int)(dist * 2.80) + 500), Math.max(1280, (int)(dist * 2.80) + 650)));
        }
        return new TrainFareDTO(
            text(data, "trainNumber", "number") != null ? text(data, "trainNumber", "number") : trainNumber,
            text(data, "trainName", "name"),
            text(data, "source", "fromStation", "sourceStation") != null ? text(data, "source", "fromStation", "sourceStation") : source,
            text(data, "destination", "toStation", "destinationStation") != null ? text(data, "destination", "toStation", "destinationStation") : destination,
            dist,
            fares
        );
    }

    private List<TrainFareDTO.ClassFare> calculateDistanceBasedFares(int dist) {
        List<TrainFareDTO.ClassFare> fares = new ArrayList<>();
        int d = Math.max(10, dist);

        // 2S (Second Sitting Reserved)
        int b2S;
        if (d <= 50) b2S = 35;
        else if (d <= 150) b2S = (int) Math.round(35 + (d - 50) * 0.35);
        else if (d <= 300) b2S = (int) Math.round(70 + (d - 150) * 0.333);
        else if (d <= 600) b2S = (int) Math.round(120 + (d - 300) * 0.28);
        else b2S = (int) Math.round(204 + (d - 600) * 0.22);
        int tot2S = (int) Math.round((b2S + 15 + 15) / 5.0) * 5;
        fares.add(new TrainFareDTO.ClassFare("2S", "Second Sitting", b2S, tot2S));

        // SL (Sleeper Class - Min Chargeable Distance: 200 km)
        int bSL;
        if (d <= 200) bSL = 150;
        else if (d <= 300) bSL = (int) Math.round(150 + (d - 200) * 0.385);
        else if (d <= 600) bSL = (int) Math.round(188 + (d - 300) * 0.50);
        else if (d <= 1000) bSL = (int) Math.round(338 + (d - 600) * 0.42);
        else if (d <= 1500) bSL = (int) Math.round(506 + (d - 1000) * 0.32);
        else if (d <= 1675) bSL = (int) Math.round(666 + (d - 1500) * 0.222);
        else if (d <= 2000) bSL = (int) Math.round(705 + (d - 1675) * 0.110);
        else bSL = (int) Math.round(741 + (d - 2000) * 0.072);
        int totSL = (int) Math.round((bSL + 20 + 30) / 5.0) * 5;
        fares.add(new TrainFareDTO.ClassFare("SL", "Sleeper Class", bSL, totSL));

        // CC (AC Chair Car - Min Chargeable Distance: 150 km)
        int bCC;
        if (d <= 150) bCC = 260;
        else if (d <= 300) bCC = (int) Math.round(260 + (d - 150) * 0.90);
        else if (d <= 600) bCC = (int) Math.round(395 + (d - 300) * 0.85);
        else if (d <= 1000) bCC = (int) Math.round(650 + (d - 600) * 0.70);
        else bCC = (int) Math.round(930 + (d - 1000) * 0.55);
        int totCC = (int) Math.round(((bCC + 40 + 45) * 1.05) / 5.0) * 5;
        fares.add(new TrainFareDTO.ClassFare("CC", "AC Chair Car", bCC, totCC));

        // 3E (AC 3 Tier Economy - Min Chargeable Distance: 300 km, Base: 453 -> Total SF: ₹565, Exp: ₹520)
        int b3E;
        if (d <= 300) b3E = 453;
        else if (d <= 600) b3E = (int) Math.round(453 + (d - 300) * 1.22);
        else if (d <= 1000) b3E = (int) Math.round(819 + (d - 600) * 1.05);
        else if (d <= 1500) b3E = (int) Math.round(1239 + (d - 1000) * 0.60);
        else if (d <= 1675) b3E = (int) Math.round(1539 + (d - 1500) * 0.46);
        else b3E = (int) Math.round(1620 + (d - 1675) * 0.33);
        int tot3E = (int) Math.round(((b3E + 40 + 45) * 1.05) / 5.0) * 5;
        fares.add(new TrainFareDTO.ClassFare("3E", "AC 3 Economy", b3E, tot3E));

        // 3A (AC 3 Tier - Min Chargeable Distance: 300 km)
        int b3A;
        if (d <= 300) b3A = 453;
        else if (d <= 600) b3A = (int) Math.round(453 + (d - 300) * 1.32);
        else if (d <= 1000) b3A = (int) Math.round(849 + (d - 600) * 1.21);
        else if (d <= 1500) b3A = (int) Math.round(1333 + (d - 1000) * 0.64);
        else if (d <= 1675) b3A = (int) Math.round(1653 + (d - 1500) * 0.46);
        else b3A = (int) Math.round(1734 + (d - 1675) * 0.36);
        int tot3A = (int) Math.round(((b3A + 40 + 45) * 1.05) / 5.0) * 5;
        fares.add(new TrainFareDTO.ClassFare("3A", "AC 3 Tier", b3A, tot3A));

        // 2A (AC 2 Tier - Min Chargeable Distance: 300 km)
        int b2A;
        if (d <= 300) b2A = 638;
        else if (d <= 600) b2A = (int) Math.round(638 + (d - 300) * 1.95);
        else if (d <= 1000) b2A = (int) Math.round(1223 + (d - 600) * 1.83);
        else if (d <= 1500) b2A = (int) Math.round(1955 + (d - 1000) * 0.90);
        else if (d <= 1675) b2A = (int) Math.round(2405 + (d - 1500) * 0.51);
        else b2A = (int) Math.round(2495 + (d - 1675) * 0.60);
        int tot2A = (int) Math.round(((b2A + 50 + 45) * 1.05) / 5.0) * 5;
        fares.add(new TrainFareDTO.ClassFare("2A", "AC 2 Tier", b2A, tot2A));

        // 1A (AC First Class - Min Chargeable Distance: 300 km)
        int b1A;
        if (d <= 300) b1A = 1115;
        else if (d <= 600) b1A = (int) Math.round(1115 + (d - 300) * 3.10);
        else if (d <= 1000) b1A = (int) Math.round(2045 + (d - 600) * 3.05);
        else if (d <= 1500) b1A = (int) Math.round(3265 + (d - 1000) * 1.50);
        else if (d <= 1675) b1A = (int) Math.round(4015 + (d - 1500) * 1.18);
        else b1A = (int) Math.round(4222 + (d - 1675) * 0.95);
        int tot1A = (int) Math.round(((b1A + 60 + 75) * 1.05) / 5.0) * 5;
        fares.add(new TrainFareDTO.ClassFare("1A", "AC First Class", b1A, tot1A));

        return fares;
    }

    public JourneyOverlayDTO getJourneyOverlay(Object requestBody) {
        String path = "/journeys/overlay";
        logger.info("RailRadar journey overlay request: POST {}", path);
        JsonNode responseNode = executePost(path, requestBody);
        JsonNode data = dataNode(responseNode);
        if (data == null || data.isNull()) {
            return new JourneyOverlayDTO(List.of());
        }
        List<JourneyOverlayDTO.LegDelay> delays = new ArrayList<>();
        JsonNode delaysNode = data.has("delays") ? data.get("delays") : (data.isArray() ? data : null);
        if (delaysNode != null && delaysNode.isArray()) {
            for (JsonNode d : delaysNode) {
                delays.add(new JourneyOverlayDTO.LegDelay(
                    text(d, "train", "trainNumber"),
                    text(d, "station", "stationCode"),
                    integer(d, "dayOfWeek"),
                    firstInteger(d, "arrivalDelayMinutes", "arrivalDelay"),
                    firstInteger(d, "departureDelayMinutes", "departureDelay"),
                    integer(d, "samples"),
                    integer(d, "maxDelayMinutes"),
                    integer(d, "ontimeCount")
                ));
            }
        }
        return new JourneyOverlayDTO(delays);
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
    private JsonNode dataNode(JsonNode responseNode) {
        if (responseNode == null) return null;
        JsonNode dataNode = responseNode.get("data");
        return dataNode == null || dataNode.isNull() ? null : dataNode;
    }

    private RailRadarTrain mapTrain(JsonNode trainNode, JsonNode routeNode) {
        if (trainNode == null || trainNode.isNull()) return null;
        List<RailRadarTrainStation> stations = new ArrayList<>();
        if (routeNode != null && routeNode.isArray()) {
            for (JsonNode stop : routeNode) {
                stations.add(mapTrainStation(stop));
            }
        }
        return new RailRadarTrain(
                text(trainNode, "trainNumber", "number"),
                text(trainNode, "trainName", "name"),
                text(trainNode, "trainType", "type"),
                text(trainNode, "category"),
                stationCode(trainNode.get("source"), text(trainNode, "sourceStation")),
                stationCode(trainNode.get("destination"), text(trainNode, "destinationStation")),
                integer(trainNode, "distance"),
                integer(trainNode, "duration"),
                runningDays(trainNode),
                true,
                text(trainNode, "coachPosition", "coach_position"),
                stations
        );
    }

    private RailRadarTrainStation mapTrainStation(JsonNode stop) {
        return new RailRadarTrainStation(
                integer(stop, "sequence"),
                stationInfo(stop),
                bool(stop, "isHalt"),
                timeText(stop, "arrival", "scheduledArrival"),
                timeText(stop, "departure", "scheduledDeparture"),
                integer(stop, "arrivalDay"),
                integer(stop, "departureDay"),
                decimal(stop, "distance"),
                decimal(stop, "speedToNextStationKmph")
        );
    }

    private RailRadarTrainLiveStatus mapLiveStatus(String requestedTrainNumber, JsonNode dataNode) {
        if (dataNode == null || dataNode.isNull()) return null;
        JsonNode route = dataNode.get("route");
        JsonNode current = dataNode.get("currentLocation");
        JsonNode previousHalt = dataNode.get("previousHalt");
        JsonNode nextHalt = dataNode.get("nextHalt");
        String currentCode = text(current, "stationCode", "code", "currentStationCode");
        JsonNode currentStop = findRouteStop(route, currentCode);
        List<RailRadarLiveStationStatus> stations = new ArrayList<>();
        List<String> skippedStations = new ArrayList<>();
        JsonNode skippedNode = dataNode.get("skippedStations");
        if (skippedNode == null) skippedNode = dataNode.get("skipped_stations");
        if (skippedNode != null && skippedNode.isArray()) {
            for (JsonNode sn : skippedNode) {
                if (sn.isTextual()) skippedStations.add(sn.asText());
                else if (sn.has("stationCode")) skippedStations.add(sn.get("stationCode").asText());
                else if (sn.has("code")) skippedStations.add(sn.get("code").asText());
            }
        }

        if (route != null && route.isArray()) {
            for (JsonNode stop : route) {
                String stopStatus = text(stop, "status");
                boolean stopCancelled = Boolean.TRUE.equals(bool(stop, "isCancelled", "is_cancelled", "cancelled"));
                boolean stopSkipped = Boolean.TRUE.equals(bool(stop, "isSkipped", "is_skipped", "skipped"));
                RailRadarStationInfo sInfo = stationInfo(stop);
                String stopCode = sInfo != null && sInfo.code() != null ? sInfo.code() : text(stop, "stationCode", "code");

                if (stopCode != null && (stopCancelled || stopSkipped || (stopStatus != null && (stopStatus.toLowerCase().contains("cancel") || stopStatus.toLowerCase().contains("skip"))))) {
                    if (!skippedStations.contains(stopCode)) {
                        skippedStations.add(stopCode);
                    }
                }

                String platform = text(stop, "platform", "platformNumber", "platform_number");
                if (platform != null && (platform.equalsIgnoreCase("null") || platform.trim().isEmpty())) {
                    platform = null;
                }

                stations.add(new RailRadarLiveStationStatus(
                        sInfo,
                        integer(stop, "sequence"),
                        text(stop, "scheduledArrival"),
                        text(stop, "scheduledDeparture"),
                        text(stop, "actualArrival"),
                        text(stop, "actualDeparture"),
                        firstInteger(stop, "delayMinutes", "delayDeparture", "delayArrival"),
                        stopStatus,
                        bool(stop, "isHalt"),
                        platform
                ));
            }
        }

        // Validate currentCode against cancelled/skipped stations
        if (currentCode != null && skippedStations.contains(currentCode)) {
            currentCode = null;
            currentStop = null;
        }

        // Resolve current location from current station name if available
        if (currentCode == null && current != null) {
            String cName = text(current, "stationName", "name");
            if (cName != null && route != null && route.isArray()) {
                for (JsonNode stop : route) {
                    RailRadarStationInfo sInfo = stationInfo(stop);
                    String sName = sInfo != null && sInfo.name() != null ? sInfo.name() : text(stop, "stationName", "name");
                    String sCode = sInfo != null && sInfo.code() != null ? sInfo.code() : text(stop, "stationCode", "code");
                    if (sName != null && (sName.equalsIgnoreCase(cName) || cName.toLowerCase().contains(sName.toLowerCase()) || sName.toLowerCase().contains(cName.toLowerCase()))) {
                        if (sCode != null && !skippedStations.contains(sCode)) {
                            currentCode = sCode;
                            currentStop = stop;
                            break;
                        }
                    }
                }
            }
        }

        // If scheduled origin is cancelled and train has not started, pick first active station as current origin
        if (currentCode == null && route != null && route.isArray() && !skippedStations.isEmpty()) {
            for (JsonNode stop : route) {
                RailRadarStationInfo sInfo = stationInfo(stop);
                String sCode = sInfo != null && sInfo.code() != null ? sInfo.code() : text(stop, "stationCode", "code");
                String sStatus = text(stop, "status");
                boolean isSkipped = sCode != null && skippedStations.contains(sCode);
                boolean isStopCancelled = sStatus != null && (sStatus.toLowerCase().contains("cancel") || sStatus.toLowerCase().contains("skip"));
                if (sCode != null && !isSkipped && !isStopCancelled) {
                    currentCode = sCode;
                    currentStop = stop;
                    break;
                }
            }
        }

        Integer currentSequence = integer(current, "sequence");
        if (currentSequence == null && currentStop != null) {
            currentSequence = integer(currentStop, "sequence");
        }
        Double progress = null;
        if (currentSequence != null && route != null && route.isArray() && route.size() > 0) {
            progress = Math.min(1.0, Math.max(0.0, currentSequence.doubleValue() / route.size()));
        }
        String nextCode = text(nextHalt, "stationCode", "code");
        String nextName = text(nextHalt, "stationName", "name");
        String runStatus = text(dataNode, "runStatus", "status");

        boolean isLastStop = false;
        if (route != null && route.isArray() && route.size() > 0) {
            JsonNode lastStop = route.get(route.size() - 1);
            RailRadarStationInfo lastInfo = stationInfo(lastStop);
            String lastCode = lastInfo != null && lastInfo.code() != null ? lastInfo.code() : text(lastStop, "stationCode", "code");
            if (currentCode != null && lastCode != null && currentCode.equalsIgnoreCase(lastCode)) {
                isLastStop = true;
            }
        }

        boolean reached = "0".equals(nextCode) || "0".equals(nextName) || 
                          isLastStop ||
                          (progress != null && progress >= 0.999) ||
                          (runStatus != null && (runStatus.toLowerCase().contains("reached") || runStatus.toLowerCase().contains("completed")));
        if (reached) {
            nextCode = "-";
            nextName = "Reached Destination";
            runStatus = "reached";
        } else {
            // Next halt must NEVER be a cancelled or skipped station!
            boolean nextIsSkipped = (nextCode != null && skippedStations.contains(nextCode)) ||
                                    (nextName != null && skippedStations.contains(nextName));
            if (nextIsSkipped || nextCode == null || nextCode.isBlank() || "-".equals(nextCode)) {
                if (route != null && route.isArray()) {
                    int searchSeq = currentSequence != null ? currentSequence : 0;
                    for (JsonNode stop : route) {
                        int seq = integer(stop, "sequence") != null ? integer(stop, "sequence") : 0;
                        if (seq > searchSeq) {
                            RailRadarStationInfo sInfo = stationInfo(stop);
                            String sCode = sInfo != null && sInfo.code() != null ? sInfo.code() : text(stop, "stationCode", "code");
                            String sName = sInfo != null && sInfo.name() != null ? sInfo.name() : text(stop, "stationName", "name");
                            boolean isHalt = Boolean.TRUE.equals(bool(stop, "isHalt"));
                            String sStatus = text(stop, "status");
                            boolean isSkipped = sCode != null && skippedStations.contains(sCode);
                            boolean isStopCancelled = sStatus != null && (sStatus.toLowerCase().contains("cancel") || sStatus.toLowerCase().contains("skip"));
                            if (isHalt && !isSkipped && !isStopCancelled) {
                                nextCode = sCode;
                                nextName = sName;
                                break;
                            }
                        }
                    }
                }
            }
        }
        
        String locInfo = locationInfo(currentCode, stationName(currentStop, text(current, "stationName", "name")), text(current, "status"), text(previousHalt, "stationName"), text(nextHalt, "stationName"));
        if (reached) {
            locInfo = "Reached Destination";
        }

        Boolean isCancelled = bool(dataNode, "isCancelled", "is_cancelled");
        Boolean isDiverted = bool(dataNode, "isDiverted", "is_diverted");
        Boolean isRescheduled = bool(dataNode, "isRescheduled", "is_rescheduled");
        String disruptionMessage = text(dataNode, "disruptionMessage", "disruption_message", "statusNote", "notice");
        if (runStatus != null) {
            String lower = runStatus.toLowerCase();
            if (lower.contains("cancel") && !lower.contains("partially")) isCancelled = true;
            if (lower.contains("divert")) isDiverted = true;
            if (lower.contains("reschedul")) isRescheduled = true;
        }
        if (!skippedStations.isEmpty()) {
            boolean allSkipped = route != null && route.isArray() && route.size() > 0 && skippedStations.size() >= route.size();
            if (allSkipped) {
                isCancelled = true;
            } else {
                // Partially cancelled: do NOT flag whole train as isCancelled
                if (isCancelled == null) {
                    isCancelled = false;
                }
            }
            if (disruptionMessage == null || disruptionMessage.isBlank()) {
                disruptionMessage = "Train cancelled / skipped at " + skippedStations.size() + " station(s): " + String.join(", ", skippedStations);
            }
        }

        return new RailRadarTrainLiveStatus(
                text(dataNode, "trainNumber", "number") != null ? text(dataNode, "trainNumber", "number") : requestedTrainNumber,
                text(dataNode, "trainName", "name"),
                runStatus,
                currentCode,
                stationName(currentStop, text(current, "stationName", "name", "currentStationName")),
                nextCode,
                nextName,
                integer(dataNode, "delayMinutes"),
                decimal(currentStop, "lat", "latitude"),
                decimal(currentStop, "lng", "longitude"),
                decimal(currentStop, "speedToNextStationKmph", "speed"),
                decimal(dataNode, "bearing"),
                progress,
                locInfo,
                text(dataNode, "lastUpdatedAt"),
                stations,
                isCancelled,
                isDiverted,
                isRescheduled,
                disruptionMessage,
                skippedStations
        );
    }

    private JsonNode findRouteStop(JsonNode route, String stationCode) {
        if (route == null || !route.isArray() || stationCode == null) return null;
        for (JsonNode stop : route) {
            if (stationCode.equalsIgnoreCase(text(stop, "stationCode", "code"))) {
                return stop;
            }
        }
        return null;
    }

    private RailRadarStationInfo stationInfo(JsonNode node) {
        if (node == null || node.isNull()) return null;
        JsonNode station = node.has("station") ? node.get("station") : node;
        return new RailRadarStationInfo(
                text(station, "stationCode", "code"),
                text(station, "stationName", "name"),
                decimal(station, "lat", "latitude"),
                decimal(station, "lng", "longitude")
        );
    }

    private String stationCode(JsonNode stationNode, String fallback) {
        String code = text(stationNode, "code", "stationCode");
        return code != null ? code : fallback;
    }

    private String stationName(JsonNode stationNode, String fallback) {
        String name = text(stationNode, "name", "stationName");
        return name != null ? name : fallback;
    }

    private String runningDays(JsonNode trainNode) {
        String direct = text(trainNode, "runningDays");
        if (direct != null) return direct;
        JsonNode runDays = trainNode.get("runDays");
        if (runDays == null || !runDays.isArray()) return null;
        List<String> days = new ArrayList<>();
        runDays.forEach(day -> days.add(day.asText()));
        return String.join(",", days);
    }

    private String locationInfo(String currentCode, String currentName, String status, String previousName, String nextName) {
        if (currentName != null || currentCode != null) {
            String place = currentName != null ? currentName : currentCode;
            return (status != null ? status : "Located") + " at " + place;
        }
        if (previousName != null && nextName != null) return "Between " + previousName + " and " + nextName;
        if (nextName != null) return "Approaching " + nextName;
        return "Live position reported by RailRadar";
    }

    private String timeText(JsonNode node, String... names) {
        String value = text(node, names);
        if (value != null && value.length() >= 16 && value.contains("T")) {
            return value.substring(11, 16);
        }
        return value;
    }

    private String text(JsonNode node, String... names) {
        if (node == null || node.isNull()) return null;
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && !value.isNull()) return value.asText();
        }
        return null;
    }

    private String firstText(JsonNode primary, JsonNode fallback, String... names) {
        String value = text(primary, names);
        return value != null ? value : text(fallback, names);
    }
    private Integer integer(JsonNode node, String name) {
        if (node == null || node.isNull()) return null;
        JsonNode value = node.get(name);
        return value == null || value.isNull() ? null : (int) Math.round(value.asDouble());
    }

    private Integer firstInteger(JsonNode node, String... names) {
        for (String name : names) {
            Integer value = integer(node, name);
            if (value != null) return value;
        }
        return null;
    }

    private Double decimal(JsonNode node, String... names) {
        if (node == null || node.isNull()) return null;
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && !value.isNull()) return value.asDouble();
        }
        return null;
    }

    private Boolean bool(JsonNode node, String... names) {
        if (node == null || node.isNull()) return null;
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && !value.isNull()) return value.asBoolean();
        }
        return null;
    }
    private <T> T unpackResponse(JsonNode responseNode, Class<T> targetClass) {
        if (responseNode == null) return null;
        JsonNode dataNode = responseNode.get("data");
        if (dataNode == null || dataNode.isNull()) {
            return null;
        }

        if (dataNode.isObject()) {
            if (dataNode.has("train")) {
                dataNode = dataNode.get("train");
            } else if (dataNode.has("route")) {
                dataNode = dataNode.get("route");
            } else if (dataNode.has("live_status")) {
                dataNode = dataNode.get("live_status");
            } else if (dataNode.has("liveStatus")) {
                dataNode = dataNode.get("liveStatus");
            } else if (dataNode.has("status")) {
                dataNode = dataNode.get("status");
            }
        }

        try {
            return this.objectMapper.treeToValue(dataNode, targetClass);
        } catch (Exception e) {
            throw new ApiException("Failed to deserialize external response into " + targetClass.getSimpleName(), e);
        }
    }

    private <T> List<T> unpackListResponse(JsonNode responseNode, Class<T> elementClass) {
        if (responseNode == null) return List.of();
        JsonNode dataNode = responseNode.get("data");
        if (dataNode == null || dataNode.isNull()) {
            return List.of();
        }

        if (dataNode.isObject()) {
            if (dataNode.has("stations")) {
                dataNode = dataNode.get("stations");
            } else if (dataNode.has("trains")) {
                dataNode = dataNode.get("trains");
            }
        }

        try {
            return this.objectMapper.readerForListOf(elementClass).readValue(dataNode);
        } catch (Exception e) {
            throw new ApiException("Failed to deserialize external response into List of " + elementClass.getSimpleName(), e);
        }
    }

    public record RailRadarStation(
            @JsonAlias({"station_code", "code"}) String stationCode,
            @JsonAlias({"station_name", "name"}) String stationName,
            String city,
            String state,
            Double latitude,
            Double longitude,
            Boolean active
    ) {}

    public record RailRadarStationInfo(
            @JsonAlias({"station_code", "stationCode"}) String code,
            @JsonAlias({"station_name", "stationName"}) String name,
            @JsonAlias({"latitude"}) Double lat,
            @JsonAlias({"longitude"}) Double lng
    ) {}

    public record RailRadarTrainStation(
            Integer sequence,
            RailRadarStationInfo station,
            @JsonAlias({"is_halt"}) Boolean isHalt,
            String arrival,
            String departure,
            @JsonAlias({"arrival_day"}) Integer arrivalDay,
            @JsonAlias({"departure_day"}) Integer departureDay,
            @JsonAlias({"distance"}) Double distance,
            @JsonAlias({"speedToNextStationKmph"}) Double speedToNextStationKmph
    ) {}

    public record RailRadarTrain(
            @JsonAlias({"train_number"}) String trainNumber,
            @JsonAlias({"train_name"}) String trainName,
            @JsonAlias({"train_type"}) String trainType,
            String category,
            @JsonAlias({"source_station"}) String sourceStation,
            @JsonAlias({"destination_station"}) String destinationStation,
            Integer distance,
            Integer duration,
            @JsonAlias({"running_days"}) String runningDays,
            Boolean active,
            @JsonAlias({"coach_position", "coachPosition"}) String coachPosition,
            List<RailRadarTrainStation> stations
    ) {}

    public record RailRadarRouteStation(
            Integer sequence,
            String code,
            String name,
            @JsonAlias({"latitude"}) Double lat,
            @JsonAlias({"longitude"}) Double lng
    ) {}

    public record RailRadarTrainRoute(
            @JsonAlias({"train_number"}) String trainNumber,
            String format,
            List<RailRadarRouteStation> stops
    ) {}

    public record RailRadarLiveStationStatus(
            RailRadarStationInfo station,
            Integer sequence,
            @JsonAlias({"scheduled_arrival"}) String scheduledArrival,
            @JsonAlias({"scheduled_departure"}) String scheduledDeparture,
            @JsonAlias({"actual_arrival"}) String actualArrival,
            @JsonAlias({"actual_departure"}) String actualDeparture,
            @JsonAlias({"delay_minutes"}) Integer delayMinutes,
            String status,
            @JsonAlias({"is_halt"}) Boolean isHalt,
            @JsonAlias({"platform_number", "platformNumber"}) String platform
    ) {
        public RailRadarLiveStationStatus(
                RailRadarStationInfo station,
                Integer sequence,
                String scheduledArrival,
                String scheduledDeparture,
                String actualArrival,
                String actualDeparture,
                Integer delayMinutes,
                String status,
                Boolean isHalt
        ) {
            this(station, sequence, scheduledArrival, scheduledDeparture, actualArrival, actualDeparture, delayMinutes, status, isHalt, null);
        }
    }

    public record RailRadarTrainLiveStatus(
            @JsonAlias({"train_number"}) String trainNumber,
            @JsonAlias({"train_name"}) String trainName,
            @JsonAlias({"run_status"}) String runStatus,
            @JsonAlias({"current_station_code"}) String currentStationCode,
            @JsonAlias({"current_station_name"}) String currentStationName,
            @JsonAlias({"next_station_code"}) String nextStationCode,
            @JsonAlias({"next_station_name"}) String nextStationName,
            @JsonAlias({"delay_minutes"}) Integer delayMinutes,
            Double latitude,
            Double longitude,
            Double speed,
            Double bearing,
            @JsonAlias({"route_progress"}) Double routeProgress,
            @JsonAlias({"last_location_info"}) String lastLocationInfo,
            @JsonAlias({"last_updated_at"}) String lastUpdatedAt,
            List<RailRadarLiveStationStatus> stations,
            @JsonAlias({"is_cancelled", "isCancelled"}) Boolean isCancelled,
            @JsonAlias({"is_diverted", "isDiverted"}) Boolean isDiverted,
            @JsonAlias({"is_rescheduled", "isRescheduled"}) Boolean isRescheduled,
            @JsonAlias({"disruption_message", "disruptionMessage"}) String disruptionMessage,
            @JsonAlias({"skipped_stations", "skippedStations"}) List<String> skippedStations
    ) {}

    public record RailRadarStationBoardTrain(
            @JsonAlias({"train_number"}) String trainNumber,
            @JsonAlias({"train_name"}) String trainName,
            @JsonAlias({"train_type"}) String trainType,
            @JsonAlias({"running_days"}) String runningDays,
            @JsonAlias({"source_station"}) String sourceStation,
            @JsonAlias({"destination_station"}) String destinationStation,
            @JsonAlias({"scheduled_arrival"}) String scheduledArrival,
            @JsonAlias({"scheduled_departure"}) String scheduledDeparture,
            @JsonAlias({"actual_arrival"}) String actualArrival,
            @JsonAlias({"actual_departure"}) String actualDeparture,
            @JsonAlias({"delay_minutes"}) Integer delayMinutes,
            @JsonAlias({"is_halt"}) Boolean isHalt,
            @JsonAlias({"current_status"}) String currentStatus,
            @JsonAlias({"arrival_day", "day_offset", "dayOffset", "day"}) Integer arrivalDay
    ) {}

    public record RailRadarTrainBetweenStations(
            @JsonAlias({"train_number"}) String trainNumber,
            @JsonAlias({"train_name"}) String trainName,
            @JsonAlias({"train_type"}) String trainType,
            @JsonAlias({"from_station_code"}) String fromStationCode,
            @JsonAlias({"to_station_code"}) String toStationCode,
            @JsonAlias({"departure_time"}) String departureTime,
            @JsonAlias({"arrival_time"}) String arrivalTime,
            @JsonAlias({"duration_minutes"}) Integer durationMinutes,
            @JsonAlias({"running_days"}) String runningDays,
            @JsonAlias({"from_sequence"}) Integer fromSequence,
            @JsonAlias({"to_sequence"}) Integer toSequence
    ) {}
}














