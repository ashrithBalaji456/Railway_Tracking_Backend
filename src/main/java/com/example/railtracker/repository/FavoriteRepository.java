package com.example.railtracker.repository;

import com.example.railtracker.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    
    List<Favorite> findByUserUsername(String username);
    
    Optional<Favorite> findByUserUsernameAndFavoriteTypeAndItemCode(String username, String favoriteType, String itemCode);
}
