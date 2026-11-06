package com.aayush.backend.controller;

import com.aayush.backend.dto.AuthResponse;
import com.aayush.backend.dto.LoginRequest;
import com.aayush.backend.dto.LogoutRequest;
import com.aayush.backend.dto.RefreshTokenRequest;
import com.aayush.backend.dto.RegisterRequest;
import com.aayush.backend.dto.TokenResponse;
import com.aayush.backend.dto.UserResponse;
import com.aayush.backend.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication REST Controller
 * 
 * Provides HTTP endpoints for user authentication operations:
 * - Registration (POST /api/auth/register)
 * - Login (POST /api/auth/login)
 * - Token refresh (POST /api/auth/refresh)
 * - Logout (POST /api/auth/logout)
 * 
 * Security Notes:
 * - All request bodies validated using Jakarta Bean Validation
 * - Errors handled by GlobalExceptionHandler
 * - Generic authentication error messages (no information disclosure)
 * - No sensitive data exposed in responses (passwords, token hashes)
 * - Atomic refresh token rotation preserves concurrency safety
 * 
 * HTTP Status Codes:
 * - 201 Created: Successful registration
 * - 200 OK: Successful login/refresh
 * - 204 No Content: Successful logout
 * - 400 Bad Request: Validation errors
 * - 401 Unauthorized: Authentication failures
 * - 409 Conflict: Duplicate registration
 * - 500 Internal Server Error: Unexpected errors
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    /**
     * Register a new user account
     * 
     * Endpoint: POST /api/auth/register
     * 
     * Request Body: RegisterRequest
     * - fullName (required, 2-100 characters)
     * - email (optional, valid email format)
     * - mobile (optional, valid mobile format)
     * - password (required, 8-100 characters with complexity rules)
     * - role (required, SENIOR or CAREGIVER only)
     * - preferredLanguage (optional, en/hi/mr)
     * 
     * Response: UserResponse (201 Created)
     * - id, fullName, email, mobile, role, status, verification flags
     * - Does NOT contain passwordHash or tokens
     * 
     * Security:
     * - At least one contact method (email or mobile) required
     * - ADMIN role cannot be assigned through public registration
     * - Password hashed before storage
     * - Duplicate email/mobile rejected with 409 Conflict
     * 
     * @param request Registration request
     * @return UserResponse with HTTP 201 Created
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Received registration request for: {}", request.getFullName());
        
        UserResponse response = authenticationService.register(request);
        
        log.info("Successfully registered user with ID: {}", response.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Authenticate user and issue tokens
     * 
     * Endpoint: POST /api/auth/login
     * 
     * Request Body: LoginRequest
     * - identifier (required, email or mobile number)
     * - password (required)
     * 
     * Response: AuthResponse (200 OK)
     * - accessToken: JWT access token (15 minutes validity)
     * - refreshToken: Opaque refresh token (30 days validity)
     * - tokenType: "Bearer"
     * - expiresIn: Access token expiry in seconds
     * - user: UserResponse (safe user info)
     * 
     * Security:
     * - Identifier normalized (email lowercase, mobile formatted)
     * - Password verified using BCrypt
     * - Generic error message "Invalid credentials" (no information disclosure)
     * - SUSPENDED and DELETED accounts rejected
     * - Raw refresh token returned once, never stored
     * - Only SHA-256 hash persisted
     * 
     * @param request Login request
     * @return AuthResponse with HTTP 200 OK
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Received login request");
        
        AuthResponse response = authenticationService.login(request);
        
        log.info("Successfully authenticated user");
        return ResponseEntity.ok(response);
    }

    /**
     * Rotate refresh token and issue new access token
     * 
     * Endpoint: POST /api/auth/refresh
     * 
     * Request Body: RefreshTokenRequest
     * - refreshToken (required)
     * 
     * Response: TokenResponse (200 OK)
     * - accessToken: New JWT access token (15 minutes validity)
     * - refreshToken: New opaque refresh token (30 days validity)
     * - tokenType: "Bearer"
     * - expiresIn: Access token expiry in seconds
     * 
     * Security:
     * - Atomic token consumption using MongoDB findAndModify
     * - Only ONE concurrent request can successfully use a token
     * - Old token revoked (marked with reason "consumed" or "token_rotation")
     * - Token reuse detection: revokes entire token family
     * - Generic error message "Invalid refresh token"
     * - Expired tokens rejected
     * - User must be ACTIVE
     * 
     * Concurrency Safety:
     * - Multiple concurrent requests with same token: only first succeeds
     * - Losing requests trigger reuse detection if token already rotated
     * - No race conditions, no duplicate token chains
     * 
     * @param request Refresh token request
     * @return TokenResponse with HTTP 200 OK
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Received token refresh request");
        
        TokenResponse response = authenticationService.refresh(request);
        
        log.info("Successfully refreshed token");
        return ResponseEntity.ok(response);
    }

    /**
     * Revoke refresh token (logout)
     * 
     * Endpoint: POST /api/auth/logout
     * 
     * Request Body: LogoutRequest
     * - refreshToken (required)
     * 
     * Response: HTTP 204 No Content (empty body)
     * 
     * Security:
     * - Refresh token revoked with reason "logout"
     * - Token preserved for audit trail (not physically deleted)
     * - Unknown tokens handled gracefully (no error)
     * - Already-revoked tokens handled gracefully (no error)
     * - Always returns 204 (no information disclosure)
     * 
     * Limitation:
     * - Access token (JWT) remains valid until expiry (cannot be revoked)
     * - Client must discard access token immediately
     * - Short access token lifetime (15 minutes) mitigates this
     * 
     * @param request Logout request
     * @return HTTP 204 No Content
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        log.info("Received logout request");
        
        authenticationService.logout(request);
        
        log.info("Successfully processed logout");
        return ResponseEntity.noContent().build();
    }
}
