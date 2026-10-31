package com.example.railtracker.service;

import com.example.railtracker.dto.LiveStationBoardResponse;
import com.example.railtracker.dto.StationBoardTrainDto;
import com.example.railtracker.dto.StationDto;
import java.util.List;

public interface StationService {
    List<StationDto> searchStations(String query);
    StationDto getStationDetails(String stationCode);

    default List<StationBoardTrainDto> getStationTrains(String stationCode, String type) {
        return getStationTrains(stationCode, type, false);
    }
    List<StationBoardTrainDto> getStationTrains(String stationCode, String type, boolean forceRefresh);

    List<StationBoardTrainDto> getStationLiveBoard(String stationCode, int hoursAhead, String type);

    default LiveStationBoardResponse getLiveStationBoardDetails(String stationCode, int hours) {
        return getLiveStationBoardDetails(stationCode, hours, false);
    }
    LiveStationBoardResponse getLiveStationBoardDetails(String stationCode, int hours, boolean forceRefresh);
}
