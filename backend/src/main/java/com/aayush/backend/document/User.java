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
 * User Document
 * Represents a user account in the AAYUSH system
 * 
 * Stored in MongoDB collection: users
 * 
 * Security Notes:
 * - Email and mobile are normalized before storage (lowercase, trimmed, digits only)
 * - passwordHash is excluded from toString() to prevent accidental logging
 * - Unique indexes enforce one account per email/mobile
 */
@Document(collection = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "passwordHash")  // Prevent password hash from appearing in logs
public class User {

    @Id
    private String id;  // MongoDB ObjectId as String

    /**
     * User's full name
     */
    private String fullName;

    /**
     * Normalized email address (lowercase, trimmed)
     * Must be unique across all users
     */
    @Indexed(unique = true, sparse = true)
    private String email;

    /**
     * Normalized mobile number (digits only, with country code)
     * Format: "919876543210" for Indian numbers
     * Must be unique across all users
     */
    @Indexed(unique = true, sparse = true)
    private String mobile;

    /**
     * BCrypt password hash
     * NEVER store plaintext passwords
     */
    private String passwordHash;

    /**
     * User's role in the system
     */
    @Indexed
    private UserRole role;

    /**
     * Current account status
     */
    @Indexed
    private AccountStatus status;

    /**
     * Whether email address has been verified
     */
    private boolean emailVerified;

    /**
     * Whether mobile number has been verified
     */
    private boolean mobileVerified;

    /**
     * Timestamp when account was created
     * Automatically populated by MongoDB auditing
     */
    @CreatedDate
    @Indexed
    private LocalDateTime createdAt;

    /**
     * Timestamp when account was last updated
     * Automatically updated by MongoDB auditing
     */
    @LastModifiedDate
    private LocalDateTime updatedAt;

    /**
     * Timestamp of last successful login
     * Updated by authentication service
     */
    private LocalDateTime lastLoginAt;

    /**
     * User's preferred language for UI and communications
     * Values: "en", "hi", "mr"
     */
    private String preferredLanguage;
}
