package com.example.railtracker.service.impl;

import com.example.railtracker.dto.SearchHistoryDto;
import com.example.railtracker.dto.SearchHistoryRequest;
import com.example.railtracker.entity.SearchHistory;
import com.example.railtracker.entity.User;
import com.example.railtracker.exception.ResourceNotFoundException;
import com.example.railtracker.repository.SearchHistoryRepository;
import com.example.railtracker.repository.UserRepository;
import com.example.railtracker.service.SearchHistoryService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class SearchHistoryServiceImpl implements SearchHistoryService {

    private final SearchHistoryRepository searchHistoryRepository;
    private final UserRepository userRepository;

    public SearchHistoryServiceImpl(SearchHistoryRepository searchHistoryRepository, UserRepository userRepository) {
        this.searchHistoryRepository = searchHistoryRepository;
        this.userRepository = userRepository;
    }

    @Override
    public SearchHistoryDto recordSearch(String username, SearchHistoryRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        String searchType = request.getSearchType().trim().toUpperCase();
        String itemCode = request.getItemCode().trim().toUpperCase();
        String itemName = request.getItemName().trim();
        String subtitle = request.getSubtitle() != null ? request.getSubtitle().trim() : null;

        Optional<SearchHistory> existing = searchHistoryRepository.findByUserAndSearchTypeAndItemCode(user, searchType, itemCode);

        SearchHistory entity;
        if (existing.isPresent()) {
            entity = existing.get();
            entity.setItemName(itemName);
            entity.setSubtitle(subtitle);
            entity.setSearchedAt(LocalDateTime.now());
        } else {
            entity = new SearchHistory(user, searchType, itemCode, itemName, subtitle);
            entity.setSearchedAt(LocalDateTime.now());
        }

        SearchHistory saved = searchHistoryRepository.save(entity);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SearchHistoryDto> getSearchHistory(String username, String searchType, int limit) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        int safeLimit = Math.min(Math.max(limit, 1), 100);
        PageRequest pageRequest = PageRequest.of(0, safeLimit);

        List<SearchHistory> results;
        if (searchType == null || searchType.trim().isEmpty() || searchType.equalsIgnoreCase("ALL")) {
            results = searchHistoryRepository.findByUserOrderBySearchedAtDesc(user, pageRequest);
        } else {
            results = searchHistoryRepository.findByUserAndSearchTypeOrderBySearchedAtDesc(user, searchType.trim().toUpperCase(), pageRequest);
        }

        return results.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    public void removeSearchItem(String username, Long id) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        searchHistoryRepository.deleteByUserAndId(user, id);
    }

    @Override
    public void clearSearchHistory(String username, String searchType) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        if (searchType == null || searchType.trim().isEmpty() || searchType.equalsIgnoreCase("ALL")) {
            searchHistoryRepository.deleteByUser(user);
        } else {
            searchHistoryRepository.deleteByUserAndSearchType(user, searchType.trim().toUpperCase());
        }
    }

    private SearchHistoryDto mapToDto(SearchHistory entity) {
        return new SearchHistoryDto(
                entity.getId(),
                entity.getSearchType(),
                entity.getItemCode(),
                entity.getItemName(),
                entity.getSubtitle(),
                entity.getSearchedAt()
        );
    }
}
