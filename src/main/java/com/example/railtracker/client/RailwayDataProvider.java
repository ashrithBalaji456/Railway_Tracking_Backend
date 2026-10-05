package com.example.railtracker.client;

import com.example.railtracker.dto.*;
import java.util.List;

public interface RailwayDataProvider {
    List<StationDto> searchStations(String query);

    List<TrainDto> searchTrains(String query);
    
    TrainDto getTrainDetails(String trainNumber);
    
    TrainDto getTrainDetails(String trainNumber, String journeyDate);
    
    TrainRouteDto getTrainRoute(String trainNumber);
    
    TrainRouteDto getTrainRoute(String trainNumber, String journeyDate);
    
    TrainLiveStatusDto getTrainLiveStatus(String trainNumber);
    
    TrainLiveStatusDto getTrainLiveStatus(String trainNumber, String journeyDate);
    
    List<StationBoardTrainDto> getStationBoard(String stationCode, boolean includeIntermediate);
    
    List<StationBoardTrainDto> getStationLiveBoard(String stationCode, int hoursAhead, String type);
    
    LiveStationBoardResponse getLiveStationBoardDetails(String stationCode, int hours);
    
    List<TrainBetweenStationsDto> getTrainsBetweenStations(String fromStationCode, String toStationCode, String date);
}

