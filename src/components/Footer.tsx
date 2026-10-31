import { Link } from 'react-router-dom';

export default function Footer() {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="landing-footer">
      <div className="footer-content">
        <div className="footer-grid">
          <div className="footer-section">
            <h3 className="footer-brand">
              AAYUSH <span className="footer-brand-hindi">आयुष</span>
            </h3>
            <p className="footer-description">
              Professional emergency coordination platform connecting patients, emergency response, 
              transport services, and hospitals through intelligent triage and real-time coordination.
            </p>
          </div>

          <div className="footer-section">
            <h4 className="footer-heading">Navigation</h4>
            <ul className="footer-links">
              <li><Link to="/">Home</Link></li>
              <li><a href="#services">Our Services</a></li>
              <li><a href="#how-it-works">How It Works</a></li>
              <li><a href="#why-aayush">About AAYUSH</a></li>
            </ul>
          </div>

          <div className="footer-section">
            <h4 className="footer-heading">Platform Access</h4>
            <ul className="footer-links">
              <li><Link to="/demo-access">Role Selection</Link></li>
              <li><Link to="/senior">Senior Citizen</Link></li>
              <li><Link to="/emergency">Emergency Response</Link></li>
              <li><Link to="/transport">Transport Partner</Link></li>
              <li><Link to="/hospital">Hospital Operations</Link></li>
            </ul>
          </div>

          <div className="footer-section">
            <h4 className="footer-heading">Emergency Services</h4>
            <p className="footer-emergency-info">
              For actual emergencies, please contact:
            </p>
            <ul className="footer-emergency-numbers">
              <li><strong>108</strong> - Ambulance Service</li>
              <li><strong>102</strong> - Medical Emergency</li>
              <li><strong>112</strong> - Unified Emergency</li>
            </ul>
          </div>
        </div>

        <div className="footer-bottom">
          <div className="footer-copyright">
            <p>© {currentYear} AAYUSH (आयुष). Emergency Coordination Platform.</p>
          </div>
        </div>
      </div>
    </footer>
  );
}
