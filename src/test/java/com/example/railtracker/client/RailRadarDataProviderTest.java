package com.example.railtracker.client;

import com.example.railtracker.client.impl.RailRadarDataProvider;
import com.example.railtracker.client.RailRadarClient.*;
import com.example.railtracker.dto.RouteStationDto;
import com.example.railtracker.dto.StationBoardTrainDto;
import com.example.railtracker.dto.TrainRouteDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class RailRadarDataProviderTest {

    private RailRadarClient client;
    private RailRadarDataProvider provider;

    @BeforeEach
    void setUp() {
        client = Mockito.mock(RailRadarClient.class);
        provider = new RailRadarDataProvider(client);
    }

    @Test
    void testTrainRouteMappingStopTypes() {
        RailRadarRouteStation haltStation = new RailRadarRouteStation(1, "BZA", "Vijayawada", 16.506, 80.648);
        RailRadarRouteStation passStation = new RailRadarRouteStation(2, "KMT", "Khammam", 17.25, 80.15);
        RailRadarRouteStation haltStation2 = new RailRadarRouteStation(3, "WL", "Warangal", 17.97, 79.60);

        RailRadarTrainRoute route = new RailRadarTrainRoute(
                "12760", "polyline",
                Arrays.asList(haltStation, passStation, haltStation2)
        );

        RailRadarTrain details = new RailRadarTrain(
                "12760", "Charminar Express", "Express", "EXPRESS", "HYB", "MS", 800, 840, "Daily", true,
                "ENG-GEN-GEN",
                Arrays.asList(
                        new RailRadarTrainStation(1, new RailRadarStationInfo("BZA", "Vijayawada", 16.506, 80.648), true, "10:30", "10:45", 0, 0, 0.0, 0.0),
                        new RailRadarTrainStation(2, new RailRadarStationInfo("KMT", "Khammam", 17.25, 80.15), false, "--", "--", 0, 0, 120.0, 0.0),
                        new RailRadarTrainStation(3, new RailRadarStationInfo("WL", "Warangal", 17.97, 79.60), true, "12:45:00", "12:50:00", 0, 0, 200.0, 0.0)
                )
        );

        when(client.getTrainRoute("12760")).thenReturn(route);
        when(client.getTrainDetails("12760")).thenReturn(details);

        TrainRouteDto result = provider.getTrainRoute("12760");

        assertNotNull(result);
        assertEquals("12760", result.trainNumber());
        assertEquals("Charminar Express", result.trainName());
        assertEquals(3, result.stations().size());

        // Halt 1
        RouteStationDto s1 = result.stations().get(0);
        assertEquals("BZA", s1.stationCode());
        assertEquals("STOPPING", s1.stopType());
        assertTrue(s1.isHalt());
        assertEquals(LocalTime.of(10, 30), s1.arrivalTime());
        assertEquals(LocalTime.of(10, 45), s1.departureTime());

        // Pass 1
        RouteStationDto s2 = result.stations().get(1);
        assertEquals("KMT", s2.stationCode());
        assertEquals("PASS_THROUGH", s2.stopType());
        assertFalse(s2.isHalt());
        assertNull(s2.arrivalTime());
        assertNull(s2.departureTime());

        // Halt 2
        RouteStationDto s3 = result.stations().get(2);
        assertEquals("WL", s3.stationCode());
        assertEquals("STOPPING", s3.stopType());
        assertTrue(s3.isHalt());
        assertEquals(LocalTime.of(12, 45), s3.arrivalTime());
        assertEquals(LocalTime.of(12, 50), s3.departureTime());
    }

    @Test
    void testStationLiveBoardFiltering() {
        RailRadarStationBoardTrain t1 = new RailRadarStationBoardTrain("12760", "Charminar Express", "Express", "mon,tue,wed,thu,fri,sat,sun", "HYB", "MS", "10:30", "10:45", "10:35", "10:50", 5, true, "RUNNING", 0);
        RailRadarStationBoardTrain t2 = new RailRadarStationBoardTrain("12840", "Chennai Mail", "Express", "mon,tue,wed,thu,fri,sat,sun", "HWH", "MAS", "10:47", "10:47", "11:00", "11:00", 13, false, "RUNNING", 0);

        when(client.getStationLiveBoard("BZA", 2)).thenReturn(Arrays.asList(t1, t2));

        // ALL
        List<StationBoardTrainDto> all = provider.getStationLiveBoard("BZA", 2, "ALL");
        assertEquals(2, all.size());

        // STOPPING
        List<StationBoardTrainDto> stopping = provider.getStationLiveBoard("BZA", 2, "STOPPING");
        assertEquals(1, stopping.size());
        assertEquals("12760", stopping.get(0).trainNumber());
        assertTrue(stopping.get(0).isHalt());

        // NON_STOP
        List<StationBoardTrainDto> nonStop = provider.getStationLiveBoard("BZA", 2, "NON_STOP");
        assertEquals(1, nonStop.size());
        assertEquals("12840", nonStop.get(0).trainNumber());
        assertFalse(nonStop.get(0).isHalt());
    }
}

