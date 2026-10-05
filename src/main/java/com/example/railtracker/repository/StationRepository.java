package com.example.railtracker.repository;

import com.example.railtracker.entity.Station;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StationRepository extends JpaRepository<Station, Long> {
    Optional<Station> findByStationCode(String stationCode);

    @Query("SELECT s FROM Station s WHERE " +
           "LOWER(s.stationCode) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.stationName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(COALESCE(s.district, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(COALESCE(s.city, '')) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "ORDER BY " +
           "CASE " +
           "  WHEN UPPER(s.stationCode) = UPPER(:query) THEN 1 " +
           "  WHEN UPPER(s.stationCode) LIKE UPPER(CONCAT(:query, '%')) THEN 2 " +
           "  WHEN UPPER(s.stationName) LIKE UPPER(CONCAT(:query, '%')) THEN 3 " +
           "  WHEN UPPER(s.stationName) LIKE UPPER(CONCAT('% ', :query, '%')) THEN 4 " +
           "  ELSE 5 " +
           "END, " +
           "CASE " +
           "  WHEN s.newCategory = 'NSG1' THEN 1 " +
           "  WHEN s.newCategory = 'NSG2' THEN 2 " +
           "  WHEN s.newCategory = 'NSG3' THEN 3 " +
           "  WHEN s.newCategory = 'NSG4' THEN 4 " +
           "  WHEN s.newCategory = 'NSG5' THEN 5 " +
           "  WHEN s.newCategory = 'SG1' THEN 6 " +
           "  WHEN s.newCategory = 'SG2' THEN 7 " +
           "  WHEN s.newCategory = 'SG3' THEN 8 " +
           "  WHEN s.newCategory = 'NSG6' THEN 9 " +
           "  WHEN s.newCategory LIKE 'HG%' THEN 10 " +
           "  ELSE 11 " +
           "END, " +
           "s.stationName ASC")
    List<Station> searchStations(@Param("query") String query, Pageable pageable);

    @Query("SELECT s FROM Station s WHERE LOWER(s.stationCode) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(s.stationName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(COALESCE(s.district, '')) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(COALESCE(s.city, '')) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Station> searchStations(@Param("query") String query);
}

