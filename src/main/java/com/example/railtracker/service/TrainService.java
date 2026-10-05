package com.example.railtracker.service;

import com.example.railtracker.dto.TrainDto;
import com.example.railtracker.dto.TrainLiveStatusDto;
import com.example.railtracker.dto.TrainRouteDto;
import java.util.List;

public interface TrainService {
    List<TrainDto> searchTrains(String query);
    TrainDto getTrainDetails(String trainNumber);
    TrainDto getTrainDetails(String trainNumber, String journeyDate);
    TrainRouteDto getTrainRoute(String trainNumber);
    TrainRouteDto getTrainRoute(String trainNumber, String journeyDate);
    TrainLiveStatusDto getTrainLiveStatus(String trainNumber);
    TrainLiveStatusDto getTrainLiveStatus(String trainNumber, String journeyDate);
}
