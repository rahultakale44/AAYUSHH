import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  AlertCircle,
  Users,
  Languages,
  FileText,
  Ambulance,
  Building2,
  Radio,
  Clock,
  Shield,
  CheckCircle,
  ArrowRight,
  Activity,
  Stethoscope,
  User
} from 'lucide-react';
import LandingNav from '../components/LandingNav';
import Footer from '../components/Footer';
import type { Language } from '../types';
import { t } from '../services/translations';

export default function LandingPage() {
  const navigate = useNavigate();
  const [language, setLanguage] = useState<Language>('en');

  const services = [
    {
      icon: AlertCircle,
      title: 'Emergency Assistance',
      description: 'Enter symptoms and patient details. Review a transparent illustrative urgency assessment and create a demo emergency session.',
      action: 'Start Emergency Request',
      link: '/senior'
    },
    {
      icon: Ambulance,
      title: 'Transport Coordination',
      description: 'View simulated transport requests and demonstrate the ambulance assignment workflow. Clearly labeled as demonstration data.',
      action: 'View Transport Demo',
      link: '/transport'
    },
    {
      icon: Building2,
      title: 'Hospital Coordination',
      description: 'Review incoming patient handovers and demonstrate admission workflows. All facility capacity information is clearly marked as mock data.',
      action: 'View Hospital Demo',
      link: '/hospital'
    },
    {
      icon: Users,
      title: 'Senior Citizen Support',
      description: 'Large accessible controls, multilingual interface (EN/HI/MR), and optional browser voice input with typed-input fallback.',
      action: 'Try Senior Interface',
      link: '/senior'
    },
    {
      icon: FileText,
      title: 'Patient Handover',
      description: 'Organize symptoms and patient details into a concise summary shared across all demo role interfaces for coordinated care.',
      action: 'Learn More',
      link: '/demo-access'
    },
    {
      icon: Radio,
      title: 'Emergency Response Dashboard',
      description: 'Review the demo emergency queue, acknowledge requests, and update status through the complete coordination workflow.',
      action: 'View Dashboard Demo',
      link: '/emergency'
    }
  ];

  const features = [
    {
      icon: Users,
      title: 'Senior-Friendly Design',
      description: 'Large text, high contrast, and intuitive controls designed specifically for older adults'
    },
    {
      icon: Languages,
      title: 'Multilingual Support',
      description: 'Available in English, Hindi (हिंदी), and Marathi (मराठी) for better accessibility'
    },
    {
      icon: FileText,
      title: 'Coordinated Handover',
      description: 'Patient information organized and shared across emergency response, transport, and hospital teams'
    },
    {
      icon: Shield,
      title: 'Transparent Triage',
      description: 'Explainable keyword-based assessment showing which terms matched and why'
    }
  ];

  const howItWorksSteps = [
    {
      number: 1,
      title: 'Share Symptoms & Details',
      description: 'Enter patient information and describe symptoms using text or optional voice input',
      icon: Stethoscope
    },
    {
      number: 2,
      title: 'Review & Confirm',
      description: 'Review all information and see illustrative urgency indicators before creating the session',
      icon: CheckCircle
    },
    {
      number: 3,
      title: 'Coordinate Response',
      description: 'Demo session moves through simulated emergency response and transport coordination',
      icon: Activity
    },
    {
      number: 4,
      title: 'Hospital Handover',
      description: 'Patient summary shared with hospital operations for demonstration of admission workflow',
      icon: Building2
    }
  ];

  const whyPoints = [
    {
      title: 'Reduce Information Gaps',
      description: 'Organize critical patient information in one place for better coordination between response teams'
    },
    {
      title: 'Easier for Older Adults',
      description: 'Large controls, multilingual support, and voice input options make emergency reporting more accessible'
    },
    {
      title: 'Transparent & Explainable',
      description: 'Keyword-based rules show exactly why an urgency level was assigned—no hidden algorithms'
    },
    {
      title: 'Organized Handover',
      description: 'Patient details and symptoms structured into a clear summary for all care providers'
    },
    {
      title: 'Complete Coordination',
      description: 'Demonstrate the full workflow from emergency request through transport to hospital admission'
    }
  ];

  return (
    <div className="landing-page">
      <LandingNav language={language} onLanguageChange={setLanguage} />

      {/* Hero Section */}
      <section id="home" className="hero-section">
        <div className="hero-content">
          <div className="hero-text">
            <h1 className="hero-headline">
              Every Second Matters.<br />
              Every Life Deserves <span className="hero-highlight">Care</span>.
            </h1>
            <p className="hero-subheadline">
              Professional emergency coordination platform connecting patients, emergency response, transport services, and hospitals through intelligent triage and real-time coordination.
            </p>
            <div className="hero-cta">
              <button
                onClick={() => navigate('/senior')}
                className="btn btn-danger btn-lg hero-btn-primary"
              >
                <AlertCircle size={20} />
                Request Emergency Assistance
              </button>
              <button
                onClick={() => {
                  const element = document.getElementById('services');
                  element?.scrollIntoView({ behavior: 'smooth' });
                }}
                className="btn btn-outline btn-lg hero-btn-secondary"
              >
                Learn More
                <ArrowRight size={20} />
              </button>
            </div>
            <p className="hero-note">
              Professional healthcare coordination platform
            </p>
          </div>
          <div className="hero-visual">
            <div className="hero-visual-card">
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1rem' }}>
                <div style={{ textAlign: 'center' }}>
                  <div style={{ width: '80px', height: '80px', borderRadius: '50%', background: 'linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%)', margin: '0 auto 0.5rem', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <User size={40} style={{ color: 'white' }} />
                  </div>
                  <p style={{ fontSize: '0.875rem', margin: 0, opacity: 0.9 }}>Senior Citizens</p>
                </div>
                <div style={{ textAlign: 'center' }}>
                  <div style={{ width: '80px', height: '80px', borderRadius: '50%', background: 'linear-gradient(135deg, #f43f5e 0%, #fb923c 100%)', margin: '0 auto 0.5rem', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <Ambulance size={40} style={{ color: 'white' }} />
                  </div>
                  <p style={{ fontSize: '0.875rem', margin: 0, opacity: 0.9 }}>Transport</p>
                </div>
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <div style={{ textAlign: 'center' }}>
                  <div style={{ width: '80px', height: '80px', borderRadius: '50%', background: 'linear-gradient(135deg, #10b981 0%, #14b8a6 100%)', margin: '0 auto 0.5rem', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <Stethoscope size={40} style={{ color: 'white' }} />
                  </div>
                  <p style={{ fontSize: '0.875rem', margin: 0, opacity: 0.9 }}>Hospital</p>
                </div>
                <div style={{ textAlign: 'center' }}>
                  <div style={{ width: '80px', height: '80px', borderRadius: '50%', background: 'linear-gradient(135deg, #14b8a6 0%, #06b6d4 100%)', margin: '0 auto 0.5rem', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <Activity size={40} style={{ color: 'white' }} />
                  </div>
                  <p style={{ fontSize: '0.875rem', margin: 0, opacity: 0.9 }}>Coordination</p>
                </div>
              </div>
              <p className="hero-visual-text" style={{ marginTop: '1.5rem' }}>Complete Emergency Ecosystem</p>
            </div>
          </div>
        </div>
      </section>

      {/* Trust Indicators */}
      <section className="trust-section">
        <div className="trust-content">
          {features.map((feature, index) => {
            const Icon = feature.icon;
            return (
              <div key={index} className="trust-indicator">
                <Icon size={24} className="trust-icon" />
                <div>
                  <h4 className="trust-title">{feature.title}</h4>
                  <p className="trust-description">{feature.description}</p>
                </div>
              </div>
            );
          })}
        </div>
      </section>

      {/* Services Section */}
      <section id="services" className="services-section">
        <div className="section-container">
          <div className="section-header">
            <h2 className="section-title">Complete Emergency Coordination</h2>
            <p className="section-subtitle">
              Professional platform connecting all stakeholders in the emergency care continuum
            </p>
          </div>

          <div className="services-grid">
            {services.map((service, index) => {
              const Icon = service.icon;
              return (
                <div key={index} className="service-card">
                  <div className="service-icon-wrapper">
                    <Icon size={32} className="service-icon" />
                  </div>
                  <h3 className="service-title">{service.title}</h3>
                  <p className="service-description">{service.description}</p>
                  <button
                    onClick={() => navigate(service.link)}
                    className="service-action"
                  >
                    {service.action}
                    <ArrowRight size={16} />
                  </button>
                </div>
              );
            })}
          </div>
        </div>
      </section>

      {/* How It Works Section */}
      <section id="how-it-works" className="how-it-works-section">
        <div className="section-container">
          <div className="section-header">
            <h2 className="section-title">How It Works</h2>
            <p className="section-subtitle">
              Seamless emergency coordination from patient request to hospital admission in 4 steps
            </p>
          </div>

          <div className="steps-container">
            {howItWorksSteps.map((step, index) => {
              const Icon = step.icon;
              return (
                <div key={index} className="step-card">
                  <div className="step-number">{step.number}</div>
                  <Icon size={40} className="step-icon" />
                  <h3 className="step-title">{step.title}</h3>
                  <p className="step-description">{step.description}</p>
                </div>
              );
            })}
          </div>

          <div className="process-info">
            <div className="process-info-item">
              <Clock size={20} />
              <span>Instant Coordination</span>
            </div>
            <div className="process-info-item">
              <Shield size={20} />
              <span>Transparent Triage</span>
            </div>
            <div className="process-info-item">
              <CheckCircle size={20} />
              <span>Complete Workflow</span>
            </div>
          </div>
        </div>
      </section>

      {/* Why AAYUSH Section */}
      <section id="why-aayush" className="why-section">
        <div className="section-container">
          <div className="section-header">
            <h2 className="section-title">Why AAYUSH?</h2>
            <p className="section-subtitle">
              Professional emergency coordination platform built with transparency and accessibility at its core
            </p>
          </div>

          <div className="why-content">
            <div className="why-grid">
              {whyPoints.map((point, index) => (
                <div key={index} className="why-card">
                  <CheckCircle size={24} className="why-icon" />
                  <div>
                    <h4 className="why-card-title">{point.title}</h4>
                    <p className="why-card-description">{point.description}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="why-disclaimer">
            <p>
              <strong>Professional Platform Demonstration:</strong> This platform demonstrates emergency coordination workflows with illustrative triage assessments. 
              For actual emergencies, always contact local emergency services directly.
            </p>
          </div>
        </div>
      </section>

      {/* Emergency CTA Section */}
      <section className="cta-section">
        <div className="cta-content">
          <div className="cta-main">
            <AlertCircle size={48} className="cta-icon" />
            <h2 className="cta-title">Experience the Platform</h2>
            <p className="cta-description">
              Explore the complete emergency coordination workflow designed for accessibility and efficiency
            </p>
            <button
              onClick={() => navigate('/senior')}
              className="btn btn-danger btn-lg cta-button"
            >
              Start Emergency Request
            </button>
          </div>

          <div className="cta-disclaimer">
            <div className="cta-disclaimer-box">
              <Shield size={24} />
              <div>
                <h4>Platform Demonstration</h4>
                <p>
                  This platform demonstrates emergency coordination workflows.
                  For real emergencies in India, call:
                </p>
                <div className="emergency-numbers">
                  <span><strong>108</strong> - Ambulance</span>
                  <span><strong>102</strong> - Medical Emergency</span>
                  <span><strong>112</strong> - Unified Emergency</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <Footer />
    </div>
  );
}
