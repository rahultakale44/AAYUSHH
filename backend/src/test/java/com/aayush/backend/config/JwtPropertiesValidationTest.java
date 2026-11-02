package com.aayush.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JwtProperties validation
 * 
 * Verifies security requirements:
 * - Missing secret is rejected
 * - Blank secret is rejected
 * - Short secret is rejected
 * - Valid secret is accepted
 * 
 * These tests verify @PostConstruct validation logic directly
 * without loading the full Spring context.
 */
class JwtPropertiesValidationTest {

    @Test
    void testValidSecretIsAccepted() {
        // Given - Valid 32+ byte secret
        JwtProperties properties = new JwtProperties();
        properties.setSecretKey("this-is-a-valid-secret-key-with-at-least-32-bytes-for-hs256");
        properties.setExpirationMs(900000L);
        properties.setIssuer("test-issuer");

        // When/Then - Should not throw
        assertDoesNotThrow(() -> properties.validateConfiguration());
    }

    @Test
    void testNullSecretIsRejected() {
        // Given - Null secret
        JwtProperties properties = new JwtProperties();
        properties.setSecretKey(null);
        properties.setExpirationMs(900000L);
        properties.setIssuer("test-issuer");

        // When/Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            properties.validateConfiguration();
        });
        
        assertTrue(exception.getMessage().contains("not configured"));
        assertTrue(exception.getMessage().contains("JWT_SECRET"));
    }

    @Test
    void testBlankSecretIsRejected() {
        // Given - Blank secret
        JwtProperties properties = new JwtProperties();
        properties.setSecretKey("   ");
        properties.setExpirationMs(900000L);
        properties.setIssuer("test-issuer");

        // When/Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            properties.validateConfiguration();
        });
        
        assertTrue(exception.getMessage().contains("not configured"));
    }

    @Test
    void testEmptySecretIsRejected() {
        // Given - Empty secret
        JwtProperties properties = new JwtProperties();
        properties.setSecretKey("");
        properties.setExpirationMs(900000L);
        properties.setIssuer("test-issuer");

        // When/Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            properties.validateConfiguration();
        });
        
        assertTrue(exception.getMessage().contains("not configured"));
    }

    @Test
    void testShortSecretIsRejected() {
        // Given - Secret with less than 32 bytes
        JwtProperties properties = new JwtProperties();
        properties.setSecretKey("short-secret");  // Only 12 bytes
        properties.setExpirationMs(900000L);
        properties.setIssuer("test-issuer");

        // When/Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            properties.validateConfiguration();
        });
        
        assertTrue(exception.getMessage().contains("too weak"));
        assertTrue(exception.getMessage().contains("32 bytes"));
    }

    @Test
    void test31ByteSecretIsRejected() {
        // Given - Secret with exactly 31 bytes (one byte short)
        JwtProperties properties = new JwtProperties();
        properties.setSecretKey("1234567890123456789012345678901");  // Exactly 31 bytes
        properties.setExpirationMs(900000L);
        properties.setIssuer("test-issuer");

        // When/Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            properties.validateConfiguration();
        });
        
        assertTrue(exception.getMessage().contains("too weak"));
        assertTrue(exception.getMessage().contains("31 bytes"));
    }

    @Test
    void test32ByteSecretIsAccepted() {
        // Given - Secret with exactly 32 bytes (minimum)
        JwtProperties properties = new JwtProperties();
        properties.setSecretKey("12345678901234567890123456789012");  // Exactly 32 bytes
        properties.setExpirationMs(900000L);
        properties.setIssuer("test-issuer");

        // When/Then - Should not throw
        assertDoesNotThrow(() -> properties.validateConfiguration());
    }

    @Test
    void test64ByteSecretIsAccepted() {
        // Given - Secret with 64 bytes (stronger than minimum)
        JwtProperties properties = new JwtProperties();
        properties.setSecretKey("1234567890123456789012345678901234567890123456789012345678901234");
        properties.setExpirationMs(900000L);
        properties.setIssuer("test-issuer");

        // When/Then - Should not throw
        assertDoesNotThrow(() -> properties.validateConfiguration());
    }

    @Test
    void testValidationMessageMentionsEnvironmentVariable() {
        // Given - Missing secret
        JwtProperties properties = new JwtProperties();
        properties.setSecretKey(null);
        properties.setExpirationMs(900000L);
        properties.setIssuer("test-issuer");

        // When/Then - Error message should guide users to JWT_SECRET
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            properties.validateConfiguration();
        });
        
        assertTrue(exception.getMessage().contains("JWT_SECRET"),
            "Error message should mention JWT_SECRET environment variable");
    }

    @Test
    void testValidationMessageForWeakSecretShowsActualLength() {
        // Given - Short secret
        JwtProperties properties = new JwtProperties();
        properties.setSecretKey("weak");  // 4 bytes
        properties.setExpirationMs(900000L);
        properties.setIssuer("test-issuer");

        // When/Then - Error message should show actual length
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            properties.validateConfiguration();
        });
        
        assertTrue(exception.getMessage().contains("4 bytes"),
            "Error message should show actual secret length");
    }
}

