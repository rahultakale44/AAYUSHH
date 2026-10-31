// Local session management with browser storage and real-time coordination
import type { EmergencySession, PatientProfile } from '../types';
import { assessTriage, generateHandoverSummary } from './triageService';
import { extractSymptomPhrases } from './nlpExtractionService';
import { coordinationService, saveSessions, loadSessions, notifyFamily } from './coordinationService';

// Generate unique session ID
function generateSessionId(): string {
  return `EMR-${Date.now()}-${Math.random().toString(36).substr(2, 9).toUpperCase()}`;
}

// Create new emergency session with enhanced NLP
export function createEmergencySession(
  patientProfile: PatientProfile,
  symptoms: string
): EmergencySession {
  const triageAssessment = assessTriage(symptoms);
  const nlpExtraction = extractSymptomPhrases(symptoms);
  
  const handoverSummary = generateHandoverSummary(
    patientProfile.name,
    patientProfile.age,
    symptoms,
    patientProfile.allergies,
    triageAssessment
  );

  // Simulated location (Pune, India area)
  const location = {
    lat: 18.5204 + (Math.random() - 0.5) * 0.1,
    lng: 73.8567 + (Math.random() - 0.5) * 0.1,
    address: 'Simulated location - Demo mode'
  };

  const newSession: EmergencySession = {
    id: generateSessionId(),
    patientProfile,
    symptoms,
    triageAssessment,
    nlpExtraction,
    status: 'CREATED',
    createdAt: new Date(),
    updatedAt: new Date(),
    handoverSummary,
    location
  };

  const sessions = loadSessions();
  sessions.unshift(newSession);
  saveSessions(sessions);

  // Broadcast new session
  coordinationService.broadcast({
    type: 'SESSION_CREATED',
    session: newSession
  });

  // Simulate family notification
  setTimeout(() => {
    notifyFamily(
      newSession.id,
      `Emergency alert: ${patientProfile.name} has requested medical assistance. Status: ${newSession.status}. Session ID: ${newSession.id}`
    );
  }, 1000);

  return newSession;
}

// Get session by ID
export function getSessionById(sessionId: string): EmergencySession | undefined {
  const sessions = loadSessions();
  return sessions.find(s => s.id === sessionId);
}

// Export from coordination service
export { updateSessionStatus, acceptTransport, acknowledgeHospital } from './coordinationService';
export { loadSessions };
