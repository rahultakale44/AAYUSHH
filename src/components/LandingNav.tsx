import { useState } from 'react';
import { Menu, X, AlertCircle } from 'lucide-react';
import { Link } from 'react-router-dom';
import type { Language } from '../types';

interface LandingNavProps {
  language: Language;
  onLanguageChange: (lang: Language) => void;
}

export default function LandingNav({ language, onLanguageChange }: LandingNavProps) {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const scrollToSection = (sectionId: string) => {
    const element = document.getElementById(sectionId);
    if (element) {
      element.scrollIntoView({ behavior: 'smooth', block: 'start' });
      setMobileMenuOpen(false);
    }
  };

  const languages: Array<{ code: Language; label: string }> = [
    { code: 'en', label: 'English' },
    { code: 'hi', label: 'हिंदी' },
    { code: 'mr', label: 'मराठी' }
  ];

  return (
    <nav className="landing-nav">
      <div className="landing-nav-content">
        <div className="nav-brand">
          <Link to="/" className="brand-link">
            <span className="brand-name">AAYUSH</span>
            <span className="brand-name-hindi">आयुष</span>
          </Link>
        </div>

        <div className={`nav-links ${mobileMenuOpen ? 'mobile-open' : ''}`}>
          <button onClick={() => scrollToSection('home')} className="nav-link">
            Home
          </button>
          <button onClick={() => scrollToSection('services')} className="nav-link">
            Our Services
          </button>
          <button onClick={() => scrollToSection('how-it-works')} className="nav-link">
            How It Works
          </button>
          <button onClick={() => scrollToSection('why-aayush')} className="nav-link">
            About AAYUSH
          </button>
          
          <div className="nav-language-selector">
            {languages.map((lang) => (
              <button
                key={lang.code}
                className={`nav-lang-btn ${language === lang.code ? 'active' : ''}`}
                onClick={() => {
                  onLanguageChange(lang.code);
                  setMobileMenuOpen(false);
                }}
              >
                {lang.label}
              </button>
            ))}
          </div>

          <Link
            to="/demo-access"
            className="btn btn-outline btn-sm"
            onClick={() => setMobileMenuOpen(false)}
          >
            Demo Access
          </Link>

          <Link
            to="/senior"
            className="btn btn-danger btn-sm"
            onClick={() => setMobileMenuOpen(false)}
          >
            <AlertCircle size={18} />
            Emergency
          </Link>
        </div>

        <button
          className="mobile-menu-toggle"
          onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
          aria-label="Toggle menu"
        >
          {mobileMenuOpen ? <X size={24} /> : <Menu size={24} />}
        </button>
      </div>
    </nav>
  );
}
