package com.aayush.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for authentication (login)
 * 
 * Contains JWT access token, refresh token, and user information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;  // "Bearer"
    private int expiresIn;  // Access token expiry in seconds
    private UserResponse user;
}
