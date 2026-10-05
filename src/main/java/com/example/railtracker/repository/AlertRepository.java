package com.example.railtracker.repository;

import com.example.railtracker.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByUserUsername(String username);

    Optional<Alert> findByUserUsernameAndTrainNumber(String username, String trainNumber);
}
