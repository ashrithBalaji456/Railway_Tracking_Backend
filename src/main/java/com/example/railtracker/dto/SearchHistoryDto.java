package com.example.railtracker.dto;

import java.time.LocalDateTime;

public class SearchHistoryDto {
    private Long id;
    private String searchType;
    private String itemCode;
    private String itemName;
    private String subtitle;
    private LocalDateTime searchedAt;

    public SearchHistoryDto() {
    }

    public SearchHistoryDto(Long id, String searchType, String itemCode, String itemName, String subtitle, LocalDateTime searchedAt) {
        this.id = id;
        this.searchType = searchType;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.subtitle = subtitle;
        this.searchedAt = searchedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
