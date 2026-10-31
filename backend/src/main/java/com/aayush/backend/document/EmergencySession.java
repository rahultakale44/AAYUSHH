package com.aayush.backend.document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Emergency Session Document
 * Represents an emergency request created by a senior citizen
 * 
 * Stored in MongoDB collection: emergency_sessions
 */
@Document(collection = "emergency_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencySession {

    @Id
    private String id;  // MongoDB ObjectId

    @NotBlank(message = "Session ID is required")
    @Indexed(unique = true)
    private String sessionId;

    // Patient Information
    @NotBlank(message = "Patient name is required")
    private String patientName;

    @NotNull(message = "Patient age is required")
    private Integer patientAge;

    private String allergies;

    @NotBlank(message = "Emergency contact is required")
    private String emergencyContact;

    // Symptoms and Assessment
    @NotBlank(message = "Symptoms are required")
    private String symptoms;

    @NotBlank(message = "Language is required")
    private String language;  // en, hi, mr

    @NotBlank(message = "Urgency level is required")
    @Indexed
    private String urgencyLevel;  // RED, ORANGE, GREEN

    private String matchedKeywords;

    private String triageReasoning;

    private String handoverSummary;

    // Session Status
    @NotBlank(message = "Status is required")
    @Indexed
    private String status;  // CREATED, ACKNOWLEDGED, TRANSPORT_ASSIGNED, etc.

    // Audit Fields
    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // Additional metadata
    private String createdBy;

    private String lastModifiedBy;
}
