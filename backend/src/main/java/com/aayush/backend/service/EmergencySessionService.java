package com.aayush.backend.service;

import com.aayush.backend.dto.*;
import com.aayush.backend.document.EmergencySession;
import com.aayush.backend.exception.ResourceNotFoundException;
import com.aayush.backend.exception.SessionIdConflictException;
import com.aayush.backend.repository.EmergencySessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Emergency Session Service
 * Handles business logic for emergency session management
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmergencySessionService {

    private final EmergencySessionRepository repository;
    private final TriageService triageService;

    /**
     * Create a new emergency session
     */
    public EmergencySessionResponse createEmergencySession(CreateEmergencySessionRequest request) {
        log.info("Creating new emergency session for patient: {}", request.getPatientProfile().getName());

        // Generate unique session ID
        String sessionId = generateSessionId();
        
        // Perform triage assessment
        EmergencySessionResponse.TriageAssessmentDTO assessment = 
            triageService.assessTriage(request.getSymptoms());
        
        // Generate handover summary
        String handoverSummary = triageService.generateHandoverSummary(
            request.getPatientProfile().getName(),
            request.getPatientProfile().getAge(),
            request.getSymptoms(),
            request.getPatientProfile().getAllergies(),
            assessment
        );

        // Create document
        EmergencySession session = EmergencySession.builder()
                .sessionId(sessionId)
                .patientName(request.getPatientProfile().getName())
                .patientAge(request.getPatientProfile().getAge())
                .allergies(request.getPatientProfile().getAllergies())
                .emergencyContact(request.getPatientProfile().getEmergencyContact())
                .symptoms(request.getSymptoms())
                .language(request.getLanguage())
                .urgencyLevel(assessment.getUrgencyLevel())
                .matchedKeywords(String.join(",", assessment.getMatchedKeywords()))
                .triageReasoning(assessment.getReasoning())
                .handoverSummary(handoverSummary)
                .status("CREATED")
                .build();

        EmergencySession saved = repository.save(session);
        
        log.info("Emergency session created: {} with urgency level: {}", 
                 saved.getSessionId(), saved.getUrgencyLevel());

        return mapToResponse(saved);
    }

    /**
     * Get emergency session by session ID
     */
    public EmergencySessionResponse getSessionBySessionId(String sessionId) {
        log.debug("Fetching session: {}", sessionId);
        
        EmergencySession session = repository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Emergency session not found: " + sessionId));

        return mapToResponse(session);
    }

    /**
     * Get all emergency sessions
     */
    public List<EmergencySessionResponse> getAllSessions() {
        log.debug("Fetching all emergency sessions");
        
        return repository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update session status
     */
    public EmergencySessionResponse updateSessionStatus(String sessionId, UpdateSessionStatusRequest request) {
        log.info("Updating session {} status to: {}", sessionId, request.getStatus());
        
        EmergencySession session = repository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Emergency session not found: " + sessionId));

        session.setStatus(request.getStatus());
        EmergencySession updated = repository.save(session);
        
        log.info("Session {} status updated successfully", sessionId);
        
        return mapToResponse(updated);
    }

    /**
     * Generate unique session ID
     */
    private String generateSessionId() {
        String sessionId;
        int attempts = 0;
        final int maxAttempts = 10;
        
        do {
            sessionId = String.format("EMR-%d-%s",
                System.currentTimeMillis(),
                generateRandomString(9).toUpperCase());
            attempts++;
            
            if (attempts >= maxAttempts) {
                throw new SessionIdConflictException("Unable to generate unique session ID after " + maxAttempts + " attempts");
            }
        } while (repository.existsBySessionId(sessionId));
        
        return sessionId;
    }

    /**
     * Generate random alphanumeric string
     */
    private String generateRandomString(int length) {
        String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder sb = new StringBuilder(length);
        java.util.Random random = new java.util.Random();
        
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return sb.toString();
    }

    /**
     * Map document to response DTO
     */
    private EmergencySessionResponse mapToResponse(EmergencySession session) {
        List<String> keywords = session.getMatchedKeywords() != null && !session.getMatchedKeywords().isEmpty()
            ? List.of(session.getMatchedKeywords().split(","))
            : List.of();

        return EmergencySessionResponse.builder()
                .id(session.getId())
                .sessionId(session.getSessionId())
                .patientProfile(PatientProfileDTO.builder()
                        .name(session.getPatientName())
                        .age(session.getPatientAge())
                        .allergies(session.getAllergies())
                        .emergencyContact(session.getEmergencyContact())
                        .build())
                .symptoms(session.getSymptoms())
                .language(session.getLanguage())
                .triageAssessment(EmergencySessionResponse.TriageAssessmentDTO.builder()
                        .urgencyLevel(session.getUrgencyLevel())
                        .matchedKeywords(keywords)
                        .reasoning(session.getTriageReasoning())
                        .build())
                .handoverSummary(session.getHandoverSummary())
                .status(session.getStatus())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }
}
