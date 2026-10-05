package com.example.railtracker.entity;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
@Table(name = "train_stations", indexes = {
    @Index(name = "idx_train_stations_train_seq", columnList = "train_id, sequenceNumber"),
    @Index(name = "idx_train_stations_station", columnList = "station_id")
})
public class TrainStation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "train_id", nullable = false)
    private Train train;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @Column(nullable = false)
    private Integer sequenceNumber;

    private LocalTime arrivalTime;
    private LocalTime departureTime;

    @Column(nullable = false)
    private Integer arrivalDay = 0;

    @Column(nullable = false)
    private Integer departureDay = 0;

    @Column(nullable = false)
    private Integer haltMinutes = 0;

    @Column(nullable = false, length = 20)
    private String stopType = "STOPPING"; // STOPPING, PASS_THROUGH

    @Column(nullable = false)
    private Boolean isHalt = true;

    private Double distance = 0.0;
    private Double speedToNextStationKmph = 0.0;

    public TrainStation() {}

    public TrainStation(Train train, Station station, Integer sequenceNumber, LocalTime arrivalTime, LocalTime departureTime, Integer arrivalDay, Integer departureDay, Integer haltMinutes, String stopType, Boolean isHalt, Double distance) {
        this(train, station, sequenceNumber, arrivalTime, departureTime, arrivalDay, departureDay, haltMinutes, stopType, isHalt, distance, 0.0);
    }

    public TrainStation(Train train, Station station, Integer sequenceNumber, LocalTime arrivalTime, LocalTime departureTime, Integer arrivalDay, Integer departureDay, Integer haltMinutes, String stopType, Boolean isHalt, Double distance, Double speedToNextStationKmph) {
        this.train = train;
        this.station = station;
        this.sequenceNumber = sequenceNumber;
        this.arrivalTime = arrivalTime;
        this.departureTime = departureTime;
        this.arrivalDay = arrivalDay;
        this.departureDay = departureDay;
        this.haltMinutes = haltMinutes;
        this.stopType = stopType;
        this.isHalt = isHalt;
        this.distance = distance;
        this.speedToNextStationKmph = speedToNextStationKmph;
    }

    public TrainStation(Train train, Station station, Integer sequenceNumber, LocalTime arrivalTime, LocalTime departureTime, Integer arrivalDay, Integer departureDay, Integer haltMinutes, String stopType, Boolean isHalt) {
        this(train, station, sequenceNumber, arrivalTime, departureTime, arrivalDay, departureDay, haltMinutes, stopType, isHalt, 0.0, 0.0);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Train getTrain() {
        return train;
    }

    public void setTrain(Train train) {
        this.train = train;
    }

    public Station getStation() {
        return station;
    }

    public void setStation(Station station) {
        this.station = station;
    }

    public Integer getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(Integer sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public LocalTime getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(LocalTime arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public LocalTime getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(LocalTime departureTime) {
        this.departureTime = departureTime;
    }

    public Integer getArrivalDay() {
        return arrivalDay;
    }

    public void setArrivalDay(Integer arrivalDay) {
        this.arrivalDay = arrivalDay;
    }

    public Integer getDepartureDay() {
        return departureDay;
    }

    public void setDepartureDay(Integer departureDay) {
        this.departureDay = departureDay;
    }

    public Integer getHaltMinutes() {
        return haltMinutes;
    }

    public void setHaltMinutes(Integer haltMinutes) {
        this.haltMinutes = haltMinutes;
    }

    public String getStopType() {
        return stopType;
    }

    public void setStopType(String stopType) {
        this.stopType = stopType;
    }

    public Boolean getIsHalt() {
        return isHalt;
    }

    public void setIsHalt(Boolean halt) {
        isHalt = halt;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }

    public Double getSpeedToNextStationKmph() {
        return speedToNextStationKmph;
    }

    public void setSpeedToNextStationKmph(Double speedToNextStationKmph) {
        this.speedToNextStationKmph = speedToNextStationKmph;
    }
}
