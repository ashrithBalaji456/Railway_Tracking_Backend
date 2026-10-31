package com.example.railtracker.controller;

import com.example.railtracker.client.RailwayDataProvider;
import com.example.railtracker.dto.*;
import com.example.railtracker.repository.StationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Arrays;
import java.util.List;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LiveStationBoardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StationRepository stationRepository;

    @MockBean
    private RailwayDataProvider railwayDataProvider;

    @BeforeEach
    void setUp() {
        stationRepository.deleteAll();
    }

    @Test
    void testGetLiveStationBoardSuccess() throws Exception {
        // Prepare mock station details to seed local repository
        StationDto mockStation = new StationDto("BZA", "Vijayawada Junction", "Vijayawada", "Andhra Pradesh", 16.506, 80.648, true);
        when(railwayDataProvider.searchStations("BZA")).thenReturn(Arrays.asList(mockStation));

        LiveStationBoardResponse.StationInfo stationInfo = new LiveStationBoardResponse.StationInfo("BZA", "VIJAYAWADA JN", "Vijayawada", 16.517968, 80.619572);
        LiveStationBoardResponse.WindowInfo windowInfo = new LiveStationBoardResponse.WindowInfo("10:35", "18:35", 4);
        
        String todayIso = java.time.LocalDate.now().toString();
        LiveStationBoardResponse.LiveStationBoardTrain train = new LiveStationBoardResponse.LiveStationBoardTrain(
                "12805",
                "Lingampalli Janmabhoomi SF Express",
                "Superfast Express",
                new LiveStationBoardResponse.StationCodeName("VSKP", "VISAKHAPATNAM"),
                new LiveStationBoardResponse.StationCodeName("LPI", "LINGAMPALLI"),
                StationPassType.STOPPING,
                "12:02",
                "12:12",
                todayIso + "T12:25:00+05:30",
                todayIso + "T12:35:00+05:30",
                23,
                "5",
                StationTrainLiveStatus.AT_STATION,
                "mon,tue,wed,thu,fri,sat,sun"
        );

        LiveStationBoardResponse mockResponse = new LiveStationBoardResponse(stationInfo, windowInfo, List.of(train));
        when(railwayDataProvider.getLiveStationBoardDetails("BZA", 4)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/stations/BZA/live")
                .param("hours", "4")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.station.code").value("BZA"))
                .andExpect(jsonPath("$.data.trains[0].trainNumber").value("12805"))
                .andExpect(jsonPath("$.data.trains[0].passType").value("STOPPING"));
    }

    @Test
    void testGetLiveStationBoardInvalidHours() throws Exception {
        mockMvc.perform(get("/api/stations/BZA/live")
                .param("hours", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Supported time windows are 2, 4, 6, or 8 hours."));
    }

    @Test
    void testGetLiveStationBoardBlankStationCode() throws Exception {
        mockMvc.perform(get("/api/stations/ /live")
                .param("hours", "4")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Station code must not be blank."));
    }

    @Test
    void testGetLiveStationBoardMidnightCrossingWindow() throws Exception {
        StationDto mockStation = new StationDto("MCI", "Manchiryal", "Mancherial", "Telangana", 18.879, 79.449, true);
        when(railwayDataProvider.searchStations("MCI")).thenReturn(Arrays.asList(mockStation));

        LiveStationBoardResponse.StationInfo stationInfo = new LiveStationBoardResponse.StationInfo("MCI", "MANCHIRYAL", "Mancherial", 18.879, 79.449);
        LiveStationBoardResponse.WindowInfo windowInfo = new LiveStationBoardResponse.WindowInfo("23:13", "05:13", 4);

        java.time.ZonedDateTime nowIst = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));
        java.time.ZonedDateTime expectedPassage = nowIst.plusMinutes(30);
        String passageIso = expectedPassage.format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        LiveStationBoardResponse.LiveStationBoardTrain train = new LiveStationBoardResponse.LiveStationBoardTrain(
                "17035",
                "Balharshah Express",
                "Mail/Express",
                new LiveStationBoardResponse.StationCodeName("KZJ", "Kazipet"),
                new LiveStationBoardResponse.StationCodeName("BPQ", "Balharshah"),
                StationPassType.STOPPING,
                "00:39",
                "00:40",
                passageIso,
                passageIso,
                0,
                "1",
                StationTrainLiveStatus.UPCOMING,
                "mon,tue,wed,thu,fri,sat,sun"
        );

        LiveStationBoardResponse mockResponse = new LiveStationBoardResponse(stationInfo, windowInfo, List.of(train));
        when(railwayDataProvider.getLiveStationBoardDetails("MCI", 4)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/stations/MCI/live")
                .param("hours", "4")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.trains[0].trainNumber").value("17035"))
                .andExpect(jsonPath("$.data.trains[0].passType").value("STOPPING"));
    }
}
