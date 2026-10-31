import { useState, useEffect } from 'react';
import { Ambulance, MapPin, Clock, User, AlertCircle, Phone, CheckCircle } from 'lucide-react';
import type { EmergencySession } from '../types';
import { loadSessions, acceptTransport, updateSessionStatus } from '../services/sessionService';
import { coordinationService } from '../services/coordinationService';

export default function TransportPartnerDashboard() {
  const [sessions, setSessions] = useState<EmergencySession[]>([]);
  const [selectedSession, setSelectedSession] = useState<EmergencySession | null>(null);

  const loadData = () => {
    const allSessions = loadSessions();
    setSessions(allSessions);
  };

  useEffect(() => {
    loadData();

    const unsubscribe = coordinationService.subscribe((event) => {
      if (event.type === 'SESSION_CREATED' || event.type === 'STATUS_UPDATED') {
        loadData();
      }
    });

    const interval = setInterval(loadData, 2000);

    return () => {
      unsubscribe();
      clearInterval(interval);
    };
  }, []);

  const handleAccept = (session: EmergencySession) => {
    const vehicleId = `AMB-${Math.floor(Math.random() * 1000)}`;
    const driverName = 'Demo Driver';
    acceptTransport(session.id, vehicleId, driverName);
    setSelectedSession(session);
    loadData();
  };

  const handleStatusUpdate = (sessionId: string, status: EmergencySession['status']) => {
    updateSessionStatus(sessionId, status, 'transport');
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

  const getStatusWorkflow = (status: EmergencySession['status']) => {
    const stages = [
      { key: 'TRANSPORT_ASSIGNED', label: 'Assigned' },
      { key: 'EN_ROUTE', label: 'En Route' },
      { key: 'ARRIVED_AT_PATIENT', label: 'Arrived' },
      { key: 'PATIENT_PICKED_UP', label: 'Picked Up' },
      { key: 'EN_ROUTE_TO_HOSPITAL', label: 'To Hospital' },
      { key: 'AT_HOSPITAL', label: 'At Hospital' }
    ];

    const currentIndex = stages.findIndex(s => s.key === status);

    return (
      <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap', marginTop: '1rem' }}>
        {stages.map((stage, idx) => (
          <div
            key={stage.key}
            style={{
              flex: 1,
              minWidth: '100px',
              padding: '0.75rem',
              borderRadius: '8px',
              textAlign: 'center',
              background: idx <= currentIndex ? '#4caf50' : '#e0e0e0',
              color: idx <= currentIndex ? 'white' : '#666',
              fontWeight: idx === currentIndex ? 'bold' : 'normal',
              fontSize: '0.85rem',
              border: idx === currentIndex ? '2px solid #2e7d32' : 'none'
            }}
          >
            {stage.label}
          </div>
        ))}
      </div>
    );
  };

  const availableActions = (session: EmergencySession) => {
    switch (session.status) {
      case 'CREATED':
      case 'ACKNOWLEDGED':
        return [{ label: 'Accept Emergency', status: 'TRANSPORT_ASSIGNED' as const, icon: CheckCircle }];
      case 'TRANSPORT_ASSIGNED':
        return [{ label: 'Start En Route', status: 'EN_ROUTE' as const, icon: Ambulance }];
      case 'EN_ROUTE':
        return [{ label: 'Mark Arrived', status: 'ARRIVED_AT_PATIENT' as const, icon: MapPin }];
      case 'ARRIVED_AT_PATIENT':
        return [{ label: 'Patient Picked Up', status: 'PATIENT_PICKED_UP' as const, icon: User }];
      case 'PATIENT_PICKED_UP':
        return [{ label: 'En Route to Hospital', status: 'EN_ROUTE_TO_HOSPITAL' as const, icon: Ambulance }];
      case 'EN_ROUTE_TO_HOSPITAL':
        return [{ label: 'Arrived at Hospital', status: 'AT_HOSPITAL' as const, icon: CheckCircle }];
      default:
        return [];
    }
  };

  return (
    <div className="container transport-view">
      <div style={{ marginBottom: '2rem' }}>
        <h1 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <Ambulance size={40} />
          Transport Partner Dashboard
        </h1>
        <p style={{ color: 'var(--text-secondary)', fontSize: '1.1rem' }}>
          Emergency transport coordination and dispatch
        </p>
      </div>

      <div className="alert alert-info" style={{ marginBottom: '2rem' }}>
        <strong>Real-time Coordination:</strong> Emergency requests appear automatically across all dashboards. 
        Updates sync in real-time using BroadcastChannel API.
      </div>

      {sessions.length === 0 && (
        <div className="card" style={{ textAlign: 'center', padding: '3rem' }}>
          <Ambulance size={64} style={{ color: '#ccc', margin: '0 auto 1rem' }} />
          <h3>No Active Emergency Requests</h3>
          <p style={{ color: '#666' }}>Waiting for emergency coordination alerts...</p>
        </div>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(350px, 1fr))', gap: '1.5rem' }}>
        {sessions.map((session) => (
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

            <div style={{ marginBottom: '1rem', padding: '0.75rem', background: '#f9f9f9', borderRadius: '8px', fontSize: '0.95rem' }}>
              <strong>Symptoms:</strong> {session.symptoms.substring(0, 100)}...
            </div>

            <div style={{ display: 'flex', gap: '1rem', fontSize: '0.9rem', marginBottom: '1rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Clock size={16} />
                {new Date(session.createdAt).toLocaleTimeString()}
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Phone size={16} />
                {session.patientProfile.emergencyContact}
              </div>
            </div>

            {session.location && (
              <div style={{ marginBottom: '1rem', padding: '0.75rem', background: '#e3f2fd', borderRadius: '8px', fontSize: '0.9rem' }}>
                <MapPin size={16} style={{ display: 'inline', marginRight: '0.5rem' }} />
                <strong>Location (Simulated):</strong> {session.location.lat.toFixed(4)}, {session.location.lng.toFixed(4)}
              </div>
            )}

            <div style={{ marginBottom: '1rem', padding: '0.75rem', background: '#fff3e0', borderRadius: '8px' }}>
              <strong>Status:</strong> <span style={{ fontWeight: 'bold', color: '#e65100' }}>{session.status.replace(/_/g, ' ')}</span>
            </div>

            {selectedSession?.id === session.id && (
              <div style={{ marginTop: '1rem', padding: '1rem', background: '#f5f5f5', borderRadius: '8px' }}>
                <h4>Status Workflow:</h4>
                {getStatusWorkflow(session.status)}

                <div style={{ marginTop: '1.5rem' }}>
                  <h4>Actions:</h4>
                  <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                    {availableActions(session).map((action) => (
                      <button
                        key={action.status}
                        className="btn btn-primary"
                        onClick={(e) => {
                          e.stopPropagation();
                          if (session.status === 'CREATED' || session.status === 'ACKNOWLEDGED') {
                            handleAccept(session);
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
                </div>

                <div style={{ marginTop: '1rem', padding: '1rem', background: '#fff', borderRadius: '8px', border: '1px solid #ddd' }}>
                  <strong>Patient Details:</strong>
                  <p style={{ margin: '0.5rem 0 0', fontSize: '0.9rem' }}>
                    Age: {session.patientProfile.age} years<br />
                    Allergies: {session.patientProfile.allergies || 'None'}<br />
                    Contact: {session.patientProfile.emergencyContact}
                  </p>
                </div>

                <div className="alert alert-warning" style={{ marginTop: '1rem', fontSize: '0.85rem' }}>
                  <AlertCircle size={16} />
                  DEMO: All locations and routes are simulated. No real ambulance has been dispatched.
                </div>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
