// Real-time coordination service using BroadcastChannel for cross-tab communication
import type { EmergencySession, SessionStatus } from '../types';

const STORAGE_KEY = 'aayush_sessions';
const CHANNEL_NAME = 'aayush_coordination';

export type CoordinationEvent = 
  | { type: 'SESSION_CREATED'; session: EmergencySession }
  | { type: 'STATUS_UPDATED'; sessionId: string; status: SessionStatus; updatedBy: string }
  | { type: 'TRANSPORT_ACCEPTED'; sessionId: string; vehicleId: string; driverName: string }
  | { type: 'HOSPITAL_ACKNOWLEDGED'; sessionId: string }
  | { type: 'FAMILY_NOTIFIED'; sessionId: string; message: string };

type EventCallback = (event: CoordinationEvent) => void;

class CoordinationService {
  private channel: BroadcastChannel | null = null;
  private listeners: EventCallback[] = [];
  private storageListener: ((e: StorageEvent) => void) | null = null;

  constructor() {
    this.initChannel();
  }

  private initChannel() {
    if (typeof window === 'undefined') return;
    
    try {
      if ('BroadcastChannel' in window) {
        this.channel = new BroadcastChannel(CHANNEL_NAME);
        this.channel.onmessage = (event) => {
          this.notifyListeners(event.data);
        };
      } else {
        // Fallback: use storage events
        this.storageListener = (e: StorageEvent) => {
          if (e.key === STORAGE_KEY && e.newValue) {
            // Trigger refresh event
            this.notifyListeners({ type: 'STATUS_UPDATED', sessionId: '', status: 'CREATED', updatedBy: 'storage' });
          }
        };
        (window as Window).addEventListener('storage', this.storageListener);
      }
    } catch (error) {
      console.error('Failed to initialize coordination channel:', error);
    }
  }

  broadcast(event: CoordinationEvent) {
    try {
      if (this.channel) {
        this.channel.postMessage(event);
      }
      // Also notify local listeners
      this.notifyListeners(event);
    } catch (error) {
      console.error('Failed to broadcast event:', error);
    }
  }

  subscribe(callback: EventCallback) {
    this.listeners.push(callback);
    return () => {
      this.listeners = this.listeners.filter(cb => cb !== callback);
    };
  }

  private notifyListeners(event: CoordinationEvent) {
    this.listeners.forEach(callback => {
      try {
        callback(event);
      } catch (error) {
        console.error('Listener error:', error);
      }
    });
  }

  cleanup() {
    if (this.channel) {
      this.channel.close();
    }
    if (this.storageListener) {
      window.removeEventListener('storage', this.storageListener);
    }
    this.listeners = [];
  }
}

// Singleton instance
export const coordinationService = new CoordinationService();

// Enhanced session management with real-time coordination
export function loadSessions(): EmergencySession[] {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (!stored) return [];
    
    const sessions = JSON.parse(stored);
    return sessions.map((s: any) => ({
      ...s,
      createdAt: new Date(s.createdAt),
      updatedAt: new Date(s.updatedAt)
    }));
  } catch (error) {
    console.error('Failed to load sessions:', error);
    return [];
  }
}

export function saveSessions(sessions: EmergencySession[]): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(sessions));
  } catch (error) {
    console.error('Failed to save sessions:', error);
  }
}

export function updateSessionStatus(sessionId: string, newStatus: SessionStatus, updatedBy: string = 'system'): void {
  const sessions = loadSessions();
  const session = sessions.find(s => s.id === sessionId);
  
  if (session) {
    session.status = newStatus;
    session.updatedAt = new Date();
    saveSessions(sessions);
    
    // Broadcast status update
    coordinationService.broadcast({
      type: 'STATUS_UPDATED',
      sessionId,
      status: newStatus,
      updatedBy
    });
  }
}

export function acceptTransport(sessionId: string, vehicleId: string, driverName: string): void {
  updateSessionStatus(sessionId, 'TRANSPORT_ASSIGNED', 'transport');
  
  coordinationService.broadcast({
    type: 'TRANSPORT_ACCEPTED',
    sessionId,
    vehicleId,
    driverName
  });
}

export function acknowledgeHospital(sessionId: string): void {
  updateSessionStatus(sessionId, 'AT_HOSPITAL', 'hospital');
  
  coordinationService.broadcast({
    type: 'HOSPITAL_ACKNOWLEDGED',
    sessionId
  });
}

export function notifyFamily(sessionId: string, message: string): void {
  coordinationService.broadcast({
    type: 'FAMILY_NOTIFIED',
    sessionId,
    message
  });
}
