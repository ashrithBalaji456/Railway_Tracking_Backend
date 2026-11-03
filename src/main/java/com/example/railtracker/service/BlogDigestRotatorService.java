package com.example.railtracker.service;

import com.example.railtracker.dto.BlogArticleDTO;
import com.example.railtracker.dto.BlogTipDTO;
import com.example.railtracker.entity.NewsletterEdition;
import com.example.railtracker.repository.NewsletterEditionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BlogDigestRotatorService {

    private static final Logger log = LoggerFactory.getLogger(BlogDigestRotatorService.class);

    private final BlogService blogService;
    private final NewsletterEditionRepository newsletterEditionRepository;

    public BlogDigestRotatorService(
            BlogService blogService,
            NewsletterEditionRepository newsletterEditionRepository) {
        this.blogService = blogService;
        this.newsletterEditionRepository = newsletterEditionRepository;
    }

    public record WeeklyDigestBundle(
            int editionNumber,
            BlogArticleDTO spotlight,
            List<BlogArticleDTO> secondaries,
            String travelerTip
    ) {}

    /**
     * Composes the weekly digest bundle using a non-repeating, cross-category rotation algorithm.
     * Brand-new blogs (added in the future) are automatically prioritized first because they have zero send history!
     */
    public WeeklyDigestBundle composeWeeklyDigest() {
        List<BlogArticleDTO> allArticles = blogService.getAllArticles(null, null);
        if (allArticles.isEmpty()) {
            throw new IllegalStateException("No blog articles available to compose weekly newsletter digest.");
        }

        // Determine next edition number
        int nextEditionNumber = newsletterEditionRepository.findTopByOrderByEditionNumberDesc()
                .map(e -> e.getEditionNumber() + 1)
                .orElse(1);

        // Fetch recent editions history (last 15 issues)
        List<NewsletterEdition> recentEditions = newsletterEditionRepository.findTop15ByOrderBySentAtDesc();

        // Build a mapping of slug -> last sent date and total times sent
        Map<String, LocalDateTime> lastSentMap = new HashMap<>();
        Map<String, Integer> sendCountMap = new HashMap<>();

        for (NewsletterEdition edition : recentEditions) {
            recordSlugUsage(edition.getSpotlightSlug(), edition.getSentAt(), lastSentMap, sendCountMap);
            recordSlugUsage(edition.getSecondarySlug1(), edition.getSentAt(), lastSentMap, sendCountMap);
            recordSlugUsage(edition.getSecondarySlug2(), edition.getSentAt(), lastSentMap, sendCountMap);
        }

        // Sort all articles:
        // 1. Articles NEVER sent (count == 0) come first (ensures newly added blogs get spotlighted immediately!)
        // 2. Then articles sent least often
        // 3. Then articles sent longest ago
        List<BlogArticleDTO> candidateArticles = new ArrayList<>(allArticles);
        candidateArticles.sort((a, b) -> {
            int countA = sendCountMap.getOrDefault(a.slug(), 0);
            int countB = sendCountMap.getOrDefault(b.slug(), 0);
            if (countA != countB) {
                return Integer.compare(countA, countB);
            }
            LocalDateTime sentA = lastSentMap.get(a.slug());
            LocalDateTime sentB = lastSentMap.get(b.slug());
            if (sentA == null && sentB == null) return 0;
            if (sentA == null) return -1;
            if (sentB == null) return 1;
            return sentA.compareTo(sentB);
        });

        // Slot 1: Spotlight (The Cover Story)
        BlogArticleDTO spotlight = candidateArticles.get(0);
        Set<String> selectedCategories = new HashSet<>();
        if (spotlight.category() != null) {
            selectedCategories.add(spotlight.category().trim().toLowerCase());
        }

        // Slot 2 & Slot 3: Pick secondaries from DIFFERENT categories for maximum variety!
        List<BlogArticleDTO> secondaries = new ArrayList<>();

        for (int i = 1; i < candidateArticles.size(); i++) {
            if (secondaries.size() >= 2) break;
            BlogArticleDTO candidate = candidateArticles.get(i);
            String candidateCat = candidate.category() != null ? candidate.category().trim().toLowerCase() : "";

            if (!selectedCategories.contains(candidateCat)) {
                secondaries.add(candidate);
                selectedCategories.add(candidateCat);
            }
        }

        // Fallback: if there weren't enough distinct categories, fill secondaries with remaining least-recently-featured
        for (int i = 1; i < candidateArticles.size() && secondaries.size() < 2; i++) {
            BlogArticleDTO candidate = candidateArticles.get(i);
            if (!secondaries.contains(candidate)) {
                secondaries.add(candidate);
            }
        }

        // Select a traveler tip from the spotlight or first secondary
        String travelerTip = resolveTravelerTip(spotlight, secondaries);

        log.info("[BlogDigestRotator] Composed Edition #{} with Spotlight: '{}' and Secondaries: {}",
                nextEditionNumber,
                spotlight.title(),
                secondaries.stream().map(BlogArticleDTO::title).collect(Collectors.toList()));

        return new WeeklyDigestBundle(nextEditionNumber, spotlight, secondaries, travelerTip);
    }

    private void recordSlugUsage(
            String slug,
            LocalDateTime sentAt,
            Map<String, LocalDateTime> lastSentMap,
            Map<String, Integer> sendCountMap) {
        if (slug == null || slug.isBlank()) return;
        sendCountMap.put(slug, sendCountMap.getOrDefault(slug, 0) + 1);
        if (!lastSentMap.containsKey(slug) || sentAt.isAfter(lastSentMap.get(slug))) {
            lastSentMap.put(slug, sentAt);
        }
    }

    private String resolveTravelerTip(BlogArticleDTO spotlight, List<BlogArticleDTO> secondaries) {
        try {
            List<BlogTipDTO> tips = blogService.getTipsForArticle(spotlight.slug());
            if (tips != null && !tips.isEmpty()) {
                return tips.get(0).tipText();
            }
        } catch (Exception ignored) {}

        if (spotlight.quickFacts() != null && !spotlight.quickFacts().isEmpty()) {
            var fact = spotlight.quickFacts().get(0);
            return fact.label() + ": " + fact.value();
        }

        return "Always keep digital copies of your train tickets and photo ID handy on mobile before boarding!";
    }
}
