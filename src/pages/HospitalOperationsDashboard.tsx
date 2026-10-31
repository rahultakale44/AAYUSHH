import { useState, useEffect } from 'react';
import { Hospital, User, AlertCircle, Clock, FileText, CheckCircle } from 'lucide-react';
import type { EmergencySession } from '../types';
import { loadSessions, acknowledgeHospital, updateSessionStatus } from '../services/sessionService';
import { coordinationService } from '../services/coordinationService';

export default function HospitalOperationsDashboard() {
  const [sessions, setSessions] = useState<EmergencySession[]>([]);
  const [selectedSession, setSelectedSession] = useState<EmergencySession | null>(null);
  const [familyNotifications, setFamilyNotifications] = useState<Array<{ sessionId: string; message: string; timestamp: Date }>>([]);

  const loadData = () => {
    const allSessions = loadSessions();
    setSessions(allSessions);
  };

  useEffect(() => {
    loadData();

    const unsubscribe = coordinationService.subscribe((event) => {
      if (event.type === 'SESSION_CREATED' || event.type === 'STATUS_UPDATED' || event.type === 'TRANSPORT_ACCEPTED') {
        loadData();
      }
      if (event.type === 'FAMILY_NOTIFIED') {
        setFamilyNotifications(prev => [...prev, {
          sessionId: event.sessionId,
          message: event.message,
          timestamp: new Date()
        }]);
      }
    });

    const interval = setInterval(loadData, 2000);

    return () => {
      unsubscribe();
      clearInterval(interval);
    };
  }, []);

  const handleAcknowledge = (sessionId: string) => {
    acknowledgeHospital(sessionId);
    loadData();
  };

  const handleStatusUpdate = (sessionId: string, status: EmergencySession['status']) => {
    updateSessionStatus(sessionId, status, 'hospital');
    loadData();
  };

  const getUrgencyColor = (level: string) => {
    switch (level) {
      case 'RED': return '#c62828';
      case 'ORANGE': return '#e65100';
      case 'GREEN': return '#2e7d32';
      default: return '#666';
    }
  };

  const availableHospitalActions = (session: EmergencySession) => {
    switch (session.status) {
      case 'EN_ROUTE_TO_HOSPITAL':
      case 'AT_HOSPITAL':
        return [
          { label: 'Acknowledge Patient', status: 'AT_HOSPITAL' as const, icon: CheckCircle },
          { label: 'Mark Completed', status: 'COMPLETED' as const, icon: FileText }
        ];
      case 'PATIENT_PICKED_UP':
        return [{ label: 'Prepare for Arrival', status: 'AT_HOSPITAL' as const, icon: Hospital }];
      default:
        return [];
    }
  };

  const incomingSessions = sessions.filter(s => 
    ['PATIENT_PICKED_UP', 'EN_ROUTE_TO_HOSPITAL', 'AT_HOSPITAL'].includes(s.status)
  );

  const allPatientSessions = sessions;

  return (
    <div className="container hospital-view">
      <div style={{ marginBottom: '2rem' }}>
        <h1 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <Hospital size={40} />
          Hospital Operations Dashboard
        </h1>
        <p style={{ color: 'var(--text-secondary)', fontSize: '1.1rem' }}>
          Emergency patient intake and coordination
        </p>
      </div>

      <div className="alert alert-info" style={{ marginBottom: '2rem' }}>
        <strong>Real-time Coordination:</strong> Patient handovers appear automatically. Status updates sync across all dashboards.
      </div>

      {/* Family Notifications Panel */}
      {familyNotifications.length > 0 && (
        <div className="card" style={{ marginBottom: '2rem', background: '#e8f5e9', border: '2px solid #4caf50' }}>
          <h3 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem' }}>
            <AlertCircle size={24} />
            📱 Family Notifications (Simulated)
          </h3>
          <div style={{ maxHeight: '200px', overflowY: 'auto' }}>
            {familyNotifications.slice(-5).reverse().map((notif, idx) => (
              <div
                key={idx}
                style={{
                  padding: '0.75rem',
                  background: 'white',
                  borderRadius: '8px',
                  marginBottom: '0.5rem',
                  border: '1px solid #4caf50'
                }}
              >
                <div style={{ fontSize: '0.85rem', color: '#666', marginBottom: '0.25rem' }}>
                  {notif.timestamp.toLocaleTimeString()} - Session: {notif.sessionId}
                </div>
                <div style={{ fontSize: '0.95rem' }}>{notif.message}</div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Incoming Patients */}
      <div style={{ marginBottom: '2rem' }}>
        <h2 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem' }}>
          <User size={28} />
          Incoming Patients ({incomingSessions.length})
        </h2>

        {incomingSessions.length === 0 ? (
          <div className="card" style={{ textAlign: 'center', padding: '2rem', background: '#f5f5f5' }}>
            <Hospital size={48} style={{ color: '#ccc', margin: '0 auto 1rem' }} />
            <p style={{ color: '#666' }}>No incoming patients at this time</p>
          </div>
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(350px, 1fr))', gap: '1.5rem' }}>
            {incomingSessions.map((session) => (
              <div
                key={session.id}
                className="card"
                style={{
                  borderLeft: `6px solid ${getUrgencyColor(session.triageAssessment.urgencyLevel)}`,
                  cursor: 'pointer'
                }}
                onClick={() => setSelectedSession(selectedSession?.id === session.id ? null : session)}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start', marginBottom: '1rem' }}>
                  <div>
                    <h3 style={{ margin: 0 }}>{session.patientProfile.name}, {session.patientProfile.age}</h3>
                    <div style={{ fontSize: '0.85rem', color: '#666', marginTop: '0.25rem' }}>
                      Session: {session.id}
                    </div>
                  </div>
                  <span
                    style={{
                      padding: '4px 12px',
                      borderRadius: '20px',
                      fontSize: '0.9rem',
                      fontWeight: 'bold',
                      background: getUrgencyColor(session.triageAssessment.urgencyLevel),
                      color: 'white'
                    }}
                  >
                    {session.triageAssessment.urgencyLevel}
                  </span>
                </div>

                <div style={{ marginBottom: '1rem', padding: '0.75rem', background: '#f9f9f9', borderRadius: '8px' }}>
                  <strong>Status:</strong> <span style={{ fontWeight: 'bold', color: '#1976d2' }}>{session.status.replace(/_/g, ' ')}</span>
                </div>

                <div style={{ marginBottom: '1rem', padding: '0.75rem', background: '#fff3e0', borderRadius: '8px', fontSize: '0.95rem' }}>
                  <strong>Chief Complaint:</strong> {session.symptoms.substring(0, 80)}...
                </div>

                {selectedSession?.id === session.id && (
                  <div style={{ marginTop: '1rem', padding: '1rem', background: '#f5f5f5', borderRadius: '8px' }}>
                    <h4>Patient Handover Summary:</h4>
                    <div style={{ padding: '1rem', background: 'white', borderRadius: '8px', fontSize: '0.9rem', marginBottom: '1rem', whiteSpace: 'pre-wrap' }}>
                      {session.handoverSummary}
                    </div>

                    <h4>Triage Assessment:</h4>
                    <div style={{ padding: '1rem', background: session.triageAssessment.urgencyLevel === 'RED' ? '#ffebee' : session.triageAssessment.urgencyLevel === 'ORANGE' ? '#fff3e0' : '#e8f5e9', borderRadius: '8px', marginBottom: '1rem' }}>
                      <div style={{ fontWeight: 'bold', fontSize: '1.1rem', color: getUrgencyColor(session.triageAssessment.urgencyLevel), marginBottom: '0.5rem' }}>
                        {session.triageAssessment.urgencyLevel} PRIORITY
                      </div>
                      <div style={{ fontSize: '0.9rem' }}>{session.triageAssessment.reasoning}</div>
                      {session.triageAssessment.matchedKeywords.length > 0 && (
                        <div style={{ marginTop: '0.5rem', fontSize: '0.85rem' }}>
                          <strong>Keywords:</strong> {session.triageAssessment.matchedKeywords.join(', ')}
                        </div>
                      )}
                    </div>

                    {session.nlpExtraction && (
                      <div style={{ marginBottom: '1rem' }}>
                        <h4>NLP Extracted Data:</h4>
                        <pre style={{ background: '#1e1e1e', color: '#d4d4d4', padding: '0.75rem', borderRadius: '8px', fontSize: '0.8rem', overflow: 'auto' }}>
                          {JSON.stringify(session.nlpExtraction.structuredSummary, null, 2)}
                        </pre>
                      </div>
                    )}

                    <h4>Actions:</h4>
                    <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                      {availableHospitalActions(session).map((action) => (
                        <button
                          key={action.status}
                          className="btn btn-primary"
                          onClick={(e) => {
                            e.stopPropagation();
                            if (action.status === 'AT_HOSPITAL' && session.status !== 'AT_HOSPITAL') {
                              handleAcknowledge(session.id);
                            } else {
                              handleStatusUpdate(session.id, action.status);
                            }
                          }}
                          style={{ flex: 1 }}
                        >
                          <action.icon size={18} />
                          {action.label}
                        </button>
                      ))}
                    </div>

                    <div className="alert alert-warning" style={{ marginTop: '1rem', fontSize: '0.85rem' }}>
                      <AlertCircle size={16} />
                      DEMO: This is an illustrative triage assessment, not a clinical diagnosis.
                    </div>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      {/* All Emergency Sessions */}
      <div style={{ marginBottom: '2rem' }}>
        <h2 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem' }}>
          <FileText size={28} />
          All Emergency Sessions ({allPatientSessions.length})
        </h2>

        {allPatientSessions.length === 0 ? (
          <div className="card" style={{ textAlign: 'center', padding: '2rem', background: '#f5f5f5' }}>
            <FileText size={48} style={{ color: '#ccc', margin: '0 auto 1rem' }} />
            <p style={{ color: '#666' }}>No emergency sessions</p>
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
              <thead>
                <tr style={{ background: '#f5f5f5', borderBottom: '2px solid #ddd' }}>
                  <th style={{ padding: '1rem', textAlign: 'left' }}>Session ID</th>
                  <th style={{ padding: '1rem', textAlign: 'left' }}>Patient</th>
                  <th style={{ padding: '1rem', textAlign: 'left' }}>Priority</th>
                  <th style={{ padding: '1rem', textAlign: 'left' }}>Status</th>
                  <th style={{ padding: '1rem', textAlign: 'left' }}>Time</th>
                </tr>
              </thead>
              <tbody>
                {allPatientSessions.map((session) => (
                  <tr key={session.id} style={{ borderBottom: '1px solid #eee' }}>
                    <td style={{ padding: '1rem', fontSize: '0.85rem' }}>{session.id}</td>
                    <td style={{ padding: '1rem' }}>
                      <div>{session.patientProfile.name}</div>
                      <div style={{ fontSize: '0.85rem', color: '#666' }}>{session.patientProfile.age} years</div>
                    </td>
                    <td style={{ padding: '1rem' }}>
                      <span
                        style={{
                          padding: '4px 8px',
                          borderRadius: '4px',
                          fontSize: '0.85rem',
                          fontWeight: 'bold',
                          background: getUrgencyColor(session.triageAssessment.urgencyLevel),
                          color: 'white'
                        }}
                      >
                        {session.triageAssessment.urgencyLevel}
                      </span>
                    </td>
                    <td style={{ padding: '1rem', fontSize: '0.9rem' }}>{session.status.replace(/_/g, ' ')}</td>
                    <td style={{ padding: '1rem', fontSize: '0.85rem', color: '#666' }}>
                      <Clock size={14} style={{ display: 'inline', marginRight: '0.25rem' }} />
                      {new Date(session.createdAt).toLocaleTimeString()}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
