package com.example.railtracker.controller;

import com.example.railtracker.client.RailwayDataProvider;
import com.example.railtracker.dto.StationBoardTrainDto;
import com.example.railtracker.dto.StationDto;
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
import java.time.LocalTime;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StationControllerTest {

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
    void testSearchStationsAndLocalCaching() throws Exception {
        StationDto mockStation = new StationDto("BZA", "Vijayawada Junction", "Vijayawada", "Andhra Pradesh", 16.506, 80.648, true);
        when(railwayDataProvider.searchStations("Vijayawada")).thenReturn(Arrays.asList(mockStation));

        mockMvc.perform(get("/api/v1/stations")
                .param("query", "Vijayawada")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].stationCode").value("BZA"))
                .andExpect(jsonPath("$.data[0].stationName").value("Vijayawada Junction"));

        // Verify it was saved to local repository
        assertTrue(stationRepository.findByStationCode("BZA").isPresent());
    }

    @Test
    void testGetStationBoardFilters() throws Exception {
        // First mock station lookup which is checked internally
        StationDto mockStation = new StationDto("BZA", "Vijayawada Junction", "Vijayawada", "Andhra Pradesh", 16.506, 80.648, true);
        when(railwayDataProvider.searchStations("BZA")).thenReturn(Arrays.asList(mockStation));

        StationBoardTrainDto t1 = new StationBoardTrainDto("12760", "Charminar", "Express", "mon,tue,wed,thu,fri,sat,sun", "HYB", "MS", LocalTime.of(10, 30), LocalTime.of(10, 45), null, null, 0, "STOPPING", true, "RUNNING", 0);
        StationBoardTrainDto t2 = new StationBoardTrainDto("12840", "Chennai Mail", "Express", "mon,tue,wed,thu,fri,sat,sun", "HWH", "MAS", LocalTime.of(10, 47), LocalTime.of(10, 47), null, null, 0, "PASS_THROUGH", false, "RUNNING", 0);

        when(railwayDataProvider.getStationBoard("BZA", true)).thenReturn(Arrays.asList(t1, t2));

        // Test filtering STOPPING
        mockMvc.perform(get("/api/v1/stations/BZA/trains")
                .param("type", "STOPPING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].trainNumber").value("12760"))
                .andExpect(jsonPath("$.data[0].stopType").value("STOPPING"));

        // Test filtering NON_STOP
        mockMvc.perform(get("/api/v1/stations/BZA/trains")
                .param("type", "NON_STOP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].trainNumber").value("12840"))
                .andExpect(jsonPath("$.data[0].stopType").value("PASS_THROUGH"));
    }
}
