package com.aayush.backend.service;

import com.aayush.backend.dto.EmergencySessionResponse.TriageAssessmentDTO;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Triage Service
 * Provides transparent keyword-based urgency assessment
 * 
 * IMPORTANT: This is a demonstration prototype using simple keyword matching.
 * NOT a validated medical triage system or clinical decision support tool.
 */
@Service
public class TriageService {

    private static final Map<String, TriageRule> TRIAGE_RULES = new LinkedHashMap<>();

    static {
        // RED - Critical symptoms
        TRIAGE_RULES.put("RED", new TriageRule(
            "RED",
            Arrays.asList(
                "chest pain", "heart attack", "cardiac", "myocardial",
                "stroke", "paralysis", "unconscious", "not breathing",
                "severe bleeding", "head injury", "seizure"
            ),
            "Critical symptoms detected that may indicate life-threatening condition"
        ));

        // ORANGE - Urgent symptoms
        TRIAGE_RULES.put("ORANGE", new TriageRule(
            "ORANGE",
            Arrays.asList(
                "difficulty breathing", "breathless", "shortness of breath",
                "high fever", "severe pain", "broken bone", "fracture",
                "vomiting blood", "severe burn", "allergic reaction",
                "dizzy", "confusion"
            ),
            "Urgent symptoms detected that require prompt medical attention"
        ));

        // GREEN - Non-urgent symptoms
        TRIAGE_RULES.put("GREEN", new TriageRule(
            "GREEN",
            Arrays.asList(
                "fever", "cold", "cough", "headache", "stomach ache",
                "nausea", "fatigue", "weakness", "rash", "minor cut", "sprain"
            ),
            "Non-urgent symptoms detected"
        ));
    }

    /**
     * Assess triage level based on symptoms
     */
    public TriageAssessmentDTO assessTriage(String symptoms) {
        String symptomsLower = symptoms.toLowerCase();
        Set<String> matchedKeywords = new HashSet<>();
        String urgencyLevel = "GREEN";
        String reasoning = "No critical or urgent keywords detected. However, medical evaluation is still recommended.";

        // Check rules in order of priority (RED, ORANGE, GREEN)
        for (Map.Entry<String, TriageRule> entry : TRIAGE_RULES.entrySet()) {
            TriageRule rule = entry.getValue();
            
            for (String keyword : rule.keywords) {
                if (symptomsLower.contains(keyword.toLowerCase())) {
                    matchedKeywords.add(keyword);
                    
                    // Set urgency level based on highest priority match
                    if ("RED".equals(rule.level) || 
                        ("ORANGE".equals(rule.level) && !"RED".equals(urgencyLevel))) {
                        urgencyLevel = rule.level;
                        reasoning = rule.reasoning;
                    }
                }
            }
        }

        return TriageAssessmentDTO.builder()
                .urgencyLevel(urgencyLevel)
                .matchedKeywords(new ArrayList<>(matchedKeywords))
                .reasoning(reasoning)
                .build();
    }

    /**
     * Generate patient handover summary
     */
    public String generateHandoverSummary(
            String patientName,
            int age,
            String symptoms,
            String allergies,
            TriageAssessmentDTO assessment) {
        
        String allergyText = (allergies != null && !allergies.trim().isEmpty()) 
            ? String.format("Known allergies: %s.", allergies)
            : "No known allergies reported.";
        
        String keywordText = !assessment.getMatchedKeywords().isEmpty()
            ? String.format("Keywords detected: %s.", String.join(", ", assessment.getMatchedKeywords()))
            : "";
        
        return String.format(
            "DEMO PATIENT HANDOVER - %s, %d years old. Reported symptoms: %s. %s Demo triage: %s - %s. %s ⚠️ This is an illustrative demo assessment, not a clinical diagnosis.",
            patientName, age, symptoms, allergyText,
            assessment.getUrgencyLevel(), assessment.getReasoning(), keywordText
        );
    }

    /**
     * Internal class to represent triage rules
     */
    private static class TriageRule {
        String level;
        List<String> keywords;
        String reasoning;

        TriageRule(String level, List<String> keywords, String reasoning) {
            this.level = level;
            this.keywords = keywords;
            this.reasoning = reasoning;
        }
    }
}
