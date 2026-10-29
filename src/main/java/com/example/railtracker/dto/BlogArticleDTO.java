package com.example.railtracker.dto;

import java.io.Serializable;
import java.util.List;

public record BlogArticleDTO(
    String id,
    String slug,
    String title,
    String subtitle,
    String category,
    String coverImage,
    String author,
    String authorRole,
    Integer readTimeMinutes,
    String publishedDate,
    Boolean featured,
    List<String> tags,
    List<QuickFact> quickFacts,
    List<RelatedTrain> relatedTrains,
    List<ArticleSection> sections
) implements Serializable {

    public record QuickFact(
        String label,
        String value,
        String icon
    ) implements Serializable {}

    public record RelatedTrain(
        String trainNumber,
        String trainName,
        String route
    ) implements Serializable {}

    public record ArticleSection(
        String type, // "heading", "paragraph", "image", "tip", "trivia", "quote"
        String title,
        String content,
        String imageUrl,
        String imageCaption,
        String altText
    ) implements Serializable {}
}

