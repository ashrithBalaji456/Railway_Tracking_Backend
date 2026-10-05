package com.example.railtracker.service;

import com.example.railtracker.dto.FavoriteDto;
import com.example.railtracker.dto.FavoriteRequest;
import java.util.List;

public interface FavoriteService {

    FavoriteDto addFavorite(String username, String favoriteType, FavoriteRequest request);

    List<FavoriteDto> getFavorites(String username);

    void removeFavorite(String username, Long favoriteId);
}
