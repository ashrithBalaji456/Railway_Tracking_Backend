package com.example.railtracker.repository;

import com.example.railtracker.entity.NewsletterEdition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NewsletterEditionRepository extends JpaRepository<NewsletterEdition, Long> {

    Optional<NewsletterEdition> findTopByOrderByEditionNumberDesc();

    List<NewsletterEdition> findTop15ByOrderBySentAtDesc();
}
