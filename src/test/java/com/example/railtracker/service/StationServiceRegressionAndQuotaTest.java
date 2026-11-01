package com.example.railtracker.service;

import com.example.railtracker.client.RailwayDataProvider;
import com.example.railtracker.dto.LiveStationBoardResponse;
import com.example.railtracker.dto.LiveStationBoardResponse.LiveStationBoardTrain;
import com.example.railtracker.dto.LiveStationBoardResponse.StationCodeName;
import com.example.railtracker.dto.LiveStationBoardResponse.StationInfo;
import com.example.railtracker.dto.LiveStationBoardResponse.WindowInfo;
import com.example.railtracker.dto.StationBoardTrainDto;
import com.example.railtracker.dto.StationPassType;
import com.example.railtracker.dto.StationTrainLiveStatus;
import com.example.railtracker.entity.Station;
import com.example.railtracker.repository.StationRepository;
import com.example.railtracker.repository.TrainRepository;
import com.example.railtracker.repository.TrainStationRepository;
import com.example.railtracker.service.impl.StationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StationServiceRegressionAndQuotaTest {

    private StationRepository stationRepository;
    private RailwayDataProvider railwayDataProvider;
    private TrainStationRepository trainStationRepository;
    private TrainRepository trainRepository;
    private TrainService trainService;
    private CacheManager cacheManager;
    private Cache trainLiveCache;
    private StationServiceImpl stationServiceImpl;

    @BeforeEach
    void setUp() {
        stationRepository = mock(StationRepository.class);
        railwayDataProvider = mock(RailwayDataProvider.class);
        trainStationRepository = mock(TrainStationRepository.class);
        trainRepository = mock(TrainRepository.class);
        trainService = mock(TrainService.class);
        cacheManager = mock(CacheManager.class);
        trainLiveCache = mock(Cache.class);

        when(cacheManager.getCache("train:live")).thenReturn(trainLiveCache);

        stationServiceImpl = new StationServiceImpl(
                stationRepository,
                railwayDataProvider,
                trainStationRepository,
                trainRepository,
                trainService);
        stationServiceImpl.setCacheManager(cacheManager);

        Station kzj = new Station();
        kzj.setStationCode("KZJ");
        kzj.setStationName("Kazipet Jn");
        when(stationRepository.findByStationCode("KZJ")).thenReturn(Optional.of(kzj));
        when(stationRepository.findByStationCodeIn(anySet())).thenReturn(List.of(kzj));
    }

    private StationBoardTrainDto createScheduled(String trainNumber, String trainName, LocalTime arr, LocalTime dep, Integer arrivalDay) {
        return new StationBoardTrainDto(
                trainNumber, trainName, "SUPERFAST", "DAILY", "DNR", "SC",
                arr, dep, null, null, null, "STOPPING", true, "SCHEDULED", arrivalDay, "1"
        );
    }

    @Test
    void testStationBoardQuota_MakesZeroIndividualTrainCalls() {
        LocalTime now = LocalTime.now(ZoneId.of("Asia/Kolkata"));
        String arr1 = now.plusMinutes(10).format(DateTimeFormatter.ofPattern("HH:mm"));
        String dep1 = now.plusMinutes(15).format(DateTimeFormatter.ofPattern("HH:mm"));

        LiveStationBoardTrain train1 = new LiveStationBoardTrain(
                "12792", "Secunderabad SF Express", "SUPERFAST",
                new StationCodeName("DNR", "Danapur"), new StationCodeName("SC", "Secunderabad"),
                StationPassType.STOPPING, arr1, dep1, arr1, dep1, 15, "1",
                StationTrainLiveStatus.UPCOMING, "DAILY", null, null, "2026-10-31");

        LiveStationBoardResponse rawResponse = new LiveStationBoardResponse(
                new StationInfo("KZJ", "Kazipet Jn", "Warangal", 17.97, 79.52),
                new WindowInfo(now.format(DateTimeFormatter.ofPattern("HH:mm")),
                        now.plusHours(4).format(DateTimeFormatter.ofPattern("HH:mm")), 4),
                List.of(train1));

        when(railwayDataProvider.getLiveStationBoardDetails("KZJ", 4)).thenReturn(rawResponse);

        // Timetable has additional scheduled train 17405
        LocalTime arr2 = now.plusMinutes(40);
        LocalTime dep2 = now.plusMinutes(45);
        StationBoardTrainDto schTrain = createScheduled("17405", "Krishna Express", arr2, dep2, 1);

        when(railwayDataProvider.getStationBoard("KZJ", true)).thenReturn(List.of(schTrain));

        LiveStationBoardResponse response = stationServiceImpl.getLiveStationBoardDetails("KZJ", 4, false);

        assertNotNull(response);
        assertEquals(2, response.trains().size());

        // CRITICAL QUOTA CHECK: zero calls to trainService.getTrainLiveStatus!
        verify(trainService, never()).getTrainLiveStatus(anyString());
        verify(trainService, never()).getTrainLiveStatus(anyString(), anyString());
    }

    @Test
    void testScheduledTrainsWithoutLiveGps_RetainedAsScheduledWithoutFalseOnTime() {
        LocalTime now = LocalTime.now(ZoneId.of("Asia/Kolkata"));
        LocalTime arr = now.plusMinutes(30);
        LocalTime dep = now.plusMinutes(35);

        StationBoardTrainDto schTrain = createScheduled("12705", "Secunderabad Intercity", arr, dep, 1);

        when(railwayDataProvider.getLiveStationBoardDetails("KZJ", 4)).thenReturn(
                new LiveStationBoardResponse(
                        new StationInfo("KZJ", "Kazipet Jn", "Warangal", 17.97, 79.52),
                        new WindowInfo("17:00", "21:00", 4), List.of()));

        when(railwayDataProvider.getStationBoard("KZJ", true)).thenReturn(List.of(schTrain));

        LiveStationBoardResponse response = stationServiceImpl.getLiveStationBoardDetails("KZJ", 4, false);

        assertNotNull(response);
        assertEquals(1, response.trains().size());
        LiveStationBoardTrain train = response.trains().get(0);
        assertEquals("12705", train.trainNumber());
        assertEquals(StationTrainLiveStatus.SCHEDULED, train.liveStatus());
        assertNull(train.actualArrival());
        assertNull(train.actualDeparture());
    }

    @Test
    void testMultiRakeDeduplication_UsesTrainNumberAndJourneyDate() {
        LocalTime now = LocalTime.now(ZoneId.of("Asia/Kolkata"));
        String arr = now.plusMinutes(20).format(DateTimeFormatter.ofPattern("HH:mm"));
        String dep = now.plusMinutes(25).format(DateTimeFormatter.ofPattern("HH:mm"));

        // Train 12792 running from yesterday's journey (2026-10-31) in live feed
        LiveStationBoardTrain yesterdayRake = new LiveStationBoardTrain(
                "12792", "Secunderabad SF Express", "SUPERFAST",
                new StationCodeName("DNR", "Danapur"), new StationCodeName("SC", "Secunderabad"),
                StationPassType.STOPPING, arr, dep, arr, dep, 0, "1",
                StationTrainLiveStatus.UPCOMING, "DAILY", null, null, "2026-10-31");

        when(railwayDataProvider.getLiveStationBoardDetails("KZJ", 4)).thenReturn(
                new LiveStationBoardResponse(
                        new StationInfo("KZJ", "Kazipet Jn", "Warangal", 17.97, 79.52),
                        new WindowInfo("17:00", "21:00", 4), List.of(yesterdayRake)));

        // Timetable contains 12792 with arrivalDay = 2 (day offset = 1, resolving to 2026-10-31)
        StationBoardTrainDto schTrain = createScheduled("12792", "Secunderabad SF Express", LocalTime.parse(arr), LocalTime.parse(dep), 2);

        when(railwayDataProvider.getStationBoard("KZJ", true)).thenReturn(List.of(schTrain));

        LiveStationBoardResponse response = stationServiceImpl.getLiveStationBoardDetails("KZJ", 4, false);

        assertNotNull(response);
        // Correct deduplication: appears exactly ONCE
        assertEquals(1, response.trains().size());
        assertEquals("12792", response.trains().get(0).trainNumber());
        assertEquals("2026-10-31", response.trains().get(0).journeyDate());
    }
}
