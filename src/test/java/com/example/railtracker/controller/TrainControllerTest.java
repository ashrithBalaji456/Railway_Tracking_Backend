package com.example.railtracker.controller;

import com.example.railtracker.client.RailwayDataProvider;
import com.example.railtracker.dto.*;
import com.example.railtracker.repository.TrainRepository;
import com.example.railtracker.repository.TrainStationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TrainControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TrainRepository trainRepository;

    @Autowired
    private TrainStationRepository trainStationRepository;

    @MockBean
    private RailwayDataProvider railwayDataProvider;

    @BeforeEach
    void setUp() {
        trainStationRepository.deleteAll();
        trainRepository.deleteAll();
    }

    @Test
    void testGetTrainRouteCaching() throws Exception {
        TrainDto mockTrain = new TrainDto("12760", "Charminar", "Express", "SF", "HYB", "MS", 800, 840, "Daily", true, "ENG-GEN-GEN");
        when(railwayDataProvider.getTrainDetails("12760")).thenReturn(mockTrain);

        RouteStationDto s1 = new RouteStationDto("HYB", "Hyderabad", 1, null, LocalTime.of(18, 0), 0, 0, 0, "STOPPING", true, 17.385, 78.486, 0.0, 0.0);
        RouteStationDto s2 = new RouteStationDto("BZA", "Vijayawada", 2, LocalTime.of(23, 30), LocalTime.of(23, 45), 0, 0, 15, "STOPPING", true, 16.506, 80.648, 450.0, 110.0);
        TrainRouteDto mockRoute = new TrainRouteDto("12760", "Charminar", "Express", "HYB", "MS", 800, 840, "Daily", "ENG-GEN-GEN", Arrays.asList(s1, s2));

        when(railwayDataProvider.getTrainRoute("12760")).thenReturn(mockRoute);

        // Before request, no route is cached locally
        assertTrue(trainStationRepository.findByTrainTrainNumberOrderBySequenceNumberAsc("12760").isEmpty());

        mockMvc.perform(get("/api/v1/trains/12760/route")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.trainNumber").value("12760"))
                .andExpect(jsonPath("$.data.stations.length()").value(2));

        // After request, route must be persisted locally in our join table
        assertFalse(trainStationRepository.findByTrainTrainNumberOrderBySequenceNumberAsc("12760").isEmpty());
    }

    @Test
    void testGetTrainLiveStatus() throws Exception {
        TrainDto mockTrain = new TrainDto("12760", "Charminar", "Express", "SF", "HYB", "MS", 800, 840, "Daily", true, "ENG-GEN-GEN");
        when(railwayDataProvider.getTrainDetails("12760")).thenReturn(mockTrain);

        TrainLiveStatusDto mockLive = new TrainLiveStatusDto(
                "12760", "Charminar", "RUNNING", "WL", "Warangal", "BZA", "Vijayawada",
                25, 17.9689, 79.5941, 75.0, 120.0, 45.0, "Diverged near Warangal",
                LocalDateTime.of(2026, 8, 12, 12, 0, 0), Collections.emptyList()
        );

        when(railwayDataProvider.getTrainLiveStatus("12760")).thenReturn(mockLive);

        mockMvc.perform(get("/api/v1/trains/12760/live")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.trainNumber").value("12760"))
                .andExpect(jsonPath("$.data.runStatus").value("RUNNING"))
                .andExpect(jsonPath("$.data.delayMinutes").value(25))
                .andExpect(jsonPath("$.data.speed").value(75.0));
    }
}
