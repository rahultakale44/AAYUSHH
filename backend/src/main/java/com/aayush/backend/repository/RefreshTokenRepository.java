package com.aayush.backend.repository;

import com.aayush.backend.document.RefreshToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for RefreshToken document
 * Provides data access operations for refresh token management
 */
@Repository
public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {

    /**
     * Find refresh token by token hash
     * 
     * @param tokenHash SHA-256 hash of the refresh token
     * @return Optional containing token if found
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Check if token hash already exists
     * 
     * @param tokenHash SHA-256 hash of the refresh token
     * @return true if token hash exists, false otherwise
     */
    boolean existsByTokenHash(String tokenHash);

    /**
     * Find all tokens (revoked and active) by user ID
     * 
     * @param userId User ID
     * @return List of all tokens for the user
     */
    List<RefreshToken> findByUserId(String userId);

    /**
     * Find active (non-revoked) tokens by user ID
     * 
     * @param userId User ID
     * @param revoked Revocation status (false for active tokens)
     * @return List of active tokens for the user
     */
    List<RefreshToken> findByUserIdAndRevoked(String userId, boolean revoked);

    /**
     * Find all tokens in a token family
     * Used for family-based revocation on suspicious activity
     * 
     * @param tokenFamilyId Token family identifier
     * @return List of all tokens in the family
     */
    List<RefreshToken> findByTokenFamilyId(String tokenFamilyId);

    /**
     * Find active tokens in a token family
     * 
     * @param tokenFamilyId Token family identifier
     * @param revoked Revocation status
     * @return List of tokens in family matching revocation status
     */
    List<RefreshToken> findByTokenFamilyIdAndRevoked(String tokenFamilyId, boolean revoked);

    /**
     * Find tokens by user ID that expire before a certain date
     * Useful for cleanup operations
     * 
     * @param userId User ID
     * @param expiresAt Expiration timestamp threshold
     * @return List of tokens expiring before the given date
     */
    List<RefreshToken> findByUserIdAndExpiresAtBefore(String userId, LocalDateTime expiresAt);

    /**
     * Delete all tokens for a user
     * Used during account deletion or security incidents
     * 
     * @param userId User ID
     * @return Number of tokens deleted
     */
    long deleteByUserId(String userId);

    /**
     * Delete all tokens in a token family
     * Used when entire token family is compromised
     * 
     * @param tokenFamilyId Token family identifier
     * @return Number of tokens deleted
     */
    long deleteByTokenFamilyId(String tokenFamilyId);

    /**
     * Count active tokens for a user
     * 
     * @param userId User ID
     * @param revoked Revocation status
     * @return Number of active tokens
     */
    long countByUserIdAndRevoked(String userId, boolean revoked);

    /**
     * Count tokens in a token family
     * 
     * @param tokenFamilyId Token family identifier
     * @return Number of tokens in family
     */
    long countByTokenFamilyId(String tokenFamilyId);
}
