package com.example.railtracker.service;

import com.example.railtracker.dto.BlogArticleDTO;
import com.example.railtracker.entity.NewsletterEdition;
import com.example.railtracker.entity.User;
import com.example.railtracker.repository.NewsletterEditionRepository;
import com.example.railtracker.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WeeklyBlogDigestScheduler {

    private static final Logger log = LoggerFactory.getLogger(WeeklyBlogDigestScheduler.class);

    private final BlogDigestRotatorService rotatorService;
    private final NewsletterEditionRepository newsletterEditionRepository;
    private final UserRepository userRepository;
    private final BrevoEmailService brevoEmailService;

    public WeeklyBlogDigestScheduler(
            BlogDigestRotatorService rotatorService,
            NewsletterEditionRepository newsletterEditionRepository,
            UserRepository userRepository,
            BrevoEmailService brevoEmailService) {
        this.rotatorService = rotatorService;
        this.newsletterEditionRepository = newsletterEditionRepository;
        this.userRepository = userRepository;
        this.brevoEmailService = brevoEmailService;
    }

    /**
     * Automated weekly cron job: Runs every Monday at 9:00 AM IST.
     * Can be customized via app.newsletter.cron in application.yml.
     */
    @Scheduled(cron = "${app.newsletter.cron:0 0 9 * * MON}", zone = "Asia/Kolkata")
    @Transactional
    public void dispatchWeeklyNewsletter() {
        log.info("[WeeklyBlogDigestScheduler] Starting automated weekly rail digest dispatch...");

        BlogDigestRotatorService.WeeklyDigestBundle digest = rotatorService.composeWeeklyDigest();
        List<User> users = userRepository.findAll();

        int sentCount = 0;
        for (User user : users) {
            if (Boolean.TRUE.equals(user.getActive()) && user.getEmail() != null && !user.getEmail().isBlank()) {
                try {
                    boolean sent = brevoEmailService.sendWeeklyBlogDigestEmail(
                            user.getEmail(),
                            user.getUsername(),
                            digest
                    );
                    if (sent) sentCount++;
                } catch (Exception e) {
                    log.warn("[WeeklyBlogDigestScheduler] Failed sending digest to user {}: {}", user.getEmail(), e.getMessage());
                }
            }
        }

        // Save edition history so subsequent issues rotate to different stories
        String sec1 = !digest.secondaries().isEmpty() ? digest.secondaries().get(0).slug() : null;
        String sec2 = digest.secondaries().size() > 1 ? digest.secondaries().get(1).slug() : null;

        NewsletterEdition edition = new NewsletterEdition(
                digest.editionNumber(),
                digest.spotlight().slug(),
                sec1,
                sec2,
                digest.travelerTip(),
                sentCount
        );
        newsletterEditionRepository.save(edition);

        log.info("[WeeklyBlogDigestScheduler] Successfully dispatched Issue #{} to {} recipients. History updated.",
                digest.editionNumber(), sentCount);
    }

    /**
     * On-demand manual test method to send this week's generated digest to a specific email.
     */
    public BlogDigestRotatorService.WeeklyDigestBundle sendTestDigest(String targetEmail, String targetName) {
        BlogDigestRotatorService.WeeklyDigestBundle digest = rotatorService.composeWeeklyDigest();

        brevoEmailService.sendWeeklyBlogDigestEmail(
                targetEmail,
                targetName != null ? targetName : "Tester",
                digest
        );

        return digest;
    }
}
