package com.aayush.backend.service;

import com.aayush.backend.document.AccountStatus;
import com.aayush.backend.document.RefreshToken;
import com.aayush.backend.document.User;
import com.aayush.backend.document.UserRole;
import com.aayush.backend.dto.AuthResponse;
import com.aayush.backend.dto.LoginRequest;
import com.aayush.backend.dto.LogoutRequest;
import com.aayush.backend.dto.RefreshTokenRequest;
import com.aayush.backend.dto.TokenResponse;
import com.aayush.backend.exception.AuthenticationException;
import com.aayush.backend.repository.RefreshTokenRepository;
import com.aayush.backend.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthenticationService login, refresh, and logout operations
 * 
 * Uses Mockito to test service logic without MongoDB
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationService Login/Refresh/Logout Tests")
class AuthenticationServiceLoginTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordService passwordService;

    @Mock
    private JwtService jwtService;

    @Mock
    private org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;

    @InjectMocks
    private AuthenticationService authenticationService;

    // ==================== LOGIN TESTS ====================

    @Test
    @DisplayName("Should successfully login with email")
    void testLogin_WithEmail_Success() {
        // Given
        LoginRequest request = LoginRequest.builder()
                .identifier("john@example.com")
                .password("SecureP@ss123")
                .build();

        User user = User.builder()
                .id("user123")
                .fullName("John Doe")
                .email("john@example.com")
                .passwordHash("$2a$12$hashedPassword")
                .role(UserRole.SENIOR)
                .status(AccountStatus.ACTIVE)
                .emailVerified(true)
                .mobileVerified(false)
                .preferredLanguage("en")
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordService.matches("SecureP@ss123", "$2a$12$hashedPassword")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("jwt.access.token");
        when(jwtService.extractExpiration("jwt.access.token")).thenReturn(
            new java.util.Date(System.currentTimeMillis() + 900000)  // 15 minutes
        );
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        AuthResponse response = authenticationService.login(request);

        // Then
        assertNotNull(response);
        assertEquals("jwt.access.token", response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertTrue(response.getExpiresIn() > 0);
        assertEquals("user123", response.getUser().getId());
        assertEquals("John Doe", response.getUser().getFullName());

        // Verify refresh token was stored
        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());
        
        RefreshToken savedToken = tokenCaptor.getValue();
        assertEquals("user123", savedToken.getUserId());
        assertNotNull(savedToken.getTokenHash());
        assertNotNull(savedToken.getTokenFamilyId());
        assertFalse(savedToken.isRevoked());
        assertNotNull(savedToken.getIssuedAt());
        assertNotNull(savedToken.getExpiresAt());
    }

    @Test
    @DisplayName("Should successfully login with mobile")
    void testLogin_WithMobile_Success() {
        // Given
        LoginRequest request = LoginRequest.builder()
                .identifier("9876543210")
                .password("SecureP@ss123")
                .build();

        User user = User.builder()
                .id("user456")
                .fullName("Jane Smith")
                .mobile("919876543210")
                .passwordHash("$2a$12$hashedPassword")
                .role(UserRole.CAREGIVER)
                .status(AccountStatus.ACTIVE)
                .emailVerified(false)
                .mobileVerified(true)
                .preferredLanguage("hi")
                .build();

        when(userRepository.findByMobile("919876543210")).thenReturn(Optional.of(user));
        when(passwordService.matches("SecureP@ss123", "$2a$12$hashedPassword")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("jwt.access.token");
        when(jwtService.extractExpiration("jwt.access.token")).thenReturn(
            new java.util.Date(System.currentTimeMillis() + 900000)
        );
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        AuthResponse response = authenticationService.login(request);

        // Then
        assertNotNull(response);
        assertEquals("user456", response.getUser().getId());
        assertEquals("Jane Smith", response.getUser().getFullName());
        verify(userRepository).findByMobile("919876543210");
    }

    @Test
    @DisplayName("Should reject login with incorrect password")
    void testLogin_IncorrectPassword_ThrowsException() {
        // Given
        LoginRequest request = LoginRequest.builder()
                .identifier("john@example.com")
                .password("WrongPassword")
                .build();

        User user = User.builder()
                .id("user123")
                .email("john@example.com")
                .passwordHash("$2a$12$hashedPassword")
                .status(AccountStatus.ACTIVE)
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordService.matches("WrongPassword", "$2a$12$hashedPassword")).thenReturn(false);

        // When/Then
        AuthenticationException exception = assertThrows(
            AuthenticationException.class,
            () -> authenticationService.login(request)
        );

        assertEquals("Invalid credentials", exception.getMessage());
        verify(jwtService, never()).generateAccessToken(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject login with unknown identifier")
    void testLogin_UnknownIdentifier_ThrowsException() {
        // Given
        LoginRequest request = LoginRequest.builder()
                .identifier("unknown@example.com")
                .password("SecureP@ss123")
                .build();

        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        // When/Then
        AuthenticationException exception = assertThrows(
            AuthenticationException.class,
            () -> authenticationService.login(request)
        );

        assertEquals("Invalid credentials", exception.getMessage());
        verify(passwordService, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("Should reject login for SUSPENDED account")
    void testLogin_SuspendedAccount_ThrowsException() {
        // Given
        LoginRequest request = LoginRequest.builder()
                .identifier("suspended@example.com")
                .password("SecureP@ss123")
                .build();

        User user = User.builder()
                .id("user123")
                .email("suspended@example.com")
                .passwordHash("$2a$12$hashedPassword")
                .status(AccountStatus.SUSPENDED)
                .build();

        when(userRepository.findByEmail("suspended@example.com")).thenReturn(Optional.of(user));
        when(passwordService.matches("SecureP@ss123", "$2a$12$hashedPassword")).thenReturn(true);

        // When/Then
        AuthenticationException exception = assertThrows(
            AuthenticationException.class,
            () -> authenticationService.login(request)
        );

        assertEquals("Account is not available", exception.getMessage());
        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    @DisplayName("Should reject login for DELETED account")
    void testLogin_DeletedAccount_ThrowsException() {
        // Given
        LoginRequest request = LoginRequest.builder()
                .identifier("deleted@example.com")
                .password("SecureP@ss123")
                .build();

        User user = User.builder()
                .id("user123")
                .email("deleted@example.com")
                .passwordHash("$2a$12$hashedPassword")
                .status(AccountStatus.DELETED)
                .build();

        when(userRepository.findByEmail("deleted@example.com")).thenReturn(Optional.of(user));
        when(passwordService.matches("SecureP@ss123", "$2a$12$hashedPassword")).thenReturn(true);

        // When/Then
        AuthenticationException exception = assertThrows(
            AuthenticationException.class,
            () -> authenticationService.login(request)
        );

        assertEquals("Account is not available", exception.getMessage());
    }

    @Test
    @DisplayName("Should reject null login request")
    void testLogin_NullRequest_ThrowsException() {
        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> authenticationService.login(null)
        );

        assertEquals("Login request cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should reject login with null identifier")
    void testLogin_NullIdentifier_ThrowsException() {
        // Given
        LoginRequest request = LoginRequest.builder()
                .identifier(null)
                .password("SecureP@ss123")
                .build();

        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> authenticationService.login(request)
        );

        assertEquals("Identifier cannot be null or blank", exception.getMessage());
    }

    @Test
    @DisplayName("Should reject login with blank password")
    void testLogin_BlankPassword_ThrowsException() {
        // Given
        LoginRequest request = LoginRequest.builder()
                .identifier("john@example.com")
                .password("   ")
                .build();

        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> authenticationService.login(request)
        );

        assertEquals("Password cannot be null or blank", exception.getMessage());
    }

    @Test
    @DisplayName("Should store only SHA-256 hash, not raw refresh token")
    void testLogin_RefreshTokenStoredAsHash() {
        // Given
        LoginRequest request = LoginRequest.builder()
                .identifier("john@example.com")
                .password("SecureP@ss123")
                .build();

        User user = User.builder()
                .id("user123")
                .email("john@example.com")
                .passwordHash("$2a$12$hashedPassword")
                .role(UserRole.SENIOR)
                .status(AccountStatus.ACTIVE)
                .build();

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordService.matches(anyString(), anyString())).thenReturn(true);
        when(jwtService.generateAccessToken(any())).thenReturn("jwt.token");
        when(jwtService.extractExpiration(anyString())).thenReturn(new java.util.Date(System.currentTimeMillis() + 900000));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        AuthResponse response = authenticationService.login(request);

        // Then
        String rawToken = response.getRefreshToken();
        assertNotNull(rawToken);
        assertFalse(rawToken.isBlank());

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());
        
        RefreshToken savedToken = tokenCaptor.getValue();
        // Token hash should be 64 hex characters (SHA-256)
        assertEquals(64, savedToken.getTokenHash().length());
        // Token hash should not equal raw token
        assertNotEquals(rawToken, savedToken.getTokenHash());
    }

    // ==================== REFRESH TESTS ====================

    @Test
    @DisplayName("Should successfully refresh token")
    void testRefresh_Success() {
        // Given
        String oldRefreshToken = "old.refresh.token";
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken(oldRefreshToken)
                .build();

        User user = User.builder()
                .id("user123")
                .fullName("John Doe")
                .email("john@example.com")
                .role(UserRole.SENIOR)
                .status(AccountStatus.ACTIVE)
                .build();

        RefreshToken storedToken = RefreshToken.builder()
                .id("token123")
                .tokenHash("hash_of_old_token")
                .userId("user123")
                .tokenFamilyId("family123")
                .issuedAt(LocalDateTime.now().minusDays(1))
                .expiresAt(LocalDateTime.now().plusDays(29))
                .revoked(false)
                .build();

        // Mock atomic consumption (mongoTemplate.findAndModify)
        when(mongoTemplate.findAndModify(any(), any(), eq(RefreshToken.class)))
                .thenReturn(storedToken);
        when(userRepository.findById("user123")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("new.jwt.token");
        when(jwtService.extractExpiration("new.jwt.token")).thenReturn(
            new java.util.Date(System.currentTimeMillis() + 900000)
        );
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            if (token.getId() == null) {
                token.setId("newtoken456");
            }
            return token;
        });

        // When
        TokenResponse response = authenticationService.refresh(request);

        // Then
        assertNotNull(response);
        assertEquals("new.jwt.token", response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertNotEquals(oldRefreshToken, response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());

        // Verify atomic consumption was called
        verify(mongoTemplate).findAndModify(any(), any(), eq(RefreshToken.class));

        // Verify new token was saved with same family
        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, atLeastOnce()).save(tokenCaptor.capture());
        
        List<RefreshToken> savedTokens = tokenCaptor.getAllValues();
        RefreshToken newToken = savedTokens.stream()
                .filter(t -> !t.isRevoked())
                .findFirst()
                .orElse(null);
        
        assertNotNull(newToken);
        assertEquals("family123", newToken.getTokenFamilyId());
        assertEquals("user123", newToken.getUserId());
    }

    @Test
    @DisplayName("Should reject refresh with unknown token")
    void testRefresh_UnknownToken_ThrowsException() {
        // Given
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("unknown.token")
                .build();

        // Mock atomic consumption returns null (token doesn't exist)
        when(mongoTemplate.findAndModify(any(), any(), eq(RefreshToken.class)))
                .thenReturn(null);
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        // When/Then
        AuthenticationException exception = assertThrows(
            AuthenticationException.class,
            () -> authenticationService.refresh(request)
        );

        assertEquals("Invalid refresh token", exception.getMessage());
    }

    @Test
    @DisplayName("Should reject refresh with expired token")
    void testRefresh_ExpiredToken_ThrowsException() {
        // Given
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("expired.token")
                .build();

        // Mock atomic consumption returns null (expired tokens don't match query criteria)
        when(mongoTemplate.findAndModify(any(), any(), eq(RefreshToken.class)))
                .thenReturn(null);
        
        RefreshToken storedToken = RefreshToken.builder()
                .tokenHash("hash")
                .userId("user123")
                .tokenFamilyId("family123")
                .issuedAt(LocalDateTime.now().minusDays(31))
                .expiresAt(LocalDateTime.now().minusDays(1))  // Expired
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(storedToken));

        // When/Then
        AuthenticationException exception = assertThrows(
            AuthenticationException.class,
            () -> authenticationService.refresh(request)
        );

        assertEquals("Invalid refresh token", exception.getMessage());
    }

    @Test
    @DisplayName("Should detect token reuse and revoke entire family")
    void testRefresh_TokenReuse_RevokesFamily() {
        // Given
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("reused.token")
                .build();

        RefreshToken revokedToken = RefreshToken.builder()
                .id("original_token")
                .tokenHash("acf74820831eb89bfd1ca7ec4b9d8c451142402fce2396227739f66e47d14eec")
                .userId("user123")
                .tokenFamilyId("family123")
                .issuedAt(LocalDateTime.now().minusDays(1))
                .expiresAt(LocalDateTime.now().plusDays(29))
                .revoked(true)  // Already revoked
                .revokedAt(LocalDateTime.now().minusHours(1))
                .revocationReason("token_rotation")
                .replacedByTokenId("replacement_token_id")  // KEY: This indicates completed rotation
                .build();

        RefreshToken activeToken1 = RefreshToken.builder()
                .id("active1")
                .tokenHash("hash1")
                .userId("user123")
                .tokenFamilyId("family123")
                .revoked(false)
                .build();

        RefreshToken activeToken2 = RefreshToken.builder()
                .id("active2")
                .tokenHash("hash2")
                .userId("user123")
                .tokenFamilyId("family123")
                .revoked(false)
                .build();

        // Mock atomic consumption returns null (already revoked)
        when(mongoTemplate.findAndModify(any(), any(), eq(RefreshToken.class)))
                .thenReturn(null);
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(revokedToken));
        when(refreshTokenRepository.findByTokenFamilyIdAndRevoked("family123", false))
                .thenReturn(Arrays.asList(activeToken1, activeToken2));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When/Then
        AuthenticationException exception = assertThrows(
            AuthenticationException.class,
            () -> authenticationService.refresh(request)
        );

        assertEquals("Invalid refresh token", exception.getMessage());

        // Verify all active tokens in family were revoked
        verify(refreshTokenRepository, times(2)).save(argThat(token -> 
            token.isRevoked() && "suspicious_reuse".equals(token.getRevocationReason())
        ));
    }

    @Test
    @DisplayName("Should reject refresh for non-ACTIVE user")
    void testRefresh_InactiveUser_ThrowsException() {
        // Given
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("valid.token")
                .build();

        User user = User.builder()
                .id("user123")
                .status(AccountStatus.SUSPENDED)
                .build();

        RefreshToken storedToken = RefreshToken.builder()
                .tokenHash("hash")
                .userId("user123")
                .tokenFamilyId("family123")
                .issuedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(30))
                .revoked(false)
                .build();

        // Mock atomic consumption succeeds
        when(mongoTemplate.findAndModify(any(), any(), eq(RefreshToken.class)))
                .thenReturn(storedToken);
        when(userRepository.findById("user123")).thenReturn(Optional.of(user));

        // When/Then
        AuthenticationException exception = assertThrows(
            AuthenticationException.class,
            () -> authenticationService.refresh(request)
        );

        assertEquals("Account is not available", exception.getMessage());
    }

    @Test
    @DisplayName("Should reject refresh when user not found")
    void testRefresh_UserNotFound_ThrowsException() {
        // Given
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("valid.token")
                .build();

        RefreshToken storedToken = RefreshToken.builder()
                .tokenHash("hash")
                .userId("nonexistent")
                .tokenFamilyId("family123")
                .issuedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(30))
                .revoked(false)
                .build();

        // Mock atomic consumption succeeds
        when(mongoTemplate.findAndModify(any(), any(), eq(RefreshToken.class)))
                .thenReturn(storedToken);
        when(userRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // When/Then
        AuthenticationException exception = assertThrows(
            AuthenticationException.class,
            () -> authenticationService.refresh(request)
        );

        assertEquals("Invalid refresh token", exception.getMessage());
    }

    @Test
    @DisplayName("Should preserve token family ID across rotation")
    void testRefresh_PreservesTokenFamily() {
        // Given
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("original.token")
                .build();

        User user = User.builder()
                .id("user123")
                .role(UserRole.SENIOR)
                .status(AccountStatus.ACTIVE)
                .build();

        RefreshToken originalToken = RefreshToken.builder()
                .id("original")
                .tokenHash("original_hash")
                .userId("user123")
                .tokenFamilyId("family_abc123")
                .issuedAt(LocalDateTime.now().minusDays(1))
                .expiresAt(LocalDateTime.now().plusDays(29))
                .revoked(false)
                .build();

        // Mock atomic consumption
        when(mongoTemplate.findAndModify(any(), any(), eq(RefreshToken.class)))
                .thenReturn(originalToken);
        when(userRepository.findById("user123")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(any())).thenReturn("jwt.token");
        when(jwtService.extractExpiration(anyString())).thenReturn(new java.util.Date(System.currentTimeMillis() + 900000));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            if (token.getId() == null) {
                token.setId("new_token_id");
            }
            return token;
        });

        // When
        TokenResponse response = authenticationService.refresh(request);

        // Then
        assertNotNull(response);

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, atLeastOnce()).save(tokenCaptor.capture());
        
        // Find the new (non-revoked) token
        RefreshToken newToken = tokenCaptor.getAllValues().stream()
                .filter(t -> !t.isRevoked() && t.getId() != null && !t.getId().equals("original"))
                .findFirst()
                .orElse(null);
        
        assertNotNull(newToken);
        assertEquals("family_abc123", newToken.getTokenFamilyId());
    }

    // ==================== LOGOUT TESTS ====================

    @Test
    @DisplayName("Should successfully logout and revoke token")
    void testLogout_Success() {
        // Given
        LogoutRequest request = LogoutRequest.builder()
                .refreshToken("active.token")
                .build();

        RefreshToken activeToken = RefreshToken.builder()
                .id("token123")
                .tokenHash("hash")
                .userId("user123")
                .tokenFamilyId("family123")
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(activeToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        authenticationService.logout(request);

        // Then
        assertTrue(activeToken.isRevoked());
        assertEquals("logout", activeToken.getRevocationReason());
        assertNotNull(activeToken.getRevokedAt());
        verify(refreshTokenRepository).save(activeToken);
    }

    @Test
    @DisplayName("Should handle logout with unknown token gracefully")
    void testLogout_UnknownToken_NoException() {
        // Given
        LogoutRequest request = LogoutRequest.builder()
                .refreshToken("unknown.token")
                .build();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        // When/Then - should not throw exception
        assertDoesNotThrow(() -> authenticationService.logout(request));
        
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle logout with already-revoked token gracefully")
    void testLogout_AlreadyRevoked_NoException() {
        // Given
        LogoutRequest request = LogoutRequest.builder()
                .refreshToken("revoked.token")
                .build();

        RefreshToken revokedToken = RefreshToken.builder()
                .id("token123")
                .tokenHash("hash")
                .userId("user123")
                .revoked(true)
                .revokedAt(LocalDateTime.now().minusHours(1))
                .revocationReason("logout")
                .build();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(revokedToken));

        // When/Then - should not throw exception or save again
        assertDoesNotThrow(() -> authenticationService.logout(request));
        
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject null logout request")
    void testLogout_NullRequest_ThrowsException() {
        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> authenticationService.logout(null)
        );

        assertEquals("Logout request cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should reject logout with null refresh token")
    void testLogout_NullToken_ThrowsException() {
        // Given
        LogoutRequest request = LogoutRequest.builder()
                .refreshToken(null)
                .build();

        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> authenticationService.logout(request)
        );

        assertEquals("Refresh token cannot be null or blank", exception.getMessage());
    }

    @Test
    @DisplayName("Should not physically delete token on logout")
    void testLogout_DoesNotDeleteToken() {
        // Given
        LogoutRequest request = LogoutRequest.builder()
                .refreshToken("active.token")
                .build();

        RefreshToken activeToken = RefreshToken.builder()
                .id("token123")
                .tokenHash("hash")
                .userId("user123")
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(activeToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        authenticationService.logout(request);

        // Then
        verify(refreshTokenRepository, never()).delete(any());
        verify(refreshTokenRepository, never()).deleteById(anyString());
        verify(refreshTokenRepository).save(activeToken);
    }
}
