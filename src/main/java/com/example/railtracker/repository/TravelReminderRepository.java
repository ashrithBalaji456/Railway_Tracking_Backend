package com.example.railtracker.repository;

import com.example.railtracker.entity.TravelReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TravelReminderRepository extends JpaRepository<TravelReminder, Long> {

    List<TravelReminder> findByTravelDateAndReminderSentFalse(LocalDate travelDate);

    List<TravelReminder> findByEmailOrderByTravelDateDesc(String email);

    Optional<TravelReminder> findByPnrAndEmail(String pnr, String email);
}

