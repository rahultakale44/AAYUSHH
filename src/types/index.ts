// Core TypeScript interfaces for AAYUSH

export type Language = 'en' | 'hi' | 'mr';

export type UrgencyLevel = 'RED' | 'ORANGE' | 'GREEN';

export type SessionStatus = 
  | 'CREATED' 
  | 'ACKNOWLEDGED' 
  | 'TRANSPORT_ASSIGNED' 
  | 'EN_ROUTE' 
  | 'ARRIVED_AT_PATIENT'
  | 'PATIENT_PICKED_UP'
  | 'EN_ROUTE_TO_HOSPITAL'
  | 'AT_HOSPITAL' 
  | 'COMPLETED';

export interface PatientProfile {
  name: string;
  age: number;
  allergies: string;
  emergencyContact: string;
}

export interface TriageAssessment {
  urgencyLevel: UrgencyLevel;
  matchedKeywords: string[];
  reasoning: string;
}

export interface Location {
  lat: number;
  lng: number;
  address?: string;
}

export interface TransportInfo {
  vehicleId?: string;
  driverName?: string;
  acceptedAt?: Date;
}

export interface EmergencySession {
  id: string;
  patientProfile: PatientProfile;
  symptoms: string;
  triageAssessment: TriageAssessment;
  status: SessionStatus;
  createdAt: Date;
  updatedAt: Date;
  handoverSummary: string;
  location?: Location;
  transportInfo?: TransportInfo;
  nlpExtraction?: any;
}

export interface TransportRequest {
  sessionId: string;
  vehicleId?: string;
  driverName?: string;
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'EN_ROUTE' | 'COMPLETED';
  estimatedArrival?: string;
}

export interface HospitalHandover {
  sessionId: string;
  patientProfile: PatientProfile;
  symptoms: string;
  triageAssessment: TriageAssessment;
  receivedAt: Date;
  acknowledged: boolean;
}

export type UserRole = 'senior' | 'emergency' | 'transport' | 'hospital';
