package com.aayush.backend.dto;

import com.aayush.backend.document.AccountStatus;
import com.aayush.backend.document.User;
import com.aayush.backend.document.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for User information
 * 
 * Security Notes:
 * - Does NOT expose passwordHash
 * - Does NOT expose internal MongoDB fields
 * - Only exposes safe, user-facing information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private String id;
    private String fullName;
    private String email;
    private String mobile;
    private UserRole role;
    private AccountStatus status;
    private boolean emailVerified;
    private boolean mobileVerified;
    private String preferredLanguage;
    private LocalDateTime createdAt;

    /**
     * Create UserResponse from User entity
     * Excludes sensitive fields like passwordHash
     */
    public static UserResponse fromUser(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .mobile(user.getMobile())
                .role(user.getRole())
                .status(user.getStatus())
                .emailVerified(user.isEmailVerified())
                .mobileVerified(user.isMobileVerified())
                .preferredLanguage(user.getPreferredLanguage())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
