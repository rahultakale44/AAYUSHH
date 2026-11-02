package com.aayush.backend.service;

import com.aayush.backend.document.AccountStatus;
import com.aayush.backend.document.User;
import com.aayush.backend.document.UserRole;
import com.aayush.backend.dto.RegisterRequest;
import com.aayush.backend.dto.UserResponse;
import com.aayush.backend.exception.DuplicateUserException;
import com.aayush.backend.exception.ResourceNotFoundException;
import com.aayush.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Authentication Service
 * 
 * Handles user registration, user lookup, and contact normalization
 * 
 * Security Features:
 * - Email and mobile normalization for consistent lookups
 * - Duplicate contact detection
 * - Password hashing integration
 * - Role enforcement (SENIOR/CAREGIVER only for public registration)
 * - Account status management
 * - No passwordHash exposure in responses
 * 
 * Important Notes:
 * - Registration does NOT issue tokens (login is a separate operation)
 * - ADMIN role cannot be assigned through public registration
 * - Email/mobile verification flags remain false until explicitly verified
 * - MongoDB unique indexes provide final duplicate protection
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordService passwordService;

    /**
     * Register a new user account
     * 
     * Process:
     * 1. Validate request
     * 2. Normalize email and mobile
     * 3. Check for duplicates
     * 4. Hash password
     * 5. Create user with defaults
     * 6. Save to database
     * 7. Return safe UserResponse
     * 
     * @param request Registration request
     * @return UserResponse (no tokens)
     * @throws IllegalArgumentException if request is null or invalid
     * @throws DuplicateUserException if email or mobile already exists
     */
    public UserResponse register(RegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Registration request cannot be null");
        }

        // Validate at least one contact method provided
        if (!request.hasAtLeastOneContactMethod()) {
            throw new IllegalArgumentException("At least one contact method (email or mobile) must be provided");
        }

        log.info("Processing registration for user: {}", request.getFullName());

        // Normalize contact details
        String normalizedEmail = normalizeEmail(request.getEmail());
        String normalizedMobile = normalizeMobile(request.getMobile());

        // Check for duplicates (application-level check for clear error messages)
        checkDuplicateContact(normalizedEmail, normalizedMobile);

        // Hash password
        String passwordHash = passwordService.encodePassword(request.getPassword());

        // Parse and validate role
        UserRole role = parseAndValidateRole(request.getRole());

        // Build user document
        User user = User.builder()
                .fullName(request.getFullName())
                .email(normalizedEmail)
                .mobile(normalizedMobile)
                .passwordHash(passwordHash)
                .role(role)
                .status(AccountStatus.ACTIVE)  // New accounts are ACTIVE by default
                .emailVerified(false)           // Must be verified separately
                .mobileVerified(false)          // Must be verified separately
                .preferredLanguage(request.getPreferredLanguage() != null ? 
                    request.getPreferredLanguage() : "en")  // Default to English
                .build();

        // Save user (MongoDB unique indexes provide concurrency protection)
        try {
            User savedUser = userRepository.save(user);
            log.info("Successfully registered user: {} with ID: {}", savedUser.getFullName(), savedUser.getId());
            
            // Convert to safe response (excludes passwordHash)
            return UserResponse.fromUser(savedUser);
            
        } catch (DuplicateKeyException e) {
            // Handle race condition where duplicate was created between check and save
            log.warn("Duplicate key detected during save for email: {} or mobile: {}", 
                normalizedEmail, normalizedMobile);
            
            // Try to determine which field is duplicate
            if (normalizedEmail != null && userRepository.existsByEmail(normalizedEmail)) {
                throw new DuplicateUserException("Email address is already registered");
            }
            if (normalizedMobile != null && userRepository.existsByMobile(normalizedMobile)) {
                throw new DuplicateUserException("Mobile number is already registered");
            }
            
            // Generic fallback
            throw new DuplicateUserException("Email or mobile number is already registered");
        }
    }

    /**
     * Get user by ID
     * 
     * @param userId User ID
     * @return UserResponse
     * @throws ResourceNotFoundException if user not found
     */
    public UserResponse getUserById(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID cannot be null or blank");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        return UserResponse.fromUser(user);
    }

    /**
     * Normalize email address for consistent storage and lookup
     * 
     * Normalization:
     * - Trim whitespace
     * - Convert to lowercase using Locale.ROOT
     * - Return null if input is null or blank after trimming
     * 
     * @param email Raw email address
     * @return Normalized email or null
     */
    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        // Trim and lowercase
        String normalized = email.trim().toLowerCase(Locale.ROOT);

        // Return null if empty after normalization
        return normalized.isEmpty() ? null : normalized;
    }

    /**
     * Normalize mobile number for consistent storage and lookup
     * 
     * Normalization for Indian numbers:
     * - Remove all whitespace, hyphens, parentheses
     * - Extract digits only
     * - If 10 digits, prepend country code (91)
     * - If already has country code, preserve it
     * - Return null if input is null or blank after trimming
     * 
     * Format: "919876543210" (country code + 10 digits)
     * 
     * @param mobile Raw mobile number
     * @return Normalized mobile or null
     */
    private String normalizeMobile(String mobile) {
        if (mobile == null || mobile.isBlank()) {
            return null;
        }

        // Remove all non-digit characters
        String digitsOnly = mobile.replaceAll("[^0-9]", "");

        // Return null if no digits
        if (digitsOnly.isEmpty()) {
            return null;
        }

        // Handle Indian mobile numbers
        if (digitsOnly.length() == 10) {
            // 10 digits: assume Indian number, prepend country code
            return "91" + digitsOnly;
        } else if (digitsOnly.length() == 12 && digitsOnly.startsWith("91")) {
            // Already has Indian country code
            return digitsOnly;
        } else if (digitsOnly.length() > 10) {
            // International format: preserve as-is
            return digitsOnly;
        }

        // Invalid format: too short
        return null;
    }

    /**
     * Check for duplicate email or mobile
     * 
     * @param email Normalized email
     * @param mobile Normalized mobile
     * @throws DuplicateUserException if duplicate found
     */
    private void checkDuplicateContact(String email, String mobile) {
        if (email != null && userRepository.existsByEmail(email)) {
            log.warn("Registration attempt with existing email: {}", email);
            throw new DuplicateUserException("Email address is already registered");
        }

        if (mobile != null && userRepository.existsByMobile(mobile)) {
            log.warn("Registration attempt with existing mobile: {}", mobile);
            throw new DuplicateUserException("Mobile number is already registered");
        }
    }

    /**
     * Parse and validate role from string
     * 
     * Security: Only SENIOR and CAREGIVER are allowed for public registration
     * ADMIN role must be assigned through admin panel
     * 
     * @param roleString Role as string
     * @return UserRole enum
     * @throws IllegalArgumentException if role is invalid or ADMIN
     */
    private UserRole parseAndValidateRole(String roleString) {
        if (roleString == null || roleString.isBlank()) {
            throw new IllegalArgumentException("Role is required");
        }

        // Security: Check for ADMIN before parsing
        if ("ADMIN".equalsIgnoreCase(roleString.trim())) {
            log.warn("Attempt to register with ADMIN role rejected");
            throw new IllegalArgumentException("ADMIN role cannot be assigned through self-registration");
        }

        try {
            UserRole role = UserRole.valueOf(roleString.toUpperCase());
            return role;

        } catch (IllegalArgumentException e) {
            log.warn("Invalid role provided: {}", roleString);
            throw new IllegalArgumentException("Invalid role. Allowed values: SENIOR, CAREGIVER");
        }
    }
}
