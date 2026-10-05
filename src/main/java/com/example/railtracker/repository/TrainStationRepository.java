package com.example.railtracker.repository;

import com.example.railtracker.entity.TrainStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TrainStationRepository extends JpaRepository<TrainStation, Long> {
    List<TrainStation> findByTrainTrainNumberOrderBySequenceNumberAsc(String trainNumber);

    List<TrainStation> findByStationStationCode(String stationCode);

    List<TrainStation> findByStationStationCodeAndStopType(String stationCode, String stopType);

    @Query("SELECT ts FROM TrainStation ts JOIN FETCH ts.train JOIN FETCH ts.station WHERE ts.station.stationCode = :stationCode")
    List<TrainStation> findByStationCodeWithDetails(@Param("stationCode") String stationCode);
}
