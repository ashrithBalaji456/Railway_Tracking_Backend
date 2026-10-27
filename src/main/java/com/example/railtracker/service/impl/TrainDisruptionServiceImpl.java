package com.example.railtracker.service.impl;

import com.example.railtracker.client.RailRadarClient;
import com.example.railtracker.dto.TrainExceptionDTO;
import com.example.railtracker.dto.TrainExceptionSummaryDTO;
import com.example.railtracker.entity.Train;
import com.example.railtracker.repository.TrainRepository;
import com.example.railtracker.service.TrainDisruptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TrainDisruptionServiceImpl implements TrainDisruptionService {

    private static final Logger logger = LoggerFactory.getLogger(TrainDisruptionServiceImpl.class);

    private final RailRadarClient railRadarClient;
    private final TrainRepository trainRepository;

    public TrainDisruptionServiceImpl(RailRadarClient railRadarClient, TrainRepository trainRepository) {
        this.railRadarClient = railRadarClient;
        this.trainRepository = trainRepository;
    }

    @Override
    @Cacheable(value = "train:exceptions", key = "#type != null && !#type.equalsIgnoreCase('ALL') ? #type : 'ALL'")
    public List<TrainExceptionDTO> getExceptions(String type) {
        logger.debug("Retrieving train exceptions for type: {}", type);
        List<TrainExceptionDTO> rawList = railRadarClient.getTrainExceptions(null);
        if (rawList == null || rawList.isEmpty()) {
            return List.of();
        }

        List<TrainExceptionDTO> enrichedList = new ArrayList<>(rawList.size());
        for (TrainExceptionDTO dto : rawList) {
            if (dto.trainNumber() == null || dto.trainNumber().isBlank()) {
                enrichedList.add(dto);
                continue;
            }

            try {
                Optional<Train> existingTrainOpt = trainRepository.findByTrainNumber(dto.trainNumber());
                if (existingTrainOpt.isPresent()) {
                    Train existing = existingTrainOpt.get();
                    String finalSrcCode = dto.sourceStationCode() != null && !dto.sourceStationCode().isBlank()
                            ? dto.sourceStationCode() : existing.getSourceStation();
                    String finalSrcName = dto.sourceStationName() != null && !dto.sourceStationName().isBlank()
                            ? dto.sourceStationName() : existing.getSourceStation();
                    String finalDstCode = dto.destinationStationCode() != null && !dto.destinationStationCode().isBlank()
                            ? dto.destinationStationCode() : existing.getDestinationStation();
                    String finalDstName = dto.destinationStationName() != null && !dto.destinationStationName().isBlank()
                            ? dto.destinationStationName() : existing.getDestinationStation();
                    String finalType = dto.trainType() != null && !dto.trainType().isBlank()
                            ? dto.trainType() : existing.getTrainType();
                    String finalDays = dto.runningDays() != null && !dto.runningDays().isBlank()
                            ? dto.runningDays() : existing.getRunningDays();
                    Integer finalDist = dto.distance() != null ? dto.distance() : existing.getDistance();
                    Integer finalDur = dto.duration() != null ? dto.duration() : existing.getDuration();

                    enrichedList.add(new TrainExceptionDTO(
                            dto.trainNumber(),
                            dto.trainName() != null && !dto.trainName().isBlank() ? dto.trainName() : existing.getTrainName(),
                            finalType,
                            dto.startDate(),
                            finalSrcCode,
                            finalSrcName,
                            finalDstCode,
                            finalDstName,
                            dto.status(),
                            dto.primaryType(),
                            dto.message(),
                            dto.exceptions(),
                            dto.lastUpdatedAt(),
                            finalDays,
                            finalDist,
                            finalDur
                    ));
                } else {
                    // Train is missing from Neon DB -> Auto-inject into Neon DB!
                    String days = dto.runningDays() != null && !dto.runningDays().isBlank()
                            ? dto.runningDays() : "Mon,Tue,Wed,Thu,Fri,Sat,Sun";
                    String src = dto.sourceStationCode() != null && !dto.sourceStationCode().isBlank()
                            ? dto.sourceStationCode() : (dto.sourceStationName() != null ? dto.sourceStationName() : "-");
                    String dst = dto.destinationStationCode() != null && !dto.destinationStationCode().isBlank()
                            ? dto.destinationStationCode() : (dto.destinationStationName() != null ? dto.destinationStationName() : "-");
                    String typeStr = dto.trainType() != null && !dto.trainType().isBlank() ? dto.trainType() : "Express";

                    Train newTrain = new Train(
                            dto.trainNumber(),
                            dto.trainName() != null && !dto.trainName().isBlank() ? dto.trainName() : "Train " + dto.trainNumber(),
                            typeStr,
                            "DISRUPTED",
                            src,
                            dst,
                            dto.distance() != null ? dto.distance() : 0,
                            dto.duration() != null ? dto.duration() : 0,
                            days
                    );
                    trainRepository.save(newTrain);
                    logger.info("Auto-injected missing disrupted train {} ({}) into Neon DB", dto.trainNumber(), dto.trainName());

                    enrichedList.add(new TrainExceptionDTO(
                            dto.trainNumber(),
                            dto.trainName(),
                            typeStr,
                            dto.startDate(),
                            src,
                            dto.sourceStationName() != null ? dto.sourceStationName() : src,
                            dst,
                            dto.destinationStationName() != null ? dto.destinationStationName() : dst,
                            dto.status(),
                            dto.primaryType(),
                            dto.message(),
                            dto.exceptions(),
                            dto.lastUpdatedAt(),
                            days,
                            dto.distance(),
                            dto.duration()
                    ));
                }
            } catch (Exception e) {
                logger.warn("Failed to check or auto-inject train {} into DB: {}", dto.trainNumber(), e.getMessage());
                enrichedList.add(dto);
            }
        }

        if (type != null && !type.isBlank() && !type.equalsIgnoreCase("ALL")) {
            String norm = type.trim().toUpperCase();
            return enrichedList.stream()
                    .filter(dto -> {
                        String t = (dto.primaryType() != null ? dto.primaryType() : dto.status() != null ? dto.status() : "").toUpperCase();
                        String m = (dto.message() != null ? dto.message() : "").toUpperCase();
                        if (norm.contains("CANCEL")) return t.contains("CANCEL") || m.contains("CANCEL");
                        if (norm.contains("DIVERT")) return t.contains("DIVERT") || m.contains("DIVERT");
                        if (norm.contains("RESCHED")) return t.contains("RESCHED") || m.contains("RESCHED");
                        return t.contains(norm) || m.contains(norm);
                    })
                    .collect(java.util.stream.Collectors.toList());
        }

        return enrichedList;
    }

    @Override
    @Cacheable(value = "train:exceptions:summary")
    public TrainExceptionSummaryDTO getExceptionsSummary() {
        logger.debug("Retrieving train exceptions summary");
        try {
            TrainExceptionSummaryDTO summary = railRadarClient.getTrainExceptionsSummary();
            if (summary != null && (summary.total() > 0 || summary.cancelled() > 0)) {
                return summary;
            }
        } catch (Exception e) {
            logger.warn("Could not retrieve summary directly from RailRadar: {}", e.getMessage());
        }

        List<TrainExceptionDTO> all = getExceptions("ALL");
        int total = all.size();
        int cancelled = 0;
        int diverted = 0;
        int rescheduled = 0;
        int partiallyCancelled = 0;

        for (TrainExceptionDTO dto : all) {
            String t = (dto.primaryType() != null ? dto.primaryType() : dto.status() != null ? dto.status() : "").toUpperCase();
            String m = (dto.message() != null ? dto.message() : "").toUpperCase();
            if (t.contains("CANCEL") || m.contains("CANCEL")) {
                cancelled++;
                if (t.contains("PARTIAL") || m.contains("PARTIAL")) {
                    partiallyCancelled++;
                }
            } else if (t.contains("DIVERT") || m.contains("DIVERT")) {
                diverted++;
            } else if (t.contains("RESCHED") || m.contains("RESCHED")) {
                rescheduled++;
            }
        }

        return new TrainExceptionSummaryDTO(
                total,
                cancelled,
                partiallyCancelled,
                0,
                0,
                diverted,
                rescheduled,
                java.time.Instant.now().toString()
        );
    }
}

