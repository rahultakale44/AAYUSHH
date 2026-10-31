package com.aayush.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for Emergency Session
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencySessionResponse {

    private String id;  // MongoDB ObjectId as String
    private String sessionId;
    
    // Patient Information
    private PatientProfileDTO patientProfile;
    
    // Symptoms and Assessment
    private String symptoms;
    private String language;
    private TriageAssessmentDTO triageAssessment;
    private String handoverSummary;
    
    // Session Status
    private String status;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TriageAssessmentDTO {
        private String urgencyLevel;
        private List<String> matchedKeywords;
        private String reasoning;
    }
}
