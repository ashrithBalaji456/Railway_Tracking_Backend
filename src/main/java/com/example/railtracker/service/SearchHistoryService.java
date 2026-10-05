package com.example.railtracker.service;

import com.example.railtracker.dto.SearchHistoryDto;
import com.example.railtracker.dto.SearchHistoryRequest;
import java.util.List;

public interface SearchHistoryService {

    SearchHistoryDto recordSearch(String username, SearchHistoryRequest request);

    List<SearchHistoryDto> getSearchHistory(String username, String searchType, int limit);

    void removeSearchItem(String username, Long id);

    void clearSearchHistory(String username, String searchType);
}
