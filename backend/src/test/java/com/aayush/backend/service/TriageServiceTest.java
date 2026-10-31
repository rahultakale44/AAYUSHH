package com.aayush.backend.service;

import com.aayush.backend.dto.EmergencySessionResponse.TriageAssessmentDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TriageService
 */
class TriageServiceTest {

    private TriageService triageService;

    @BeforeEach
    void setUp() {
        triageService = new TriageService();
    }

    @Test
    void testAssessTriage_RedLevel_ChestPain() {
        // Given
        String symptoms = "I have severe chest pain and feeling breathless";

        // When
        TriageAssessmentDTO result = triageService.assessTriage(symptoms);

        // Then
        assertEquals("RED", result.getUrgencyLevel());
        assertTrue(result.getMatchedKeywords().contains("chest pain"));
        assertNotNull(result.getReasoning());
    }

    @Test
    void testAssessTriage_OrangeLevel_HighFever() {
        // Given
        String symptoms = "I have high fever and difficulty breathing";

        // When
        TriageAssessmentDTO result = triageService.assessTriage(symptoms);

        // Then
        assertTrue(result.getUrgencyLevel().equals("RED") || result.getUrgencyLevel().equals("ORANGE"));
        assertFalse(result.getMatchedKeywords().isEmpty());
    }

    @Test
    void testAssessTriage_GreenLevel_CommonCold() {
        // Given
        String symptoms = "I have a cold and mild cough";

        // When
        TriageAssessmentDTO result = triageService.assessTriage(symptoms);

        // Then
        assertEquals("GREEN", result.getUrgencyLevel());
        assertTrue(result.getMatchedKeywords().contains("cold") || 
                   result.getMatchedKeywords().contains("cough"));
    }

    @Test
    void testAssessTriage_NoKeywords() {
        // Given
        String symptoms = "I feel slightly unwell today";

        // When
        TriageAssessmentDTO result = triageService.assessTriage(symptoms);

        // Then
        assertEquals("GREEN", result.getUrgencyLevel());
        assertTrue(result.getMatchedKeywords().isEmpty());
        assertNotNull(result.getReasoning());
    }

    @Test
    void testGenerateHandoverSummary() {
        // Given
        String patientName = "Test Patient";
        int age = 75;
        String symptoms = "chest pain";
        String allergies = "Penicillin";
        TriageAssessmentDTO assessment = triageService.assessTriage(symptoms);

        // When
        String summary = triageService.generateHandoverSummary(
            patientName, age, symptoms, allergies, assessment);

        // Then
        assertNotNull(summary);
        assertTrue(summary.contains(patientName));
        assertTrue(summary.contains(String.valueOf(age)));
        assertTrue(summary.contains(symptoms));
        assertTrue(summary.contains(allergies));
        assertTrue(summary.contains("DEMO PATIENT HANDOVER"));
    }

    @Test
    void testGenerateHandoverSummary_NoAllergies() {
        // Given
        TriageAssessmentDTO assessment = triageService.assessTriage("fever");

        // When
        String summary = triageService.generateHandoverSummary(
            "Patient", 50, "fever", null, assessment);

        // Then
        assertTrue(summary.contains("No known allergies"));
    }
}
