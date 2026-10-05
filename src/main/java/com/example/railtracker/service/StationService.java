package com.example.railtracker.service;

import com.example.railtracker.dto.LiveStationBoardResponse;
import com.example.railtracker.dto.StationBoardTrainDto;
import com.example.railtracker.dto.StationDto;
import java.util.List;

public interface StationService {
    List<StationDto> searchStations(String query);
    StationDto getStationDetails(String stationCode);
    List<StationBoardTrainDto> getStationTrains(String stationCode, String type);
    List<StationBoardTrainDto> getStationLiveBoard(String stationCode, int hoursAhead, String type);
    LiveStationBoardResponse getLiveStationBoardDetails(String stationCode, int hours);
}
