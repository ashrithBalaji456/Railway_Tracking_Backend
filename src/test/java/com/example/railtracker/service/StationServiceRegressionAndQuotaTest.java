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

import com.example.railtracker.dto.TrainLiveStatusDto;

import com.example.railtracker.entity.Station;

import com.example.railtracker.repository.StationRepository;

import com.example.railtracker.repository.TrainRepository;

import com.example.railtracker.repository.TrainStationRepository;

import com.example.railtracker.service.impl.StationServiceImpl;

import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;

import org.springframework.cache.Cache;

import org.springframework.cache.CacheManager;



import java.time.DayOfWeek;

import java.time.LocalDate;

import java.time.LocalDateTime;

import java.time.LocalTime;

import java.time.ZoneId;

import java.time.ZonedDateTime;

import java.time.format.DateTimeFormatter;

import java.util.ArrayList;

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

        return createScheduled(trainNumber, trainName, arr, dep, arrivalDay, "DAILY", true);

    }



    private StationBoardTrainDto createScheduled(String trainNumber, String trainName, LocalTime arr, LocalTime dep, Integer arrivalDay, String runningDays, boolean isHalt) {

        return new StationBoardTrainDto(

                trainNumber, trainName, "SUPERFAST", runningDays, "DNR", "SC",

                arr, dep, null, null, null, isHalt ? "STOPPING" : "PASS_THROUGH", isHalt, "SCHEDULED", arrivalDay, arrivalDay, "1"

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

        assertNull(train.delayMinutes(), "Delay must remain null to prevent false 'On Time' indication");

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

        assertEquals(1, response.trains().size());

        assertEquals("12792", response.trains().get(0).trainNumber());

        assertEquals("2026-10-31", response.trains().get(0).journeyDate());

    }



    @Test

    void test1_NonOperatingWeekdayTrain_IsExcluded() {

        LocalTime now = LocalTime.now(ZoneId.of("Asia/Kolkata"));

        DayOfWeek today = LocalDate.now(ZoneId.of("Asia/Kolkata")).getDayOfWeek();

        DayOfWeek nonOperatingDay = today.plus(2); // strictly a different weekday

        String dayCode = nonOperatingDay.name().substring(0, 3);



        StationBoardTrainDto otherDayTrain = createScheduled("22670", "PNBE ERS Express", now.plusMinutes(30), now.plusMinutes(35), 1, dayCode, true);



        when(railwayDataProvider.getLiveStationBoardDetails(eq("KZJ"), anyInt())).thenReturn(null);

        when(railwayDataProvider.getStationBoard("KZJ", true)).thenReturn(List.of(otherDayTrain));



        LiveStationBoardResponse response = stationServiceImpl.getLiveStationBoardDetails("KZJ", 4, true);



        assertNotNull(response);

        assertEquals(0, response.trains().size(), "Non-operating day train must be excluded");

    }



    @Test

    void test2_MultiDayOriginDay_MatchesCorrectly() {

        LocalTime now = LocalTime.now(ZoneId.of("Asia/Kolkata"));

        DayOfWeek today = LocalDate.now(ZoneId.of("Asia/Kolkata")).getDayOfWeek();

        DayOfWeek originDay = today.minus(1); // origin ran yesterday, passes today (arrivalDay=2)

        String dayCode = originDay.name().substring(0, 3);



        StationBoardTrainDto satTrain = createScheduled("22663", "MS Jodhpur Express", now.plusMinutes(40), now.plusMinutes(45), 2, dayCode, true);



        when(railwayDataProvider.getLiveStationBoardDetails(eq("KZJ"), anyInt())).thenReturn(null);

        when(railwayDataProvider.getStationBoard("KZJ", true)).thenReturn(List.of(satTrain));



        LiveStationBoardResponse response = stationServiceImpl.getLiveStationBoardDetails("KZJ", 4, true);



        assertNotNull(response);

        assertEquals(1, response.trains().size(), "Train starting on origin day reaching today with arrivalDay=2 must be included");

        assertEquals("22663", response.trains().get(0).trainNumber());

    }



    @Test

    void test3_DailyTrains_AreIncluded() {

        LocalTime now = LocalTime.now(ZoneId.of("Asia/Kolkata"));

        StationBoardTrainDto dailyTrain = createScheduled("12728", "Godavari Express", now.plusMinutes(20), now.plusMinutes(25), 1, "DAILY", true);



        when(railwayDataProvider.getLiveStationBoardDetails(eq("KZJ"), anyInt())).thenReturn(null);

        when(railwayDataProvider.getStationBoard("KZJ", true)).thenReturn(List.of(dailyTrain));



        LiveStationBoardResponse response = stationServiceImpl.getLiveStationBoardDetails("KZJ", 4, true);



        assertNotNull(response);

        assertEquals(1, response.trains().size(), "Daily train must always be included within window");

        assertEquals("12728", response.trains().get(0).trainNumber());

    }



    @Test

    void test4_MultipleOperatingDays_Respected() {

        LocalTime now = LocalTime.now(ZoneId.of("Asia/Kolkata"));

        DayOfWeek today = LocalDate.now(ZoneId.of("Asia/Kolkata")).getDayOfWeek();

        String dayCode = today.name().substring(0, 3);

        StationBoardTrainDto train = createScheduled("07236", "Sirpur Special", now.plusMinutes(20), now.plusMinutes(25), 1, "FRI, " + dayCode, true);



        when(railwayDataProvider.getLiveStationBoardDetails(eq("KZJ"), anyInt())).thenReturn(null);

        when(railwayDataProvider.getStationBoard("KZJ", true)).thenReturn(List.of(train));



        LiveStationBoardResponse response = stationServiceImpl.getLiveStationBoardDetails("KZJ", 4, true);



        assertNotNull(response);

        assertEquals(1, response.trains().size());

        assertEquals("07236", response.trains().get(0).trainNumber());

    }



    @Test

    void test7_OvernightPassage_AppearsInWindow() {

        LocalTime now = LocalTime.now(ZoneId.of("Asia/Kolkata"));

        StationBoardTrainDto overnightTrain = createScheduled("12723", "Telangana Express", now.plusMinutes(120), now.plusMinutes(125), 1, "DAILY", true);



        when(railwayDataProvider.getLiveStationBoardDetails(eq("KZJ"), anyInt())).thenReturn(null);

        when(railwayDataProvider.getStationBoard("KZJ", true)).thenReturn(List.of(overnightTrain));



        LiveStationBoardResponse response = stationServiceImpl.getLiveStationBoardDetails("KZJ", 4, true);



        assertNotNull(response);

        assertEquals(1, response.trains().size());

    }



    @Test

    void test8_TrainOutsideWindow_IsExcluded() {

        LocalTime now = LocalTime.now(ZoneId.of("Asia/Kolkata"));

        // 5 hours ahead (outside 4h window)

        StationBoardTrainDto farTrain = createScheduled("99999", "Far Away Express", now.plusHours(5), now.plusHours(5).plusMinutes(5), 1, "DAILY", true);



        when(railwayDataProvider.getLiveStationBoardDetails(eq("KZJ"), anyInt())).thenReturn(null);

        when(railwayDataProvider.getStationBoard("KZJ", true)).thenReturn(List.of(farTrain));



        LiveStationBoardResponse response = stationServiceImpl.getLiveStationBoardDetails("KZJ", 4, true);



        assertNotNull(response);

        assertEquals(0, response.trains().size(), "Train 5h ahead must be excluded from 4h window");

    }



    @Test

    void test9_DelayedTrainEnteringWindow_IsIncluded() {

        LocalTime now = LocalTime.now(ZoneId.of("Asia/Kolkata"));

        // Scheduled 15 minutes before now (outside [now, now+4h] without delay)

        LocalTime schArr = now.minusMinutes(15);

        LocalTime schDep = schArr.plusMinutes(5);



        StationBoardTrainDto schTrain = createScheduled("12345", "Delayed SF", schArr, schDep, 1, "DAILY", true);



        String today = LocalDate.now(ZoneId.of("Asia/Kolkata")).toString();

        TrainLiveStatusDto.StationStatusDto stop = new TrainLiveStatusDto.StationStatusDto(

                "KZJ", "Kazipet", 1,

                schArr.toString(), schDep.toString(), null, null,

                35, "upcoming", true, "1",

                null, null, false

        );

        TrainLiveStatusDto cachedLive = new TrainLiveStatusDto(

                "12345", "Delayed SF", "RUNNING", "WL", "Warangal", "KZJ", "Kazipet",

                35, 17.9, 79.5, 60.0, 45.0, 0.5, "Running",

                LocalDateTime.now(), List.of(stop), false, false, false, null, null, today

        );

        when(trainLiveCache.get("12345_" + today)).thenReturn(() -> cachedLive);



        when(railwayDataProvider.getLiveStationBoardDetails(eq("KZJ"), anyInt())).thenReturn(null);

        when(railwayDataProvider.getStationBoard("KZJ", true)).thenReturn(List.of(schTrain));



        LiveStationBoardResponse response = stationServiceImpl.getLiveStationBoardDetails("KZJ", 4, true);



        assertNotNull(response);

        assertEquals(1, response.trains().size(), "Train delayed into the window must be included");

        assertEquals("12345", response.trains().get(0).trainNumber());

    }



    @Test

    void test12_NonStoppingTrain_FilteredCorrectly() {

        LocalTime now = LocalTime.now(ZoneId.of("Asia/Kolkata"));

        LocalTime pass = now.plusMinutes(50);

        StationBoardTrainDto nonStopTrain = createScheduled("12645", "Millennium Express", pass, pass, 1, "DAILY", false);



        when(railwayDataProvider.getLiveStationBoardDetails(eq("KZJ"), anyInt())).thenReturn(null);

        when(railwayDataProvider.getStationBoard("KZJ", true)).thenReturn(List.of(nonStopTrain));



        LiveStationBoardResponse response = stationServiceImpl.getLiveStationBoardDetails("KZJ", 4, true);



        assertNotNull(response);

        assertEquals(1, response.trains().size());

        assertEquals(StationPassType.PASS_THROUGH, response.trains().get(0).passType());

    }



    @Test

    void test13_ScheduledOnlyTrainsWithNullDelay_DoNotShowZeroDelay() {

        LocalTime now = LocalTime.now(ZoneId.of("Asia/Kolkata"));

        StationBoardTrainDto schTrain = createScheduled("12705", "Secunderabad Intercity", now.plusMinutes(30), now.plusMinutes(35), 1, "DAILY", true);



        when(railwayDataProvider.getLiveStationBoardDetails(eq("KZJ"), anyInt())).thenReturn(null);

        when(railwayDataProvider.getStationBoard("KZJ", true)).thenReturn(List.of(schTrain));



        LiveStationBoardResponse response = stationServiceImpl.getLiveStationBoardDetails("KZJ", 4, true);



        assertNotNull(response);

        assertEquals(1, response.trains().size());

        LiveStationBoardTrain train = response.trains().get(0);

        assertNull(train.delayMinutes(), "Scheduled-only train without live telemetry must have null delayMinutes");

    }



    @Test

    void test14_WindowMetadata_DisplaysCorrectStartAndEndDates() {

        when(railwayDataProvider.getLiveStationBoardDetails(eq("KZJ"), anyInt())).thenReturn(null);

        when(railwayDataProvider.getStationBoard("KZJ", true)).thenReturn(List.of());



        LiveStationBoardResponse response = stationServiceImpl.getLiveStationBoardDetails("KZJ", 4, true);



        assertNotNull(response);

        assertNotNull(response.window());

        assertNotNull(response.window().fromDateTime());

        assertNotNull(response.window().toDateTime());

        assertTrue(response.window().fromDateTime().contains("T"), "fromDateTime must be ISO timestamp");

        assertTrue(response.window().toDateTime().contains("T"), "toDateTime must be ISO timestamp");

    }



    @Test

    void test15_WindowSwitching_2hVs4h_Isolated() {

        LocalTime now = LocalTime.now(ZoneId.of("Asia/Kolkata"));

        // Train 1: 1 hour ahead (in 2h and 4h)

        StationBoardTrainDto t1 = createScheduled("11111", "Train 1h", now.plusHours(1), now.plusHours(1).plusMinutes(5), 1, "DAILY", true);

        // Train 2: 3 hours ahead (outside 2h, inside 4h)

        StationBoardTrainDto t2 = createScheduled("22222", "Train 3h", now.plusHours(3), now.plusHours(3).plusMinutes(5), 1, "DAILY", true);



        when(railwayDataProvider.getLiveStationBoardDetails(eq("KZJ"), anyInt())).thenReturn(null);

        when(railwayDataProvider.getStationBoard("KZJ", true)).thenReturn(List.of(t1, t2));



        LiveStationBoardResponse res2h = stationServiceImpl.getLiveStationBoardDetails("KZJ", 2, true);

        assertEquals(1, res2h.trains().size(), "2h window must include only trains within 2 hours");

        assertEquals("11111", res2h.trains().get(0).trainNumber());

        assertEquals(2, res2h.window().hours());



        LiveStationBoardResponse res4h = stationServiceImpl.getLiveStationBoardDetails("KZJ", 4, true);

        assertEquals(2, res4h.trains().size(), "4h window must include trains within 4 hours");

        assertEquals(4, res4h.window().hours());

    }



    @Test

    void test16_Realistic54TrainDataset_FiltersExactlyTo20ValidJourneys_ExcludingPastWindowTrain() {

        LocalTime now = LocalTime.now(ZoneId.of("Asia/Kolkata"));

        DayOfWeek today = LocalDate.now(ZoneId.of("Asia/Kolkata")).getDayOfWeek();

        java.util.function.BiFunction<Integer, Integer, String> resolveOriginDay = (minOffset, arrDay) -> {
            LocalTime targetTime = now.plusMinutes(minOffset);
            DayOfWeek arrivalDow = targetTime.isBefore(now) ? today.plus(1) : today;
            DayOfWeek originDow = arrivalDow.minus(arrDay - 1);
            return originDow.name().substring(0, 3);
        };

        List<StationBoardTrainDto> timetable = new ArrayList<>();

        // 1. 14 Daily trains strictly inside the 4h window
        for (int i = 1; i <= 14; i++) {
            LocalTime t = now.plusMinutes(i * 15); // up to 210 mins (< 240m)
            timetable.add(createScheduled("DAILY_" + i, "Daily Express " + i, t, t.plusMinutes(2), 1, "DAILY", true));
        }

        // DAILY_15 is scheduled past window end (e.g. +245 mins > 240 mins) - MUST BE EXCLUDED
        timetable.add(createScheduled("DAILY_15", "Daily Express 15", now.plusMinutes(245), now.plusMinutes(247), 1, "DAILY", true));

        // 2. 4 Multi-day trains (arrDay = 2) arriving within 4h window
        timetable.add(createScheduled("22663", "MS Jodhpur Express", now.plusMinutes(40), now.plusMinutes(42), 2, resolveOriginDay.apply(40, 2), true));
        timetable.add(createScheduled("22669", "Rameswaram Okha Express", now.plusMinutes(70), now.plusMinutes(72), 2, resolveOriginDay.apply(70, 2), true));
        timetable.add(createScheduled("12645", "Millennium Express", now.plusMinutes(110), now.plusMinutes(110), 2, resolveOriginDay.apply(110, 2), false)); // Non-stopping
        timetable.add(createScheduled("12803", "Swarna Jayanti Express", now.plusMinutes(125), now.plusMinutes(127), 2, resolveOriginDay.apply(125, 2), true));

        // 3. 2 Today-operating trains (arrDay = 1)
        timetable.add(createScheduled("07236", "Sirpur - Kacheguda Spl", now.plusMinutes(30), now.plusMinutes(32), 1, "FRI, " + resolveOriginDay.apply(30, 1), true));
        timetable.add(createScheduled("07055", "Secunderabad Special", now.plusMinutes(130), now.plusMinutes(132), 1, resolveOriginDay.apply(130, 1), true));

        // 4. 29 Trains operating ONLY on other weekdays
        List<String> otherDays = new ArrayList<>();
        for (DayOfWeek d : DayOfWeek.values()) {
            if (d != today && d != today.minus(1) && d != today.plus(1) && d != today.plus(2)) {
                otherDays.add(d.name().substring(0, 3));
            }
        }

        for (int i = 1; i <= 29; i++) {

            String day = otherDays.get(i % otherDays.size());

            LocalTime t = now.plusMinutes((i * 7) % 220); // within clock window

            timetable.add(createScheduled("OTHER_" + i, "Other Day Train " + i, t, t.plusMinutes(2), 1, day, true));

        }



        // 5. 4 Trains scheduled clearly outside the window (relative to now)

        timetable.add(createScheduled("OUT_1", "Early Train 1", now.minusHours(4), now.minusHours(4).plusMinutes(5), 1, "DAILY", true));

        timetable.add(createScheduled("OUT_2", "Early Train 2", now.minusHours(2), now.minusHours(2).plusMinutes(5), 1, "DAILY", true));

        timetable.add(createScheduled("OUT_3", "Late Train 1", now.plusHours(5), now.plusHours(5).plusMinutes(5), 1, "DAILY", true));

        timetable.add(createScheduled("OUT_4", "Late Train 2", now.plusHours(6), now.plusHours(6).plusMinutes(5), 1, "DAILY", true));



        assertEquals(54, timetable.size(), "Pre-filter timetable must have exactly 54 candidate trains");



        when(railwayDataProvider.getLiveStationBoardDetails(eq("KZJ"), anyInt())).thenReturn(null);

        when(railwayDataProvider.getStationBoard("KZJ", true)).thenReturn(timetable);



        // Execute board details for KZJ (4h window)

        LiveStationBoardResponse response = stationServiceImpl.getLiveStationBoardDetails("KZJ", 4, true);



        assertNotNull(response);

        // Excluded: 29 other-weekday trains + 4 outside-window trains + DAILY_15 (> refEnd) = 34 trains rejected!

        // Retained: 14 daily + 4 multi-day + 2 today-operating = exactly 20 valid trains!

        for (LiveStationBoardTrain t : response.trains()) { System.out.println("RETAINED: " + t.trainNumber()); }
        assertEquals(20, response.trains().size(), "Must filter down to exactly 20 legitimate train journeys (excluding DAILY_15 past window)");



        boolean hasDaily15 = response.trains().stream().anyMatch(t -> "DAILY_15".equals(t.trainNumber()));

        assertFalse(hasDaily15, "DAILY_15 at 00:39 past 00:37 window boundary must be excluded");



        for (LiveStationBoardTrain t : response.trains()) {

            assertNotNull(t.journeyDate());

            assertNotNull(t.trainNumber());

            assertFalse(t.trainNumber().startsWith("OTHER_"), "No other-weekday train should be present: " + t.trainNumber());

            assertFalse(t.trainNumber().startsWith("OUT_"), "No outside-window train should be present: " + t.trainNumber());

        }

    }



    @Test

    void test17_AllSevenIsoNumericWeekdays_MatchStrictly() {

        LocalDate monday = LocalDate.of(2026, 11, 2);

        LocalDate tuesday = LocalDate.of(2026, 11, 3);

        LocalDate wednesday = LocalDate.of(2026, 11, 4);

        LocalDate thursday = LocalDate.of(2026, 11, 5);

        LocalDate friday = LocalDate.of(2026, 11, 6);

        LocalDate saturday = LocalDate.of(2026, 11, 7);

        LocalDate sunday = LocalDate.of(2026, 11, 8);



        // ISO 1 = Monday

        assertTrue(stationServiceImpl.isOperatingOnDate("1", monday));

        assertFalse(stationServiceImpl.isOperatingOnDate("1", sunday), "ISO 1 must not match Sunday");

        assertFalse(stationServiceImpl.isOperatingOnDate("1", tuesday));



        // ISO 2 = Tuesday

        assertTrue(stationServiceImpl.isOperatingOnDate("2", tuesday));

        assertFalse(stationServiceImpl.isOperatingOnDate("2", monday));



        // ISO 3 = Wednesday

        assertTrue(stationServiceImpl.isOperatingOnDate("3", wednesday));

        assertFalse(stationServiceImpl.isOperatingOnDate("3", tuesday));



        // ISO 4 = Thursday

        assertTrue(stationServiceImpl.isOperatingOnDate("4", thursday));

        assertFalse(stationServiceImpl.isOperatingOnDate("4", friday));



        // ISO 5 = Friday

        assertTrue(stationServiceImpl.isOperatingOnDate("5", friday));

        assertFalse(stationServiceImpl.isOperatingOnDate("5", saturday));



        // ISO 6 = Saturday

        assertTrue(stationServiceImpl.isOperatingOnDate("6", saturday));

        assertFalse(stationServiceImpl.isOperatingOnDate("6", sunday));



        // ISO 7 = Sunday

        assertTrue(stationServiceImpl.isOperatingOnDate("7", sunday));

        assertFalse(stationServiceImpl.isOperatingOnDate("7", monday), "ISO 7 must not match Monday");

        assertFalse(stationServiceImpl.isOperatingOnDate("7", saturday));

    }



    @Test

    void test18_IndependentDayOffsets_ArrivalVsDeparture() {

        LocalTime now = LocalTime.now(ZoneId.of("Asia/Kolkata"));



        // Train A: Originating train (no arrival, only departure) with departureDay = 2

        StationBoardTrainDto trainOrigin = new StationBoardTrainDto(

                "ORIG_01", "Origin Train", "EXP", "DAILY", "KZJ", "NDLS",

                null, now.plusMinutes(30), null, null, null,

                "STOPPING", true, "SCHEDULED", null, 2, "1"

        );



        when(railwayDataProvider.getLiveStationBoardDetails(eq("KZJ"), anyInt())).thenReturn(null);

        when(railwayDataProvider.getStationBoard("KZJ", true)).thenReturn(List.of(trainOrigin));



        LiveStationBoardResponse response = stationServiceImpl.getLiveStationBoardDetails("KZJ", 4, true);

        assertNotNull(response);

        assertEquals(1, response.trains().size());

        LiveStationBoardTrain resTrain = response.trains().get(0);

        assertNotNull(resTrain.journeyDate());

        LocalDate expectedJourney = LocalDate.now(ZoneId.of("Asia/Kolkata")).minusDays(1); // departureDay=2 -> minus 1 day

        assertEquals(expectedJourney.toString(), resTrain.journeyDate());

    }



    @Test

    void test19_ExpectedPassageTimes_SynchronizedWithDelayMinutes() {

        LocalTime now = LocalTime.now(ZoneId.of("Asia/Kolkata"));

        LocalTime schPass = now.plusMinutes(20);

        StationBoardTrainDto nonStop = new StationBoardTrainDto(

                "12645", "Millennium SF Express", "SF", "DAILY", "ERS", "NZM",

                schPass, schPass, null, null, null,

                "PASS_THROUGH", false, "SCHEDULED", 2, 2, null

        );



        when(railwayDataProvider.getLiveStationBoardDetails(eq("KZJ"), anyInt())).thenReturn(null);

        when(railwayDataProvider.getStationBoard("KZJ", true)).thenReturn(List.of(nonStop));



        String jDate = LocalDate.now(ZoneId.of("Asia/Kolkata")).minusDays(1).toString();

        TrainLiveStatusDto.StationStatusDto stopStatus = new TrainLiveStatusDto.StationStatusDto(

                "KZJ", "Kazipet Jn", 10,

                schPass.toString(), schPass.toString(), null, null,

                38, "upcoming", false, "2",

                null, null, false

        );

        TrainLiveStatusDto cachedLive = new TrainLiveStatusDto(

                "12645", "Millennium SF Express", "RUNNING", "WL", "Warangal", "KZJ", "Kazipet Jn",

                38, 17.9, 79.5, 80.0, 45.0, 0.65, "Departed WL",

                LocalDateTime.now(), List.of(stopStatus), false, false, false, null, null, jDate

        );

        when(trainLiveCache.get("12645_" + jDate)).thenReturn(() -> cachedLive);



        LiveStationBoardResponse response = stationServiceImpl.getLiveStationBoardDetails("KZJ", 4, true);

        assertNotNull(response);

        assertEquals(1, response.trains().size());

        LiveStationBoardTrain train = response.trains().get(0);

        assertEquals(Integer.valueOf(38), train.delayMinutes());



        LocalTime expectedTime = schPass.plusMinutes(38);

        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

        assertTrue(train.expectedArrival().contains(expectedTime.format(timeFmt)) || train.expectedDeparture().contains(expectedTime.format(timeFmt)),

                "Expected passage time in response must be mathematically consistent with scheduled + 38m delay: " + expectedTime.format(timeFmt));

    }

}

