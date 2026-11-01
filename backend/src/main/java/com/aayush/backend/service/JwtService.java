package com.aayush.backend.service;

import com.aayush.backend.config.JwtProperties;
import com.aayush.backend.document.User;
import com.aayush.backend.document.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * JWT Service
 * 
 * Responsible for generating and validating JWT access tokens
 * 
 * Security Features:
 * - HS256 (HMAC-SHA256) signing algorithm
 * - Unique JWT ID (jti) for each token
 * - Issuer validation
 * - Expiration enforcement
 * - Signature verification
 * - Claims validation
 * 
 * Important Notes:
 * - Only handles ACCESS tokens (not refresh tokens)
 * - Refresh tokens are opaque random values stored in MongoDB
 * - Never logs tokens or secrets
 * - Does not expose internal cryptographic exceptions to clients
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {

    private final JwtProperties jwtProperties;

    /**
     * Generate JWT access token for authenticated user
     * 
     * Token Claims:
     * - sub (subject): User ID
     * - role: User's application role
     * - iss (issuer): Configured issuer
     * - iat (issued at): Current timestamp
     * - exp (expiration): Issued at + expiration duration
     * - jti (JWT ID): Unique token identifier
     * 
     * @param user Authenticated user
     * @return Signed JWT access token
     */
    public String generateAccessToken(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User and user ID must not be null");
        }

        if (user.getRole() == null) {
            throw new IllegalArgumentException("User role must not be null");
        }

        Instant now = Instant.now();
        Instant expiration = now.plusMillis(jwtProperties.getExpirationMs());

        return Jwts.builder()
                .subject(user.getId())
                .claim("role", user.getRole().name())
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .id(UUID.randomUUID().toString())
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Validate JWT token
     * 
     * Checks:
     * - Signature validity
     * - Token not expired
     * - Issuer matches configuration
     * - Required claims present
     * 
     * @param token JWT token to validate
     * @return true if token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            
            // Validate issuer
            if (!jwtProperties.getIssuer().equals(claims.getIssuer())) {
                log.warn("JWT validation failed: Invalid issuer");
                return false;
            }
            
            // Validate required claims
            if (claims.getSubject() == null || claims.getSubject().isBlank()) {
                log.warn("JWT validation failed: Missing subject claim");
                return false;
            }
            
            if (claims.get("role") == null) {
                log.warn("JWT validation failed: Missing role claim");
                return false;
            }
            
            // Validate role is valid enum value
            try {
                UserRole.valueOf(claims.get("role", String.class));
            } catch (IllegalArgumentException e) {
                log.warn("JWT validation failed: Invalid role value");
                return false;
            }
            
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT validation failed: Token expired");
            return false;
        } catch (MalformedJwtException e) {
            log.debug("JWT validation failed: Malformed token");
            return false;
        } catch (SignatureException e) {
            log.debug("JWT validation failed: Invalid signature");
            return false;
        } catch (Exception e) {
            log.error("JWT validation failed: Unexpected error", e);
            return false;
        }
    }

    /**
     * Extract user ID from JWT token
     * 
     * @param token JWT token
     * @return User ID (subject claim)
     * @throws IllegalArgumentException if token is invalid or subject is missing
     */
    public String extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        String userId = claims.getSubject();
        
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Token does not contain valid user ID");
        }
        
        return userId;
    }

    /**
     * Extract user role from JWT token
     * 
     * @param token JWT token
     * @return User role
     * @throws IllegalArgumentException if token is invalid or role is missing/invalid
     */
    public UserRole extractRole(String token) {
        Claims claims = extractAllClaims(token);
        String roleString = claims.get("role", String.class);
        
        if (roleString == null || roleString.isBlank()) {
            throw new IllegalArgumentException("Token does not contain valid role");
        }
        
        try {
            return UserRole.valueOf(roleString);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Token contains invalid role value: " + roleString);
        }
    }

    /**
     * Extract expiration date from JWT token
     * 
     * @param token JWT token
     * @return Token expiration date
     * @throws IllegalArgumentException if token is invalid or expiration is missing
     */
    public Date extractExpiration(String token) {
        Claims claims = extractAllClaims(token);
        Date expiration = claims.getExpiration();
        
        if (expiration == null) {
            throw new IllegalArgumentException("Token does not contain valid expiration");
        }
        
        return expiration;
    }

    /**
     * Check if JWT token is expired
     * 
     * @param token JWT token
     * @return true if token is expired, false otherwise
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = extractExpiration(token);
            return expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            // If we can't parse the token, consider it expired
            return true;
        }
    }

    /**
     * Extract all claims from JWT token
     * 
     * @param token JWT token
     * @return Claims object
     * @throws ExpiredJwtException if token is expired
     * @throws MalformedJwtException if token is malformed
     * @throws SignatureException if signature is invalid
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .requireIssuer(jwtProperties.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Get signing key for JWT operations
     * 
     * @return SecretKey for HMAC-SHA256
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
