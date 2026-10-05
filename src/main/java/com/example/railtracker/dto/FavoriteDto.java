package com.example.railtracker.dto;

public record FavoriteDto(
    Long id,
    String favoriteType,
    String itemCode,
    String itemName
) {}
