package com.example.railtracker.repository;

import com.example.railtracker.entity.UserNotificationPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserNotificationPreferencesRepository extends JpaRepository<UserNotificationPreferences, Long> {

    Optional<UserNotificationPreferences> findByEmailIgnoreCase(String email);

    Optional<UserNotificationPreferences> findByUsernameIgnoreCase(String username);
}

