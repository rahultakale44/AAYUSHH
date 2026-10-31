# AAYUSH (आयुष) - Senior-Friendly Emergency Assistance Platform

**InnovateX 2.0 Hackathon Project**

A comprehensive web-based emergency coordination platform designed to help senior citizens communicate symptoms and coordinate emergency response, transport, and hospital operations.

## 🚀 Quick Start

### Prerequisites
- Node.js 24.x
- npm 11.x
- Java 26 (for future backend integration)

### Installation & Running

```bash
# Install dependencies (already done)
npm install

# Development server
npm run dev

# Production build
npm run build

# Preview production build
npm run preview
```

The application will be available at: **http://localhost:5173**

## 🎯 Features

### ✅ Completed (Milestone 2)

#### 1. **Role Selection Landing Page**
- Clean, professional healthcare-themed interface
- Four role cards with icons and descriptions:
  - Senior Citizen
  - Emergency Response
  - Transport Partner
  - Hospital Operations
- Responsive design for desktop and mobile

#### 2. **Senior Citizen Experience** 🏆 Priority Feature
- **Multi-language support**: English, Hindi (हिंदी), Marathi (मराठी)
- **Large, accessible UI** with high contrast and generous spacing
- **Patient details form**:
  - Name, age, allergies, emergency contact
  - Clear validation with helpful error messages
- **Symptom input**:
  - Large text area for typed input
  - Optional browser voice recognition (graceful fallback)
  - Support for English, Hindi, and Marathi voice input
- **Emergency request review** step before submission
- **Transparent triage assessment**:
  - Local keyword-based RED/ORANGE/GREEN urgency classification
  - Clear display of matched keywords and reasoning
  - Explicit disclaimer that this is a demo, not medical advice
- **Patient handover summary** generation
- **Safety messaging**: Clear instructions to contact local emergency services (108, 102, 112)

#### 3. **Emergency Response Dashboard**
- Real-time demo emergency queue
- Session details with urgency indicators
- Patient information and symptoms
- Demo triage assessment display
- Status progression workflow:
  - Created → Acknowledged → Transport Assigned → En Route → Arrived → At Hospital → Completed
- Functional acknowledge and status update actions
- Statistics overview (active requests, critical, urgent)

#### 4. **Transport Partner Dashboard**
- Mock fleet status with 3 demo vehicles
- Transport request queue
- Accept/reject transport requests (demo)
- Status updates for transport flow
- Simulated ETA and assignment information
- Clear labeling that all data is mock/simulated

#### 5. **Hospital Operations Dashboard**
- Mock facility capacity indicators:
  - Emergency beds availability
  - ICU beds availability
  - Staff on duty
- Incoming patient handovers
- Patient information and triage assessment
- Admit patient functionality
- Complete session workflow
- Clear indication that all capacity data is simulated

### 🎨 Design System

- **Color Palette**: Navy/blue healthcare theme with accessible contrast
- **Typography**: Large, readable fonts optimized for senior users
- **Components**: Consistent cards, buttons, badges, and alerts
- **Icons**: Lucide React icons (no emoji in UI)
- **Responsive**: Mobile-first design with tablet and desktop layouts
- **Accessibility**: High contrast, keyboard navigation, screen reader friendly

### ⚠️ Safety & Compliance

The application includes multiple safety measures:

1. **Clear Disclaimers**: All triage assessments labeled as "illustrative demo classifications, not clinical decisions"
2. **Emergency Service Prompts**: Prominent messages to contact local emergency services
3. **Mock Data Labels**: All simulated data (ambulance dispatch, hospital capacity) clearly marked
4. **Transparent Logic**: Keyword-based rules are visible and documented
5. **No False Reassurance**: System never claims symptoms are "safe" just because keywords weren't matched

### 🔧 Technical Architecture

#### Frontend Stack
- **React 19.3** with TypeScript
- **Vite 8.3** for fast builds and HMR
- **React Router** for navigation
- **Lucide React** for icons
- **Custom CSS** with CSS variables for theming

#### Data Management
- TypeScript interfaces for type safety
- Service layer for business logic separation
- Browser localStorage for session persistence
- Sample demo data for realistic presentation

#### File Structure
```
src/
├── types/
│   └── index.ts                    # TypeScript interfaces
├── services/
│   ├── triageService.ts           # Keyword-based triage logic
│   ├── sessionService.ts          # Session management
│   └── translations.ts            # Multi-language support
├── components/
│   └── Header.tsx                 # App header with navigation
├── pages/
│   ├── RoleSelection.tsx          # Landing page
│   ├── SeniorCitizenHome.tsx      # Senior citizen interface
│   ├── EmergencyResponseDashboard.tsx
│   ├── TransportPartnerDashboard.tsx
│   └── HospitalOperationsDashboard.tsx
├── App.tsx                         # Main app with routing
├── App.css                         # Component styles
└── index.css                       # Global styles
```

## 🧪 Testing Guide

### Test Scenario 1: Senior Citizen Emergency Request

1. Navigate to http://localhost:5173
2. Click **"Senior Citizen"** role card
3. Select a language (English/Hindi/Marathi)
4. Fill in patient details:
   - Name: "Test Patient"
   - Age: 75
   - Allergies: "Penicillin"
   - Emergency Contact: "+91-9876543210"
5. Enter symptoms (try keywords like "chest pain" or "difficulty breathing" for RED classification)
6. Click **"Review Emergency Request"**
7. Review information and click **"Confirm Emergency"**
8. Verify:
   - Session ID is generated
   - Urgency badge appears (RED/ORANGE/GREEN)
   - Matched keywords are displayed
   - Handover summary is shown
   - Safety disclaimers are visible

### Test Scenario 2: Emergency Response Workflow

1. From home, click **"Emergency Response"**
2. Verify the demo session from previous test appears
3. Click **"Acknowledge Request"**
4. Progress through status updates:
   - Assign Transport → En Route → Arrived → At Hospital → Complete
5. Verify statistics update correctly

### Test Scenario 3: Transport Partner Workflow

1. Click **"Transport Partner"** from home
2. View mock fleet status (3 demo vehicles)
3. Find an acknowledged request
4. Click **"Accept Transport Request"**
5. Verify simulated vehicle assignment
6. Click **"Start Route"** and then **"Mark Arrived"**
7. Verify status updates across dashboards

### Test Scenario 4: Hospital Operations

1. Click **"Hospital Operations"** from home
2. View mock facility capacity
3. Find an en-route or arrived patient
4. Click **"Admit Patient"**
5. View patient handover summary
6. Click **"Complete Session"**
7. Verify patient removed from active lists

### Test Scenario 5: Multi-Language Support

1. Go to Senior Citizen page
2. Switch between English, हिंदी, and मराठी
3. Verify all labels update correctly
4. Test voice input (if browser supports it) in selected language

### Test Scenario 6: Mobile Responsiveness

1. Open browser DevTools
2. Toggle device toolbar (responsive mode)
3. Test at different viewport sizes:
   - Mobile: 375px width
   - Tablet: 768px width
   - Desktop: 1200px width
4. Verify layouts adapt appropriately

## 🎓 Demo Keywords for Testing

### RED Level (Critical)
- chest pain
- heart attack
- stroke
- unconscious
- not breathing
- severe bleeding

### ORANGE Level (Urgent)
- difficulty breathing
- high fever
- severe pain
- broken bone
- dizzy

### GREEN Level (Non-urgent)
- fever
- cough
- headache
- stomach ache

## 📊 What's Working

✅ Complete frontend application with all 4 role views
✅ Multi-language support (EN, HI, MR)
✅ Browser voice recognition with graceful fallback
✅ Transparent keyword-based triage assessment
✅ Session management with localStorage persistence
✅ Responsive design for mobile, tablet, desktop
✅ Professional healthcare-themed UI
✅ Complete emergency workflow from citizen to hospital
✅ Real-time session updates across dashboards
✅ Proper safety disclaimers and mock data labeling
✅ TypeScript type safety
✅ Production build verified

## 🔮 Future Enhancements (Post-Milestone 2)

### Milestone 3-5 (If Time Permits)
- [ ] Leaflet + OpenStreetMap integration for location visualization
- [ ] Spring Boot REST API backend
- [ ] PostgreSQL database integration
- [ ] Python FastAPI service for advanced symptom extraction
- [ ] JWT authentication for staff roles
- [ ] WebSocket for real-time updates
- [ ] Export patient reports as PDF

## ⚠️ Important Notes

### This is a Demonstration Prototype
- **Not for medical use**: All triage assessments are illustrative only
- **Mock data**: Ambulance dispatch, hospital capacity, ETAs are simulated
- **No actual dispatch**: The system does not contact real emergency services
- **Keyword rules**: Simple pattern matching, not AI or validated medical logic

### Always Contact Emergency Services
For real emergencies in India, contact:
- **108** - Ambulance Service
- **102** - Medical Emergency
- **112** - Unified Emergency Number

## 👥 Credits

**InnovateX 2.0 Hackathon Project**
- Platform: AAYUSH (आयुष)
- Focus: Senior-friendly emergency coordination
- Built with: React, TypeScript, Vite
- Designed for: Accessibility and clarity

---

**Status**: ✅ Milestone 2 Complete - Full Frontend Implementation
**Build**: ✅ Passing (299KB production bundle)
**Ready for**: Demo and further development
