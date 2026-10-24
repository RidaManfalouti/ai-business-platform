package com.aibi.auth_service.dto;

public record AuthResponse(
    String token,
    String tokenType,
    Long userId,
    String username,
    String email,
    String role
) {
    public AuthResponse(String token, String tokenType, Long userId, String username, String email) {
        this(token, tokenType, userId, username, email, "USER");
    }
}
