package com.example.railtracker.service;

import com.example.railtracker.client.RailwayDataProvider;
import com.example.railtracker.dto.TrainLiveStatusDto;
import com.example.railtracker.dto.TrainLiveStatusDto.StationStatusDto;
import com.example.railtracker.exception.ResourceNotFoundException;
import com.example.railtracker.repository.StationRepository;
import com.example.railtracker.repository.TrainRepository;
import com.example.railtracker.repository.TrainStationRepository;
import com.example.railtracker.service.impl.TrainServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TrainServiceMultiRakeLiveTrackingTest {

    private TrainRepository trainRepository;
    private StationRepository stationRepository;
    private TrainStationRepository trainStationRepository;
    private RailwayDataProvider railwayDataProvider;
    private TrainServiceImpl trainService;

    @BeforeEach
    void setUp() {
        trainRepository = mock(TrainRepository.class);
        stationRepository = mock(StationRepository.class);
        trainStationRepository = mock(TrainStationRepository.class);
        railwayDataProvider = mock(RailwayDataProvider.class);

        trainService = new TrainServiceImpl(
                trainRepository,
                stationRepository,
                trainStationRepository,
                railwayDataProvider
        );
    }

    @Test
    void testMultiRake_RequestedJourneyDateMatchesStartDate() {
        String trainNumber = "12721";
        String requestedDate = "2026-10-31";

        StationStatusDto stopMci = new StationStatusDto(
                "MCI", "Mancherial", 15, "02:40", "02:42",
                "03:15", "03:17", 35, "DEPARTED", true, "1",
                null, null, false
        );

        TrainLiveStatusDto rakeA = new TrainLiveStatusDto(
                trainNumber, "Dakshin SF Express", "RUNNING",
                "MCI", "Mancherial", "RDM", "Ramagundam",
                35, 18.867, 79.463, 72.0, 160.0, 0.65,
                "Departed MCI", LocalDateTime.now(), List.of(stopMci),
                false, false, false, null, List.of(),
                "2026-10-31"
        );

        when(railwayDataProvider.getTrainLiveStatus(trainNumber, requestedDate)).thenReturn(rakeA);

        TrainLiveStatusDto result = trainService.getTrainLiveStatus(trainNumber, requestedDate);

        assertNotNull(result);
        assertEquals("12721", result.trainNumber());
        assertEquals("2026-10-31", result.startDate());
        assertEquals("MCI", result.currentStationCode());
        verify(railwayDataProvider, times(1)).getTrainLiveStatus(trainNumber, requestedDate);
    }

    @Test
    void testMultiRake_RequestedJourneyDateMismatchesStartDate_ThrowsNotFound() {
        String trainNumber = "12721";
        String requestedFutureDate = "2026-11-01"; // Rake C has not started yet

        // Provider mistakenly returns Rake A (started 2026-10-31)
        TrainLiveStatusDto rakeA = new TrainLiveStatusDto(
                trainNumber, "Dakshin SF Express", "RUNNING",
                "MCI", "Mancherial", "RDM", "Ramagundam",
                35, 18.867, 79.463, 72.0, 160.0, 0.65,
                "Departed MCI", LocalDateTime.now(), List.of(),
                false, false, false, null, List.of(),
                "2026-10-31"
        );

        when(railwayDataProvider.getTrainLiveStatus(trainNumber, requestedFutureDate)).thenReturn(rakeA);

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                trainService.getTrainLiveStatus(trainNumber, requestedFutureDate)
        );

        assertTrue(ex.getMessage().contains("No running rake found for train 12721 with journey date 2026-11-01"));
        assertTrue(ex.getMessage().contains("2026-10-31"));
    }

    @Test
    void testIntermediateStationPredictedVsActualMapping() {
        StationStatusDto passedStation = new StationStatusDto(
                "BPQ", "Balharshah", 12, "01:10", "01:15",
                "01:45", "01:50", 35, "DEPARTED", true, "1",
                null, null, false
        );

        StationStatusDto upcomingStation = new StationStatusDto(
                "KZJ", "Kazipet Jn", 18, "04:30", "04:32",
                null, null, 35, "UPCOMING", true, "2",
                "05:05", "05:07", true
        );

        TrainLiveStatusDto liveStatus = new TrainLiveStatusDto(
                "12763", "Padmavathi SF Express", "RUNNING",
                "WL", "Warangal", "KZJ", "Kazipet",
                34, 17.97, 79.60, 65.0, 270.0, 0.8,
                "Approaching KZJ", LocalDateTime.now(),
                List.of(passedStation, upcomingStation),
                false, false, false, null, List.of(),
                "2026-10-31"
        );

        when(railwayDataProvider.getTrainLiveStatus("12763", "2026-10-31")).thenReturn(liveStatus);

        TrainLiveStatusDto result = trainService.getTrainLiveStatus("12763", "2026-10-31");

        assertNotNull(result);
        assertEquals(2, result.stations().size());

        // Passed station has confirmed actual timings and isPredicted=false
        StationStatusDto s1 = result.stations().get(0);
        assertEquals("BPQ", s1.stationCode());
        assertEquals("01:45", s1.actualArrival());
        assertEquals("01:50", s1.actualDeparture());
        assertFalse(Boolean.TRUE.equals(s1.isPredicted()));

        // Upcoming station has expected predicted timings and isPredicted=true, actualArrival is null
        StationStatusDto s2 = result.stations().get(1);
        assertEquals("KZJ", s2.stationCode());
        assertNull(s2.actualArrival());
        assertNull(s2.actualDeparture());
        assertEquals("05:05", s2.expectedArrival());
        assertEquals("05:07", s2.expectedDeparture());
        assertTrue(Boolean.TRUE.equals(s2.isPredicted()));
    }

    @Test
    void testNullJourneyDate_DelegatesToSingleArgProvider() {
        TrainLiveStatusDto defaultLive = new TrainLiveStatusDto(
                "12760", "Charminar Express", "RUNNING",
                "SC", "Secunderabad", "KZJ", "Kazipet",
                0, 17.43, 78.50, 80.0, 90.0, 0.1,
                "Near SC", LocalDateTime.now(), List.of(),
                false, false, false, null, List.of(),
                "2026-11-01"
        );

        when(railwayDataProvider.getTrainLiveStatus("12760")).thenReturn(defaultLive);

        TrainLiveStatusDto result = trainService.getTrainLiveStatus("12760", null);

        assertNotNull(result);
        assertEquals("12760", result.trainNumber());
        verify(railwayDataProvider, times(1)).getTrainLiveStatus("12760");
    }
}

