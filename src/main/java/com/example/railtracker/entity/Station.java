package com.example.railtracker.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "stations", indexes = {
    @Index(name = "idx_stations_code", columnList = "stationCode"),
    @Index(name = "idx_stations_name", columnList = "stationName"),
    @Index(name = "idx_stations_district", columnList = "district"),
    @Index(name = "idx_stations_state", columnList = "state")
})
public class Station {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer serialNumber;

    @Column(unique = true, nullable = false, length = 20)
    private String stationCode;

    @Column(nullable = false, length = 200)
    private String stationName;

    @Column(length = 50)
    private String oldCategory;

    @Column(length = 255)
    private String newCategory;

    @Column(length = 50)
    private String division;

    @Column(length = 50)
    private String zone;

    @Column(length = 100)
    private String district;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    private Double latitude;
    private Double longitude;

    @Column(nullable = false)
    private Boolean active = true;

    public Station() {}

    public Station(String stationCode, String stationName, String city, String state, Double latitude, Double longitude) {
        this.stationCode = stationCode;
        this.stationName = stationName;
        this.city = city;
        this.state = state;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Station(Integer serialNumber, String stationCode, String stationName, String oldCategory, String newCategory,
                   String division, String zone, String district, String city, String state, Double latitude, Double longitude) {
        this.serialNumber = serialNumber;
        this.stationCode = stationCode;
        this.stationName = stationName;
        this.oldCategory = oldCategory;
        this.newCategory = newCategory;
        this.division = division;
        this.zone = zone;
        this.district = district;
        this.city = city;
        this.state = state;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(Integer serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getStationCode() {
        return stationCode;
    }

    public void setStationCode(String stationCode) {
        this.stationCode = stationCode;
    }

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public String getOldCategory() {
        return oldCategory;
    }

    public void setOldCategory(String oldCategory) {
        this.oldCategory = oldCategory;
    }

    public String getNewCategory() {
        return newCategory;
    }

    public void setNewCategory(String newCategory) {
        this.newCategory = newCategory;
    }

    public String getDivision() {
        return division;
    }

    public void setDivision(String division) {
        this.division = division;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}

