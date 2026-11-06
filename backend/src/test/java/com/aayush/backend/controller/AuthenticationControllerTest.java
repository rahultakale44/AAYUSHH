package com.aayush.backend.controller;

import com.aayush.backend.document.AccountStatus;
import com.aayush.backend.document.User;
import com.aayush.backend.document.UserRole;
import com.aayush.backend.dto.*;
import com.aayush.backend.exception.AuthenticationException;
import com.aayush.backend.exception.DuplicateUserException;
import com.aayush.backend.service.AuthenticationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthenticationController
 * 
 * Tests HTTP endpoint behavior, request validation, and response formats.
 * Uses MockBean for AuthenticationService to isolate controller layer testing.
 * Real service logic is tested in AuthenticationServiceTest and AuthenticationServiceLoginTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationService authenticationService;

    // ===========================
    // Registration Tests
    // ===========================

    @Test
    void testRegister_Success() throws Exception {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("John Doe")
                .email("john@example.com")
                .mobile("9876543210")
                .password("SecurePass123!")
                .role("SENIOR")
                .preferredLanguage("en")
                .build();

        UserResponse mockResponse = UserResponse.builder()
                .id("user123")
                .fullName("John Doe")
                .email("john@example.com")
                .mobile("919876543210")
                .role(UserRole.SENIOR)
                .status(AccountStatus.ACTIVE)
                .emailVerified(false)
                .mobileVerified(false)
                .preferredLanguage("en")
                .createdAt(LocalDateTime.now())
                .build();

        when(authenticationService.register(any(RegisterRequest.class))).thenReturn(mockResponse);

        // When/Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("user123"))
                .andExpect(jsonPath("$.fullName").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.mobile").value("919876543210"))
                .andExpect(jsonPath("$.role").value("SENIOR"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.emailVerified").value(false))
                .andExpect(jsonPath("$.mobileVerified").value(false))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        verify(authenticationService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    void testRegister_ValidationError_MissingFullName() throws Exception {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .email("john@example.com")
                .password("SecurePass123!")
                .role("SENIOR")
                .build();

        // When/Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.validationErrors.fullName").exists());

        verify(authenticationService, never()).register(any());
    }

    @Test
    void testRegister_ValidationError_InvalidEmail() throws Exception {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("John Doe")
                .email("not-an-email")
                .password("SecurePass123!")
                .role("SENIOR")
                .build();

        // When/Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.validationErrors.email").exists());

        verify(authenticationService, never()).register(any());
    }

    @Test
    void testRegister_ValidationError_WeakPassword() throws Exception {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("John Doe")
                .email("john@example.com")
                .password("weak")
                .role("SENIOR")
                .build();

        // When/Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.validationErrors.password").exists());

        verify(authenticationService, never()).register(any());
    }

    @Test
    void testRegister_ValidationError_InvalidRole() throws Exception {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("John Doe")
                .email("john@example.com")
                .password("SecurePass123!")
                .role("ADMIN")  // ADMIN not allowed in public registration
                .build();

        // When/Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.validationErrors.role").exists());

        verify(authenticationService, never()).register(any());
    }

    @Test
    void testRegister_DuplicateEmail() throws Exception {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("John Doe")
                .email("duplicate@example.com")
                .password("SecurePass123!")
                .role("SENIOR")
                .build();

        when(authenticationService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateUserException("Email address is already registered"));

        // When/Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Email address is already registered"));

        verify(authenticationService, times(1)).register(any(RegisterRequest.class));
    }

    // ===========================
    // Login Tests
    // ===========================

    @Test
    void testLogin_Success() throws Exception {
        // Given
        LoginRequest request = LoginRequest.builder()
                .identifier("john@example.com")
                .password("SecurePass123!")
                .build();

        UserResponse user = UserResponse.builder()
                .id("user123")
                .fullName("John Doe")
                .email("john@example.com")
                .mobile("919876543210")
                .role(UserRole.SENIOR)
                .status(AccountStatus.ACTIVE)
                .emailVerified(true)
                .mobileVerified(false)
                .preferredLanguage("en")
                .createdAt(LocalDateTime.now())
                .build();

        AuthResponse mockResponse = AuthResponse.builder()
                .accessToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
                .refreshToken("xK9mP2vN8jQ4fR7tY3wE6sA1hL5bC0dZ")
                .tokenType("Bearer")
                .expiresIn(900)
                .user(user)
                .build();

        when(authenticationService.login(any(LoginRequest.class))).thenReturn(mockResponse);

        // When/Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.user.id").value("user123"))
                .andExpect(jsonPath("$.user.fullName").value("John Doe"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());

        verify(authenticationService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    void testLogin_ValidationError_MissingIdentifier() throws Exception {
        // Given
        LoginRequest request = LoginRequest.builder()
                .password("SecurePass123!")
                .build();

        // When/Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.validationErrors.identifier").exists());

        verify(authenticationService, never()).login(any());
    }

    @Test
    void testLogin_ValidationError_MissingPassword() throws Exception {
        // Given
        LoginRequest request = LoginRequest.builder()
                .identifier("john@example.com")
                .build();

        // When/Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.validationErrors.password").exists());

        verify(authenticationService, never()).login(any());
    }

    @Test
    void testLogin_InvalidCredentials() throws Exception {
        // Given
        LoginRequest request = LoginRequest.builder()
                .identifier("john@example.com")
                .password("WrongPassword123!")
                .build();

        when(authenticationService.login(any(LoginRequest.class)))
                .thenThrow(new AuthenticationException("Invalid credentials"));

        // When/Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid credentials"));

        verify(authenticationService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    void testLogin_AccountNotAvailable() throws Exception {
        // Given
        LoginRequest request = LoginRequest.builder()
                .identifier("suspended@example.com")
                .password("SecurePass123!")
                .build();

        when(authenticationService.login(any(LoginRequest.class)))
                .thenThrow(new AuthenticationException("Account is not available"));

        // When/Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Account is not available"));

        verify(authenticationService, times(1)).login(any(LoginRequest.class));
    }

    // ===========================
    // Refresh Token Tests
    // ===========================

    @Test
    void testRefresh_Success() throws Exception {
        // Given
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("xK9mP2vN8jQ4fR7tY3wE6sA1hL5bC0dZ")
                .build();

        TokenResponse mockResponse = TokenResponse.builder()
                .accessToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
                .refreshToken("n4P9qK2mV6xR8jY3wL7tB5sE1hC0fZ9")
                .tokenType("Bearer")
                .expiresIn(900)
                .build();

        when(authenticationService.refresh(any(RefreshTokenRequest.class))).thenReturn(mockResponse);

        // When/Then
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.refreshToken").value("n4P9qK2mV6xR8jY3wL7tB5sE1hC0fZ9"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.user").doesNotExist());  // Token refresh doesn't include user

        verify(authenticationService, times(1)).refresh(any(RefreshTokenRequest.class));
    }

    @Test
    void testRefresh_ValidationError_MissingToken() throws Exception {
        // Given
        RefreshTokenRequest request = RefreshTokenRequest.builder().build();

        // When/Then
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.validationErrors.refreshToken").exists());

        verify(authenticationService, never()).refresh(any());
    }

    @Test
    void testRefresh_InvalidToken() throws Exception {
        // Given
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("invalid-token")
                .build();

        when(authenticationService.refresh(any(RefreshTokenRequest.class)))
                .thenThrow(new AuthenticationException("Invalid refresh token"));

        // When/Then
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid refresh token"));

        verify(authenticationService, times(1)).refresh(any(RefreshTokenRequest.class));
    }

    @Test
    void testRefresh_ExpiredToken() throws Exception {
        // Given
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("expired-token")
                .build();

        when(authenticationService.refresh(any(RefreshTokenRequest.class)))
                .thenThrow(new AuthenticationException("Refresh token expired"));

        // When/Then
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Refresh token expired"));

        verify(authenticationService, times(1)).refresh(any(RefreshTokenRequest.class));
    }

    @Test
    void testRefresh_TokenReuseDetected() throws Exception {
        // Given
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("reused-token")
                .build();

        when(authenticationService.refresh(any(RefreshTokenRequest.class)))
                .thenThrow(new AuthenticationException("Invalid refresh token"));

        // When/Then - Reuse detection returns same generic error
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid refresh token"));

        verify(authenticationService, times(1)).refresh(any(RefreshTokenRequest.class));
    }

    // ===========================
    // Logout Tests
    // ===========================

    @Test
    void testLogout_Success() throws Exception {
        // Given
        LogoutRequest request = LogoutRequest.builder()
                .refreshToken("xK9mP2vN8jQ4fR7tY3wE6sA1hL5bC0dZ")
                .build();

        doNothing().when(authenticationService).logout(any(LogoutRequest.class));

        // When/Then
        mockMvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$").doesNotExist());  // No response body

        verify(authenticationService, times(1)).logout(any(LogoutRequest.class));
    }

    @Test
    void testLogout_ValidationError_MissingToken() throws Exception {
        // Given
        LogoutRequest request = LogoutRequest.builder().build();

        // When/Then
        mockMvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.validationErrors.refreshToken").exists());

        verify(authenticationService, never()).logout(any());
    }

    @Test
    void testLogout_UnknownToken_StillSucceeds() throws Exception {
        // Given
        LogoutRequest request = LogoutRequest.builder()
                .refreshToken("unknown-token")
                .build();

        // Service handles unknown tokens gracefully (no exception)
        doNothing().when(authenticationService).logout(any(LogoutRequest.class));

        // When/Then
        mockMvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(authenticationService, times(1)).logout(any(LogoutRequest.class));
    }

    @Test
    void testLogout_AlreadyRevokedToken_StillSucceeds() throws Exception {
        // Given
        LogoutRequest request = LogoutRequest.builder()
                .refreshToken("already-revoked-token")
                .build();

        // Service handles already-revoked tokens gracefully (no exception)
        doNothing().when(authenticationService).logout(any(LogoutRequest.class));

        // When/Then
        mockMvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(authenticationService, times(1)).logout(any(LogoutRequest.class));
    }

    // ===========================
    // JSON Serialization Tests
    // ===========================

    @Test
    void testLogin_ResponseDoesNotExposePasswordHash() throws Exception {
        // Given
        LoginRequest request = LoginRequest.builder()
                .identifier("john@example.com")
                .password("SecurePass123!")
                .build();

        UserResponse user = UserResponse.builder()
                .id("user123")
                .fullName("John Doe")
                .email("john@example.com")
                .mobile("919876543210")
                .role(UserRole.SENIOR)
                .status(AccountStatus.ACTIVE)
                .emailVerified(true)
                .mobileVerified(false)
                .preferredLanguage("en")
                .createdAt(LocalDateTime.now())
                .build();

        AuthResponse mockResponse = AuthResponse.builder()
                .accessToken("access")
                .refreshToken("refresh")
                .tokenType("Bearer")
                .expiresIn(900)
                .user(user)
                .build();

        when(authenticationService.login(any(LoginRequest.class))).thenReturn(mockResponse);

        // When/Then
        String responseBody = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Verify no sensitive fields in response
        assert !responseBody.contains("passwordHash");
        assert !responseBody.contains("tokenHash");
    }

    @Test
    void testRegister_ResponseDoesNotExposePasswordHash() throws Exception {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("John Doe")
                .email("john@example.com")
                .password("SecurePass123!")
                .role("SENIOR")
                .build();

        UserResponse mockResponse = UserResponse.builder()
                .id("user123")
                .fullName("John Doe")
                .email("john@example.com")
                .mobile(null)
                .role(UserRole.SENIOR)
                .status(AccountStatus.ACTIVE)
                .emailVerified(false)
                .mobileVerified(false)
                .preferredLanguage("en")
                .createdAt(LocalDateTime.now())
                .build();

        when(authenticationService.register(any(RegisterRequest.class))).thenReturn(mockResponse);

        // When/Then
        String responseBody = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // Verify no sensitive fields in response
        assert !responseBody.contains("passwordHash");
        assert !responseBody.contains("password");
        assert !responseBody.contains("SecurePass123!");
    }
}
