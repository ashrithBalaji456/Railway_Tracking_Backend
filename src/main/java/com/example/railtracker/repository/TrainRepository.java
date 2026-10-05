package com.example.railtracker.repository;

import com.example.railtracker.entity.Train;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TrainRepository extends JpaRepository<Train, Long> {
    Optional<Train> findByTrainNumber(String trainNumber);

    @Query("SELECT t FROM Train t WHERE t.trainNumber LIKE %:query% OR LOWER(t.trainName) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Train> searchTrains(@Param("query") String query);
}
