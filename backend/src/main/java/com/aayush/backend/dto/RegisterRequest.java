package com.aayush.backend.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for user registration
 * 
 * Security Notes:
 * - Does not include role field (assigned by business logic)
 * - Does not allow setting account status or verification flags
 * - Password will be hashed before storage (never stored as plaintext)
 * - At least one contact method (email or mobile) must be provided
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @Pattern(
        regexp = "^\\+?[0-9\\-\\s()]{10,20}$",
        message = "Invalid mobile number format"
    )
    private String mobile;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,100}$",
        message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character (@$!%*?&#)"
    )
    private String password;

    @NotBlank(message = "Role is required")
    @Pattern(
        regexp = "^(SENIOR|CAREGIVER)$",
        message = "Role must be SENIOR or CAREGIVER"
    )
    private String role;

    @Pattern(
        regexp = "^(en|hi|mr)$",
        message = "Preferred language must be en, hi, or mr"
    )
    private String preferredLanguage;

    /**
     * Custom validation to ensure at least one contact method is provided
     * This will be checked in the service layer
     */
    public boolean hasAtLeastOneContactMethod() {
        return (email != null && !email.trim().isEmpty()) ||
               (mobile != null && !mobile.trim().isEmpty());
    }
}
