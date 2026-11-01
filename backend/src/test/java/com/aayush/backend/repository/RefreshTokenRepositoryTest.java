package com.aayush.backend.repository;

import com.aayush.backend.document.RefreshToken;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for RefreshTokenRepository
 * Uses Docker MongoDB container at localhost:27017 with dedicated test database
 */
@SpringBootTest
@ActiveProfiles("test")
class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MongoClient mongoClient;

    @AfterEach
    void cleanUp() {
        // Clean up test data after each test to ensure isolation
        refreshTokenRepository.deleteAll();
    }

    @Test
    void testSaveRefreshToken_Success() {
        // Given
        RefreshToken token = RefreshToken.builder()
                .tokenHash("a7f3c2e1b9d8f4a6c3e2b1d9f8a7c6e5b4d3a2c1b9d8f4a6c3e2b1d9f8a7c6e5")
                .userId("user123")
                .tokenFamilyId("family123")
                .issuedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();

        // When
        RefreshToken savedToken = refreshTokenRepository.save(token);

        // Then
        assertNotNull(savedToken.getId());
        assertEquals("a7f3c2e1b9d8f4a6c3e2b1d9f8a7c6e5b4d3a2c1b9d8f4a6c3e2b1d9f8a7c6e5", 
                savedToken.getTokenHash());
        assertEquals("user123", savedToken.getUserId());
        assertEquals("family123", savedToken.getTokenFamilyId());
        assertNotNull(savedToken.getIssuedAt());
        assertNotNull(savedToken.getExpiresAt());
        assertFalse(savedToken.isRevoked());
        assertNull(savedToken.getRevokedAt());
        assertNull(savedToken.getReplacedByTokenId());
        assertNull(savedToken.getRevocationReason());
        assertNotNull(savedToken.getCreatedAt());
        assertNotNull(savedToken.getUpdatedAt());
    }

    @Test
    void testSaveRefreshToken_DuplicateTokenHash_ThrowsException() {
        // Given - First token
        String duplicateHash = "duplicate_hash_" + UUID.randomUUID();
        RefreshToken token1 = createToken(duplicateHash, "user1", "family1");
        refreshTokenRepository.save(token1);

        // When - Second token with same hash
        RefreshToken token2 = createToken(duplicateHash, "user2", "family2");

        // Then - Should throw DuplicateKeyException
        assertThrows(DuplicateKeyException.class, () -> {
            refreshTokenRepository.save(token2);
        });
    }

    @Test
    void testFindByTokenHash_Found() {
        // Given
        String tokenHash = "hash_" + UUID.randomUUID();
        RefreshToken token = createAndSaveToken(tokenHash, "user123", "family123");

        // When
        Optional<RefreshToken> found = refreshTokenRepository.findByTokenHash(tokenHash);

        // Then
        assertTrue(found.isPresent());
        assertEquals(tokenHash, found.get().getTokenHash());
        assertEquals(token.getId(), found.get().getId());
    }

    @Test
    void testFindByTokenHash_NotFound() {
        // When
        Optional<RefreshToken> found = refreshTokenRepository.findByTokenHash("nonexistent_hash");

        // Then
        assertFalse(found.isPresent());
    }

    @Test
    void testExistsByTokenHash() {
        // Given
        String tokenHash = "hash_" + UUID.randomUUID();
        createAndSaveToken(tokenHash, "user123", "family123");

        // When & Then
        assertTrue(refreshTokenRepository.existsByTokenHash(tokenHash));
        assertFalse(refreshTokenRepository.existsByTokenHash("nonexistent_hash"));
    }

    @Test
    void testFindByUserId() {
        // Given
        String userId = "user_" + UUID.randomUUID();
        createAndSaveToken("hash1_" + UUID.randomUUID(), userId, "family1");
        createAndSaveToken("hash2_" + UUID.randomUUID(), userId, "family2");
        createAndSaveToken("hash3_" + UUID.randomUUID(), "other_user", "family3");

        // When
        List<RefreshToken> userTokens = refreshTokenRepository.findByUserId(userId);

        // Then
        assertEquals(2, userTokens.size());
        assertTrue(userTokens.stream().allMatch(t -> t.getUserId().equals(userId)));
    }

    @Test
    void testFindByUserIdAndRevoked() {
        // Given
        String userId = "user_" + UUID.randomUUID();
        createAndSaveToken("hash1_" + UUID.randomUUID(), userId, "family1", false);
        createAndSaveToken("hash2_" + UUID.randomUUID(), userId, "family2", false);
        createAndSaveRevokedToken("hash3_" + UUID.randomUUID(), userId, "family3");

        // When
        List<RefreshToken> activeTokens = refreshTokenRepository.findByUserIdAndRevoked(userId, false);
        List<RefreshToken> revokedTokens = refreshTokenRepository.findByUserIdAndRevoked(userId, true);

        // Then
        assertEquals(2, activeTokens.size());
        assertEquals(1, revokedTokens.size());
        assertTrue(activeTokens.stream().noneMatch(RefreshToken::isRevoked));
        assertTrue(revokedTokens.stream().allMatch(RefreshToken::isRevoked));
    }

    @Test
    void testFindByTokenFamilyId() {
        // Given
        String familyId = "family_" + UUID.randomUUID();
        createAndSaveToken("hash1_" + UUID.randomUUID(), "user1", familyId);
        createAndSaveToken("hash2_" + UUID.randomUUID(), "user2", familyId);
        createAndSaveToken("hash3_" + UUID.randomUUID(), "user3", familyId);
        createAndSaveToken("hash4_" + UUID.randomUUID(), "user4", "other_family");

        // When
        List<RefreshToken> familyTokens = refreshTokenRepository.findByTokenFamilyId(familyId);

        // Then
        assertEquals(3, familyTokens.size());
        assertTrue(familyTokens.stream().allMatch(t -> t.getTokenFamilyId().equals(familyId)));
    }

    @Test
    void testFindByTokenFamilyIdAndRevoked() {
        // Given
        String familyId = "family_" + UUID.randomUUID();
        createAndSaveToken("hash1_" + UUID.randomUUID(), "user1", familyId, false);
        createAndSaveToken("hash2_" + UUID.randomUUID(), "user2", familyId, false);
        createAndSaveRevokedToken("hash3_" + UUID.randomUUID(), "user3", familyId);

        // When
        List<RefreshToken> activeTokens = refreshTokenRepository.findByTokenFamilyIdAndRevoked(
                familyId, false);
        List<RefreshToken> revokedTokens = refreshTokenRepository.findByTokenFamilyIdAndRevoked(
                familyId, true);

        // Then
        assertEquals(2, activeTokens.size());
        assertEquals(1, revokedTokens.size());
    }

    @Test
    void testFindByUserIdAndExpiresAtBefore() {
        // Given
        String userId = "user_" + UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime future = now.plusDays(7);
        LocalDateTime past = now.minusDays(1);

        // Expired token
        createAndSaveTokenWithExpiry("hash1_" + UUID.randomUUID(), userId, "family1", past);
        // Valid token
        createAndSaveTokenWithExpiry("hash2_" + UUID.randomUUID(), userId, "family2", future);

        // When
        List<RefreshToken> expiredTokens = refreshTokenRepository.findByUserIdAndExpiresAtBefore(
                userId, now);

        // Then
        assertEquals(1, expiredTokens.size());
        assertTrue(expiredTokens.get(0).getExpiresAt().isBefore(now));
    }

    @Test
    void testDeleteByUserId() {
        // Given
        String userId = "user_" + UUID.randomUUID();
        createAndSaveToken("hash1_" + UUID.randomUUID(), userId, "family1");
        createAndSaveToken("hash2_" + UUID.randomUUID(), userId, "family2");
        createAndSaveToken("hash3_" + UUID.randomUUID(), "other_user", "family3");

        // When
        long deletedCount = refreshTokenRepository.deleteByUserId(userId);

        // Then
        assertEquals(2, deletedCount);
        assertEquals(0, refreshTokenRepository.findByUserId(userId).size());
        assertEquals(1, refreshTokenRepository.count());
    }

    @Test
    void testDeleteByTokenFamilyId() {
        // Given
        String familyId = "family_" + UUID.randomUUID();
        createAndSaveToken("hash1_" + UUID.randomUUID(), "user1", familyId);
        createAndSaveToken("hash2_" + UUID.randomUUID(), "user2", familyId);
        createAndSaveToken("hash3_" + UUID.randomUUID(), "user3", "other_family");

        // When
        long deletedCount = refreshTokenRepository.deleteByTokenFamilyId(familyId);

        // Then
        assertEquals(2, deletedCount);
        assertEquals(0, refreshTokenRepository.findByTokenFamilyId(familyId).size());
        assertEquals(1, refreshTokenRepository.count());
    }

    @Test
    void testCountByUserIdAndRevoked() {
        // Given
        String userId = "user_" + UUID.randomUUID();
        createAndSaveToken("hash1_" + UUID.randomUUID(), userId, "family1", false);
        createAndSaveToken("hash2_" + UUID.randomUUID(), userId, "family2", false);
        createAndSaveRevokedToken("hash3_" + UUID.randomUUID(), userId, "family3");

        // When
        long activeCount = refreshTokenRepository.countByUserIdAndRevoked(userId, false);
        long revokedCount = refreshTokenRepository.countByUserIdAndRevoked(userId, true);

        // Then
        assertEquals(2, activeCount);
        assertEquals(1, revokedCount);
    }

    @Test
    void testCountByTokenFamilyId() {
        // Given
        String familyId = "family_" + UUID.randomUUID();
        createAndSaveToken("hash1_" + UUID.randomUUID(), "user1", familyId);
        createAndSaveToken("hash2_" + UUID.randomUUID(), "user2", familyId);
        createAndSaveToken("hash3_" + UUID.randomUUID(), "user3", familyId);

        // When
        long count = refreshTokenRepository.countByTokenFamilyId(familyId);

        // Then
        assertEquals(3, count);
    }

    @Test
    void testRevokedTokenWithMetadata() {
        // Given
        RefreshToken token = RefreshToken.builder()
                .tokenHash("revoked_hash_" + UUID.randomUUID())
                .userId("user123")
                .tokenFamilyId("family123")
                .issuedAt(LocalDateTime.now().minusDays(1))
                .expiresAt(LocalDateTime.now().plusDays(6))
                .revoked(true)
                .revokedAt(LocalDateTime.now())
                .replacedByTokenId("new_token_id_123")
                .revocationReason("token_rotation")
                .build();

        // When
        RefreshToken savedToken = refreshTokenRepository.save(token);

        // Then
        assertTrue(savedToken.isRevoked());
        assertNotNull(savedToken.getRevokedAt());
        assertEquals("new_token_id_123", savedToken.getReplacedByTokenId());
        assertEquals("token_rotation", savedToken.getRevocationReason());
    }

    @Test
    void testTokenHashExcludedFromToString() {
        // Given
        RefreshToken token = RefreshToken.builder()
                .tokenHash("secret_hash_should_not_appear_in_logs")
                .userId("user123")
                .tokenFamilyId("family123")
                .issuedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();

        // When
        String tokenString = token.toString();

        // Then
        assertFalse(tokenString.contains("secret_hash_should_not_appear_in_logs"));
        assertFalse(tokenString.contains("tokenHash"));
        assertTrue(tokenString.contains("user123"));
    }

    @Test
    void testAuditFieldsAutoPopulated() throws InterruptedException {
        // Given
        RefreshToken token = RefreshToken.builder()
                .tokenHash("audit_hash_" + UUID.randomUUID())
                .userId("user123")
                .tokenFamilyId("family123")
                .issuedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();

        // When
        RefreshToken savedToken = refreshTokenRepository.save(token);
        LocalDateTime createdAt = savedToken.getCreatedAt();
        assertNotNull(createdAt);

        // Wait a bit to ensure updatedAt changes
        Thread.sleep(100);

        // Update token
        savedToken.setRevoked(true);
        savedToken.setRevokedAt(LocalDateTime.now());
        RefreshToken updatedToken = refreshTokenRepository.save(savedToken);

        // Then
        assertNotNull(updatedToken.getCreatedAt());
        assertNotNull(updatedToken.getUpdatedAt());
        assertEquals(createdAt, updatedToken.getCreatedAt());
        assertTrue(updatedToken.getUpdatedAt().isAfter(updatedToken.getCreatedAt()) ||
                   updatedToken.getUpdatedAt().isEqual(updatedToken.getCreatedAt()));
    }

    @Test
    void testExpiresAtBsonDateMapping() {
        // Given
        LocalDateTime expiry = LocalDateTime.now().plusDays(7);
        RefreshToken token = RefreshToken.builder()
                .tokenHash("expiry_hash_" + UUID.randomUUID())
                .userId("user123")
                .tokenFamilyId("family123")
                .issuedAt(LocalDateTime.now())
                .expiresAt(expiry)
                .revoked(false)
                .build();

        // When
        RefreshToken savedToken = refreshTokenRepository.save(token);
        RefreshToken retrievedToken = refreshTokenRepository.findById(savedToken.getId()).orElseThrow();

        // Then
        assertNotNull(retrievedToken.getExpiresAt());
        // Allow for some millisecond precision loss in date conversion
        assertTrue(Math.abs(
                java.time.Duration.between(expiry, retrievedToken.getExpiresAt()).toMillis()
        ) < 1000);
    }

    @Test
    void testTTLIndexExists() {
        // This test verifies that the TTL index is defined on the expiresAt field
        // Note: We don't test actual TTL deletion as it's asynchronous and takes time
        
        // When
        MongoCollection<Document> collection = mongoClient
                .getDatabase(mongoTemplate.getDb().getName())
                .getCollection("refresh_tokens");

        // Then
        boolean ttlIndexFound = false;
        for (Document index : collection.listIndexes()) {
            Document key = (Document) index.get("key");
            if (key != null && key.containsKey("expiresAt")) {
                // Check if expireAfterSeconds is set
                if (index.containsKey("expireAfterSeconds")) {
                    ttlIndexFound = true;
                    assertEquals(0, index.getInteger("expireAfterSeconds"));
                }
            }
        }

        assertTrue(ttlIndexFound, "TTL index on expiresAt field should exist");
    }

    @Test
    void testUniqueIndexOnTokenHash() {
        // This test verifies that a unique index exists on tokenHash field
        
        // When
        MongoCollection<Document> collection = mongoClient
                .getDatabase(mongoTemplate.getDb().getName())
                .getCollection("refresh_tokens");

        // Then
        boolean uniqueIndexFound = false;
        for (Document index : collection.listIndexes()) {
            Document key = (Document) index.get("key");
            if (key != null && key.containsKey("tokenHash")) {
                Boolean unique = index.getBoolean("unique");
                if (unique != null && unique) {
                    uniqueIndexFound = true;
                }
            }
        }

        assertTrue(uniqueIndexFound, "Unique index on tokenHash field should exist");
    }

    // Helper methods

    private RefreshToken createToken(String tokenHash, String userId, String familyId) {
        return RefreshToken.builder()
                .tokenHash(tokenHash)
                .userId(userId)
                .tokenFamilyId(familyId)
                .issuedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();
    }

    private RefreshToken createToken(String tokenHash, String userId, String familyId, boolean revoked) {
        RefreshToken.RefreshTokenBuilder builder = RefreshToken.builder()
                .tokenHash(tokenHash)
                .userId(userId)
                .tokenFamilyId(familyId)
                .issuedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(revoked);

        if (revoked) {
            builder.revokedAt(LocalDateTime.now())
                   .revocationReason("test_revocation");
        }

        return builder.build();
    }

    private RefreshToken createAndSaveToken(String tokenHash, String userId, String familyId) {
        RefreshToken token = createToken(tokenHash, userId, familyId);
        return refreshTokenRepository.save(token);
    }

    private RefreshToken createAndSaveToken(String tokenHash, String userId, String familyId, 
                                            boolean revoked) {
        RefreshToken token = createToken(tokenHash, userId, familyId, revoked);
        return refreshTokenRepository.save(token);
    }

    private RefreshToken createAndSaveRevokedToken(String tokenHash, String userId, String familyId) {
        RefreshToken token = RefreshToken.builder()
                .tokenHash(tokenHash)
                .userId(userId)
                .tokenFamilyId(familyId)
                .issuedAt(LocalDateTime.now().minusDays(1))
                .expiresAt(LocalDateTime.now().plusDays(6))
                .revoked(true)
                .revokedAt(LocalDateTime.now())
                .revocationReason("test_revocation")
                .build();
        return refreshTokenRepository.save(token);
    }

    private RefreshToken createAndSaveTokenWithExpiry(String tokenHash, String userId, 
                                                       String familyId, LocalDateTime expiresAt) {
        RefreshToken token = RefreshToken.builder()
                .tokenHash(tokenHash)
                .userId(userId)
                .tokenFamilyId(familyId)
                .issuedAt(LocalDateTime.now().minusDays(7))
                .expiresAt(expiresAt)
                .revoked(false)
                .build();
        return refreshTokenRepository.save(token);
    }
}
