package com.example.railtracker.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "trains", indexes = {
    @Index(name = "idx_trains_number", columnList = "trainNumber")
})
public class Train {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 20)
    private String trainNumber;

    @Column(nullable = false, length = 255)
    private String trainName;

    @Column(length = 100)
    private String trainType;

    @Column(length = 100)
    private String category;

    @Column(length = 150)
    private String sourceStation;

    @Column(length = 150)
    private String destinationStation;

    private Integer distance; // in km
    private Integer duration; // in minutes

    @Column(length = 100)
    private String runningDays; // e.g. "Mon,Tue,Wed,Thu,Fri,Sat,Sun" or "1,2,3,4,5,6,7"

    @Column(length = 500)
    private String coachPosition;

    @Column(nullable = false)
    private Boolean active = true;

    public Train() {}

    public Train(String trainNumber, String trainName, String trainType, String category, String sourceStation, String destinationStation, Integer distance, Integer duration, String runningDays) {
        this(trainNumber, trainName, trainType, category, sourceStation, destinationStation, distance, duration, runningDays, null);
    }

    public Train(String trainNumber, String trainName, String trainType, String category, String sourceStation, String destinationStation, Integer distance, Integer duration, String runningDays, String coachPosition) {
        this.trainNumber = trainNumber;
        this.trainName = trainName;
        this.trainType = trainType;
        this.category = category;
        this.sourceStation = sourceStation;
        this.destinationStation = destinationStation;
        this.distance = distance;
        this.duration = duration;
        this.runningDays = runningDays;
        this.coachPosition = coachPosition;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getTrainType() {
        return trainType;
    }

    public void setTrainType(String trainType) {
        this.trainType = trainType;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSourceStation() {
        return sourceStation;
    }

    public void setSourceStation(String sourceStation) {
        this.sourceStation = sourceStation;
    }

    public String getDestinationStation() {
        return destinationStation;
    }

    public void setDestinationStation(String destinationStation) {
        this.destinationStation = destinationStation;
    }

    public Integer getDistance() {
        return distance;
    }

    public void setDistance(Integer distance) {
        this.distance = distance;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getRunningDays() {
        return runningDays;
    }

    public void setRunningDays(String runningDays) {
        this.runningDays = runningDays;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getCoachPosition() {
        return coachPosition;
    }

    public void setCoachPosition(String coachPosition) {
        this.coachPosition = coachPosition;
    }
}
