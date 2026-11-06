package com.aayush.backend.service;

import com.aayush.backend.document.AccountStatus;
import com.aayush.backend.document.RefreshToken;
import com.aayush.backend.document.User;
import com.aayush.backend.document.UserRole;
import com.aayush.backend.dto.AuthResponse;
import com.aayush.backend.dto.LoginRequest;
import com.aayush.backend.dto.LogoutRequest;
import com.aayush.backend.dto.RefreshTokenRequest;
import com.aayush.backend.dto.RegisterRequest;
import com.aayush.backend.dto.TokenResponse;
import com.aayush.backend.dto.UserResponse;
import com.aayush.backend.exception.AuthenticationException;
import com.aayush.backend.exception.DuplicateUserException;
import com.aayush.backend.exception.ResourceNotFoundException;
import com.aayush.backend.repository.RefreshTokenRepository;
import com.aayush.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Authentication Service
 * 
 * Handles user registration, login, refresh token rotation, logout, and contact normalization
 * 
 * Security Features:
 * - Email and mobile normalization for consistent lookups
 * - Duplicate contact detection
 * - Password hashing integration (BCrypt)
 * - Role enforcement (SENIOR/CAREGIVER only for public registration)
 * - Account status management
 * - No passwordHash exposure in responses
 * - Opaque refresh tokens with SHA-256 storage
 * - Refresh token rotation on each use
 * - Token family tracking for reuse detection
 * - Generic authentication errors (no information disclosure)
 * 
 * Important Notes:
 * - Registration does NOT issue tokens (login is a separate operation)
 * - ADMIN role cannot be assigned through public registration
 * - Email/mobile verification flags remain false until explicitly verified
 * - MongoDB unique indexes provide final duplicate protection
 * - Raw refresh tokens are never persisted (only SHA-256 hash)
 * - Access tokens are stateless JWTs (cannot be revoked before expiry)
 * - Refresh tokens enable session revocation via logout
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordService passwordService;
    private final JwtService jwtService;
    private final MongoTemplate mongoTemplate;

    // Refresh token configuration
    private static final int REFRESH_TOKEN_BYTES = 32;  // 256 bits
    private static final long REFRESH_TOKEN_VALIDITY_DAYS = 30;  // 30 days
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

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
     * Authenticate user and issue tokens
     * 
     * Process:
     * 1. Normalize identifier (email or mobile)
     * 2. Find user by normalized identifier
     * 3. Verify password
     * 4. Check account status
     * 5. Generate JWT access token
     * 6. Generate opaque refresh token
     * 7. Store refresh token hash in database
     * 8. Return AuthResponse with tokens and user info
     * 
     * Security:
     * - Generic error messages (no information disclosure)
     * - Password verified using BCrypt
     * - SUSPENDED and DELETED accounts rejected
     * - Raw refresh token never stored
     * - Only SHA-256 hash persisted
     * 
     * @param request Login request with identifier and password
     * @return AuthResponse with access token, refresh token, and user info
     * @throws AuthenticationException if credentials are invalid or account is not ACTIVE
     */
    public AuthResponse login(LoginRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Login request cannot be null");
        }

        if (request.getIdentifier() == null || request.getIdentifier().isBlank()) {
            throw new IllegalArgumentException("Identifier cannot be null or blank");
        }

        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password cannot be null or blank");
        }

        log.info("Processing login attempt for identifier: {}", maskIdentifier(request.getIdentifier()));

        // Normalize identifier
        String normalizedEmail = normalizeEmail(request.getIdentifier());
        String normalizedMobile = normalizeMobile(request.getIdentifier());

        // Find user by email or mobile
        User user = findUserByIdentifier(normalizedEmail, normalizedMobile);

        // Verify password
        if (!passwordService.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Failed login attempt: invalid password for user ID: {}", user.getId());
            throw new AuthenticationException("Invalid credentials");
        }

        // Check account status
        if (user.getStatus() != AccountStatus.ACTIVE) {
            log.warn("Failed login attempt: account not active for user ID: {}, status: {}", 
                user.getId(), user.getStatus());
            throw new AuthenticationException("Account is not available");
        }

        // Generate JWT access token
        String accessToken = jwtService.generateAccessToken(user);

        // Generate opaque refresh token
        String refreshToken = generateOpaqueToken();
        String tokenHash = hashToken(refreshToken);

        // Create new token family
        String tokenFamilyId = UUID.randomUUID().toString();

        // Store refresh token (only hash)
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .tokenHash(tokenHash)
                .userId(user.getId())
                .tokenFamilyId(tokenFamilyId)
                .issuedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(REFRESH_TOKEN_VALIDITY_DAYS))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshTokenEntity);

        log.info("Successful login for user ID: {}", user.getId());

        // Return auth response (DO NOT log tokens)
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn((int) (jwtService.extractExpiration(accessToken).getTime() - System.currentTimeMillis()) / 1000)
                .user(UserResponse.fromUser(user))
                .build();
    }

    /**
     * Rotate refresh token and issue new access token
     * 
     * Process:
     * 1. Hash the provided refresh token
     * 2. Atomically consume the token (mark as revoked if not already revoked and not expired)
     * 3. If consumption fails (already revoked), detect token reuse and revoke entire family
     * 4. Check user still exists and is ACTIVE
     * 5. Generate new refresh token
     * 6. Store new token hash with same family ID
     * 7. Update old token with replacement link
     * 8. Generate new access token
     * 9. Return TokenResponse
     * 
     * Security:
     * - Atomic token consumption prevents concurrent reuse
     * - Token reuse detection: revokes entire family if revoked token is reused
     * - Only SHA-256 hashes stored
     * - Generic error messages
     * 
     * Concurrency:
     * - Uses MongoDB findAndModify for atomic token consumption
     * - Only ONE concurrent request can successfully consume a token
     * - Losing requests trigger token reuse detection and family revocation
     * 
     * @param request Refresh token request
     * @return TokenResponse with new access and refresh tokens
     * @throws AuthenticationException if token is invalid, expired, revoked, or reused
     */
    public TokenResponse refresh(RefreshTokenRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Refresh token request cannot be null");
        }

        if (request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
            throw new IllegalArgumentException("Refresh token cannot be null or blank");
        }

        log.info("Processing token refresh request");

        // Hash the provided token
        String tokenHash = hashToken(request.getRefreshToken());

        // Atomically consume the token (find and mark as revoked in single operation)
        RefreshToken consumedToken = atomicallyConsumeToken(tokenHash);

        // If null, token was already consumed (reuse detected) or doesn't exist
        if (consumedToken == null) {
            log.warn("Token consumption failed: checking for reuse scenario");
            
            // Check if token exists but is already revoked (reuse attempt)
            RefreshToken existingToken = refreshTokenRepository.findByTokenHash(tokenHash).orElse(null);
            
            if (existingToken != null && existingToken.isRevoked()) {
                String reason = existingToken.getRevocationReason();
                
                // Handle different revocation reasons
                if ("consumed".equals(reason) || "token_rotation".equals(reason)) {
                    // Token was consumed/rotated
                    // This could be:
                    // 1. Concurrent refresh attempt (two requests at the same time)
                    // 2. Genuine reuse of an old rotated token
                    
                    // The key distinction: if replacedByTokenId is set, rotation completed
                    if (existingToken.getReplacedByTokenId() != null) {
                        // Rotation completed - this is genuine reuse of a rotated token
                        // This means the client is trying to use an old token
                        log.warn("Token reuse detected for token family: {} (previously rotated)", 
                            existingToken.getTokenFamilyId());
                        revokeTokenFamily(existingToken.getTokenFamilyId(), "suspicious_reuse");
                        throw new AuthenticationException("Invalid refresh token");
                    }
                    
                    // If replacedByTokenId is NOT set:
                    // - Could be concurrent refresh attempt (atomic consumption won, update pending)
                    // - Could be rotation in progress (rare timing issue)
                    // Either way, deny the request WITHOUT revoking the family
                    // The legitimate request already succeeded
                    log.warn("Concurrent token consumption detected for family: {} - denying duplicate refresh", 
                        existingToken.getTokenFamilyId());
                    throw new AuthenticationException("Invalid refresh token");
                }
                
                // Other revocation reasons (logout, suspicious_reuse, family_revoked)
                // These are always suspicious if reused
                log.warn("Attempt to reuse revoked token (reason: {}) for family: {}", 
                    reason, existingToken.getTokenFamilyId());
                revokeTokenFamily(existingToken.getTokenFamilyId(), "suspicious_reuse");
                throw new AuthenticationException("Invalid refresh token");
            }
            
            // Token doesn't exist at all
            log.warn("Token refresh failed: unknown token");
            throw new AuthenticationException("Invalid refresh token");
        }

        // Find user
        User user = userRepository.findById(consumedToken.getUserId())
                .orElseThrow(() -> {
                    log.warn("Token refresh failed: user not found");
                    return new AuthenticationException("Invalid refresh token");
                });

        // Check user account status
        if (user.getStatus() != AccountStatus.ACTIVE) {
            log.warn("Token refresh failed: account not active for user ID: {}", user.getId());
            throw new AuthenticationException("Account is not available");
        }

        // Generate new refresh token
        String newRefreshToken = generateOpaqueToken();
        String newTokenHash = hashToken(newRefreshToken);

        // Create replacement token (same family)
        RefreshToken newTokenEntity = RefreshToken.builder()
                .tokenHash(newTokenHash)
                .userId(user.getId())
                .tokenFamilyId(consumedToken.getTokenFamilyId())  // Preserve family
                .issuedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(REFRESH_TOKEN_VALIDITY_DAYS))
                .revoked(false)
                .build();

        // Save new token
        RefreshToken savedNewToken = refreshTokenRepository.save(newTokenEntity);

        // Update old token with replacement link and revocation reason atomically
        // This MUST happen before returning to minimize the window where the token
        // appears as "consumed" without a replacement link
        try {
            consumedToken.setReplacedByTokenId(savedNewToken.getId());
            consumedToken.setRevocationReason("token_rotation");
            refreshTokenRepository.save(consumedToken);
        } catch (Exception e) {
            // If this fails, the new token is orphaned but the old token is still revoked
            // This is a rare case but we should not fail the refresh
            log.error("CRITICAL: Failed to update replacement link for consumed token {}: {}", 
                consumedToken.getId(), e.getMessage());
        }

        // Generate new access token
        String newAccessToken = jwtService.generateAccessToken(user);

        log.info("Successfully rotated refresh token for user ID: {}", user.getId());

        // Return token response (DO NOT log tokens)
        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn((int) (jwtService.extractExpiration(newAccessToken).getTime() - System.currentTimeMillis()) / 1000)
                .build();
    }

    /**
     * Atomically consume a refresh token (mark as revoked)
     * 
     * Uses MongoDB findAndModify to ensure only ONE request can successfully consume a token.
     * This prevents race conditions where two concurrent requests try to use the same token.
     * 
     * Query criteria (ALL must match):
     * - tokenHash matches
     * - revoked = false (not already consumed)
     * - expiresAt > now (not expired)
     * 
     * Update operation:
     * - Set revoked = true
     * - Set revokedAt = now
     * - Set revocationReason = "consumed"
     * 
     * Note: The reason is "consumed" (not "token_rotation") because rotation is completed
     * only after the replacement token is successfully created. If rotation fails after
     * consumption, the token remains consumed with this reason.
     * 
     * @param tokenHash SHA-256 hash of the refresh token
     * @return RefreshToken if successfully consumed, null if already consumed or expired
     */
    private RefreshToken atomicallyConsumeToken(String tokenHash) {
        LocalDateTime now = LocalDateTime.now();
        
        // Build query: find non-revoked, non-expired token with matching hash
        Query query = new Query();
        query.addCriteria(Criteria.where("tokenHash").is(tokenHash)
                .and("revoked").is(false)
                .and("expiresAt").gt(now));
        
        // Build update: mark as consumed
        Update update = new Update()
                .set("revoked", true)
                .set("revokedAt", now)
                .set("revocationReason", "consumed");
        
        // Execute atomic findAndModify
        // Returns the document BEFORE the update (or null if no match)
        RefreshToken consumedToken = mongoTemplate.findAndModify(
                query,
                update,
                RefreshToken.class
        );
        
        if (consumedToken != null) {
            log.debug("Successfully consumed token atomically");
            // Update the returned object to reflect the changes (since findAndModify returns pre-update state)
            consumedToken.setRevoked(true);
            consumedToken.setRevokedAt(now);
            consumedToken.setRevocationReason("consumed");
        }
        
        return consumedToken;
    }

    /**
     * Revoke refresh token (logout)
     * 
     * Process:
     * 1. Hash the provided refresh token
     * 2. Find stored token record
     * 3. Revoke token if active
     * 4. Set revocation timestamp and reason
     * 
     * Security:
     * - Handles unknown tokens safely (no information disclosure)
     * - Handles already-revoked tokens safely
     * - Does NOT physically delete token (audit trail)
     * - Generic success response regardless of token state
     * 
     * Limitation:
     * - Access tokens remain valid until expiry (cannot revoke JWT)
     * - Client should discard access token immediately
     * - Short access token lifetime mitigates this limitation
     * 
     * @param request Logout request with refresh token
     */
    public void logout(LogoutRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Logout request cannot be null");
        }

        if (request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
            throw new IllegalArgumentException("Refresh token cannot be null or blank");
        }

        log.info("Processing logout request");

        // Hash the provided token
        String tokenHash = hashToken(request.getRefreshToken());

        // Find stored token
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresentOrElse(
            token -> {
                if (!token.isRevoked()) {
                    // Revoke token
                    token.setRevoked(true);
                    token.setRevokedAt(LocalDateTime.now());
                    token.setRevocationReason("logout");
                    refreshTokenRepository.save(token);
                    log.info("Successfully revoked refresh token for user ID: {}", token.getUserId());
                } else {
                    log.info("Logout attempted with already-revoked token");
                }
            },
            () -> log.info("Logout attempted with unknown token")
        );

        // Always succeed (no information disclosure)
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

    /**
     * Find user by email or mobile identifier
     * 
     * @param normalizedEmail Normalized email
     * @param normalizedMobile Normalized mobile
     * @return User if found
     * @throws AuthenticationException with generic message if user not found
     */
    private User findUserByIdentifier(String normalizedEmail, String normalizedMobile) {
        User user = null;

        // Try email first
        if (normalizedEmail != null) {
            user = userRepository.findByEmail(normalizedEmail).orElse(null);
        }

        // Try mobile if email didn't match
        if (user == null && normalizedMobile != null) {
            user = userRepository.findByMobile(normalizedMobile).orElse(null);
        }

        // Generic error message (don't reveal if email/mobile exists)
        if (user == null) {
            log.warn("Login attempt with unknown identifier");
            throw new AuthenticationException("Invalid credentials");
        }

        return user;
    }

    /**
     * Generate cryptographically secure opaque refresh token
     * 
     * Uses SecureRandom to generate 256 bits of entropy
     * Encoded as Base64 URL-safe string
     * 
     * @return Base64-encoded random token
     */
    private String generateOpaqueToken() {
        byte[] randomBytes = new byte[REFRESH_TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Hash token using SHA-256
     * 
     * @param token Raw token string
     * @return Hex-encoded SHA-256 hash
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 should always be available
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Revoke all tokens in a token family
     * 
     * Used when token reuse is detected (security incident)
     * 
     * @param tokenFamilyId Token family ID
     * @param reason Revocation reason
     */
    private void revokeTokenFamily(String tokenFamilyId, String reason) {
        List<RefreshToken> familyTokens = refreshTokenRepository.findByTokenFamilyIdAndRevoked(
            tokenFamilyId, false
        );

        LocalDateTime now = LocalDateTime.now();
        for (RefreshToken token : familyTokens) {
            token.setRevoked(true);
            token.setRevokedAt(now);
            token.setRevocationReason(reason);
            refreshTokenRepository.save(token);
        }

        log.warn("Revoked {} tokens in family {} due to: {}", 
            familyTokens.size(), tokenFamilyId, reason);
    }

    /**
     * Mask identifier for logging (prevent PII exposure)
     * 
     * @param identifier Email or mobile
     * @return Masked identifier
     */
    private String maskIdentifier(String identifier) {
        if (identifier == null || identifier.length() < 4) {
            return "***";
        }
        return identifier.substring(0, 3) + "***";
    }
}
