import { useNavigate, useLocation } from 'react-router-dom';
import { Home, ArrowLeft } from 'lucide-react';

export default function Header() {
  const navigate = useNavigate();
  const location = useLocation();
  
  const pageTitles: Record<string, string> = {
    '/senior': 'Senior Citizen Emergency Assistance',
    '/emergency': 'Emergency Response Dashboard',
    '/transport': 'Transport Partner Dashboard',
    '/hospital': 'Hospital Operations Dashboard'
  };

  const pageTitle = pageTitles[location.pathname] || 'AAYUSH (आयुष)';

  return (
    <header className="app-header">
      <div className="header-content">
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <button 
            className="btn btn-secondary btn-sm"
            onClick={() => navigate(-1)}
            title="Go back"
          >
            <ArrowLeft size={18} />
          </button>
          <h1 className="header-title">{pageTitle}</h1>
        </div>
        <div className="header-actions">
          <button 
            className="btn btn-secondary btn-sm"
            onClick={() => navigate('/')}
          >
            <Home size={18} />
            Home
          </button>
          <button 
            className="btn btn-outline btn-sm"
            onClick={() => navigate('/demo-access')}
          >
            Switch Role
          </button>
        </div>
      </div>
    </header>
  );
}
