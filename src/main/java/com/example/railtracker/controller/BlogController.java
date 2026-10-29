package com.example.railtracker.controller;

import com.example.railtracker.dto.ApiResponse;
import com.example.railtracker.dto.BlogArticleDTO;
import com.example.railtracker.dto.BlogTipDTO;
import com.example.railtracker.service.BlogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/blogs")
public class BlogController {

    private final BlogService blogService;

    public BlogController(BlogService blogService) {
        this.blogService = blogService;
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
}
