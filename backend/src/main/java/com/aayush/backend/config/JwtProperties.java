package com.aayush.backend.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

/**
 * JWT Configuration Properties
 * 
 * Reads JWT configuration from application.yml under 'aayush.security.jwt'
 * 
 * Security Notes:
 * - Secret key must be at least 256 bits (32 bytes) for HS256
 * - Production secret MUST be provided via environment variable
 * - Never log the secret key
 * - Validates secret strength at startup
 */
@Configuration
@ConfigurationProperties(prefix = "aayush.security.jwt")
@Data
public class JwtProperties {

    /**
     * Secret key for signing JWTs
     * MUST be at least 256 bits (32 bytes) for HS256 algorithm
     * 
     * Production: Set via JWT_SECRET environment variable
     * Test: Use dedicated test secret
     */
    private String secretKey;

    /**
     * Access token expiration time in milliseconds
     * Default: 900000 (15 minutes)
     */
    private long expirationMs = 900000L;

    /**
     * JWT issuer (typically application or organization name)
     * Default: "aayush-backend"
     */
    private String issuer = "aayush-backend";

    /**
     * Validate JWT configuration at startup
     * Ensures secret key is strong enough for HS256
     * 
     * Production configuration requires JWT_SECRET environment variable.
     * Application will fail to start if secret is missing, blank, or too weak.
     */
    @PostConstruct
    public void validateConfiguration() {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException(
                "JWT secret key is not configured. " +
                "Production requires JWT_SECRET environment variable. " +
                "Test profile uses dedicated test secret from application-test.yml."
            );
        }

        // HS256 requires at least 256 bits (32 bytes)
        byte[] secretBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                String.format(
                    "JWT secret key is too weak. HS256 requires at least 256 bits (32 bytes). " +
                    "Current length: %d bytes. Provide a stronger secret via JWT_SECRET environment variable.",
                    secretBytes.length
                )
            );
        }
    }
}
