package com.example.railtracker.service;

import com.example.railtracker.client.RailwayDataProvider;
import com.example.railtracker.dto.LiveStationBoardResponse;
import com.example.railtracker.dto.LiveStationBoardResponse.LiveStationBoardTrain;
import com.example.railtracker.dto.LiveStationBoardResponse.StationCodeName;
import com.example.railtracker.dto.LiveStationBoardResponse.StationInfo;
import com.example.railtracker.dto.LiveStationBoardResponse.WindowInfo;
import com.example.railtracker.dto.StationPassType;
import com.example.railtracker.dto.StationTrainLiveStatus;
import com.example.railtracker.dto.TrainLiveStatusDto;
import com.example.railtracker.dto.TrainLiveStatusDto.StationStatusDto;
import com.example.railtracker.entity.Station;
import com.example.railtracker.repository.StationRepository;
import com.example.railtracker.repository.TrainRepository;
import com.example.railtracker.repository.TrainStationRepository;
import com.example.railtracker.service.impl.StationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import static org.mockito.Mockito.*;

class StationServiceLiveBoardReconciliationTest {

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

                Station mci = new Station();
                mci.setStationCode("MCI");
                mci.setStationName("Mancherial");
                mci.setLatitude(18.86);
                mci.setLongitude(79.46);
                when(stationRepository.findByStationCode("MCI")).thenReturn(Optional.of(mci));
                when(stationRepository.findByStationCodeIn(anySet())).thenReturn(List.of(mci));
        }

        private LiveStationBoardTrain createTestTrain(String trainNumber, String scheduledArr, String scheduledDep,
                        String expectedArr, String expectedDep,
                        StationTrainLiveStatus status, String actArr, String actDep) {
                return new LiveStationBoardTrain(
                                trainNumber,
                                "Singareni MEMU Express",
                                "MEMU",
                                new StationCodeName("SRUR", "Sirpur Town"),
                                new StationCodeName("BDCR", "Bhadrachalam Road"),
                                StationPassType.STOPPING,
                                scheduledArr,
                                scheduledDep,
                                expectedArr,
                                expectedDep,
                                10,
                                "3",
                                status,
                                "DAILY",
                                actArr,
                                actDep,
                                "2026-11-01");
        }

        @Test
        void testConfirmedArrivalWithoutDeparture_ResolvesToAtStation() {
                LocalTime now = LocalTime.now(ZoneId.of("Asia/Kolkata"));
                String actArr = now.minusMinutes(5).format(DateTimeFormatter.ofPattern("HH:mm"));
                String expDep = now.plusMinutes(10).format(DateTimeFormatter.ofPattern("HH:mm"));

                LiveStationBoardTrain train = createTestTrain("17034", "13:30", "13:45", actArr, expDep,
                                StationTrainLiveStatus.UPCOMING, actArr, null);

                LiveStationBoardResponse rawResponse = new LiveStationBoardResponse(
                                new StationInfo("MCI", "Mancherial", "Mancherial", 18.86, 79.46),
                                new WindowInfo("13:00", "17:00", 4),
                                List.of(train));
                when(railwayDataProvider.getLiveStationBoardDetails("MCI", 4)).thenReturn(rawResponse);

                LiveStationBoardResponse result = stationServiceImpl.getLiveStationBoardDetails("MCI", 4, false);

                assertNotNull(result);
                assertEquals(1, result.trains().size());
                assertEquals(StationTrainLiveStatus.AT_STATION, result.trains().get(0).liveStatus());
                assertNull(result.trains().get(0).actualDeparture());
        }

        @Test
        void testConfirmedDeparture_ResolvesToDeparted() {
                LocalTime now = LocalTime.now(ZoneId.of("Asia/Kolkata"));
                String actDep = now.minusMinutes(3).format(DateTimeFormatter.ofPattern("HH:mm"));

                LiveStationBoardTrain train = createTestTrain("17034", "13:30", "13:45", "13:38", "13:48",
                                StationTrainLiveStatus.UPCOMING, "13:38", actDep);

                LiveStationBoardResponse rawResponse = new LiveStationBoardResponse(
                                new StationInfo("MCI", "Mancherial", "Mancherial", 18.86, 79.46),
                                new WindowInfo("13:00", "17:00", 4),
                                List.of(train));
                when(railwayDataProvider.getLiveStationBoardDetails("MCI", 4)).thenReturn(rawResponse);

                LiveStationBoardResponse result = stationServiceImpl.getLiveStationBoardDetails("MCI", 4, false);

                assertNotNull(result);
                assertEquals(1, result.trains().size());
                assertEquals(StationTrainLiveStatus.DEPARTED, result.trains().get(0).liveStatus());
        }

        @Test
        void testElapsedPredictedArrival_WithoutReliableEvidence_RemainsUpcoming() {
                LocalTime now = LocalTime.now(ZoneId.of("Asia/Kolkata"));
                // Expected arrival passed 3 minutes ago, but no actual arrival telemetry and
                // trainService has no at-station telemetry
                String pastArr = now.minusMinutes(3).format(DateTimeFormatter.ofPattern("HH:mm"));
                String futureDep = now.plusMinutes(5).format(DateTimeFormatter.ofPattern("HH:mm"));

                LiveStationBoardTrain train = createTestTrain("17034", "13:30", "13:45", pastArr, futureDep,
                                StationTrainLiveStatus.UPCOMING, null, null);

                LiveStationBoardResponse rawResponse = new LiveStationBoardResponse(
                                new StationInfo("MCI", "Mancherial", "Mancherial", 18.86, 79.46),
                                new WindowInfo("13:00", "17:00", 4),
                                List.of(train));
                when(railwayDataProvider.getLiveStationBoardDetails("MCI", 4)).thenReturn(rawResponse);
                // trainService has no live record or train is running between stations
                when(trainService.getTrainLiveStatus(eq("17034"), anyString())).thenReturn(null);

                LiveStationBoardResponse result = stationServiceImpl.getLiveStationBoardDetails("MCI", 4, false);

                assertNotNull(result);
                assertEquals(1, result.trains().size());
                // Must NOT be falsely confirmed as AT_STATION without reliable evidence
                assertEquals(StationTrainLiveStatus.UPCOMING, result.trains().get(0).liveStatus());
        }

        @Test
        void testReliableTrainTrackingTelemetry_PromotesUpcomingToAtStation() {
                LocalTime now = LocalTime.now(ZoneId.of("Asia/Kolkata"));
                String pastArr = now.minusMinutes(3).format(DateTimeFormatter.ofPattern("HH:mm"));
                String futureDep = now.plusMinutes(5).format(DateTimeFormatter.ofPattern("HH:mm"));

                LiveStationBoardTrain train = createTestTrain("17034", "13:30", "13:45", pastArr, futureDep,
                                StationTrainLiveStatus.UPCOMING, null, null);

                LiveStationBoardResponse rawResponse = new LiveStationBoardResponse(
                                new StationInfo("MCI", "Mancherial", "Mancherial", 18.86, 79.46),
                                new WindowInfo("13:00", "17:00", 4),
                                List.of(train));
                when(railwayDataProvider.getLiveStationBoardDetails("MCI", 4)).thenReturn(rawResponse);

                // trainService GPS telemetry confirms train is AT_STATION at MCI
                StationStatusDto mciStop = new StationStatusDto(
                                "MCI", "Mancherial", 10, pastArr, futureDep, pastArr, futureDep, 10, "AT_STATION",
                                true, "3", pastArr, null, false);
                TrainLiveStatusDto liveStatus = new TrainLiveStatusDto(
                                "17034", "Singareni MEMU Express", "AT_STATION",
                                "MCI", "Mancherial", "RDM", "Ramagundam",
                                10, 18.86, 79.46, 0.0, 110.0, 0.5,
                                "at-station at Mancherial", LocalDateTime.now(), List.of(mciStop),
                                false, false, false, null, List.of(), "2026-11-01");
                when(trainService.getTrainLiveStatus(eq("17034"), anyString())).thenReturn(liveStatus);
                when(trainLiveCache.get(startsWith("17034"))).thenReturn(() -> liveStatus);

                LiveStationBoardResponse result = stationServiceImpl.getLiveStationBoardDetails("MCI", 4, false);

                assertNotNull(result);
                assertEquals(1, result.trains().size());
                assertEquals(StationTrainLiveStatus.AT_STATION, result.trains().get(0).liveStatus());
        }

        @Test
        void testPlatformDwell_NotRestrictedBy7MinuteDepartureGracePeriod() {
                ZonedDateTime nowIst = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
                // Train arrived 12 minutes ago (beyond 7 mins), scheduled departure is in 3
                // minutes
                String actArr = nowIst.minusMinutes(12).format(DateTimeFormatter.ofPattern("HH:mm"));
                String expDep = nowIst.plusMinutes(3).format(DateTimeFormatter.ofPattern("HH:mm"));

                LiveStationBoardTrain train = createTestTrain("17034", "13:30", "13:45", actArr, expDep,
                                StationTrainLiveStatus.AT_STATION, actArr, null);

                // Verify shouldIncludeInLiveBoard retains the train during its 15-minute dwell
                boolean included = stationServiceImpl.shouldIncludeInLiveBoard(train, nowIst, nowIst.plusHours(4));
                assertTrue(included, "Train must remain included on the board throughout its platform dwell");
        }

        @Test
        void testRecentlyDepartedTrain_RetainedWithin7Minutes_AndDroppedAfter() {
                ZonedDateTime nowIst = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));

                // Train departed 4 minutes ago (within 7m grace period)
                String recentDep = nowIst.minusMinutes(4).format(DateTimeFormatter.ofPattern("HH:mm"));
                LiveStationBoardTrain trainRecent = createTestTrain("17034", "13:30", "13:45", "13:38", recentDep,
                                StationTrainLiveStatus.DEPARTED, "13:38", recentDep);
                assertTrue(stationServiceImpl.shouldIncludeInLiveBoard(trainRecent, nowIst, nowIst.plusHours(4)),
                                "Recently departed train (4m ago) must be retained on the board");

                // Train departed 9 minutes ago (exceeded 7m grace period)
                String oldDep = nowIst.minusMinutes(9).format(DateTimeFormatter.ofPattern("HH:mm"));
                LiveStationBoardTrain trainOld = createTestTrain("17034", "13:30", "13:45", "13:38", oldDep,
                                StationTrainLiveStatus.DEPARTED, "13:38", oldDep);
                assertFalse(stationServiceImpl.shouldIncludeInLiveBoard(trainOld, nowIst, nowIst.plusHours(4)),
                                "Departed train older than 7 minutes must be dropped from the board");
        }

        @Test
        void testStaleDwell_DroppedAfter30Minutes() {
                ZonedDateTime nowIst = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));

                // Train expected departure was 35 minutes ago with no departure logged (stale
                // dwell)
                String staleArr = nowIst.minusMinutes(50).format(DateTimeFormatter.ofPattern("HH:mm"));
                String staleDep = nowIst.minusMinutes(35).format(DateTimeFormatter.ofPattern("HH:mm"));
                LiveStationBoardTrain staleTrain = createTestTrain("17034", "13:00", "13:15", staleArr, staleDep,
                                StationTrainLiveStatus.AT_STATION, staleArr, null);

                assertFalse(stationServiceImpl.shouldIncludeInLiveBoard(staleTrain, nowIst, nowIst.plusHours(4)),
                                "Train dwelling older than 30 minutes past departure without updates must be dropped by stale-dwell guard");
        }

        @Test
        void testDelayedIntoWindow_TrainScheduled7HoursAgo_IsIncludedWhenDelayBringsItIntoWindow() {
                ZonedDateTime nowIst = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
                // Train scheduled 7 hours 20 minutes ago (440 mins ago)
                LocalTime schTime = nowIst.minusMinutes(440).toLocalTime();
                int delayMinutes = 470; // Expected passage in 30 mins (inside the 4-hour window)

                com.example.railtracker.dto.StationBoardTrainDto keralaExp = new com.example.railtracker.dto.StationBoardTrainDto(
                                "12626", "Kerala Express", "SUPERFAST", "DAILY", "NDLS", "TVC",
                                schTime, schTime.plusMinutes(5), null, null, null,
                                "STOPPING", true, "SCHEDULED", 1, 1, "1");

                when(railwayDataProvider.getLiveStationBoardDetails(eq("MCI"), anyInt())).thenReturn(null);
                when(railwayDataProvider.getStationBoard(eq("MCI"), anyBoolean()))
                                .thenReturn(List.of(keralaExp));

                // Cache live telemetry showing 470 minutes delay
                StationStatusDto stop = new StationStatusDto(
                                "MCI", "Mancherial", 10, null, null, null, null, delayMinutes, "UPCOMING",
                                true, "1", null, null, false);
                TrainLiveStatusDto liveStatus = new TrainLiveStatusDto(
                                "12626", "Kerala Express", "RUNNING",
                                "SKZR", "Sirpur Kaghaznagar", "MCI", "Mancherial",
                                delayMinutes, 19.35, 79.48, 85.0, 160.0, 35.0,
                                "Departed SKZR, approaching MCI", LocalDateTime.now(), List.of(stop),
                                false, false, false, null, List.of(), nowIst.minusMinutes(440).toLocalDate().toString());

                when(trainLiveCache.get(startsWith("12626"))).thenReturn(() -> liveStatus);

                LiveStationBoardResponse result = stationServiceImpl.getLiveStationBoardDetails("MCI", 4, true);

                assertNotNull(result);
                assertEquals(1, result.trains().size(), "Train delayed by 7+ hours whose expected passage is in 30 mins must be included");
                LiveStationBoardTrain t = result.trains().get(0);
                assertEquals("12626", t.trainNumber());
                assertEquals(470, t.delayMinutes());
                assertEquals(StationTrainLiveStatus.UPCOMING, t.liveStatus());
        }

        @Test
        void testDelayedOutOfWindow_TrainScheduledInWindow_IsExcludedWhenDelayPushesPastWindowEnd() {
                ZonedDateTime nowIst = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
                // Train scheduled in 30 mins (inside 2h window)
                LocalTime schTime = nowIst.plusMinutes(30).toLocalTime();
                int delayMinutes = 180; // 3 hours delay pushes expected arrival to 3h 30m (outside 2h window)

                com.example.railtracker.dto.StationBoardTrainDto delayedOutTrain = new com.example.railtracker.dto.StationBoardTrainDto(
                                "12722", "Dakshin Express", "SUPERFAST", "DAILY", "NZM", "HYB",
                                schTime, schTime.plusMinutes(5), null, null, null,
                                "STOPPING", true, "SCHEDULED", 1, 1, "1");

                when(railwayDataProvider.getLiveStationBoardDetails(eq("MCI"), anyInt())).thenReturn(null);
                when(railwayDataProvider.getStationBoard(eq("MCI"), anyBoolean()))
                                .thenReturn(List.of(delayedOutTrain));

                // Cache live telemetry showing 180 minutes delay
                StationStatusDto stop = new StationStatusDto(
                                "MCI", "Mancherial", 10, null, null, null, null, delayMinutes, "UPCOMING",
                                true, "1", null, null, false);
                TrainLiveStatusDto liveStatus = new TrainLiveStatusDto(
                                "12722", "Dakshin Express", "RUNNING",
                                "BPQ", "Balharshah", "MCI", "Mancherial",
                                delayMinutes, 19.8, 79.3, 70.0, 150.0, 90.0,
                                "Delayed by 3 hours", LocalDateTime.now(), List.of(stop),
                                false, false, false, null, List.of(), nowIst.toLocalDate().toString());

                when(trainLiveCache.get(startsWith("12722"))).thenReturn(() -> liveStatus);

                LiveStationBoardResponse result = stationServiceImpl.getLiveStationBoardDetails("MCI", 2, true);

                assertNotNull(result);
                assertEquals(0, result.trains().size(), "Train delayed 3h past 2h window must be excluded");
        }
}
