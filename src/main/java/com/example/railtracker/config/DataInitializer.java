package com.example.railtracker.config;

import com.example.railtracker.entity.Station;
import com.example.railtracker.entity.Train;
import com.example.railtracker.entity.TrainStation;
import com.example.railtracker.repository.StationRepository;
import com.example.railtracker.repository.TrainRepository;
import com.example.railtracker.repository.TrainStationRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalTime;
import java.util.*;

@Component
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final StationRepository stationRepository;
    private final TrainRepository trainRepository;
    private final TrainStationRepository trainStationRepository;
    private final ObjectMapper objectMapper;

    public DataInitializer(
            StationRepository stationRepository,
            TrainRepository trainRepository,
            TrainStationRepository trainStationRepository,
            ObjectMapper objectMapper) {
        this.stationRepository = stationRepository;
        this.trainRepository = trainRepository;
        this.trainStationRepository = trainStationRepository;
        this.objectMapper = objectMapper;
    }

    private static record StationSeedRecord(
            Integer serialNumber,
            String stationCode,
            String stationName,
            String oldCategory,
            String newCategory,
            String division,
            String zone,
            String district,
            String state
    ) {}

    private static final Map<String, double[]> KNOWN_COORDINATES = Map.ofEntries(
            Map.entry("HYB", new double[]{17.385, 78.486}),
            Map.entry("SC", new double[]{17.439, 78.502}),
            Map.entry("KZJ", new double[]{17.978, 79.521}),
            Map.entry("WL", new double[]{17.969, 79.598}),
            Map.entry("KMT", new double[]{17.250, 80.150}),
            Map.entry("BZA", new double[]{16.506, 80.648}),
            Map.entry("TEL", new double[]{16.241, 80.645}),
            Map.entry("OGL", new double[]{15.503, 80.052}),
            Map.entry("NLR", new double[]{14.449, 79.988}),
            Map.entry("GDR", new double[]{14.108, 79.851}),
            Map.entry("MS", new double[]{13.082, 80.270}),
            Map.entry("MAS", new double[]{13.0827, 80.2707}),
            Map.entry("BPQ", new double[]{19.855, 79.348}),
            Map.entry("NGP", new double[]{21.152, 79.088}),
            Map.entry("ET", new double[]{22.612, 77.785}),
            Map.entry("BPL", new double[]{23.259, 77.412}),
            Map.entry("VGLJ", new double[]{25.448, 78.568}),
            Map.entry("VGLB", new double[]{25.448, 78.568}),
            Map.entry("AGC", new double[]{27.158, 77.994}),
            Map.entry("NDLS", new double[]{28.614, 77.209}),
            Map.entry("HWH", new double[]{22.5839, 88.3428}),
            Map.entry("CSMT", new double[]{18.9401, 72.8353}),
            Map.entry("MMCT", new double[]{18.9696, 72.8193}),
            Map.entry("PUNE", new double[]{18.5284, 73.8739}),
            Map.entry("SBC", new double[]{12.9781, 77.5696}),
            Map.entry("YPR", new double[]{13.0236, 77.5503})
    );

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        seedStations();
        seedSampleTrains();
    }

    private void seedStations() {
        long currentCount = stationRepository.count();
        if (currentCount >= 8000) {
            logger.info("Station database already contains {} stations. Skipping seed.", currentCount);
            return;
        }

        logger.info("Seeding Indian Railway stations into database from stations_data.json...");
        try (InputStream is = getClass().getResourceAsStream("/stations_data.json")) {
            if (is == null) {
                logger.error("stations_data.json not found in classpath!");
                return;
            }

            List<StationSeedRecord> records = objectMapper.readValue(is, new TypeReference<List<StationSeedRecord>>() {});
            List<Station> stationsToSave = new ArrayList<>();

            for (StationSeedRecord r : records) {
                if (r.stationCode() == null || r.stationCode().isBlank()) continue;

                String code = r.stationCode().trim().toUpperCase();
                double[] coords = KNOWN_COORDINATES.get(code);
                Double lat = coords != null ? coords[0] : null;
                Double lon = coords != null ? coords[1] : null;

                Station st = new Station(
                        r.serialNumber(),
                        code,
                        r.stationName() != null ? r.stationName().trim() : code,
                        r.oldCategory(),
                        r.newCategory(),
                        r.division(),
                        r.zone(),
                        r.district(),
                        r.district() != null ? r.district() : "",
                        r.state(),
                        lat,
                        lon
                );
                stationsToSave.add(st);
            }

            stationRepository.saveAll(stationsToSave);
            logger.info("Successfully seeded {} Indian Railway stations into database!", stationsToSave.size());
        } catch (Exception e) {
            logger.error("Failed to seed stations from stations_data.json", e);
        }
    }

    private void seedSampleTrains() {
        if (trainRepository.count() > 0) {
            logger.info("Sample trains already seeded. Skipping.");
            return;
        }

        logger.info("Seeding sample trains (12760 Charminar Express and 12626 Kerala Express)...");

        // Helper to retrieve or create fallback station
        Station hyb = getOrCreateStation("HYB", "Hyderabad Deccan", "Hyderabad", "Telangana", 17.385, 78.486);
        Station sc = getOrCreateStation("SC", "Secunderabad Junction", "Secunderabad", "Telangana", 17.439, 78.502);
        Station kzj = getOrCreateStation("KZJ", "Kazipet Junction", "Kazipet", "Telangana", 17.978, 79.521);
        Station wl = getOrCreateStation("WL", "Warangal", "Warangal", "Telangana", 17.969, 79.598);
        Station kmt = getOrCreateStation("KMT", "Khammam", "Khammam", "Telangana", 17.250, 80.150);
        Station bza = getOrCreateStation("BZA", "Vijayawada Junction", "Vijayawada", "Andhra Pradesh", 16.506, 80.648);
        Station tel = getOrCreateStation("TEL", "Tenali Junction", "Tenali", "Andhra Pradesh", 16.241, 80.645);
        Station ogl = getOrCreateStation("OGL", "Ongole", "Ongole", "Andhra Pradesh", 15.503, 80.052);
        Station nlr = getOrCreateStation("NLR", "Nellore", "Nellore", "Andhra Pradesh", 14.449, 79.988);
        Station gdr = getOrCreateStation("GDR", "Gudur Junction", "Gudur", "Andhra Pradesh", 14.108, 79.851);
        Station ms = getOrCreateStation("MS", "Chennai Egmore", "Chennai", "Tamil Nadu", 13.082, 80.270);

        Station bpq = getOrCreateStation("BPQ", "Balharshah Junction", "Balharshah", "Maharashtra", 19.855, 79.348);
        Station ngp = getOrCreateStation("NGP", "Nagpur Junction", "Nagpur", "Maharashtra", 21.152, 79.088);
        Station et = getOrCreateStation("ET", "Itarsi Junction", "Itarsi", "Madhya Pradesh", 22.612, 77.785);
        Station bpl = getOrCreateStation("BPL", "Bhopal Junction", "Bhopal", "Madhya Pradesh", 23.259, 77.412);
        Station vglj = getOrCreateStation("VGLJ", "VGL Jhansi Junction", "Jhansi", "Uttar Pradesh", 25.448, 78.568);
        Station agc = getOrCreateStation("AGC", "Agra Cantt", "Agra", "Uttar Pradesh", 27.158, 77.994);
        Station ndls = getOrCreateStation("NDLS", "New Delhi", "New Delhi", "Delhi", 28.614, 77.209);

        // 2. Create Trains
        Train t12760 = new Train(
                "12760",
                "Charminar Express",
                "Superfast",
                "EXPRESS",
                "HYB",
                "MS",
                660,
                720,
                "Mon,Tue,Wed,Thu,Fri,Sat,Sun"
        );

        Train t12626 = new Train(
                "12626",
                "Kerala Express",
                "Superfast",
                "EXPRESS",
                "BZA",
                "NDLS",
                1400,
                1260,
                "Mon,Tue,Wed,Thu,Fri,Sat,Sun"
        );

        trainRepository.saveAll(List.of(t12760, t12626));

        // 3. Create Halts for Train 12760 (Charminar Express)
        TrainStation ts1 = new TrainStation(t12760, hyb, 1, null, LocalTime.of(18, 0), 0, 0, 0, "STOPPING", true);
        TrainStation ts2 = new TrainStation(t12760, sc, 2, LocalTime.of(18, 20), LocalTime.of(18, 40), 0, 0, 20, "STOPPING", true);
        TrainStation ts3 = new TrainStation(t12760, kzj, 3, LocalTime.of(20, 30), LocalTime.of(20, 30), 0, 0, 0, "PASS_THROUGH", false);
        TrainStation ts4 = new TrainStation(t12760, wl, 4, LocalTime.of(20, 50), LocalTime.of(20, 55), 0, 0, 5, "STOPPING", true);
        TrainStation ts5 = new TrainStation(t12760, kmt, 5, LocalTime.of(21, 45), LocalTime.of(21, 45), 0, 0, 0, "PASS_THROUGH", false);
        TrainStation ts6 = new TrainStation(t12760, bza, 6, LocalTime.of(23, 20), LocalTime.of(23, 35), 0, 0, 15, "STOPPING", true);
        TrainStation ts7 = new TrainStation(t12760, tel, 7, LocalTime.of(0, 10), LocalTime.of(0, 12), 1, 1, 2, "STOPPING", true);
        TrainStation ts8 = new TrainStation(t12760, ogl, 8, LocalTime.of(1, 30), LocalTime.of(1, 30), 1, 1, 0, "PASS_THROUGH", false);
        TrainStation ts9 = new TrainStation(t12760, nlr, 9, LocalTime.of(3, 0), LocalTime.of(3, 2), 1, 1, 2, "STOPPING", true);
        TrainStation ts10 = new TrainStation(t12760, gdr, 10, LocalTime.of(3, 40), LocalTime.of(3, 40), 1, 1, 0, "PASS_THROUGH", false);
        TrainStation ts11 = new TrainStation(t12760, ms, 11, LocalTime.of(6, 0), null, 1, 1, 0, "STOPPING", true);

        // 4. Create Halts for Train 12626 (Kerala Express)
        TrainStation tsK1 = new TrainStation(t12626, bza, 1, null, LocalTime.of(1, 15), 1, 1, 0, "STOPPING", true);
        TrainStation tsK2 = new TrainStation(t12626, wl, 2, LocalTime.of(3, 30), LocalTime.of(3, 35), 1, 1, 5, "STOPPING", true);
        TrainStation tsK3 = new TrainStation(t12626, kzj, 3, LocalTime.of(3, 50), LocalTime.of(3, 50), 1, 1, 0, "PASS_THROUGH", false);
        TrainStation tsK4 = new TrainStation(t12626, bpq, 4, LocalTime.of(7, 30), LocalTime.of(7, 40), 1, 1, 10, "STOPPING", true);
        TrainStation tsK5 = new TrainStation(t12626, ngp, 5, LocalTime.of(10, 50), LocalTime.of(11, 0), 1, 1, 10, "STOPPING", true);
        TrainStation tsK6 = new TrainStation(t12626, et, 6, LocalTime.of(15, 30), LocalTime.of(15, 30), 1, 1, 0, "PASS_THROUGH", false);
        TrainStation tsK7 = new TrainStation(t12626, bpl, 7, LocalTime.of(17, 10), LocalTime.of(17, 15), 1, 1, 5, "STOPPING", true);
        TrainStation tsK8 = new TrainStation(t12626, vglj, 8, LocalTime.of(21, 0), LocalTime.of(21, 0), 1, 1, 0, "PASS_THROUGH", false);
        TrainStation tsK9 = new TrainStation(t12626, agc, 9, LocalTime.of(0, 30), LocalTime.of(0, 35), 2, 2, 5, "STOPPING", true);
        TrainStation tsK10 = new TrainStation(t12626, ndls, 10, LocalTime.of(3, 30), null, 2, 2, 0, "STOPPING", true);

        trainStationRepository.saveAll(List.of(
                ts1, ts2, ts3, ts4, ts5, ts6, ts7, ts8, ts9, ts10, ts11,
                tsK1, tsK2, tsK3, tsK4, tsK5, tsK6, tsK7, tsK8, tsK9, tsK10
        ));

        logger.info("Railway sample trains initialization complete!");
    }

    private Station getOrCreateStation(String code, String defaultName, String city, String state, double lat, double lon) {
        return stationRepository.findByStationCode(code)
                .map(s -> {
                    if (s.getLatitude() == null) {
                        s.setLatitude(lat);
                        s.setLongitude(lon);
                        return stationRepository.save(s);
                    }
                    return s;
                })
                .orElseGet(() -> {
                    Station s = new Station(code, defaultName, city, state, lat, lon);
                    return stationRepository.save(s);
                });
    }
}



