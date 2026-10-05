package com.example.railtracker.controller;

import com.example.railtracker.dto.ApiResponse;
import com.example.railtracker.dto.SearchHistoryDto;
import com.example.railtracker.dto.SearchHistoryRequest;
import com.example.railtracker.service.SearchHistoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/history")
public class SearchHistoryController {

    private final SearchHistoryService searchHistoryService;

    public SearchHistoryController(SearchHistoryService searchHistoryService) {
        this.searchHistoryService = searchHistoryService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SearchHistoryDto>> recordSearch(
            Principal principal,
            @Valid @RequestBody SearchHistoryRequest request) {
        SearchHistoryDto dto = searchHistoryService.recordSearch(principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SearchHistoryDto>>> getSearchHistory(
            Principal principal,
            @RequestParam(value = "type", defaultValue = "ALL") String type,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        List<SearchHistoryDto> history = searchHistoryService.getSearchHistory(principal.getName(), type, limit);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> removeSearchItem(
            Principal principal,
            @PathVariable Long id) {
        searchHistoryService.removeSearchItem(principal.getName(), id);
        return ResponseEntity.ok(ApiResponse.success("Search history item removed successfully"));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<String>> clearSearchHistory(
            Principal principal,
            @RequestParam(value = "type", defaultValue = "ALL") String type) {
        searchHistoryService.clearSearchHistory(principal.getName(), type);
        return ResponseEntity.ok(ApiResponse.success("Search history cleared successfully"));
    }
}
