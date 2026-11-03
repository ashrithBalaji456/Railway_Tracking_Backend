package com.example.railtracker.controller;

import com.example.railtracker.dto.ApiResponse;
import com.example.railtracker.dto.BlogArticleDTO;
import com.example.railtracker.dto.BlogTipDTO;
import com.example.railtracker.entity.User;
import com.example.railtracker.repository.UserRepository;
import com.example.railtracker.service.BlogDigestRotatorService;
import com.example.railtracker.service.BlogService;
import com.example.railtracker.service.WeeklyBlogDigestScheduler;
import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/blogs")
public class BlogController {

    private final BlogService blogService;
    private final WeeklyBlogDigestScheduler weeklyBlogDigestScheduler;
    private final UserRepository userRepository;

    public BlogController(
            BlogService blogService,
            WeeklyBlogDigestScheduler weeklyBlogDigestScheduler,
            UserRepository userRepository) {
        this.blogService = blogService;
        this.weeklyBlogDigestScheduler = weeklyBlogDigestScheduler;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BlogArticleDTO>>> getArticles(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String q) {
        List<BlogArticleDTO> list = blogService.getAllArticles(category, q);
        return ResponseEntity.ok(ApiResponse.success("Articles retrieved successfully", list));
    }

    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<List<BlogArticleDTO>>> getFeaturedArticles() {
        List<BlogArticleDTO> featured = blogService.getFeaturedArticles();
        return ResponseEntity.ok(ApiResponse.success("Featured articles retrieved successfully", featured));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<String>>> getCategories() {
        List<String> categories = blogService.getCategories();
        return ResponseEntity.ok(ApiResponse.success("Categories retrieved successfully", categories));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<BlogArticleDTO>> getArticleBySlug(@PathVariable String slug) {
        return blogService.getArticleBySlug(slug)
            .map(article -> ResponseEntity.ok(ApiResponse.success("Article retrieved successfully", article)))
            .orElseGet(() -> ResponseEntity.status(404).body(ApiResponse.error("Article not found with slug: " + slug)));
    }

    @GetMapping("/{slug}/tips")
    public ResponseEntity<ApiResponse<List<BlogTipDTO>>> getArticleTips(@PathVariable String slug) {
        List<BlogTipDTO> tips = blogService.getTipsForArticle(slug);
        return ResponseEntity.ok(ApiResponse.success("Passenger tips retrieved successfully", tips));
    }

    @PostMapping("/{slug}/tips")
    public ResponseEntity<ApiResponse<BlogTipDTO>> addArticleTip(
            @PathVariable String slug,
            @RequestBody BlogTipDTO tipDTO) {
        if (tipDTO.tipText() == null || tipDTO.tipText().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Tip text cannot be empty"));
        }
        BlogTipDTO created = blogService.addTip(slug, tipDTO);
        return ResponseEntity.ok(ApiResponse.success("Passenger tip shared successfully", created));
    }

    @PostMapping("/{slug}/tips/{tipId}/like")
    public ResponseEntity<ApiResponse<BlogTipDTO>> likeArticleTip(
            @PathVariable String slug,
            @PathVariable String tipId) {
        BlogTipDTO updated = blogService.likeTip(slug, tipId);
        if (updated != null) {
            return ResponseEntity.ok(ApiResponse.success("Tip upvoted", updated));
        }
        return ResponseEntity.status(404).body(ApiResponse.error("Tip not found"));
    }

    @PostMapping("/newsletter/test")
    public ResponseEntity<ApiResponse<BlogDigestRotatorService.WeeklyDigestBundle>> testWeeklyNewsletter(
            @RequestParam(required = false) String email,
            Principal principal) {

        String targetEmail = email;
        String targetName = "Passenger";

        if ((targetEmail == null || targetEmail.isBlank()) && principal != null) {
            targetEmail = userRepository.findByUsername(principal.getName())
                    .map(User::getEmail)
                    .orElse(null);
            targetName = principal.getName();
        }

        if (targetEmail == null || targetEmail.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Target email is required to test the newsletter"));
        }

        BlogDigestRotatorService.WeeklyDigestBundle bundle = weeklyBlogDigestScheduler.sendTestDigest(targetEmail.trim(), targetName);
        return ResponseEntity.ok(ApiResponse.success("Issue #" + bundle.editionNumber() + " dispatched to " + targetEmail, bundle));
    }
}
