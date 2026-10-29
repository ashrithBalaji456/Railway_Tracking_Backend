package com.example.railtracker.service;

import com.example.railtracker.dto.BlogArticleDTO;
import java.util.List;
import java.util.Optional;

public interface BlogService {
    List<BlogArticleDTO> getAllArticles(String category, String query);
    Optional<BlogArticleDTO> getArticleBySlug(String slug);
    List<BlogArticleDTO> getFeaturedArticles();
    List<String> getCategories();
}
