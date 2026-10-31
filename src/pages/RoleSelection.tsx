import { useNavigate } from 'react-router-dom';
import { UserRound, Radio, Ambulance, Building2 } from 'lucide-react';

export default function RoleSelection() {
  const navigate = useNavigate();

  const roles = [
    {
      id: 'senior',
      title: 'Senior Citizen',
      icon: UserRound,
      path: '/senior',
      description: 'Request emergency assistance'
    },
    {
      id: 'emergency',
      title: 'Emergency Response',
      icon: Radio,
      path: '/emergency',
      description: 'Manage emergency requests'
    },
    {
      id: 'transport',
      title: 'Transport Partner',
      icon: Ambulance,
      path: '/transport',
      description: 'Coordinate patient transport'
    },
    {
      id: 'hospital',
      title: 'Hospital Operations',
      icon: Building2,
      path: '/hospital',
      description: 'Manage incoming patients'
    }
  ];

  return (
    <div className="container">
      <div style={{ textAlign: 'center', marginBottom: '3rem' }}>
        <h1 style={{ fontSize: '2.5rem', color: 'var(--navy-900)', marginBottom: '0.5rem' }}>
          AAYUSH (आयुष)
        </h1>
        <p style={{ fontSize: '1.25rem', color: 'var(--text-secondary)' }}>
          Senior-Friendly Emergency Assistance & Coordination Platform
        </p>
      </div>

      <div className="card card-lg" style={{ maxWidth: '900px', margin: '0 auto' }}>
        <h2 style={{ textAlign: 'center', marginBottom: '1.5rem' }}>Select Your Role</h2>
        <div className="role-grid">
          {roles.map(role => {
            const Icon = role.icon;
            return (
              <button
                key={role.id}
                className="role-card"
                onClick={() => navigate(role.path)}
              >
                <Icon className="role-icon" />
                <div>
                  <h3 className="role-title">{role.title}</h3>
                  <p style={{ color: 'var(--text-secondary)', margin: '0.5rem 0 0' }}>
                    {role.description}
                  </p>
                </div>
              </button>
            );
          })}
        </div>
      </div>

      <div className="alert alert-info" style={{ maxWidth: '900px', margin: '2rem auto 0' }}>
        <strong>Demonstration Prototype</strong>
        <p style={{ margin: '0.5rem 0 0' }}>
          This is a hackathon prototype for InnovateX 2.0. All assessments are illustrative and not for actual medical use.
          Always contact local emergency services (108, 102, 112) for real emergencies.
        </p>
      </div>
    </div>
  );
}
