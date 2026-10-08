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
    public RailRadarTrain getTrainDetails(String trainNumber) {
        return getTrainDetails(trainNumber, null);
    }

    public RailRadarTrain getTrainDetails(String trainNumber, String journeyDate) {
        String path = "/trains/" + trainNumber;
        if (journeyDate != null && !journeyDate.isBlank()) {
            path += "?journeyDate=" + journeyDate;
        }
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
            path += "&journeyDate=" + journeyDate;
        }
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
            path += "?journeyDate=" + journeyDate;
        }
        JsonNode responseNode = executeGet(path);
        logger.debug("RailRadar live response for {} on date {}: {}", trainNumber, journeyDate, responseNode);
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
        return mapStationBoardResponse(responseNode);
    }

    public List<RailRadarStationBoardTrain> getStationLiveBoard(String stationCode, int hoursAhead) {
        JsonNode responseNode = executeGet("/stations/" + stationCode + "/live?includeIntermediate=true");
        return mapStationBoardResponse(responseNode);
    }

    public LiveStationBoardResponse getLiveStationBoardDetails(String stationCode, int hours) {
        String path = "/stations/" + stationCode + "/live?hours=" + hours + "&includeIntermediate=true";
        JsonNode responseNode = executeGet(path);
        logger.info("RailRadar live station board response for '{}' (hours={}): {}", stationCode, hours, responseNode);
        return mapLiveStationBoard(responseNode, hours);
    }

    private LiveStationBoardResponse mapLiveStationBoard(JsonNode responseNode, int hours) {
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
                StationPassType passType = isHalt ? StationPassType.STOPPING : StationPassType.PASS_THROUGH;

                String scheduledArrival = text(stop, "arrival");
                String scheduledDeparture = text(stop, "departure");

                String expectedArrival = text(live, "expectedArrivalTime");
                String expectedDeparture = text(live, "expectedDepartureTime");

                Integer delayMinutes = integer(live, "delayMinutes");
                String platform = text(stop, "platform");
                if (platform == null || platform.equalsIgnoreCase("null") || platform.trim().isEmpty()) {
                    platform = null;
                }

                String liveType = text(live, "type");
                StationTrainLiveStatus liveStatus = mapLiveStatusEnum(liveType);

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
                        runDaysStr
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

    private List<RailRadarStationBoardTrain> mapStationBoardResponse(JsonNode responseNode) {
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
            trains.add(mapStationBoardTrain(item));
        }
        return trains;
    }

    private RailRadarStationBoardTrain mapStationBoardTrain(JsonNode item) {
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

        Integer arrivalDay = firstInteger(stop, "arrivalDay", "dayOffset", "arrivalDayOffset", "day");
        if (arrivalDay == null) arrivalDay = firstInteger(item, "arrivalDay", "dayOffset", "arrivalDayOffset", "day");
        if (arrivalDay == null) arrivalDay = 0;

        return new RailRadarStationBoardTrain(
                firstText(train, item, "trainNumber", "number", "trainNo", "train_no"),
                firstText(train, item, "trainName", "name", "train_name"),
                firstText(train, item, "trainType", "type", "train_type", "category"),
                runningDays(train),
                stationCode(source, firstText(train, item, "sourceStation", "source")),
                stationCode(destination, firstText(train, item, "destinationStation", "destination")),
                timeText(stop, "scheduledArrival", "arrival", "arrivalTime"),
                timeText(stop, "scheduledDeparture", "departure", "departureTime"),
                timeText(live, "actualArrival", "liveArrival", "expectedArrival"),
                timeText(live, "actualDeparture", "liveDeparture", "expectedDeparture"),
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
        if (route != null && route.isArray()) {
            for (JsonNode stop : route) {
                stations.add(new RailRadarLiveStationStatus(
                        stationInfo(stop),
                        integer(stop, "sequence"),
                        text(stop, "scheduledArrival"),
                        text(stop, "scheduledDeparture"),
                        text(stop, "actualArrival"),
                        text(stop, "actualDeparture"),
                        firstInteger(stop, "delayMinutes", "delayDeparture", "delayArrival"),
                        text(stop, "status"),
                        bool(stop, "isHalt")
                ));
            }
        }
        Integer currentSequence = integer(current, "sequence");
        Double progress = null;
        if (currentSequence != null && route != null && route.isArray() && route.size() > 0) {
            progress = Math.min(1.0, Math.max(0.0, currentSequence.doubleValue() / route.size()));
        }
        String nextCode = text(nextHalt, "stationCode", "code");
        String nextName = text(nextHalt, "stationName", "name");
        String runStatus = text(dataNode, "runStatus", "status");
        boolean reached = "0".equals(nextCode) || "0".equals(nextName) || 
                          (runStatus != null && (runStatus.toLowerCase().contains("reached") || runStatus.toLowerCase().contains("completed")));
        if (reached) {
            nextCode = "-";
            nextName = "Reached Destination";
        }
        
        String locInfo = locationInfo(currentCode, stationName(currentStop, text(current, "stationName", "name")), text(current, "status"), text(previousHalt, "stationName"), text(nextHalt, "stationName"));
        if (reached) {
            locInfo = "Reached Destination";
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
                stations
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

    private Boolean bool(JsonNode node, String name) {
        if (node == null || node.isNull()) return null;
        JsonNode value = node.get(name);
        return value == null || value.isNull() ? null : value.asBoolean();
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
            @JsonAlias({"is_halt"}) Boolean isHalt
    ) {}

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
            List<RailRadarLiveStationStatus> stations
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














