package com.aayush.backend.filter;

import com.aayush.backend.document.AccountStatus;
import com.aayush.backend.document.User;
import com.aayush.backend.document.UserRole;
import com.aayush.backend.repository.UserRepository;
import com.aayush.backend.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JwtAuthenticationFilter
 * 
 * Tests JWT token processing, validation, and Spring Security context population
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JWT Authentication Filter Tests")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        // Clear SecurityContext before each test
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        // Clean up SecurityContext after each test
        SecurityContextHolder.clearContext();
    }

    // ==================== SUCCESSFUL AUTHENTICATION TESTS ====================

    @Test
    @DisplayName("Should authenticate user with valid JWT for SENIOR role")
    void testValidJwt_SeniorRole_Success() throws ServletException, IOException {
        // Given
        String validToken = "valid.jwt.token";
        String userId = "user123";
        
        User user = User.builder()
                .id(userId)
                .fullName("John Senior")
                .email("john@example.com")
                .role(UserRole.SENIOR)
                .status(AccountStatus.ACTIVE)
                .build();

        request.addHeader("Authorization", "Bearer " + validToken);

        when(jwtService.validateToken(validToken)).thenReturn(true);
        when(jwtService.extractUserId(validToken)).thenReturn(userId);
        when(jwtService.extractRole(validToken)).thenReturn(UserRole.SENIOR);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication, "Authentication should be set");
        assertEquals(userId, authentication.getPrincipal(), "Principal should be user ID");
        assertTrue(authentication.getAuthorities().contains(
            new SimpleGrantedAuthority("ROLE_SENIOR")), "Should have SENIOR authority");
        assertTrue(authentication.isAuthenticated(), "Should be authenticated");
        
        verify(filterChain).doFilter(request, response);
        verify(jwtService).validateToken(validToken);
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("Should authenticate user with valid JWT for CAREGIVER role")
    void testValidJwt_CaregiverRole_Success() throws ServletException, IOException {
        // Given
        String validToken = "valid.jwt.token";
        String userId = "user456";
        
        User user = User.builder()
                .id(userId)
                .fullName("Jane Caregiver")
                .email("jane@example.com")
                .role(UserRole.CAREGIVER)
                .status(AccountStatus.ACTIVE)
                .build();

        request.addHeader("Authorization", "Bearer " + validToken);

        when(jwtService.validateToken(validToken)).thenReturn(true);
        when(jwtService.extractUserId(validToken)).thenReturn(userId);
        when(jwtService.extractRole(validToken)).thenReturn(UserRole.CAREGIVER);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication, "Authentication should be set");
        assertEquals(userId, authentication.getPrincipal());
        assertTrue(authentication.getAuthorities().contains(
            new SimpleGrantedAuthority("ROLE_CAREGIVER")));
        
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should authenticate user with valid JWT for ADMIN role")
    void testValidJwt_AdminRole_Success() throws ServletException, IOException {
        // Given
        String validToken = "valid.jwt.token";
        String userId = "admin123";
        
        User user = User.builder()
                .id(userId)
                .fullName("Admin User")
                .email("admin@example.com")
                .role(UserRole.ADMIN)
                .status(AccountStatus.ACTIVE)
                .build();

        request.addHeader("Authorization", "Bearer " + validToken);

        when(jwtService.validateToken(validToken)).thenReturn(true);
        when(jwtService.extractUserId(validToken)).thenReturn(userId);
        when(jwtService.extractRole(validToken)).thenReturn(UserRole.ADMIN);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.getAuthorities().contains(
            new SimpleGrantedAuthority("ROLE_ADMIN")));
        
        verify(filterChain).doFilter(request, response);
    }

    // ==================== MISSING/INVALID HEADER TESTS ====================

    @Test
    @DisplayName("Should continue without authentication when Authorization header is missing")
    void testMissingAuthorizationHeader_NoAuthentication() throws ServletException, IOException {
        // Given: No Authorization header

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNull(authentication, "Should not set authentication when header is missing");
        
        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).validateToken(anyString());
        verify(userRepository, never()).findById(anyString());
    }

    @Test
    @DisplayName("Should continue without authentication when Authorization header is blank")
    void testBlankAuthorizationHeader_NoAuthentication() throws ServletException, IOException {
        // Given
        request.addHeader("Authorization", "   ");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).validateToken(anyString());
    }

    @Test
    @DisplayName("Should continue without authentication when Bearer prefix is missing")
    void testMissingBearerPrefix_NoAuthentication() throws ServletException, IOException {
        // Given
        request.addHeader("Authorization", "NotBearer valid.jwt.token");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).validateToken(anyString());
    }

    @Test
    @DisplayName("Should continue without authentication when token is empty after Bearer prefix")
    void testEmptyTokenAfterBearer_NoAuthentication() throws ServletException, IOException {
        // Given
        request.addHeader("Authorization", "Bearer ");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).validateToken(anyString());
    }

    @Test
    @DisplayName("Should continue without authentication when token is only whitespace after Bearer")
    void testWhitespaceTokenAfterBearer_NoAuthentication() throws ServletException, IOException {
        // Given
        request.addHeader("Authorization", "Bearer    ");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).validateToken(anyString());
    }

    // ==================== INVALID JWT TESTS ====================

    @Test
    @DisplayName("Should continue without authentication when JWT validation fails")
    void testInvalidJwt_NoAuthentication() throws ServletException, IOException {
        // Given
        String invalidToken = "invalid.jwt.token";
        request.addHeader("Authorization", "Bearer " + invalidToken);

        when(jwtService.validateToken(invalidToken)).thenReturn(false);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(jwtService).validateToken(invalidToken);
        verify(jwtService, never()).extractUserId(anyString());
        verify(userRepository, never()).findById(anyString());
    }

    @Test
    @DisplayName("Should continue without authentication when JWT is malformed")
    void testMalformedJwt_NoAuthentication() throws ServletException, IOException {
        // Given
        String malformedToken = "malformed.token";
        request.addHeader("Authorization", "Bearer " + malformedToken);

        when(jwtService.validateToken(malformedToken)).thenReturn(false);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should continue without authentication when JWT is expired")
    void testExpiredJwt_NoAuthentication() throws ServletException, IOException {
        // Given
        String expiredToken = "expired.jwt.token";
        request.addHeader("Authorization", "Bearer " + expiredToken);

        when(jwtService.validateToken(expiredToken)).thenReturn(false);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should continue without authentication when JWT has invalid signature")
    void testInvalidSignature_NoAuthentication() throws ServletException, IOException {
        // Given
        String tokenWithBadSignature = "token.with.bad.signature";
        request.addHeader("Authorization", "Bearer " + tokenWithBadSignature);

        when(jwtService.validateToken(tokenWithBadSignature)).thenReturn(false);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    // ==================== USER EXTRACTION FAILURE TESTS ====================

    @Test
    @DisplayName("Should continue without authentication when user ID extraction fails")
    void testUserIdExtractionFailure_NoAuthentication() throws ServletException, IOException {
        // Given
        String validToken = "valid.jwt.token";
        request.addHeader("Authorization", "Bearer " + validToken);

        when(jwtService.validateToken(validToken)).thenReturn(true);
        when(jwtService.extractUserId(validToken)).thenThrow(
            new IllegalArgumentException("Invalid user ID in token"));

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(userRepository, never()).findById(anyString());
    }

    @Test
    @DisplayName("Should continue without authentication when role extraction fails")
    void testRoleExtractionFailure_NoAuthentication() throws ServletException, IOException {
        // Given
        String validToken = "valid.jwt.token";
        String userId = "user123";
        request.addHeader("Authorization", "Bearer " + validToken);

        when(jwtService.validateToken(validToken)).thenReturn(true);
        when(jwtService.extractUserId(validToken)).thenReturn(userId);
        when(jwtService.extractRole(validToken)).thenThrow(
            new IllegalArgumentException("Invalid role in token"));

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(userRepository, never()).findById(anyString());
    }

    // ==================== USER NOT FOUND TESTS ====================

    @Test
    @DisplayName("Should continue without authentication when user not found in database")
    void testUserNotFound_NoAuthentication() throws ServletException, IOException {
        // Given
        String validToken = "valid.jwt.token";
        String unknownUserId = "unknown123";
        request.addHeader("Authorization", "Bearer " + validToken);

        when(jwtService.validateToken(validToken)).thenReturn(true);
        when(jwtService.extractUserId(validToken)).thenReturn(unknownUserId);
        when(jwtService.extractRole(validToken)).thenReturn(UserRole.SENIOR);
        when(userRepository.findById(unknownUserId)).thenReturn(Optional.empty());

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(userRepository).findById(unknownUserId);
    }

    // ==================== ACCOUNT STATUS TESTS ====================

    @Test
    @DisplayName("Should not authenticate SUSPENDED user")
    void testSuspendedUser_NoAuthentication() throws ServletException, IOException {
        // Given
        String validToken = "valid.jwt.token";
        String userId = "user123";
        
        User suspendedUser = User.builder()
                .id(userId)
                .fullName("Suspended User")
                .email("suspended@example.com")
                .role(UserRole.SENIOR)
                .status(AccountStatus.SUSPENDED)  // Suspended
                .build();

        request.addHeader("Authorization", "Bearer " + validToken);

        when(jwtService.validateToken(validToken)).thenReturn(true);
        when(jwtService.extractUserId(validToken)).thenReturn(userId);
        when(jwtService.extractRole(validToken)).thenReturn(UserRole.SENIOR);
        when(userRepository.findById(userId)).thenReturn(Optional.of(suspendedUser));

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertNull(SecurityContextHolder.getContext().getAuthentication(),
            "Should not authenticate suspended user");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not authenticate DELETED user")
    void testDeletedUser_NoAuthentication() throws ServletException, IOException {
        // Given
        String validToken = "valid.jwt.token";
        String userId = "user123";
        
        User deletedUser = User.builder()
                .id(userId)
                .fullName("Deleted User")
                .email("deleted@example.com")
                .role(UserRole.CAREGIVER)
                .status(AccountStatus.DELETED)  // Deleted
                .build();

        request.addHeader("Authorization", "Bearer " + validToken);

        when(jwtService.validateToken(validToken)).thenReturn(true);
        when(jwtService.extractUserId(validToken)).thenReturn(userId);
        when(jwtService.extractRole(validToken)).thenReturn(UserRole.CAREGIVER);
        when(userRepository.findById(userId)).thenReturn(Optional.of(deletedUser));

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertNull(SecurityContextHolder.getContext().getAuthentication(),
            "Should not authenticate deleted user");
        verify(filterChain).doFilter(request, response);
    }

    // ==================== ROLE MISMATCH TESTS ====================

    @Test
    @DisplayName("Should not authenticate when JWT role does not match database role")
    void testRoleMismatch_NoAuthentication() throws ServletException, IOException {
        // Given
        String validToken = "valid.jwt.token";
        String userId = "user123";
        
        User user = User.builder()
                .id(userId)
                .fullName("User")
                .email("user@example.com")
                .role(UserRole.SENIOR)  // Database says SENIOR
                .status(AccountStatus.ACTIVE)
                .build();

        request.addHeader("Authorization", "Bearer " + validToken);

        when(jwtService.validateToken(validToken)).thenReturn(true);
        when(jwtService.extractUserId(validToken)).thenReturn(userId);
        when(jwtService.extractRole(validToken)).thenReturn(UserRole.CAREGIVER);  // JWT says CAREGIVER
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertNull(SecurityContextHolder.getContext().getAuthentication(),
            "Should not authenticate when roles don't match");
        verify(filterChain).doFilter(request, response);
    }

    // ==================== SECURITY CONTEXT TESTS ====================

    @Test
    @DisplayName("Should not overwrite existing authentication")
    void testExistingAuthentication_NotOverwritten() throws ServletException, IOException {
        // Given: Set up existing authentication
        Authentication existingAuth = mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        String validToken = "valid.jwt.token";
        request.addHeader("Authorization", "Bearer " + validToken);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        assertSame(existingAuth, currentAuth, "Should not overwrite existing authentication");
        
        verify(filterChain).doFilter(request, response);
        // Filter should skip JWT processing
        verify(jwtService, never()).validateToken(anyString());
        verify(userRepository, never()).findById(anyString());
    }

    @Test
    @DisplayName("Should clear authentication between requests")
    void testAuthenticationCleanup_BetweenRequests() throws ServletException, IOException {
        // First request with valid JWT
        String validToken = "valid.jwt.token";
        String userId = "user123";
        
        User user = User.builder()
                .id(userId)
                .fullName("User")
                .role(UserRole.SENIOR)
                .status(AccountStatus.ACTIVE)
                .build();

        request.addHeader("Authorization", "Bearer " + validToken);

        when(jwtService.validateToken(validToken)).thenReturn(true);
        when(jwtService.extractUserId(validToken)).thenReturn(userId);
        when(jwtService.extractRole(validToken)).thenReturn(UserRole.SENIOR);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());

        // Simulate cleanup (normally done by SecurityContextPersistenceFilter)
        SecurityContextHolder.clearContext();

        // Second request without JWT
        MockHttpServletRequest newRequest = new MockHttpServletRequest();
        MockHttpServletResponse newResponse = new MockHttpServletResponse();
        
        jwtAuthenticationFilter.doFilterInternal(newRequest, newResponse, filterChain);

        // Should not have authentication from previous request
        assertNull(SecurityContextHolder.getContext().getAuthentication(),
            "Authentication should be cleared between requests");
    }

    // ==================== EXCEPTION HANDLING TESTS ====================

    @Test
    @DisplayName("Should continue filter chain even when unexpected exception occurs")
    void testUnexpectedException_ContinuesFilterChain() throws ServletException, IOException {
        // Given
        String validToken = "valid.jwt.token";
        request.addHeader("Authorization", "Bearer " + validToken);

        when(jwtService.validateToken(validToken)).thenThrow(
            new RuntimeException("Unexpected error"));

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    // ==================== TOKEN TRIMMING TESTS ====================

    @Test
    @DisplayName("Should handle JWT with whitespace after Bearer prefix")
    void testJwtWithWhitespace_Success() throws ServletException, IOException {
        // Given
        String validToken = "valid.jwt.token";
        String userId = "user123";
        
        User user = User.builder()
                .id(userId)
                .fullName("User")
                .role(UserRole.SENIOR)
                .status(AccountStatus.ACTIVE)
                .build();

        // Extra spaces after Bearer
        request.addHeader("Authorization", "Bearer   " + validToken);

        when(jwtService.validateToken(validToken)).thenReturn(true);
        when(jwtService.extractUserId(validToken)).thenReturn(userId);
        when(jwtService.extractRole(validToken)).thenReturn(UserRole.SENIOR);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertNotNull(SecurityContextHolder.getContext().getAuthentication(),
            "Should authenticate even with extra whitespace");
        verify(filterChain).doFilter(request, response);
    }
}
