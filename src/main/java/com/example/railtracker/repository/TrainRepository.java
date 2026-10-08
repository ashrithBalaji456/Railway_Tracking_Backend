package com.example.railtracker.repository;

import com.example.railtracker.entity.Train;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TrainRepository extends JpaRepository<Train, Long> {
    Optional<Train> findByTrainNumber(String trainNumber);
    boolean existsByTrainNumber(String trainNumber);

    @Query("SELECT t FROM Train t WHERE t.trainNumber LIKE CONCAT(:prefix, '%') ORDER BY t.trainNumber ASC")
    List<Train> findByTrainNumberStartingWith(@Param("prefix") String prefix);

    @Query("SELECT t FROM Train t WHERE LOWER(t.trainName) LIKE LOWER(CONCAT('%', :name, '%')) " +
           "ORDER BY CASE WHEN LOWER(t.trainName) LIKE LOWER(CONCAT(:name, '%')) THEN 0 ELSE 1 END, t.trainName ASC")
    List<Train> searchByTrainName(@Param("name") String name, Pageable pageable);

    @Query("SELECT t FROM Train t WHERE t.trainNumber LIKE %:query% OR LOWER(t.trainName) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Train> searchTrains(@Param("query") String query);
}
