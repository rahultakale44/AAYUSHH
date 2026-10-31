// Transparent local keyword-based triage rules for demo purposes
// WARNING: This is a hackathon prototype, NOT a validated medical triage system

import type { TriageAssessment, UrgencyLevel } from '../types';

interface KeywordRule {
  keywords: string[];
  level: UrgencyLevel;
  reasoning: string;
}

const triageRules: KeywordRule[] = [
  {
    keywords: ['chest pain', 'heart attack', 'cardiac', 'myocardial', 'stroke', 'paralysis', 'unconscious', 'not breathing', 'severe bleeding', 'head injury', 'seizure'],
    level: 'RED',
    reasoning: 'Critical symptoms detected that may indicate life-threatening condition'
  },
  {
    keywords: ['difficulty breathing', 'breathless', 'shortness of breath', 'high fever', 'severe pain', 'broken bone', 'fracture', 'vomiting blood', 'severe burn', 'allergic reaction', 'dizzy', 'confusion'],
    level: 'ORANGE',
    reasoning: 'Urgent symptoms detected that require prompt medical attention'
  },
  {
    keywords: ['fever', 'cold', 'cough', 'headache', 'stomach ache', 'nausea', 'fatigue', 'weakness', 'rash', 'minor cut', 'sprain'],
    level: 'GREEN',
    reasoning: 'Non-urgent symptoms detected'
  }
];

export function assessTriage(symptoms: string): TriageAssessment {
  const symptomsLower = symptoms.toLowerCase();
  const matchedKeywords: string[] = [];
  let urgencyLevel: UrgencyLevel = 'GREEN';
  let reasoning = 'No critical or urgent keywords detected. However, medical evaluation is still recommended.';

  // Check RED level first (highest priority)
  for (const rule of triageRules) {
    for (const keyword of rule.keywords) {
      if (symptomsLower.includes(keyword.toLowerCase())) {
        matchedKeywords.push(keyword);
        if (rule.level === 'RED' || (rule.level === 'ORANGE' && urgencyLevel !== 'RED')) {
          urgencyLevel = rule.level;
          reasoning = rule.reasoning;
        }
      }
    }
  }

  return {
    urgencyLevel,
    matchedKeywords: [...new Set(matchedKeywords)], // Remove duplicates
    reasoning
  };
}

export function generateHandoverSummary(
  patientName: string,
  age: number,
  symptoms: string,
  allergies: string,
  assessment: TriageAssessment
): string {
  const allergyText = allergies.trim() ? `Known allergies: ${allergies}.` : 'No known allergies reported.';
  const keywordText = assessment.matchedKeywords.length > 0 
    ? `Keywords detected: ${assessment.matchedKeywords.join(', ')}.`
    : '';
  
  return `DEMO PATIENT HANDOVER - ${patientName}, ${age} years old. Reported symptoms: ${symptoms}. ${allergyText} Demo triage: ${assessment.urgencyLevel} - ${assessment.reasoning}. ${keywordText} ⚠️ This is an illustrative demo assessment, not a clinical diagnosis.`;
}
