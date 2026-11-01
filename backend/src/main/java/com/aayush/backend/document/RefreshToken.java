package com.aayush.backend.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Refresh Token Document
 * Stores refresh token metadata for JWT authentication
 * 
 * Stored in MongoDB collection: refresh_tokens
 * 
 * Security Notes:
 * - Only stores SHA-256 hash of the actual token, never the raw token
 * - tokenHash is excluded from toString() to prevent accidental logging
 * - Supports token rotation through tokenFamilyId and replacedByTokenId
 * - TTL index on expiresAt enables automatic cleanup of expired tokens
 * - Revoked tokens remain until expiry to enable reuse detection
 * 
 * Token Lifecycle:
 * 1. Token issued: issuedAt, expiresAt set, revoked = false
 * 2. Token used for refresh: new token issued, old token marked revoked
 * 3. Token expires: MongoDB TTL monitor deletes automatically
 * 4. Suspicious reuse: entire token family can be revoked
 */
@Document(collection = "refresh_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "tokenHash")  // Prevent token hash from appearing in logs
public class RefreshToken {

    @Id
    private String id;  // MongoDB ObjectId as String

    /**
     * SHA-256 hash of the actual refresh token
     * The raw token is never stored in the database
     * Must be unique across all tokens
     */
    @Indexed(unique = true)
    private String tokenHash;

    /**
     * Reference to the User document (User.id)
     * User who owns this refresh token
     */
    @Indexed
    private String userId;

    /**
     * Token family identifier
     * All tokens in the same rotation chain share this ID
     * Used to revoke entire family on suspicious activity
     */
    @Indexed
    private String tokenFamilyId;

    /**
     * Timestamp when token was issued
     */
    private LocalDateTime issuedAt;

    /**
     * Timestamp when token expires
     * MongoDB TTL index enables automatic deletion after expiry
     * Application must check expiry independently (TTL deletion is asynchronous)
     */
    @Indexed(expireAfterSeconds = 0)
    private LocalDateTime expiresAt;

    /**
     * Whether this token has been revoked
     * Revoked tokens remain in database until expiry for reuse detection
     */
    @Indexed
    private boolean revoked;

    /**
     * Timestamp when token was revoked
     * Null if token is not revoked
     */
    private LocalDateTime revokedAt;

    /**
     * ID of the token that replaced this one during rotation
     * Null if token was not rotated
     */
    private String replacedByTokenId;

    /**
     * Reason for revocation
     * Examples: "logout", "token_rotation", "suspicious_reuse", "family_revoked"
     */
    private String revocationReason;

    /**
     * Timestamp when record was created
     * Automatically populated by MongoDB auditing
     */
    @CreatedDate
    private LocalDateTime createdAt;

    /**
     * Timestamp when record was last updated
     * Automatically updated by MongoDB auditing
     */
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
