package com.example.railtracker.service;

import com.example.railtracker.dto.BlogArticleDTO;
import com.example.railtracker.dto.BlogTipDTO;
import java.util.List;
import java.util.Optional;

public interface BlogService {
    List<BlogArticleDTO> getAllArticles(String category, String query);
    Optional<BlogArticleDTO> getArticleBySlug(String slug);
    List<BlogArticleDTO> getFeaturedArticles();
    List<String> getCategories();
    List<BlogTipDTO> getTipsForArticle(String slug);
    BlogTipDTO addTip(String slug, BlogTipDTO tipDTO);
    BlogTipDTO likeTip(String slug, String tipId);
}

