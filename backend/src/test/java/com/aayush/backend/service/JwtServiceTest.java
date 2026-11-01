package com.aayush.backend.service;

import com.aayush.backend.config.JwtProperties;
import com.aayush.backend.document.AccountStatus;
import com.aayush.backend.document.User;
import com.aayush.backend.document.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for JwtService
 * 
 * Covers:
 * - Token generation
 * - Token validation
 * - Claims extraction
 * - Expiration handling
 * - Signature verification
 * - Malformed token handling
 * - All user roles
 */
@SpringBootTest
@ActiveProfiles("test")
class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtProperties jwtProperties;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id("user123")
                .fullName("Test User")
                .email("test@example.com")
                .role(UserRole.SENIOR)
                .status(AccountStatus.ACTIVE)
                .emailVerified(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testGenerateAccessToken_Success() {
        // When
        String token = jwtService.generateAccessToken(testUser);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3);  // JWT has 3 parts: header.payload.signature
    }

    @Test
    void testGenerateAccessToken_ContainsCorrectSubject() {
        // When
        String token = jwtService.generateAccessToken(testUser);
        String extractedUserId = jwtService.extractUserId(token);

        // Then
        assertEquals("user123", extractedUserId);
    }

    @Test
    void testGenerateAccessToken_ContainsCorrectRole() {
        // When
        String token = jwtService.generateAccessToken(testUser);
        UserRole extractedRole = jwtService.extractRole(token);

        // Then
        assertEquals(UserRole.SENIOR, extractedRole);
    }

    @Test
    void testGenerateAccessToken_ContainsCorrectIssuer() {
        // Given
        String token = jwtService.generateAccessToken(testUser);

        // When
        Claims claims = parseToken(token);

        // Then
        assertEquals(jwtProperties.getIssuer(), claims.getIssuer());
    }

    @Test
    void testGenerateAccessToken_ContainsIssuedAtAndExpiration() {
        // Given
        Instant beforeGeneration = Instant.now().minus(1, ChronoUnit.SECONDS);
        
        // When
        String token = jwtService.generateAccessToken(testUser);
        Date expiration = jwtService.extractExpiration(token);
        
        Instant afterGeneration = Instant.now().plus(1, ChronoUnit.SECONDS);
        Claims claims = parseToken(token);

        // Then
        assertNotNull(claims.getIssuedAt());
        assertNotNull(expiration);
        
        // Issued at should be around now
        assertTrue(claims.getIssuedAt().toInstant().isAfter(beforeGeneration));
        assertTrue(claims.getIssuedAt().toInstant().isBefore(afterGeneration));
        
        // Expiration should be issued at + expiration duration
        long expectedExpirationMs = claims.getIssuedAt().getTime() + jwtProperties.getExpirationMs();
        assertEquals(expectedExpirationMs, expiration.getTime(), 1000);  // Allow 1 second tolerance
    }

    @Test
    void testGenerateAccessToken_UniqueJwtIds() {
        // When - Generate multiple tokens
        Set<String> jwtIds = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            String token = jwtService.generateAccessToken(testUser);
            Claims claims = parseToken(token);
            jwtIds.add(claims.getId());
        }

        // Then - All JWT IDs should be unique
        assertEquals(10, jwtIds.size());
    }

    @Test
    void testGenerateAccessToken_WithNullUser_ThrowsException() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            jwtService.generateAccessToken(null);
        });
    }

    @Test
    void testGenerateAccessToken_WithNullUserId_ThrowsException() {
        // Given
        User userWithoutId = User.builder()
                .fullName("No ID User")
                .role(UserRole.SENIOR)
                .build();

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            jwtService.generateAccessToken(userWithoutId);
        });
    }

    @Test
    void testGenerateAccessToken_WithNullRole_ThrowsException() {
        // Given
        User userWithoutRole = User.builder()
                .id("user456")
                .fullName("No Role User")
                .build();

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            jwtService.generateAccessToken(userWithoutRole);
        });
    }

    @Test
    void testValidateToken_ValidToken_ReturnsTrue() {
        // Given
        String token = jwtService.generateAccessToken(testUser);

        // When
        boolean isValid = jwtService.validateToken(token);

        // Then
        assertTrue(isValid);
    }

    @Test
    void testValidateToken_ExpiredToken_ReturnsFalse() {
        // Given - Create token with past expiration
        Instant past = Instant.now().minus(1, ChronoUnit.HOURS);
        String expiredToken = Jwts.builder()
                .subject(testUser.getId())
                .claim("role", testUser.getRole().name())
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(past.minus(2, ChronoUnit.HOURS)))
                .expiration(Date.from(past))
                .id("expired-token")
                .signWith(getSigningKey())
                .compact();

        // When
        boolean isValid = jwtService.validateToken(expiredToken);

        // Then
        assertFalse(isValid);
    }

    @Test
    void testValidateToken_MalformedToken_ReturnsFalse() {
        // Given
        String malformedToken = "this.is.not.a.valid.jwt";

        // When
        boolean isValid = jwtService.validateToken(malformedToken);

        // Then
        assertFalse(isValid);
    }

    @Test
    void testValidateToken_InvalidSignature_ReturnsFalse() {
        // Given - Create token with different secret
        SecretKey wrongKey = Keys.hmacShaKeyFor(
            "wrong-secret-key-at-least-32-bytes-for-hs256-algorithm".getBytes(StandardCharsets.UTF_8)
        );
        
        String tokenWithWrongSignature = Jwts.builder()
                .subject(testUser.getId())
                .claim("role", testUser.getRole().name())
                .issuer(jwtProperties.getIssuer())
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .id("wrong-sig-token")
                .signWith(wrongKey)
                .compact();

        // When
        boolean isValid = jwtService.validateToken(tokenWithWrongSignature);

        // Then
        assertFalse(isValid);
    }

    @Test
    void testValidateToken_WrongIssuer_ReturnsFalse() {
        // Given
        String tokenWithWrongIssuer = Jwts.builder()
                .subject(testUser.getId())
                .claim("role", testUser.getRole().name())
                .issuer("wrong-issuer")
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .id("wrong-issuer-token")
                .signWith(getSigningKey())
                .compact();

        // When
        boolean isValid = jwtService.validateToken(tokenWithWrongIssuer);

        // Then
        assertFalse(isValid);
    }

    @Test
    void testValidateToken_MissingSubject_ReturnsFalse() {
        // Given
        String tokenWithoutSubject = Jwts.builder()
                .claim("role", testUser.getRole().name())
                .issuer(jwtProperties.getIssuer())
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .id("no-subject-token")
                .signWith(getSigningKey())
                .compact();

        // When
        boolean isValid = jwtService.validateToken(tokenWithoutSubject);

        // Then
        assertFalse(isValid);
    }

    @Test
    void testValidateToken_MissingRole_ReturnsFalse() {
        // Given
        String tokenWithoutRole = Jwts.builder()
                .subject(testUser.getId())
                .issuer(jwtProperties.getIssuer())
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .id("no-role-token")
                .signWith(getSigningKey())
                .compact();

        // When
        boolean isValid = jwtService.validateToken(tokenWithoutRole);

        // Then
        assertFalse(isValid);
    }

    @Test
    void testValidateToken_InvalidRole_ReturnsFalse() {
        // Given
        String tokenWithInvalidRole = Jwts.builder()
                .subject(testUser.getId())
                .claim("role", "INVALID_ROLE")
                .issuer(jwtProperties.getIssuer())
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .id("invalid-role-token")
                .signWith(getSigningKey())
                .compact();

        // When
        boolean isValid = jwtService.validateToken(tokenWithInvalidRole);

        // Then
        assertFalse(isValid);
    }

    @Test
    void testExtractUserId_ValidToken_ReturnsUserId() {
        // Given
        String token = jwtService.generateAccessToken(testUser);

        // When
        String userId = jwtService.extractUserId(token);

        // Then
        assertEquals("user123", userId);
    }

    @Test
    void testExtractRole_ValidToken_ReturnsRole() {
        // Given
        String token = jwtService.generateAccessToken(testUser);

        // When
        UserRole role = jwtService.extractRole(token);

        // Then
        assertEquals(UserRole.SENIOR, role);
    }

    @Test
    void testExtractExpiration_ValidToken_ReturnsExpiration() {
        // When - Generate token
        String token = jwtService.generateAccessToken(testUser);
        
        // Then - Extract and verify expiration
        Date expiration = jwtService.extractExpiration(token);
        assertNotNull(expiration);
        
        // Expiration should be approximately now + expirationMs
        // Since token was just generated, calculate expected expiration from current time
        long expectedExpirationMs = Instant.now().toEpochMilli() + jwtProperties.getExpirationMs();
        long actualExpirationMs = expiration.getTime();
        
        // Allow up to 2 seconds difference (token was generated moments ago)
        long diff = Math.abs(actualExpirationMs - expectedExpirationMs);
        assertTrue(diff <= 2000,
            "Expiration time difference " + diff + "ms exceeds 2000ms tolerance");
    }

    @Test
    void testIsTokenExpired_ValidToken_ReturnsFalse() {
        // Given
        String token = jwtService.generateAccessToken(testUser);

        // When
        boolean isExpired = jwtService.isTokenExpired(token);

        // Then
        assertFalse(isExpired);
    }

    @Test
    void testIsTokenExpired_ExpiredToken_ReturnsTrue() {
        // Given
        Instant past = Instant.now().minus(1, ChronoUnit.HOURS);
        String expiredToken = Jwts.builder()
                .subject(testUser.getId())
                .claim("role", testUser.getRole().name())
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(past.minus(2, ChronoUnit.HOURS)))
                .expiration(Date.from(past))
                .id("expired-token")
                .signWith(getSigningKey())
                .compact();

        // When
        boolean isExpired = jwtService.isTokenExpired(expiredToken);

        // Then
        assertTrue(isExpired);
    }

    @Test
    void testIsTokenExpired_MalformedToken_ReturnsTrue() {
        // Given
        String malformedToken = "not.a.valid.token";

        // When
        boolean isExpired = jwtService.isTokenExpired(malformedToken);

        // Then
        assertTrue(isExpired);  // Malformed tokens are considered expired
    }

    @Test
    void testGenerateAccessToken_SeniorRole() {
        // Given
        testUser.setRole(UserRole.SENIOR);

        // When
        String token = jwtService.generateAccessToken(testUser);
        UserRole extractedRole = jwtService.extractRole(token);

        // Then
        assertEquals(UserRole.SENIOR, extractedRole);
    }

    @Test
    void testGenerateAccessToken_CaregiverRole() {
        // Given
        testUser.setRole(UserRole.CAREGIVER);

        // When
        String token = jwtService.generateAccessToken(testUser);
        UserRole extractedRole = jwtService.extractRole(token);

        // Then
        assertEquals(UserRole.CAREGIVER, extractedRole);
    }

    @Test
    void testGenerateAccessToken_AdminRole() {
        // Given
        testUser.setRole(UserRole.ADMIN);

        // When
        String token = jwtService.generateAccessToken(testUser);
        UserRole extractedRole = jwtService.extractRole(token);

        // Then
        assertEquals(UserRole.ADMIN, extractedRole);
    }

    @Test
    void testTokenDoesNotContainSensitiveData() {
        // Given
        testUser.setEmail("sensitive@example.com");
        testUser.setMobile("919876543210");
        
        // When
        String token = jwtService.generateAccessToken(testUser);
        Claims claims = parseToken(token);

        // Then - Verify sensitive data is NOT in token
        assertFalse(token.contains("sensitive@example.com"));
        assertFalse(token.contains("919876543210"));
        assertNull(claims.get("email"));
        assertNull(claims.get("mobile"));
        assertNull(claims.get("passwordHash"));
    }

    @Test
    void testExtractRole_TokenWithMissingRole_ThrowsException() {
        // Given
        String tokenWithoutRole = Jwts.builder()
                .subject(testUser.getId())
                .issuer(jwtProperties.getIssuer())
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .id("no-role")
                .signWith(getSigningKey())
                .compact();

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            jwtService.extractRole(tokenWithoutRole);
        });
    }

    @Test
    void testExtractRole_TokenWithInvalidRole_ThrowsException() {
        // Given
        String tokenWithInvalidRole = Jwts.builder()
                .subject(testUser.getId())
                .claim("role", "INVALID_ROLE_VALUE")
                .issuer(jwtProperties.getIssuer())
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .id("invalid-role")
                .signWith(getSigningKey())
                .compact();

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            jwtService.extractRole(tokenWithInvalidRole);
        });
    }

    @Test
    void testExtractUserId_TokenWithMissingSubject_ThrowsException() {
        // Given
        String tokenWithoutSubject = Jwts.builder()
                .claim("role", UserRole.SENIOR.name())
                .issuer(jwtProperties.getIssuer())
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .id("no-subject")
                .signWith(getSigningKey())
                .compact();

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            jwtService.extractUserId(tokenWithoutSubject);
        });
    }

    @Test
    void testTamperedToken_ReturnsFalse() {
        // Given
        String token = jwtService.generateAccessToken(testUser);
        
        // Tamper with token by modifying payload (change middle part)
        String[] parts = token.split("\\.");
        String tamperedToken = parts[0] + ".eyJzdWIiOiAidGFtcGVyZWQifQ." + parts[2];

        // When
        boolean isValid = jwtService.validateToken(tamperedToken);

        // Then
        assertFalse(isValid);
    }

    /**
     * Helper method to parse token for testing
     */
    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Helper method to get signing key
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
