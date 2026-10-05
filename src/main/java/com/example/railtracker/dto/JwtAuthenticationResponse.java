package com.example.railtracker.dto;

import java.util.List;

public record JwtAuthenticationResponse(
    String token,
    String tokenType,
    Long id,
    String username,
    String email,
    List<String> roles
) {
    public JwtAuthenticationResponse(String token, Long id, String username, String email, List<String> roles) {
        this(token, "Bearer", id, username, email, roles);
    }
}
