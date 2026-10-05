package com.example.railtracker.repository;

import com.example.railtracker.entity.SearchHistory;
import com.example.railtracker.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {

    List<SearchHistory> findByUserOrderBySearchedAtDesc(User user, Pageable pageable);

    List<SearchHistory> findByUserAndSearchTypeOrderBySearchedAtDesc(User user, String searchType, Pageable pageable);

    Optional<SearchHistory> findByUserAndSearchTypeAndItemCode(User user, String searchType, String itemCode);

    void deleteByUserAndId(User user, Long id);

    void deleteByUser(User user);

    void deleteByUserAndSearchType(User user, String searchType);
}
