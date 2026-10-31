package com.aayush.backend.repository;

import com.aayush.backend.document.EmergencySession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Emergency Session document
 */
@Repository
public interface EmergencySessionRepository extends MongoRepository<EmergencySession, String> {

    /**
     * Find emergency session by session ID
     */
    Optional<EmergencySession> findBySessionId(String sessionId);

    /**
     * Find all sessions ordered by creation date (newest first)
     */
    List<EmergencySession> findAllByOrderByCreatedAtDesc();

    /**
     * Find sessions by status
     */
    List<EmergencySession> findByStatusOrderByCreatedAtDesc(String status);

    /**
     * Find sessions by urgency level
     */
    List<EmergencySession> findByUrgencyLevelOrderByCreatedAtDesc(String urgencyLevel);

    /**
     * Check if session ID already exists
     */
    boolean existsBySessionId(String sessionId);

    /**
     * Count sessions by status
     */
    long countByStatus(String status);

    /**
     * Count sessions by urgency level and status not completed
     */
    long countByUrgencyLevelAndStatusNot(String urgencyLevel, String status);
}
