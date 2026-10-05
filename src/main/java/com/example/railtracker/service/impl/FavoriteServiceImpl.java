package com.example.railtracker.service.impl;

import com.example.railtracker.dto.FavoriteDto;
import com.example.railtracker.dto.FavoriteRequest;
import com.example.railtracker.entity.Favorite;
import com.example.railtracker.entity.User;
import com.example.railtracker.exception.BadRequestException;
import com.example.railtracker.exception.ResourceNotFoundException;
import com.example.railtracker.repository.FavoriteRepository;
import com.example.railtracker.repository.UserRepository;
import com.example.railtracker.service.FavoriteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FavoriteServiceImpl implements FavoriteService {

    private static final Logger logger = LoggerFactory.getLogger(FavoriteServiceImpl.class);

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;

    public FavoriteServiceImpl(FavoriteRepository favoriteRepository, UserRepository userRepository) {
        this.favoriteRepository = favoriteRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public FavoriteDto addFavorite(String username, String favoriteType, FavoriteRequest request) {
        logger.debug("User '{}' adding favorite: [{}] {}", username, favoriteType, request.itemCode());

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // Check for duplicates
        if (favoriteRepository.findByUserUsernameAndFavoriteTypeAndItemCode(username, favoriteType, request.itemCode()).isPresent()) {
            throw new BadRequestException("Item already bookmarked as favorite");
        }

        Favorite favorite = new Favorite(user, favoriteType, request.itemCode(), request.itemName());
        Favorite saved = favoriteRepository.save(favorite);

        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FavoriteDto> getFavorites(String username) {
        logger.debug("Retrieving favorites for user '{}'", username);
        List<Favorite> favorites = favoriteRepository.findByUserUsername(username);
        return favorites.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void removeFavorite(String username, Long favoriteId) {
        logger.debug("User '{}' removing favorite: {}", username, favoriteId);

        Favorite favorite = favoriteRepository.findById(favoriteId)
                .orElseThrow(() -> new ResourceNotFoundException("Favorite item not found"));

        // Security check: ensure favorite belongs to requesting user
        if (!favorite.getUser().getUsername().equalsIgnoreCase(username)) {
            throw new BadRequestException("You do not have permission to delete this favorite item");
        }

        favoriteRepository.delete(favorite);
    }

    private FavoriteDto mapToDto(Favorite favorite) {
        return new FavoriteDto(
                favorite.getId(),
                favorite.getFavoriteType(),
                favorite.getItemCode(),
                favorite.getItemName()
        );
    }
}
