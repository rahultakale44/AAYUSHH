import { useState, useEffect } from 'react';
import { Mic, MicOff, AlertCircle, CheckCircle, Phone, MapPin } from 'lucide-react';
import type { Language, PatientProfile, EmergencySession } from '../types';
import { createEmergencySession } from '../services/sessionService';
import { t } from '../services/translations';
import { DEMO_SCENARIOS, extractSymptomPhrases, type ExtractedPhrase } from '../services/nlpExtractionService';
import { coordinationService } from '../services/coordinationService';

type Step = 'form' | 'nlp-review' | 'confirm' | 'submitted';

export default function SeniorCitizenHome() {
  const [language, setLanguage] = useState<Language>('en');
  const [step, setStep] = useState<Step>('form');
  const [session, setSession] = useState<EmergencySession | null>(null);
  
  const [formData, setFormData] = useState<PatientProfile & { symptoms: string }>({
    name: '',
    age: 0,
    allergies: '',
    emergencyContact: '',
    symptoms: ''
  });

  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isListening, setIsListening] = useState(false);
  const [transcript, setTranscript] = useState('');
  const [nlpExtraction, setNlpExtraction] = useState<any>(null);
  const [isProcessing, setIsProcessing] = useState(false);
  const [voiceSubmitTimeout, setVoiceSubmitTimeout] = useState<number | null>(null);

  // Listen for status updates
  useEffect(() => {
    const unsubscribe = coordinationService.subscribe((event) => {
      if (event.type === 'STATUS_UPDATED' && session && event.sessionId === session.id) {
        setSession(prev => prev ? { ...prev, status: event.status, updatedAt: new Date() } : null);
      }
    });

    return unsubscribe;
  }, [session]);

  const handleVoiceInput = () => {
    if (!('webkitSpeechRecognition' in window) && !('SpeechRecognition' in window)) {
      alert('Voice input is not supported in your browser.\n\nPlease use Chrome, Edge, or Safari.\nOr type your symptoms in the text area.');
      return;
    }

    const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    const recognition = new SpeechRecognition();
    
    recognition.lang = language === 'hi' ? 'hi-IN' : language === 'mr' ? 'mr-IN' : 'en-US';
    recognition.continuous = false;
    recognition.interimResults = true;
    recognition.maxAlternatives = 1;

    recognition.onstart = () => {
      setIsListening(true);
      setTranscript('');
      if (voiceSubmitTimeout) {
        clearTimeout(voiceSubmitTimeout);
        setVoiceSubmitTimeout(null);
      }
    };

    recognition.onresult = (event: any) => {
      let interimTranscript = '';
      let finalTranscript = '';

      for (let i = event.resultIndex; i < event.results.length; i++) {
        const transcript = event.results[i][0].transcript;
        if (event.results[i].isFinal) {
          finalTranscript += transcript + ' ';
        } else {
          interimTranscript += transcript;
        }
      }

      setTranscript(finalTranscript || interimTranscript);

      if (finalTranscript) {
        const updatedSymptoms = (formData.symptoms + ' ' + finalTranscript).trim();
        setFormData(prev => ({
          ...prev,
          symptoms: updatedSymptoms
        }));
        
        // Clear any existing timeout
        if (voiceSubmitTimeout) {
          clearTimeout(voiceSubmitTimeout);
        }
        
        // Set new timeout to auto-submit after 2 seconds of silence
        const timeout = setTimeout(() => {
          if (updatedSymptoms.length > 10 && !isProcessing) {
            // Auto-submit voice input
            autoSubmitVoiceInput(updatedSymptoms);
          }
        }, 2000);
        setVoiceSubmitTimeout(timeout);
      }
    };

    recognition.onerror = (event: any) => {
      setIsListening(false);
      if (voiceSubmitTimeout) {
        clearTimeout(voiceSubmitTimeout);
        setVoiceSubmitTimeout(null);
      }
      
      if (event.error === 'not-allowed') {
        alert('Microphone permission denied.\n\nPlease allow microphone access and try again, or type your symptoms.');
      } else if (event.error === 'no-speech') {
        // Silent error for no-speech - user just didn't speak
      } else {
        alert(`Voice recognition error: ${event.error}\n\nPlease type your symptoms instead.`);
      }
    };

    recognition.onend = () => {
      setIsListening(false);
      setTranscript('');
    };

    try {
      recognition.start();
    } catch (error) {
      setIsListening(false);
      alert('Could not start voice recognition. Please type your symptoms.');
    }
  };

  const autoSubmitVoiceInput = (symptoms: string) => {
    // Prevent duplicate submissions
    if (isProcessing) return;
    
    setIsProcessing(true);
    setIsListening(false);
    
    // Validate that we have minimum required data
    const hasBasicInfo = formData.name.trim() && formData.age > 0 && formData.emergencyContact.trim();
    
    if (!hasBasicInfo) {
      // If patient info missing, just add to symptoms field and let user fill form
      setIsProcessing(false);
      return;
    }
    
    // If we have all required info, auto-submit
    if (symptoms.length > 10) {
      const extraction = extractSymptomPhrases(symptoms);
      setNlpExtraction(extraction);
      setStep('nlp-review');
      setIsProcessing(false);
    } else {
      setIsProcessing(false);
    }
  };

  const stopListening = () => {
    setIsListening(false);
    if (voiceSubmitTimeout) {
      clearTimeout(voiceSubmitTimeout);
      setVoiceSubmitTimeout(null);
    }
  };

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      if (voiceSubmitTimeout) {
        clearTimeout(voiceSubmitTimeout);
      }
    };
  }, [voiceSubmitTimeout]);

  const loadDemoScenario = (scenario: 'RED' | 'ORANGE' | 'GREEN') => {
    setFormData(prev => ({
      ...prev,
      symptoms: DEMO_SCENARIOS[scenario].text
    }));
  };

  const validateForm = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!formData.name.trim()) newErrors.name = 'Name is required';
    if (!formData.age || formData.age < 1 || formData.age > 120) newErrors.age = 'Valid age required';
    if (!formData.emergencyContact.trim()) newErrors.emergencyContact = 'Emergency contact required';
    if (!formData.symptoms.trim()) newErrors.symptoms = 'Please describe symptoms';

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleAnalyze = () => {
    if (validateForm()) {
      const extraction = extractSymptomPhrases(formData.symptoms);
      setNlpExtraction(extraction);
      setStep('nlp-review');
    }
  };

  const handleConfirm = () => {
    const newSession = createEmergencySession(
      {
        name: formData.name,
        age: formData.age,
        allergies: formData.allergies,
        emergencyContact: formData.emergencyContact
      },
      formData.symptoms
    );
    setSession(newSession);
    setStep('submitted');
  };

  const renderHighlightedText = () => {
    if (!nlpExtraction) return null;

    const { originalText, extractedPhrases } = nlpExtraction;
    const parts: { text: string; category?: string }[] = [];
    let lastIndex = 0;

    extractedPhrases.forEach((phrase: ExtractedPhrase) => {
      if (phrase.startIndex > lastIndex) {
        parts.push({ text: originalText.substring(lastIndex, phrase.startIndex) });
      }
      parts.push({ text: phrase.phrase, category: phrase.category });
      lastIndex = phrase.endIndex;
    });

    if (lastIndex < originalText.length) {
      parts.push({ text: originalText.substring(lastIndex) });
    }

    return (
      <div style={{ lineHeight: '2', fontSize: '1.1rem' }}>
        {parts.map((part, idx) => (
          part.category ? (
            <span
              key={idx}
              style={{
                backgroundColor: part.category === 'RED' ? '#fee' : part.category === 'ORANGE' ? '#ffeaa7' : '#e8f5e9',
                padding: '2px 6px',
                borderRadius: '4px',
                fontWeight: '600',
                color: part.category === 'RED' ? '#c00' : part.category === 'ORANGE' ? '#d63031' : '#27ae60'
              }}
            >
              {part.text}
            </span>
          ) : (
            <span key={idx}>{part.text}</span>
          )
        ))}
      </div>
    );
  };

  const getStatusText = (status: string) => {
    const statusMap: Record<string, string> = {
      'CREATED': 'Request Created',
      'ACKNOWLEDGED': 'Acknowledged by Emergency Response',
      'TRANSPORT_ASSIGNED': 'Ambulance Assigned',
      'EN_ROUTE': 'Ambulance En Route to You',
      'ARRIVED_AT_PATIENT': 'Ambulance Arrived',
      'PATIENT_PICKED_UP': 'Patient Picked Up',
      'EN_ROUTE_TO_HOSPITAL': 'En Route to Hospital',
      'AT_HOSPITAL': 'At Hospital',
      'COMPLETED': 'Completed'
    };
    return statusMap[status] || status;
  };

  return (
    <div className="container container-narrow senior-view">
      {step === 'form' && (
        <>
          <div style={{ marginBottom: '2rem', textAlign: 'center' }}>
            <h1 style={{ fontSize: '2.5rem', marginBottom: '0.5rem' }}><AlertCircle size={36} style={{ display: 'inline', verticalAlign: 'middle', marginRight: '0.5rem', color: '#dc2626' }} /> {t('emergencyAssistance', language)}</h1>
            <p style={{ fontSize: '1.25rem', color: 'var(--text-secondary)' }}>
              {t('appTagline', language)}
            </p>
          </div>

          <div className="card card-lg">
            <div style={{ marginBottom: '2rem' }}>
              <label className="form-label" style={{ fontSize: '1.1rem' }}>{t('language', language)}</label>
              <div className="language-selector">
                <button className={`language-btn ${language === 'en' ? 'active' : ''}`} onClick={() => setLanguage('en')}>English</button>
                <button className={`language-btn ${language === 'hi' ? 'active' : ''}`} onClick={() => setLanguage('hi')}>हिंदी</button>
                <button className={`language-btn ${language === 'mr' ? 'active' : ''}`} onClick={() => setLanguage('mr')}>मराठी</button>
              </div>
            </div>

            <h2 style={{ marginBottom: '1.5rem', fontSize: '1.5rem' }}>{t('patientDetails', language)}</h2>

            <div className="form-group">
              <label className="form-label">{t('name', language)}</label>
              <input type="text" className="form-input" value={formData.name} onChange={(e) => setFormData({ ...formData, name: e.target.value })} style={{ fontSize: '1.1rem' }} />
              {errors.name && <div className="form-error">{errors.name}</div>}
            </div>

            <div className="form-group">
              <label className="form-label">{t('age', language)}</label>
              <input type="number" className="form-input" value={formData.age || ''} onChange={(e) => setFormData({ ...formData, age: parseInt(e.target.value) || 0 })} style={{ fontSize: '1.1rem' }} />
              {errors.age && <div className="form-error">{errors.age}</div>}
            </div>

            <div className="form-group">
              <label className="form-label">{t('allergies', language)}</label>
              <input type="text" className="form-input" placeholder="e.g., Penicillin, None" value={formData.allergies} onChange={(e) => setFormData({ ...formData, allergies: e.target.value })} style={{ fontSize: '1.1rem' }} />
            </div>

            <div className="form-group">
              <label className="form-label">{t('emergencyContact', language)}</label>
              <input type="tel" className="form-input" placeholder="+91-9876543210" value={formData.emergencyContact} onChange={(e) => setFormData({ ...formData, emergencyContact: e.target.value })} style={{ fontSize: '1.1rem' }} />
              {errors.emergencyContact && <div className="form-error">{errors.emergencyContact}</div>}
            </div>

            <div className="form-group">
              <label className="form-label" style={{ fontSize: '1.2rem', fontWeight: 'bold' }}>{t('symptoms', language)}</label>
              <textarea
                className="form-textarea"
                placeholder={t('symptomsPlaceholder', language)}
                value={formData.symptoms}
                onChange={(e) => setFormData({ ...formData, symptoms: e.target.value })}
                style={{ fontSize: '1.1rem', minHeight: '120px' }}
              />
              {errors.symptoms && <div className="form-error">{errors.symptoms}</div>}
              
              {isListening && (
                <div style={{ marginTop: '1rem', padding: '1rem', background: '#e3f2fd', borderRadius: '8px', border: '2px solid #2196f3' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
                    <Mic size={20} style={{ color: '#2196f3' }} className="animate-pulse" />
                    <strong>Listening... Speak clearly</strong>
                  </div>
                  <div style={{ fontStyle: 'italic', color: '#1976d2' }}>{transcript || 'Waiting for speech...'}</div>
                  <div style={{ fontSize: '0.875rem', color: '#666', marginTop: '0.5rem' }}>
                    Will auto-submit 2 seconds after you finish speaking
                  </div>
                </div>
              )}

              {isProcessing && (
                <div style={{ marginTop: '1rem', padding: '1rem', background: '#fff3e0', borderRadius: '8px', border: '2px solid #ff9800' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <div className="animate-pulse">⚡</div>
                    <strong>Processing your emergency request...</strong>
                  </div>
                </div>
              )}

              <div style={{ display: 'flex', gap: '1rem', marginTop: '1rem' }}>
                <button
                  type="button"
                  className={`btn ${isListening ? 'btn-danger' : 'btn-secondary'}`}
                  onClick={isListening ? stopListening : handleVoiceInput}
                  style={{ flex: 1, fontSize: '1.1rem', padding: '1rem' }}
                >
                  {isListening ? <><MicOff size={24} /> Stop</> : <><Mic size={24} /> {t('voiceInput', language)}</>}
                </button>
              </div>

              <div style={{ marginTop: '1rem', padding: '1rem', background: '#f5f5f5', borderRadius: '8px' }}>
                <strong>Quick Examples:</strong>
                <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.5rem', flexWrap: 'wrap' }}>
                  <button className="btn btn-sm" onClick={() => loadDemoScenario('RED')} style={{ background: '#ffebee', color: '#c62828', border: '1px solid #c62828' }}>
                    RED - Critical
                  </button>
                  <button className="btn btn-sm" onClick={() => loadDemoScenario('ORANGE')} style={{ background: '#fff3e0', color: '#e65100', border: '1px solid #e65100' }}>
                    ORANGE - Urgent
                  </button>
                  <button className="btn btn-sm" onClick={() => loadDemoScenario('GREEN')} style={{ background: '#e8f5e9', color: '#2e7d32', border: '1px solid #2e7d32' }}>
                    GREEN - Stable
                  </button>
                </div>
              </div>
            </div>

            <div className="alert alert-warning" style={{ fontSize: '1.05rem' }}>
              <Phone size={20} />
              <strong>Emergency: Call 108, 102, or 112 immediately if life-threatening</strong>
            </div>

            <button className="btn btn-primary btn-lg" onClick={handleAnalyze} style={{ width: '100%', fontSize: '1.25rem', padding: '1.25rem' }}>
              <AlertCircle size={24} />
              Analyze & Submit Emergency
            </button>
          </div>
        </>
      )}

      {step === 'nlp-review' && nlpExtraction && (
        <>
          <div style={{ marginBottom: '2rem' }}>
            <h1>📊 Symptom Analysis</h1>
            <p style={{ color: 'var(--text-secondary)' }}>AI-powered extraction (Demonstration)</p>
          </div>

          <div className="card card-lg">
            <div style={{ marginBottom: '2rem' }}>
              <h3>Original Statement:</h3>
              <div style={{ padding: '1rem', background: '#f9f9f9', borderRadius: '8px', border: '1px solid #ddd' }}>
                {renderHighlightedText()}
              </div>
              <div style={{ marginTop: '0.5rem', fontSize: '0.9rem', color: '#666' }}>
                <span style={{ background: '#fee', padding: '2px 8px', borderRadius: '4px', marginRight: '8px' }}>RED: Critical</span>
                <span style={{ background: '#ffeaa7', padding: '2px 8px', borderRadius: '4px', marginRight: '8px' }}>ORANGE: Urgent</span>
                <span style={{ background: '#e8f5e9', padding: '2px 8px', borderRadius: '4px' }}>GREEN: Non-urgent</span>
              </div>
            </div>

            <div style={{ padding: '1.5rem', background: nlpExtraction.urgencyLevel === 'RED' ? '#ffebee' : nlpExtraction.urgencyLevel === 'ORANGE' ? '#fff3e0' : '#e8f5e9', borderRadius: '12px', border: `3px solid ${nlpExtraction.urgencyLevel === 'RED' ? '#c62828' : nlpExtraction.urgencyLevel === 'ORANGE' ? '#e65100' : '#2e7d32'}` }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1rem' }}>
                {nlpExtraction.urgencyLevel === 'RED' ? <AlertCircle size={48} style={{ color: '#c62828' }} /> : nlpExtraction.urgencyLevel === 'ORANGE' ? <AlertCircle size={48} style={{ color: '#e65100' }} /> : <CheckCircle size={48} style={{ color: '#2e7d32' }} />}
                <div>
                  <h2 style={{ margin: 0, color: nlpExtraction.urgencyLevel === 'RED' ? '#c62828' : nlpExtraction.urgencyLevel === 'ORANGE' ? '#e65100' : '#2e7d32' }}>
                    {nlpExtraction.urgencyLevel} PRIORITY
                  </h2>
                  <p style={{ margin: '0.5rem 0 0', fontSize: '1.1rem' }}>{nlpExtraction.reasoning}</p>
                </div>
              </div>
            </div>

            <div style={{ marginTop: '1.5rem' }}>
              <h3>Structured Summary (JSON):</h3>
              <pre style={{ background: '#1e1e1e', color: '#d4d4d4', padding: '1rem', borderRadius: '8px', overflow: 'auto', fontSize: '0.95rem' }}>
                {JSON.stringify(nlpExtraction.structuredSummary, null, 2)}
              </pre>
            </div>

            <div className="alert alert-warning" style={{ marginTop: '1.5rem' }}>
              <AlertCircle size={20} /> <strong>ASSESSMENT NOTICE:</strong> This is an illustrative triage assessment, NOT a validated medical diagnosis. Always consult healthcare professionals.
            </div>

            <div style={{ display: 'flex', gap: '1rem', marginTop: '1.5rem' }}>
              <button className="btn btn-secondary btn-lg" onClick={() => setStep('form')} style={{ flex: 1 }}>
                ← Back to Edit
              </button>
              <button className="btn btn-danger btn-lg" onClick={handleConfirm} style={{ flex: 2, fontSize: '1.2rem' }}>
                <AlertCircle size={24} />
                Confirm Emergency Request
              </button>
            </div>
          </div>
        </>
      )}

      {step === 'submitted' && session && (
        <>
          <div style={{ marginBottom: '2rem', textAlign: 'center' }}>
            <CheckCircle size={80} style={{ color: 'var(--green-600)', margin: '0 auto 1rem' }} />
            <h1 style={{ fontSize: '2rem' }}>EMERGENCY REQUEST SENT</h1>
          </div>

          <div className="card card-lg">
            <div className="alert alert-success" style={{ fontSize: '1.1rem' }}>
              <strong style={{ fontSize: '1.3rem' }}>Session ID: {session.id}</strong>
              <p style={{ margin: '0.5rem 0 0' }}>Your emergency has been broadcast to the AAYUSH coordination network.</p>
            </div>

            <div style={{ padding: '1.5rem', background: '#f5f5f5', borderRadius: '12px', marginTop: '1.5rem' }}>
              <h3>Current Status:</h3>
              <div style={{ fontSize: '1.3rem', fontWeight: 'bold', color: '#1976d2', marginTop: '0.5rem' }}>
                🔄 {getStatusText(session.status)}
              </div>
              <div style={{ fontSize: '0.9rem', color: '#666', marginTop: '0.5rem' }}>
                Last updated: {session.updatedAt.toLocaleTimeString()}
              </div>
            </div>

            {session.location && (
              <div style={{ marginTop: '1.5rem' }}>
                <h3><MapPin size={20} /> Your Location (Simulated):</h3>
                <div style={{ padding: '1rem', background: '#e3f2fd', borderRadius: '8px' }}>
                  <p><strong>Coordinates:</strong> {session.location.lat.toFixed(4)}, {session.location.lng.toFixed(4)}</p>
                  <p style={{ margin: '0.5rem 0 0', fontSize: '0.9rem', color: '#666' }}>{session.location.address}</p>
                </div>
              </div>
            )}

            <div style={{ marginTop: '1.5rem', padding: '1.5rem', background: session.triageAssessment.urgencyLevel === 'RED' ? '#ffebee' : session.triageAssessment.urgencyLevel === 'ORANGE' ? '#fff3e0' : '#e8f5e9', borderRadius: '12px' }}>
              <h3>Triage Assessment:</h3>
              <div style={{ fontSize: '1.5rem', fontWeight: 'bold', marginTop: '0.5rem', color: session.triageAssessment.urgencyLevel === 'RED' ? '#c62828' : session.triageAssessment.urgencyLevel === 'ORANGE' ? '#e65100' : '#2e7d32' }}>
                {session.triageAssessment.urgencyLevel} PRIORITY
              </div>
              <p style={{ marginTop: '0.5rem' }}>{session.triageAssessment.reasoning}</p>
            </div>

            <div className="alert alert-info" style={{ marginTop: '1.5rem' }}>
              <Phone size={20} />
              <strong>For immediate life-threatening emergencies, call 108, 102, or 112</strong>
            </div>

            <button className="btn btn-primary btn-lg" onClick={() => window.location.reload()} style={{ width: '100%', marginTop: '1rem' }}>
              Create New Request
            </button>
          </div>
        </>
      )}
    </div>
  );
}
