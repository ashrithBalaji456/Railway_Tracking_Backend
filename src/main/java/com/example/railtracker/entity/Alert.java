package com.example.railtracker.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "alerts", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "train_number"})
})
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "train_number", nullable = false, length = 50)
    private String trainNumber;

    @Column(name = "train_name", nullable = false, length = 100)
    private String trainName;

    @Column(name = "threshold_minutes", nullable = false)
    private Integer thresholdMinutes;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    public Alert() {
    }

    public Alert(User user, String trainNumber, String trainName, Integer thresholdMinutes) {
        this.user = user;
        this.trainNumber = trainNumber;
        this.trainName = trainName;
        this.thresholdMinutes = thresholdMinutes;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getTrainNumber() {
        return trainNumber;
    }

    public void setTrainNumber(String trainNumber) {
        this.trainNumber = trainNumber;
    }

    public String getTrainName() {
        return trainName;
    }

    public void setTrainName(String trainName) {
        this.trainName = trainName;
    }

    public Integer getThresholdMinutes() {
        return thresholdMinutes;
    }

    public void setThresholdMinutes(Integer thresholdMinutes) {
        this.thresholdMinutes = thresholdMinutes;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
