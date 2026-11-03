package com.example.railtracker.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_notification_preferences")
public class UserNotificationPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String username;

    @Column
    private String whatsappPhone;

    @Column(nullable = false)
    private Boolean weeklyDigestEnabled = true;

    @Column(nullable = false)
    private Integer delayAlertThresholdMinutes = 15;

    @Column(nullable = false)
    private Boolean travelReminderEnabled = true;

    @Column(nullable = false)
    private Boolean stationWakeUpAlarmEnabled = true;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void prePersistOrUpdate() {
        if (this.weeklyDigestEnabled == null) this.weeklyDigestEnabled = true;
        if (this.delayAlertThresholdMinutes == null) this.delayAlertThresholdMinutes = 15;
        if (this.travelReminderEnabled == null) this.travelReminderEnabled = true;
        if (this.stationWakeUpAlarmEnabled == null) this.stationWakeUpAlarmEnabled = true;
        this.updatedAt = LocalDateTime.now();
    }

    public UserNotificationPreferences() {}

    public UserNotificationPreferences(String email, String username, String whatsappPhone) {
        this.email = email;
        this.username = username;
        this.whatsappPhone = whatsappPhone;
        this.weeklyDigestEnabled = true;
        this.delayAlertThresholdMinutes = 15;
        this.travelReminderEnabled = true;
        this.stationWakeUpAlarmEnabled = true;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getWhatsappPhone() {
        return whatsappPhone;
    }

    public void setWhatsappPhone(String whatsappPhone) {
        this.whatsappPhone = whatsappPhone;
    }

    public Boolean getWeeklyDigestEnabled() {
        return weeklyDigestEnabled != null ? weeklyDigestEnabled : true;
    }

    public void setWeeklyDigestEnabled(Boolean weeklyDigestEnabled) {
        this.weeklyDigestEnabled = weeklyDigestEnabled;
    }

    public Integer getDelayAlertThresholdMinutes() {
        return delayAlertThresholdMinutes != null ? delayAlertThresholdMinutes : 15;
    }

    public void setDelayAlertThresholdMinutes(Integer delayAlertThresholdMinutes) {
        this.delayAlertThresholdMinutes = delayAlertThresholdMinutes;
    }

    public Boolean getTravelReminderEnabled() {
        return travelReminderEnabled != null ? travelReminderEnabled : true;
    }

    public void setTravelReminderEnabled(Boolean travelReminderEnabled) {
        this.travelReminderEnabled = travelReminderEnabled;
    }

    public Boolean getStationWakeUpAlarmEnabled() {
        return stationWakeUpAlarmEnabled != null ? stationWakeUpAlarmEnabled : true;
    }

    public void setStationWakeUpAlarmEnabled(Boolean stationWakeUpAlarmEnabled) {
        this.stationWakeUpAlarmEnabled = stationWakeUpAlarmEnabled;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

