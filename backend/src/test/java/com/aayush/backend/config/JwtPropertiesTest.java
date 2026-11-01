package com.aayush.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JwtProperties configuration
 * 
 * Verifies:
 * - Configuration loads correctly
 * - Secret key meets minimum length requirement
 * - Properties are accessible
 */
@SpringBootTest
@ActiveProfiles("test")
class JwtPropertiesTest {

    @Autowired
    private JwtProperties jwtProperties;

    @Test
    void testJwtPropertiesLoaded() {
        // Then
        assertNotNull(jwtProperties);
    }

    @Test
    void testSecretKeyLoaded() {
        // Then
        assertNotNull(jwtProperties.getSecretKey());
        assertFalse(jwtProperties.getSecretKey().isEmpty());
    }

    @Test
    void testSecretKeyMeetsMinimumLength() {
        // Then - HS256 requires at least 32 bytes
        assertTrue(jwtProperties.getSecretKey().getBytes().length >= 32,
            "Secret key must be at least 32 bytes for HS256");
    }

    @Test
    void testExpirationConfigured() {
        // Then
        assertTrue(jwtProperties.getExpirationMs() > 0);
    }

    @Test
    void testIssuerConfigured() {
        // Then
        assertNotNull(jwtProperties.getIssuer());
        assertFalse(jwtProperties.getIssuer().isEmpty());
    }

    @Test
    void testTestProfileUsesTestSecret() {
        // Then - Verify we're using test secret, not production default
        String secretKey = jwtProperties.getSecretKey();
        assertTrue(secretKey.contains("test"), 
            "Test profile should use test-specific secret key");
    }

    @Test
    void testTestProfileUsesTestIssuer() {
        // Then
        assertEquals("aayush-backend-test", jwtProperties.getIssuer(),
            "Test profile should use test-specific issuer");
    }
}
