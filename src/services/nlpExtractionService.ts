// Enhanced NLP extraction service with visual phrase highlighting
import type { UrgencyLevel } from '../types';

export interface ExtractedPhrase {
  phrase: string;
  category: UrgencyLevel;
  startIndex: number;
  endIndex: number;
}

export interface NLPExtraction {
  originalText: string;
  extractedPhrases: ExtractedPhrase[];
  urgencyLevel: UrgencyLevel;
  reasoning: string;
  structuredSummary: {
    primarySymptoms: string[];
    redFlags: string[];
    duration: string | null;
    severity: string | null;
  };
}

interface KeywordPattern {
  keywords: string[];
  category: UrgencyLevel;
  description: string;
}

const keywordPatterns: KeywordPattern[] = [
  {
    keywords: [
      'chest pain', 'heart attack', 'cardiac arrest', 'myocardial',
      'stroke', 'paralysis', 'unconscious', 'not breathing',
      'severe bleeding', 'heavy bleeding', 'head injury', 'severe head injury',
      'seizure', 'convulsion', 'collapsed', 'unresponsive'
    ],
    category: 'RED',
    description: 'Life-threatening symptoms requiring immediate emergency response'
  },
  {
    keywords: [
      'difficulty breathing', 'breathless', 'shortness of breath', 'can\'t breathe',
      'high fever', 'very high fever', 'severe pain', 'intense pain',
      'broken bone', 'fracture', 'dislocated', 'vomiting blood', 'blood in vomit',
      'severe burn', 'major burn', 'allergic reaction', 'severe allergy',
      'dizzy', 'dizziness', 'confusion', 'confused', 'disoriented',
      'severe abdominal pain', 'severe stomach pain', 'can\'t move'
    ],
    category: 'ORANGE',
    description: 'Urgent symptoms requiring prompt medical attention'
  },
  {
    keywords: [
      'fever', 'cold', 'cough', 'headache', 'mild headache',
      'stomach ache', 'mild pain', 'nausea', 'fatigue', 'tired',
      'weakness', 'weak', 'rash', 'skin rash', 'minor cut',
      'sprain', 'sore throat', 'runny nose', 'congestion'
    ],
    category: 'GREEN',
    description: 'Non-urgent symptoms - medical evaluation still recommended'
  }
];

export function extractSymptomPhrases(text: string): NLPExtraction {
  const textLower = text.toLowerCase();
  const extractedPhrases: ExtractedPhrase[] = [];
  const primarySymptoms: string[] = [];
  const redFlags: string[] = [];
  let highestUrgency: UrgencyLevel = 'GREEN';

  // Extract phrases by matching keywords
  for (const pattern of keywordPatterns) {
    for (const keyword of pattern.keywords) {
      const keywordLower = keyword.toLowerCase();
      let searchIndex = 0;
      
      while (true) {
        const index = textLower.indexOf(keywordLower, searchIndex);
        if (index === -1) break;
        
        // Check if we already have this range
        const overlap = extractedPhrases.some(p => 
          (index >= p.startIndex && index < p.endIndex) ||
          (index + keyword.length > p.startIndex && index + keyword.length <= p.endIndex)
        );
        
        if (!overlap) {
          extractedPhrases.push({
            phrase: text.substring(index, index + keyword.length),
            category: pattern.category,
            startIndex: index,
            endIndex: index + keyword.length
          });
          
          primarySymptoms.push(keyword);
          
          if (pattern.category === 'RED') {
            redFlags.push(keyword);
            highestUrgency = 'RED';
          } else if (pattern.category === 'ORANGE' && highestUrgency !== 'RED') {
            highestUrgency = 'ORANGE';
          }
        }
        
        searchIndex = index + keyword.length;
      }
    }
  }

  // Sort phrases by their position in text
  extractedPhrases.sort((a, b) => a.startIndex - b.startIndex);

  // Extract duration if mentioned
  const durationMatch = text.match(/(\d+)\s*(minute|hour|day|week)s?/i);
  const duration = durationMatch ? durationMatch[0] : null;

  // Extract severity indicators
  const severityMatch = text.match(/(severe|mild|moderate|extreme|intense|slight)/i);
  const severity = severityMatch ? severityMatch[0] : null;

  // Generate reasoning
  let reasoning = '';
  if (highestUrgency === 'RED') {
    reasoning = `CRITICAL: Life-threatening symptoms detected (${redFlags.join(', ')}). Requires immediate emergency response. This is a DEMONSTRATION classification only.`;
  } else if (highestUrgency === 'ORANGE') {
    reasoning = `URGENT: Serious symptoms detected requiring prompt medical attention (${[...new Set(primarySymptoms)].slice(0, 3).join(', ')}). This is a DEMONSTRATION classification only.`;
  } else {
    reasoning = `NON-URGENT: Symptoms detected that should be evaluated by medical professional. This is a DEMONSTRATION classification - professional assessment still recommended.`;
  }

  return {
    originalText: text,
    extractedPhrases,
    urgencyLevel: highestUrgency,
    reasoning,
    structuredSummary: {
      primarySymptoms: [...new Set(primarySymptoms)],
      redFlags,
      duration,
      severity
    }
  };
}

// Demo scenarios for testing
export const DEMO_SCENARIOS = {
  RED: {
    text: 'Severe chest pain and difficulty breathing for the past 20 minutes. Feel like heart is racing.',
    description: 'Critical emergency - cardiac symptoms'
  },
  ORANGE: {
    text: 'Very high fever since morning, severe headache, and feeling very dizzy. Having trouble standing up.',
    description: 'Urgent case - requires prompt attention'
  },
  GREEN: {
    text: 'Mild fever and cough for 2 days. Feeling tired and having runny nose.',
    description: 'Non-urgent - routine medical evaluation'
  }
};
