package com.example.railtracker.dto;

import java.time.LocalDateTime;

public record BlogTipDTO(
    String id,
    String articleSlug,
    String authorName,
    String authorHandle,
    String category,
    String tipText,
    String photoUrl,
    String travelDate,
    int likesCount,
    LocalDateTime createdAt
) {}

