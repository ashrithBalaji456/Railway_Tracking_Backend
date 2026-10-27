package com.example.railtracker.service;

import com.example.railtracker.dto.*;
import java.util.List;

public interface TrainService {
    List<TrainDto> searchTrains(String query);
    TrainDto getTrainDetails(String trainNumber);
    TrainDto getTrainDetails(String trainNumber, String journeyDate);
    TrainRouteDto getTrainRoute(String trainNumber);
    TrainRouteDto getTrainRoute(String trainNumber, String journeyDate);
    TrainLiveStatusDto getTrainLiveStatus(String trainNumber);
    TrainLiveStatusDto getTrainLiveStatus(String trainNumber, String journeyDate);
    TrainDelayReportDTO getTrainDelayReport(String trainNumber);
    TrainFareDTO getTrainFare(String trainNumber, String source, String destination);
    List<TrainBetweenStationsDto> getTrainsBetween(String from, String to, String date);
}
