package com.example.railtracker.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "search_histories", indexes = {
    @Index(name = "idx_search_history_user", columnList = "user_id"),
    @Index(name = "idx_search_history_user_time", columnList = "user_id, searched_at")
}, uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "search_type", "item_code"})
})
public class SearchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "search_type", nullable = false, length = 20)
    private String searchType; // "TRAIN" or "STATION"

    @Column(name = "item_code", nullable = false, length = 50)
    private String itemCode; // train number or station code

    @Column(name = "item_name", nullable = false, length = 150)
    private String itemName; // train name or station name

    @Column(name = "subtitle", length = 255)
    private String subtitle; // extra details (e.g. "Hyderabad to Tambaram" or "Krishna, AP")

    @Column(name = "searched_at", nullable = false)
    private LocalDateTime searchedAt;

    public SearchHistory() {
        this.searchedAt = LocalDateTime.now();
    }

    public SearchHistory(User user, String searchType, String itemCode, String itemName, String subtitle) {
        this.user = user;
        this.searchType = searchType;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.subtitle = subtitle;
        this.searchedAt = LocalDateTime.now();
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

    public String getSearchType() {
        return searchType;
    }

    public void setSearchType(String searchType) {
        this.searchType = searchType;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public LocalDateTime getSearchedAt() {
        return searchedAt;
    }

    public void setSearchedAt(LocalDateTime searchedAt) {
        this.searchedAt = searchedAt;
    }
}
