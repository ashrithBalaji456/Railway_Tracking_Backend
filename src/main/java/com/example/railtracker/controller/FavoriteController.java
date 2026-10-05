package com.example.railtracker.controller;

import com.example.railtracker.dto.ApiResponse;
import com.example.railtracker.dto.FavoriteDto;
import com.example.railtracker.dto.FavoriteRequest;
import com.example.railtracker.service.FavoriteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @PostMapping("/train")
    public ResponseEntity<ApiResponse<FavoriteDto>> addTrainFavorite(
            Principal principal,
            @RequestBody FavoriteRequest request) {
        FavoriteDto favorite = favoriteService.addFavorite(principal.getName(), "TRAIN", request);
        return ResponseEntity.ok(ApiResponse.success(favorite));
    }

    @PostMapping("/station")
    public ResponseEntity<ApiResponse<FavoriteDto>> addStationFavorite(
            Principal principal,
            @RequestBody FavoriteRequest request) {
        FavoriteDto favorite = favoriteService.addFavorite(principal.getName(), "STATION", request);
        return ResponseEntity.ok(ApiResponse.success(favorite));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FavoriteDto>>> getFavorites(Principal principal) {
        List<FavoriteDto> favorites = favoriteService.getFavorites(principal.getName());
        return ResponseEntity.ok(ApiResponse.success(favorites));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> removeFavorite(
            Principal principal,
            @PathVariable Long id) {
        favoriteService.removeFavorite(principal.getName(), id);
        return ResponseEntity.ok(ApiResponse.success("Favorite item removed successfully"));
    }
}
