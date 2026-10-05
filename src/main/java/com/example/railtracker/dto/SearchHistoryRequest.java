package com.example.railtracker.dto;

import jakarta.validation.constraints.NotBlank;

public class SearchHistoryRequest {

    @NotBlank(message = "Search type is required")
    private String searchType; // "TRAIN" or "STATION"

    @NotBlank(message = "Item code is required")
    private String itemCode;

    @NotBlank(message = "Item name is required")
    private String itemName;

    private String subtitle;

    public SearchHistoryRequest() {
    }

    public SearchHistoryRequest(String searchType, String itemCode, String itemName, String subtitle) {
        this.searchType = searchType;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.subtitle = subtitle;
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
}
