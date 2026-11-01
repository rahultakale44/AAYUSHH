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
     */
    @PostConstruct
    public void validateConfiguration() {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException(
                "JWT secret key is not configured. Set JWT_SECRET environment variable or aayush.security.jwt.secret-key property."
            );
        }

        // HS256 requires at least 256 bits (32 bytes)
        byte[] secretBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                String.format(
                    "JWT secret key is too weak. HS256 requires at least 256 bits (32 bytes). Current length: %d bytes. " +
                    "Please provide a stronger secret via JWT_SECRET environment variable.",
                    secretBytes.length
                )
            );
        }

        // Warn if using default secret in production-like environment
        if (secretKey.contains("demo") || secretKey.contains("default") || secretKey.contains("change")) {
            System.err.println(
                "WARNING: Using a demo/default JWT secret key. " +
                "This is insecure for production. Set a strong secret via JWT_SECRET environment variable."
            );
        }
    }
}
