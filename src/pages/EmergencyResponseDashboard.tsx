import { useState, useEffect } from 'react';
import { AlertCircle, CheckCircle, Clock } from 'lucide-react';
import type { EmergencySession, SessionStatus } from '../types';
import { loadSessions, updateSessionStatus } from '../services/sessionService';

export default function EmergencyResponseDashboard() {
  const [sessions, setSessions] = useState<EmergencySession[]>([]);

  useEffect(() => {
    loadSessionsData();
    // Reload every 5 seconds to simulate real-time updates
    const interval = setInterval(loadSessionsData, 5000);
    return () => clearInterval(interval);
  }, []);

  const loadSessionsData = () => {
    setSessions(loadSessions());
  };

  const handleStatusUpdate = (sessionId: string, newStatus: SessionStatus) => {
    updateSessionStatus(sessionId, newStatus);
    loadSessionsData();
  };

  const activeSessions = sessions.filter(s => 
    s.status !== 'COMPLETED'
  );

  const getStatusDisplay = (status: SessionStatus): string => {
    const statusMap: Record<SessionStatus, string> = {
      'CREATED': 'New Request',
      'ACKNOWLEDGED': 'Acknowledged',
      'TRANSPORT_ASSIGNED': 'Transport Assigned',
      'EN_ROUTE': 'En Route',
      'ARRIVED_AT_PATIENT': 'Arrived at Patient',
      'PATIENT_PICKED_UP': 'Patient Picked Up',
      'EN_ROUTE_TO_HOSPITAL': 'En Route to Hospital',
      'AT_HOSPITAL': 'At Hospital',
      'COMPLETED': 'Completed'
    };
    return statusMap[status];
  };

  const stats = {
    active: activeSessions.length,
    critical: sessions.filter(s => s.triageAssessment.urgencyLevel === 'RED' && s.status !== 'COMPLETED').length,
    urgent: sessions.filter(s => s.triageAssessment.urgencyLevel === 'ORANGE' && s.status !== 'COMPLETED').length
  };

  return (
    <div className="container">
      <div style={{ marginBottom: '2rem' }}>
        <h1>Emergency Response Dashboard</h1>
        <p style={{ color: 'var(--text-secondary)', fontSize: '1.125rem' }}>
          Manage and coordinate emergency requests
        </p>
      </div>

      <div className="dashboard-grid">
        <div className="stat-card">
          <div className="stat-label">Active Requests</div>
          <div className="stat-value">{stats.active}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Critical (Demo)</div>
          <div className="stat-value" style={{ color: 'var(--red-600)' }}>{stats.critical}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Urgent (Demo)</div>
          <div className="stat-value" style={{ color: 'var(--orange-600)' }}>{stats.urgent}</div>
        </div>
      </div>

      <div className="alert alert-info">
        <strong>Demo Dashboard</strong> - All data shown is simulated for demonstration purposes. 
        This does not represent actual emergency dispatch or medical triage.
      </div>

      {activeSessions.length === 0 ? (
        <div className="empty-state">
          <CheckCircle className="empty-state-icon" />
          <h3>No Active Emergency Requests</h3>
          <p>All current requests have been processed</p>
        </div>
      ) : (
        <div className="session-list">
          {activeSessions.map(session => (
            <div key={session.id} className="session-card">
              <div className="session-header">
                <div>
                  <div className="session-id">{session.id}</div>
                  <div style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginTop: '0.25rem' }}>
                    <Clock size={14} style={{ display: 'inline', marginRight: '0.25rem' }} />
                    {new Date(session.createdAt).toLocaleString()}
                  </div>
                </div>
                <div>
                  <span className={`urgency-badge urgency-${session.triageAssessment.urgencyLevel}`}>
                    <AlertCircle size={16} />
                    {session.triageAssessment.urgencyLevel}
                  </span>
                </div>
              </div>

              <div className="session-meta">
                <p><strong>Patient:</strong> {session.patientProfile.name}, {session.patientProfile.age} years</p>
                <p><strong>Contact:</strong> {session.patientProfile.emergencyContact}</p>
                <p><strong>Allergies:</strong> {session.patientProfile.allergies || 'None reported'}</p>
                <p><strong>Status:</strong> <span className="status-badge">{getStatusDisplay(session.status)}</span></p>
              </div>

              <div style={{ marginTop: '1rem' }}>
                <h4>Symptoms</h4>
                <p style={{ padding: '0.75rem', background: 'var(--gray-50)', borderRadius: '0.375rem', fontSize: '0.95rem' }}>
                  {session.symptoms}
                </p>
              </div>

              <div style={{ marginTop: '1rem' }}>
                <h4>Demo Triage Assessment</h4>
                <p style={{ fontSize: '0.95rem' }}>{session.triageAssessment.reasoning}</p>
                {session.triageAssessment.matchedKeywords.length > 0 && (
                  <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
                    Keywords: {session.triageAssessment.matchedKeywords.join(', ')}
                  </p>
                )}
              </div>

              <div style={{ marginTop: '1rem' }}>
                <h4>Handover Summary</h4>
                <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
                  {session.handoverSummary}
                </p>
              </div>

              <div className="session-actions">
                {session.status === 'CREATED' && (
                  <button
                    className="btn btn-primary"
                    onClick={() => handleStatusUpdate(session.id, 'ACKNOWLEDGED')}
                  >
                    Acknowledge Request
                  </button>
                )}
                {session.status === 'ACKNOWLEDGED' && (
                  <button
                    className="btn btn-primary"
                    onClick={() => handleStatusUpdate(session.id, 'TRANSPORT_ASSIGNED')}
                  >
                    Assign Transport (Demo)
                  </button>
                )}
                {session.status === 'TRANSPORT_ASSIGNED' && (
                  <button
                    className="btn btn-primary"
                    onClick={() => handleStatusUpdate(session.id, 'EN_ROUTE')}
                  >
                    Mark En Route (Demo)
                  </button>
                )}
                {session.status === 'EN_ROUTE' && (
                  <button
                    className="btn btn-primary"
                    onClick={() => handleStatusUpdate(session.id, 'ARRIVED_AT_PATIENT')}
                  >
                    Mark Arrived (Demo)
                  </button>
                )}
                {session.status === 'ARRIVED_AT_PATIENT' && (
                  <button
                    className="btn btn-primary"
                    onClick={() => handleStatusUpdate(session.id, 'AT_HOSPITAL')}
                  >
                    At Hospital (Demo)
                  </button>
                )}
                {session.status === 'AT_HOSPITAL' && (
                  <button
                    className="btn btn-primary"
                    onClick={() => handleStatusUpdate(session.id, 'COMPLETED')}
                  >
                    Complete Session
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
