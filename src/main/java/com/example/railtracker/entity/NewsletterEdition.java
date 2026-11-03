package com.example.railtracker.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "newsletter_editions")
public class NewsletterEdition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "edition_number", nullable = false)
    private Integer editionNumber;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt = LocalDateTime.now();

    @Column(name = "spotlight_slug", nullable = false, length = 150)
    private String spotlightSlug;

    @Column(name = "secondary_slug_1", length = 150)
    private String secondarySlug1;

    @Column(name = "secondary_slug_2", length = 150)
    private String secondarySlug2;

    @Column(name = "tip_snippet", length = 500)
    private String tipSnippet;

    @Column(name = "recipients_count", nullable = false)
    private Integer recipientsCount = 0;

    public NewsletterEdition() {}

    public NewsletterEdition(
            Integer editionNumber,
            String spotlightSlug,
            String secondarySlug1,
            String secondarySlug2,
            String tipSnippet,
            Integer recipientsCount) {
        this.editionNumber = editionNumber;
        this.sentAt = LocalDateTime.now();
        this.spotlightSlug = spotlightSlug;
        this.secondarySlug1 = secondarySlug1;
        this.secondarySlug2 = secondarySlug2;
        this.tipSnippet = tipSnippet;
        this.recipientsCount = recipientsCount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getEditionNumber() { return editionNumber; }
    public void setEditionNumber(Integer editionNumber) { this.editionNumber = editionNumber; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    public String getSpotlightSlug() { return spotlightSlug; }
    public void setSpotlightSlug(String spotlightSlug) { this.spotlightSlug = spotlightSlug; }
    public String getSecondarySlug1() { return secondarySlug1; }
    public void setSecondarySlug1(String secondarySlug1) { this.secondarySlug1 = secondarySlug1; }
    public String getSecondarySlug2() { return secondarySlug2; }
    public void setSecondarySlug2(String secondarySlug2) { this.secondarySlug2 = secondarySlug2; }
    public String getTipSnippet() { return tipSnippet; }
    public void setTipSnippet(String tipSnippet) { this.tipSnippet = tipSnippet; }
    public Integer getRecipientsCount() { return recipientsCount; }
    public void setRecipientsCount(Integer recipientsCount) { this.recipientsCount = recipientsCount; }
}
