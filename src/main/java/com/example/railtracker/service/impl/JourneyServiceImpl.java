package com.example.railtracker.service.impl;

import com.example.railtracker.dto.JourneyOptionDto;
import com.example.railtracker.dto.TrainBetweenStationsDto;
import com.example.railtracker.dto.StationBoardTrainDto;
import com.example.railtracker.dto.TrainRouteDto;
import com.example.railtracker.dto.RouteStationDto;
import com.example.railtracker.entity.TrainStation;
import com.example.railtracker.repository.TrainStationRepository;
import com.example.railtracker.repository.StationRepository;
import com.example.railtracker.client.RailwayDataProvider;
import com.example.railtracker.service.JourneyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.example.railtracker.entity.Train;
import com.example.railtracker.repository.TrainRepository;

@Service
public class JourneyServiceImpl implements JourneyService {

    private static final Logger logger = LoggerFactory.getLogger(JourneyServiceImpl.class);
    private final TrainStationRepository trainStationRepository;
    private final RailwayDataProvider railwayDataProvider;
    private final StationRepository stationRepository;
    private final TrainRepository trainRepository;

    public JourneyServiceImpl(
            TrainStationRepository trainStationRepository,
            RailwayDataProvider railwayDataProvider,
            StationRepository stationRepository,
            TrainRepository trainRepository) {
        this.trainStationRepository = trainStationRepository;
        this.railwayDataProvider = railwayDataProvider;
        this.stationRepository = stationRepository;
        this.trainRepository = trainRepository;
    }

    @Override
    @Transactional
    public List<JourneyOptionDto> planJourney(String sourceCode, String destinationCode, String date) {
        logger.info("Solving journeys from '{}' to '{}' with date '{}'", sourceCode, destinationCode, date);
        
        List<JourneyOptionDto> options = new ArrayList<>();
        
        String selectedDay = null;
        if (date != null && !date.isBlank()) {
            try {
                java.time.LocalDate d = java.time.LocalDate.parse(date);
                selectedDay = d.getDayOfWeek().getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH).toLowerCase();
            } catch (Exception e) {
                logger.warn("Failed to parse date '{}': {}", date, e.getMessage());
            }
        }
        
        // 1. Retrieve all halts for source and destination
        List<TrainStation> sourceHalts = trainStationRepository.findByStationCodeWithDetails(sourceCode);
        List<TrainStation> destHalts = trainStationRepository.findByStationCodeWithDetails(destinationCode);

        // 2. Resolve direct trains
        for (TrainStation sHalt : sourceHalts) {
            for (TrainStation dHalt : destHalts) {
                if (sHalt.getTrain().getId().equals(dHalt.getTrain().getId()) 
                        && sHalt.getSequenceNumber() < dHalt.getSequenceNumber()) {
                    
                    if (selectedDay != null && sHalt.getTrain().getRunningDays() != null) {
                        String runningDays = sHalt.getTrain().getRunningDays().toLowerCase();
                        if (!runningDays.contains(selectedDay)) {
                            continue;
                        }
                    }

                    int duration = calculateDuration(
                            sHalt.getDepartureTime(), sHalt.getDepartureDay(),
                            dHalt.getArrivalTime(), dHalt.getArrivalDay()
                    );

                    options.add(new JourneyOptionDto(
                            true,
                            sHalt.getTrain().getTrainNumber(),
                            sHalt.getTrain().getTrainName(),
                            sourceCode,
                            sHalt.getStation().getStationName(),
                            sHalt.getDepartureTime(),
                            destinationCode,
                            dHalt.getStation().getStationName(),
                            dHalt.getArrivalTime(),
                            null, null, null, null, null, null,
                            0, null,
                            duration,
                            sHalt.getTrain().getRunningDays(),
                            null
                    ));
                }
            }
        }

        // 3. Resolve connecting trains (1 transfer)
        for (TrainStation sHalt : sourceHalts) {
            for (TrainStation dHalt : destHalts) {
                // Ignore same trains
                if (sHalt.getTrain().getId().equals(dHalt.getTrain().getId())) {
                    continue;
                }

                if (selectedDay != null && sHalt.getTrain().getRunningDays() != null) {
                    String runningDays = sHalt.getTrain().getRunningDays().toLowerCase();
                    if (!runningDays.contains(selectedDay)) {
                        continue;
                    }
                }

                // Fetch routes stops mapping
                List<TrainStation> haltsA = trainStationRepository.findByTrainTrainNumberOrderBySequenceNumberAsc(sHalt.getTrain().getTrainNumber())
                        .stream()
                        .filter(ts -> ts.getSequenceNumber() > sHalt.getSequenceNumber())
                        .collect(Collectors.toList());

                List<TrainStation> haltsB = trainStationRepository.findByTrainTrainNumberOrderBySequenceNumberAsc(dHalt.getTrain().getTrainNumber())
                        .stream()
                        .filter(ts -> ts.getSequenceNumber() < dHalt.getSequenceNumber())
                        .collect(Collectors.toList());

                // Find matching transfer halts Sc
                for (TrainStation hA : haltsA) {
                    for (TrainStation hB : haltsB) {
                        if (hA.getStation().getStationCode().equalsIgnoreCase(hB.getStation().getStationCode())) {
                            
                            // Layover duration mapping
                            int layover = calculateDuration(
                                    hA.getArrivalTime(), hA.getArrivalDay(),
                                    hB.getDepartureTime(), hB.getDepartureDay()
                            );

                            // Connection window check (30 mins to 6 hours)
                            if (layover >= 30 && layover <= 360) {
                                int firstLeg = calculateDuration(
                                        sHalt.getDepartureTime(), sHalt.getDepartureDay(),
                                        hA.getArrivalTime(), hA.getArrivalDay()
                                );

                                int secondLeg = calculateDuration(
                                        hB.getDepartureTime(), hB.getDepartureDay(),
                                        dHalt.getArrivalTime(), dHalt.getArrivalDay()
                                );

                                int totalDuration = firstLeg + layover + secondLeg;

                                // Layover warnings resolution
                                String warning = null;
                                if (layover < 45) {
                                    warning = "TIGHT_CONNECTION";
                                } else if (layover > 240) {
                                    warning = "LONG_LAYOVER";
                                }

                                options.add(new JourneyOptionDto(
                                        false,
                                        sHalt.getTrain().getTrainNumber(),
                                        sHalt.getTrain().getTrainName(),
                                        sourceCode,
                                        sHalt.getStation().getStationName(),
                                        sHalt.getDepartureTime(),
                                        destinationCode,
                                        dHalt.getStation().getStationName(),
                                        dHalt.getArrivalTime(),
                                        hB.getTrain().getTrainNumber(),
                                        hB.getTrain().getTrainName(),
                                        hA.getStation().getStationCode(),
                                        hA.getStation().getStationName(),
                                        hA.getArrivalTime(),
                                        hB.getDepartureTime(),
                                        layover,
                                        warning,
                                        totalDuration,
                                        sHalt.getTrain().getRunningDays(),
                                        hB.getTrain().getRunningDays()
                                ));
                            }
                        }
                    }
                }
            }
        }

        // Fallback to external provider if no local options found
        if (options.isEmpty()) {
            logger.info("No local journeys found between '{}' and '{}' with date '{}'. Querying external provider.", sourceCode, destinationCode, date);
            try {
                List<TrainBetweenStationsDto> trains = railwayDataProvider.getTrainsBetweenStations(sourceCode, destinationCode, date);
                if (trains != null && !trains.isEmpty()) {
                    String fromName = stationRepository.findByStationCode(sourceCode)
                            .map(s -> s.getStationName())
                            .orElse(sourceCode);
                    String toName = stationRepository.findByStationCode(destinationCode)
                            .map(s -> s.getStationName())
                            .orElse(destinationCode);
                            
                    for (TrainBetweenStationsDto t : trains) {
                        saveTrainIfMissing(t);
                        options.add(new JourneyOptionDto(
                                true,
                                t.trainNumber(),
                                t.trainName(),
                                sourceCode,
                                fromName,
                                t.departureTime(),
                                destinationCode,
                                toName,
                                t.arrivalTime(),
                                null, null, null, null, null, null,
                                0, null,
                                t.durationMinutes(),
                                t.runningDays(),
                                null
                        ));
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to solve external journeys between '{}' and '{}' with date '{}'", sourceCode, destinationCode, date, e);
            }
        }

        // Sort journeys: direct options first, then sort by total duration
        options.sort((o1, o2) -> {
            if (o1.direct() != o2.direct()) {
                return o1.direct() ? -1 : 1;
            }
            return o1.totalDurationMinutes().compareTo(o2.totalDurationMinutes());
        });

        return options;
    }

    private void saveTrainIfMissing(TrainBetweenStationsDto t) {
        if (t == null || t.trainNumber() == null || t.trainNumber().isBlank()) return;
        try {
            String num = t.trainNumber().trim();
            if (!trainRepository.existsByTrainNumber(num)) {
                String name = t.trainName() != null ? t.trainName().trim() : "Express";
                String type = t.trainType() != null ? t.trainType().trim() : "Express";
                String cat = determineCategory(name, type);
                Train train = new Train(
                        num,
                        name,
                        type,
                        cat,
                        t.fromStationCode() != null ? t.fromStationCode().trim() : "",
                        t.toStationCode() != null ? t.toStationCode().trim() : "",
                        null,
                        t.durationMinutes(),
                        t.runningDays() != null ? t.runningDays() : "Daily"
                );
                trainRepository.save(train);
                logger.info("Auto-populated new train {} ({}) into database from journey search", num, name);
            }
        } catch (Exception e) {
            logger.warn("Failed to auto-populate train {}: {}", t.trainNumber(), e.getMessage());
        }
    }

    private String determineCategory(String trainName, String trainType) {
        String text = ((trainName != null ? trainName : "") + " " + (trainType != null ? trainType : "")).toUpperCase();
        if (text.contains("VANDE BHARAT")) return "VANDE_BHARAT";
        if (text.contains("RAJDHANI")) return "RAJDHANI";
        if (text.contains("SHATABDI")) return "SHATABDI";
        if (text.contains("DURONTO")) return "DURONTO";
        if (text.contains("GARIB RATH")) return "GARIB_RATH";
        if (text.contains("HUMSAFAR")) return "HUMSAFAR";
        if (text.contains("TEJAS")) return "TEJAS";
        if (text.contains("SUPERFAST") || text.contains("SF")) return "SUPERFAST";
        if (text.contains("PASSENGER")) return "PASSENGER";
        if (text.contains("MEMU") || text.contains("DEMU") || text.contains("EMU")) return "SUBURBAN";
        if (text.contains("MAIL")) return "MAIL";
        return "EXPRESS";
    }

    private int calculateDuration(LocalTime startTime, int startDay, LocalTime endTime, int endDay) {
        int dayDiff = endDay - startDay;
        int startSecs = startTime.toSecondOfDay();
        int endSecs = endTime.toSecondOfDay();
        
        int durationSecs = (dayDiff * 86400) + (endSecs - startSecs);
        
        // Handle connections rolling over past midnight
        if (durationSecs < 0) {
            durationSecs += 86400; // roll forward 24h
        }
        
        return durationSecs / 60;
    }
}
