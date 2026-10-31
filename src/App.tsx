import { BrowserRouter as Router, Routes, Route, useLocation } from 'react-router-dom';
import './App.css';
import './landing.css';
import LandingPage from './pages/LandingPage';
import DemoAccess from './pages/DemoAccess';
import SeniorCitizenHome from './pages/SeniorCitizenHome';
import EmergencyResponseDashboard from './pages/EmergencyResponseDashboard';
import TransportPartnerDashboard from './pages/TransportPartnerDashboard';
import HospitalOperationsDashboard from './pages/HospitalOperationsDashboard';
import Header from './components/Header';

function AppContent() {
  const location = useLocation();
  const isLandingPage = location.pathname === '/';
  const isDemoAccess = location.pathname === '/demo-access';

  return (
    <>
      {!isLandingPage && !isDemoAccess && <Header />}
      <Routes>
        <Route path="/" element={<LandingPage />} />
        <Route path="/demo-access" element={<DemoAccess />} />
        <Route path="/senior" element={<SeniorCitizenHome />} />
        <Route path="/emergency" element={<EmergencyResponseDashboard />} />
        <Route path="/transport" element={<TransportPartnerDashboard />} />
        <Route path="/hospital" element={<HospitalOperationsDashboard />} />
      </Routes>
    </>
  );
}

function App() {
  return (
    <Router>
      <AppContent />
    </Router>
  );
}

export default App;
