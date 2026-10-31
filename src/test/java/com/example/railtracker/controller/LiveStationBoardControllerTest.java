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
import static org.hamcrest.Matchers.hasSize;
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
        
        java.time.ZonedDateTime nowIst = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));
        String expArrIso = nowIst.plusMinutes(15).format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        String expDepIso = nowIst.plusMinutes(25).format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        LiveStationBoardResponse.LiveStationBoardTrain train = new LiveStationBoardResponse.LiveStationBoardTrain(
                "12805",
                "Lingampalli Janmabhoomi SF Express",
                "Superfast Express",
                new LiveStationBoardResponse.StationCodeName("VSKP", "VISAKHAPATNAM"),
                new LiveStationBoardResponse.StationCodeName("LPI", "LINGAMPALLI"),
                StationPassType.STOPPING,
                "12:02",
                "12:12",
                expArrIso,
                expDepIso,
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

    @Test
    void testTrainArrivedAtStationNotDeparted() throws Exception {
        StationDto mockStation = new StationDto("MCI", "Manchiryal", "Mancherial", "Telangana", 18.879, 79.449, true);
        when(railwayDataProvider.searchStations("MCI")).thenReturn(Arrays.asList(mockStation));

        LiveStationBoardResponse.StationInfo stationInfo = new LiveStationBoardResponse.StationInfo("MCI", "MANCHIRYAL", "Mancherial", 18.879, 79.449);
        LiveStationBoardResponse.WindowInfo windowInfo = new LiveStationBoardResponse.WindowInfo("01:30", "05:30", 4);

        java.time.ZonedDateTime nowIst = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));
        String actArrIso = nowIst.minusMinutes(5).format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        String expDepIso = nowIst.plusMinutes(10).format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        LiveStationBoardResponse.LiveStationBoardTrain train = new LiveStationBoardResponse.LiveStationBoardTrain(
                "12724", "Telangana Express", "Superfast Express",
                new LiveStationBoardResponse.StationCodeName("NDLS", "New Delhi"),
                new LiveStationBoardResponse.StationCodeName("HYB", "Hyderabad"),
                StationPassType.STOPPING,
                "01:25", "01:35",
                actArrIso, expDepIso,
                5, "1",
                StationTrainLiveStatus.ARRIVED,
                "daily",
                actArrIso, null
        );

        LiveStationBoardResponse mockResponse = new LiveStationBoardResponse(stationInfo, windowInfo, List.of(train));
        when(railwayDataProvider.getLiveStationBoardDetails("MCI", 4)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/stations/MCI/live").param("hours", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trains", hasSize(1)))
                .andExpect(jsonPath("$.data.trains[0].trainNumber").value("12724"))
                .andExpect(jsonPath("$.data.trains[0].liveStatus").value("ARRIVED"));
    }

    @Test
    void testTrainCurrentlyAtStation() throws Exception {
        StationDto mockStation = new StationDto("MCI", "Manchiryal", "Mancherial", "Telangana", 18.879, 79.449, true);
        when(railwayDataProvider.searchStations("MCI")).thenReturn(Arrays.asList(mockStation));

        LiveStationBoardResponse.StationInfo stationInfo = new LiveStationBoardResponse.StationInfo("MCI", "MANCHIRYAL", "Mancherial", 18.879, 79.449);
        LiveStationBoardResponse.WindowInfo windowInfo = new LiveStationBoardResponse.WindowInfo("01:30", "05:30", 4);

        java.time.ZonedDateTime nowIst = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));
        String expDepIso = nowIst.plusMinutes(8).format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        LiveStationBoardResponse.LiveStationBoardTrain train = new LiveStationBoardResponse.LiveStationBoardTrain(
                "17007", "Darbhanga Express", "Mail/Express",
                new LiveStationBoardResponse.StationCodeName("CHZ", "Charlapalli"),
                new LiveStationBoardResponse.StationCodeName("DBG", "Darbhanga"),
                StationPassType.STOPPING,
                "01:30", "01:40",
                null, expDepIso,
                0, "2",
                StationTrainLiveStatus.AT_STATION,
                "tue,sat",
                null, null
        );

        LiveStationBoardResponse mockResponse = new LiveStationBoardResponse(stationInfo, windowInfo, List.of(train));
        when(railwayDataProvider.getLiveStationBoardDetails("MCI", 4)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/stations/MCI/live").param("hours", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trains", hasSize(1)))
                .andExpect(jsonPath("$.data.trains[0].trainNumber").value("17007"))
                .andExpect(jsonPath("$.data.trains[0].liveStatus").value("AT_STATION"));
    }

    @Test
    void testTrainDepartedWithin7MinutesKept() throws Exception {
        StationDto mockStation = new StationDto("MCI", "Manchiryal", "Mancherial", "Telangana", 18.879, 79.449, true);
        when(railwayDataProvider.searchStations("MCI")).thenReturn(Arrays.asList(mockStation));

        LiveStationBoardResponse.StationInfo stationInfo = new LiveStationBoardResponse.StationInfo("MCI", "MANCHIRYAL", "Mancherial", 18.879, 79.449);
        LiveStationBoardResponse.WindowInfo windowInfo = new LiveStationBoardResponse.WindowInfo("01:30", "05:30", 4);

        java.time.ZonedDateTime nowIst = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));
        String actDepIso = nowIst.minusMinutes(5).format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        LiveStationBoardResponse.LiveStationBoardTrain train = new LiveStationBoardResponse.LiveStationBoardTrain(
                "20482", "Humsafar Express", "Humsafar Express",
                new LiveStationBoardResponse.StationCodeName("TPJ", "Tiruchirappalli"),
                new LiveStationBoardResponse.StationCodeName("JU", "Jodhpur"),
                StationPassType.PASS_THROUGH,
                "01:08", "01:08",
                actDepIso, actDepIso,
                29, "1",
                StationTrainLiveStatus.DEPARTED,
                "sat",
                null, actDepIso
        );

        LiveStationBoardResponse mockResponse = new LiveStationBoardResponse(stationInfo, windowInfo, List.of(train));
        when(railwayDataProvider.getLiveStationBoardDetails("MCI", 4)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/stations/MCI/live").param("hours", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trains", hasSize(1)))
                .andExpect(jsonPath("$.data.trains[0].trainNumber").value("20482"))
                .andExpect(jsonPath("$.data.trains[0].liveStatus").value("DEPARTED"));
    }

    @Test
    void testTrainDeparted8MinutesAgoFilteredOut() throws Exception {
        StationDto mockStation = new StationDto("MCI", "Manchiryal", "Mancherial", "Telangana", 18.879, 79.449, true);
        when(railwayDataProvider.searchStations("MCI")).thenReturn(Arrays.asList(mockStation));

        LiveStationBoardResponse.StationInfo stationInfo = new LiveStationBoardResponse.StationInfo("MCI", "MANCHIRYAL", "Mancherial", 18.879, 79.449);
        LiveStationBoardResponse.WindowInfo windowInfo = new LiveStationBoardResponse.WindowInfo("01:30", "05:30", 4);

        java.time.ZonedDateTime nowIst = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));
        String actDepIso = nowIst.minusMinutes(8).format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        LiveStationBoardResponse.LiveStationBoardTrain train = new LiveStationBoardResponse.LiveStationBoardTrain(
                "20482", "Humsafar Express", "Humsafar Express",
                new LiveStationBoardResponse.StationCodeName("TPJ", "Tiruchirappalli"),
                new LiveStationBoardResponse.StationCodeName("JU", "Jodhpur"),
                StationPassType.PASS_THROUGH,
                "01:08", "01:08",
                actDepIso, actDepIso,
                29, "1",
                StationTrainLiveStatus.DEPARTED,
                "sat",
                null, actDepIso
        );

        LiveStationBoardResponse mockResponse = new LiveStationBoardResponse(stationInfo, windowInfo, List.of(train));
        when(railwayDataProvider.getLiveStationBoardDetails("MCI", 4)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/stations/MCI/live").param("hours", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trains", hasSize(0)));
    }

    @Test
    void testTrainDepartedPreviousStationIsUpcomingAtCurrentStation() throws Exception {
        StationDto mockStation = new StationDto("MCI", "Manchiryal", "Mancherial", "Telangana", 18.879, 79.449, true);
        when(railwayDataProvider.searchStations("MCI")).thenReturn(Arrays.asList(mockStation));

        LiveStationBoardResponse.StationInfo stationInfo = new LiveStationBoardResponse.StationInfo("MCI", "MANCHIRYAL", "Mancherial", 18.879, 79.449);
        LiveStationBoardResponse.WindowInfo windowInfo = new LiveStationBoardResponse.WindowInfo("01:30", "05:30", 4);

        java.time.ZonedDateTime nowIst = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));
        String expArrIso = nowIst.plusMinutes(40).format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        LiveStationBoardResponse.LiveStationBoardTrain train = new LiveStationBoardResponse.LiveStationBoardTrain(
                "12772", "Secunderabad SF Express", "Superfast Express",
                new LiveStationBoardResponse.StationCodeName("R", "Raipur"),
                new LiveStationBoardResponse.StationCodeName("SC", "Secunderabad"),
                StationPassType.STOPPING,
                "03:09", "03:10",
                expArrIso, expArrIso,
                35, "2",
                StationTrainLiveStatus.UPCOMING,
                "tue,thu,sat",
                null, null
        );

        LiveStationBoardResponse mockResponse = new LiveStationBoardResponse(stationInfo, windowInfo, List.of(train));
        when(railwayDataProvider.getLiveStationBoardDetails("MCI", 4)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/stations/MCI/live").param("hours", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trains", hasSize(1)))
                .andExpect(jsonPath("$.data.trains[0].trainNumber").value("12772"))
                .andExpect(jsonPath("$.data.trains[0].liveStatus").value("UPCOMING"))
                .andExpect(jsonPath("$.data.trains[0].actualDeparture").doesNotExist());
    }

    @Test
    void testDelayedTrainIntoFutureWindow() throws Exception {
        StationDto mockStation = new StationDto("MCI", "Manchiryal", "Mancherial", "Telangana", 18.879, 79.449, true);
        when(railwayDataProvider.searchStations("MCI")).thenReturn(Arrays.asList(mockStation));

        LiveStationBoardResponse.StationInfo stationInfo = new LiveStationBoardResponse.StationInfo("MCI", "MANCHIRYAL", "Mancherial", 18.879, 79.449);
        LiveStationBoardResponse.WindowInfo windowInfo = new LiveStationBoardResponse.WindowInfo("01:30", "05:30", 4);

        java.time.ZonedDateTime nowIst = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));
        String schArrIso = nowIst.minusMinutes(40).format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        String expArrIso = nowIst.plusMinutes(40).format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        LiveStationBoardResponse.LiveStationBoardTrain train = new LiveStationBoardResponse.LiveStationBoardTrain(
                "12295", "Sanghamitra SF Express", "Superfast Express",
                new LiveStationBoardResponse.StationCodeName("SMVB", "Bengaluru"),
                new LiveStationBoardResponse.StationCodeName("DNR", "Danapur"),
                StationPassType.PASS_THROUGH,
                schArrIso, schArrIso,
                expArrIso, expArrIso,
                80, "-",
                StationTrainLiveStatus.UPCOMING,
                "daily",
                null, null
        );

        LiveStationBoardResponse mockResponse = new LiveStationBoardResponse(stationInfo, windowInfo, List.of(train));
        when(railwayDataProvider.getLiveStationBoardDetails("MCI", 4)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/stations/MCI/live").param("hours", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trains", hasSize(1)))
                .andExpect(jsonPath("$.data.trains[0].trainNumber").value("12295"))
                .andExpect(jsonPath("$.data.trains[0].delayMinutes").value(80));
    }

    @Test
    void testWindowBoundaryFiltering() throws Exception {
        StationDto mockStation = new StationDto("MCI", "Manchiryal", "Mancherial", "Telangana", 18.879, 79.449, true);
        when(railwayDataProvider.searchStations("MCI")).thenReturn(Arrays.asList(mockStation));

        LiveStationBoardResponse.StationInfo stationInfo = new LiveStationBoardResponse.StationInfo("MCI", "MANCHIRYAL", "Mancherial", 18.879, 79.449);
        java.time.ZonedDateTime nowIst = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));
        String expArrIso = nowIst.plusHours(5).format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        LiveStationBoardResponse.LiveStationBoardTrain train = new LiveStationBoardResponse.LiveStationBoardTrain(
                "04171", "Prayagraj Special", "Train on Demand",
                new LiveStationBoardResponse.StationCodeName("PRYJ", "Prayagraj"),
                new LiveStationBoardResponse.StationCodeName("TEN", "Tirunelveli"),
                StationPassType.PASS_THROUGH,
                "07:05", "07:05",
                expArrIso, expArrIso,
                0, "-",
                StationTrainLiveStatus.SCHEDULED,
                "sat",
                null, null
        );

        LiveStationBoardResponse response4h = new LiveStationBoardResponse(stationInfo, new LiveStationBoardResponse.WindowInfo("01:30", "05:30", 4), List.of(train));
        LiveStationBoardResponse response6h = new LiveStationBoardResponse(stationInfo, new LiveStationBoardResponse.WindowInfo("01:30", "07:30", 6), List.of(train));
        when(railwayDataProvider.getLiveStationBoardDetails("MCI", 4)).thenReturn(response4h);
        when(railwayDataProvider.getLiveStationBoardDetails("MCI", 6)).thenReturn(response6h);

        // 4h window should exclude train at +5 hours
        mockMvc.perform(get("/api/stations/MCI/live").param("hours", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trains", hasSize(0)));

        // 6h window should include train at +5 hours
        mockMvc.perform(get("/api/stations/MCI/live").param("hours", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trains", hasSize(1)))
                .andExpect(jsonPath("$.data.trains[0].trainNumber").value("04171"));
    }

    @Test
    void testCacheIsolationAcrossHours() throws Exception {
        StationDto mockStation = new StationDto("MCI", "Manchiryal", "Mancherial", "Telangana", 18.879, 79.449, true);
        when(railwayDataProvider.searchStations("MCI")).thenReturn(Arrays.asList(mockStation));

        LiveStationBoardResponse.StationInfo stationInfo = new LiveStationBoardResponse.StationInfo("MCI", "MANCHIRYAL", "Mancherial", 18.879, 79.449);
        java.time.ZonedDateTime nowIst = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));

        String train1Time = nowIst.plusHours(1).format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        String train2Time = nowIst.plusHours(3).format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        LiveStationBoardResponse.LiveStationBoardTrain train1 = new LiveStationBoardResponse.LiveStationBoardTrain(
                "11111", "Train 1", "Express",
                new LiveStationBoardResponse.StationCodeName("A", "A"),
                new LiveStationBoardResponse.StationCodeName("B", "B"),
                StationPassType.STOPPING, "01:00", "01:05",
                train1Time, train1Time, 0, "1",
                StationTrainLiveStatus.UPCOMING, "daily", null, null
        );

        LiveStationBoardResponse.LiveStationBoardTrain train2 = new LiveStationBoardResponse.LiveStationBoardTrain(
                "22222", "Train 2", "Express",
                new LiveStationBoardResponse.StationCodeName("C", "C"),
                new LiveStationBoardResponse.StationCodeName("D", "D"),
                StationPassType.STOPPING, "03:00", "03:05",
                train2Time, train2Time, 0, "2",
                StationTrainLiveStatus.UPCOMING, "daily", null, null
        );

        LiveStationBoardResponse res2h = new LiveStationBoardResponse(stationInfo, new LiveStationBoardResponse.WindowInfo("01:00", "03:00", 2), List.of(train1));
        LiveStationBoardResponse res4h = new LiveStationBoardResponse(stationInfo, new LiveStationBoardResponse.WindowInfo("01:00", "05:00", 4), List.of(train1, train2));

        when(railwayDataProvider.getLiveStationBoardDetails("MCI", 2)).thenReturn(res2h);
        when(railwayDataProvider.getLiveStationBoardDetails("MCI", 4)).thenReturn(res4h);

        mockMvc.perform(get("/api/stations/MCI/live").param("hours", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trains", hasSize(1)))
                .andExpect(jsonPath("$.data.trains[0].trainNumber").value("11111"));

        mockMvc.perform(get("/api/stations/MCI/live").param("hours", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trains", hasSize(2)))
                .andExpect(jsonPath("$.data.trains[1].trainNumber").value("22222"));
    }

    @Test
    void testTerminatingTrainWithoutDepartureStaysFor30Minutes() throws Exception {
        StationDto mockStation = new StationDto("MCI", "Manchiryal", "Mancherial", "Telangana", 18.879, 79.449, true);
        when(railwayDataProvider.searchStations("MCI")).thenReturn(Arrays.asList(mockStation));

        LiveStationBoardResponse.StationInfo stationInfo = new LiveStationBoardResponse.StationInfo("MCI", "MANCHIRYAL", "Mancherial", 18.879, 79.449);
        LiveStationBoardResponse.WindowInfo windowInfo = new LiveStationBoardResponse.WindowInfo("01:30", "05:30", 4);

        java.time.ZonedDateTime nowIst = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));
        String actArrIso = nowIst.minusMinutes(20).format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        LiveStationBoardResponse.LiveStationBoardTrain train = new LiveStationBoardResponse.LiveStationBoardTrain(
                "17003", "Ramgiri MEMU Express", "MEMU",
                new LiveStationBoardResponse.StationCodeName("KZJ", "Kazipet"),
                new LiveStationBoardResponse.StationCodeName("MCI", "Manchiryal"),
                StationPassType.STOPPING,
                "01:10", null,
                actArrIso, null,
                0, "3",
                StationTrainLiveStatus.ARRIVED,
                "daily",
                actArrIso, null
        );

        LiveStationBoardResponse mockResponse = new LiveStationBoardResponse(stationInfo, windowInfo, List.of(train));
        when(railwayDataProvider.getLiveStationBoardDetails("MCI", 4)).thenReturn(mockResponse);

        // Train arrived 20 minutes ago without departure (terminating): retained within 30-min window
        mockMvc.perform(get("/api/stations/MCI/live").param("hours", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trains", hasSize(1)))
                .andExpect(jsonPath("$.data.trains[0].trainNumber").value("17003"));
    }

    @Test
    void testTerminatingTrainWithoutDepartureFilteredAfter30Minutes() throws Exception {
        StationDto mockStation = new StationDto("MCI", "Manchiryal", "Mancherial", "Telangana", 18.879, 79.449, true);
        when(railwayDataProvider.searchStations("MCI")).thenReturn(Arrays.asList(mockStation));

        LiveStationBoardResponse.StationInfo stationInfo = new LiveStationBoardResponse.StationInfo("MCI", "MANCHIRYAL", "Mancherial", 18.879, 79.449);
        LiveStationBoardResponse.WindowInfo windowInfo = new LiveStationBoardResponse.WindowInfo("01:30", "05:30", 4);

        java.time.ZonedDateTime nowIst = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));
        String actArrIso = nowIst.minusMinutes(35).format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        LiveStationBoardResponse.LiveStationBoardTrain train = new LiveStationBoardResponse.LiveStationBoardTrain(
                "17003", "Ramgiri MEMU Express", "MEMU",
                new LiveStationBoardResponse.StationCodeName("KZJ", "Kazipet"),
                new LiveStationBoardResponse.StationCodeName("MCI", "Manchiryal"),
                StationPassType.STOPPING,
                "00:55", null,
                actArrIso, null,
                0, "3",
                StationTrainLiveStatus.ARRIVED,
                "daily",
                actArrIso, null
        );

        LiveStationBoardResponse mockResponse = new LiveStationBoardResponse(stationInfo, windowInfo, List.of(train));
        when(railwayDataProvider.getLiveStationBoardDetails("MCI", 4)).thenReturn(mockResponse);

        // Train arrived 35 minutes ago without departure: excluded (> 30 min platform dwell threshold)
        mockMvc.perform(get("/api/stations/MCI/live").param("hours", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trains", hasSize(0)));
    }
}
