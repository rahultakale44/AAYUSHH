import { useNavigate } from 'react-router-dom';
import { User, Ambulance, Hospital, Home } from 'lucide-react';

export default function DemoAccess() {
  const navigate = useNavigate();

  const roles = [
    {
      id: 'senior',
      title: 'Senior Citizen',
      icon: User,
      path: '/senior',
      color: '#2196f3',
      description: 'Request emergency assistance with voice or text input'
    },
    {
      id: 'transport',
      title: 'Transport Partner',
      icon: Ambulance,
      path: '/transport',
      color: '#f44336',
      description: 'View and manage emergency transport requests'
    },
    {
      id: 'hospital',
      title: 'Hospital Operations',
      icon: Hospital,
      path: '/hospital',
      color: '#4caf50',
      description: 'Receive and process incoming patient handovers'
    }
  ];

  return (
    <div className="demo-access-page">
      <div className="demo-access-header">
        <button onClick={() => navigate('/')} className="btn btn-secondary btn-sm">
          <Home size={18} />
          Back to Home
        </button>
      </div>

      <div className="container">
        <div className="demo-access-intro">
          <h1 style={{ fontSize: '2.5rem', textAlign: 'center' }}>AAYUSH Platform</h1>
          <p className="demo-access-subtitle" style={{ fontSize: '1.3rem', textAlign: 'center', marginBottom: '2rem' }}>
            Emergency Coordination System
          </p>
        </div>

        <div className="card card-lg demo-access-card">
          <h2 style={{ textAlign: 'center', marginBottom: '2rem', fontSize: '1.75rem' }}>Select Your Role</h2>
          <div className="role-grid">
            {roles.map(role => {
              const Icon = role.icon;
              return (
                <button
                  key={role.id}
                  className="role-card"
                  onClick={() => navigate(role.path)}
                  style={{ borderTop: `4px solid ${role.color}` }}
                >
                  <Icon className="role-icon" size={56} style={{ color: role.color }} />
                  <div>
                    <h3 className="role-title" style={{ color: role.color, fontSize: '1.5rem' }}>{role.title}</h3>
                    <p style={{ color: 'var(--text-secondary)', margin: '0.75rem 0', fontSize: '1.05rem', lineHeight: '1.6' }}>
                      {role.description}
                    </p>
                    <div className="btn btn-primary" style={{ background: role.color, marginTop: '1rem', width: '100%' }}>
                      Open Dashboard →
                    </div>
                  </div>
                </button>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
}
