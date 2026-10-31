# AAYUSH SYSTEM ARCHITECTURE
## Enterprise Java Full Stack + Python NLP Application

**Document Version:** 2.0 (CORRECTED)  
**Last Updated:** October 31, 2026  
**Status:** Architecture Design - Reviewed and Corrected  
**Target Architecture:** Production-Grade Portfolio Application

**⚠️ IMPORTANT: This document has been revised based on critical architecture review**

**Key Corrections Made:**
1. ✅ Added Direct SOS workflow (< 500ms, bypasses NLP/profile)
2. ✅ Clarified Python NLP limitations (no Marathi, generic models, no triage decisions)
3. ✅ Added time-series vital readings (never overwrite history)
4. ✅ Restricted public registration to SENIOR/CAREGIVER roles only
5. ✅ Added refresh token rotation and revocation
6. ✅ Defined WebSocket subscription authorization
7. ✅ PostgreSQL-first updates (WebSocket best-effort broadcast)
8. ✅ Unknown symptoms escalate to ORANGE (never GREEN)
9. ✅ All mock integrations clearly labeled
10. ✅ Reordered implementation roadmap (safety first)

**Read ARCHITECTURE_DECISIONS.md for detailed rationale behind each correction.**

---

## TABLE OF CONTENTS

1. [Architecture Overview](#1-architecture-overview)
2. [System Components](#2-system-components)
3. [Database Design](#3-database-design)
4. [API Specifications](#4-api-specifications)
5. [WebSocket Events](#5-websocket-events)
6. [Authentication & Authorization](#6-authentication--authorization)
7. [Python NLP Service](#7-python-nlp-service)
8. [Emergency Lifecycle](#8-emergency-lifecycle)
9. [Integration Architecture](#9-integration-architecture)
10. [Deployment Architecture](#10-deployment-architecture)
11. [Security Architecture](#11-security-architecture)
12. [Testing Strategy](#12-testing-strategy)
13. [Implementation Roadmap](#13-implementation-roadmap)

---

## 1. ARCHITECTURE OVERVIEW

**⚠️ CRITICAL: Read ARCHITECTURE_DECISIONS.md for detailed design corrections**

### 1.1 System Context

AAYUSH is a **senior-friendly emergency assistance platform** that coordinates emergency requests across multiple stakeholders:

**Primary Users:**
- Senior Citizens (patients)
- Emergency Response Coordinators
- Transport/Ambulance Providers
- Hospital Emergency Staff
- Caregivers (family members)
- System Administrators

**Key Business Flows:**

#### **Path A: Direct SOS (Life-Threatening)**
1. Senior presses Emergency SOS button
2. **Immediate** session creation in PostgreSQL (< 500ms)
3. Assumes RED urgency (critical)
4. Skips NLP extraction, profile lookup, location query
5. Background jobs enhance session asynchronously
6. Coordinates ambulance dispatch (MOCK in demo)
7. Notifies caregivers and hospital

#### **Path B: Standard Emergency (Non-Critical)**
1. Senior describes symptoms via voice/text
2. **Optional** Python NLP extraction (3-second timeout)
3. **Java triage engine** determines urgency (RED/ORANGE/GREEN)
4. Uses health profile if available (not required)
5. Coordinates ambulance dispatch (MOCK in demo)
6. Notifies hospital with patient handover brief
7. Sends real-time updates to family caregivers
8. Tracks emergency lifecycle to completion

**Critical Safety Principles:**
- **Never block** emergency creation due to missing data, failed NLP, or incomplete profile
- **PostgreSQL is source of truth** - WebSockets only distribute updates
- **Python extracts symptoms, Java decides urgency** - no triage logic in Python
- **Unknown symptoms escalate to ORANGE** (human review), never GREEN
- **All mock integrations clearly labeled** (ambulance dispatch, hospital capacity, ETA)

### 1.2 Architectural Principles

1. **Safety First - Never Block Emergency Assistance**
   - Direct SOS path bypasses all non-critical services
   - Emergency creation succeeds with zero profile data
   - Python NLP failure triggers keyword fallback (not GREEN)
   - Unknown symptoms escalate to ORANGE (human review)
   - PostgreSQL write failure is only blocking condition

2. **Separation of Concerns**
   - Frontend: User interface and experience
   - Java Backend: Business logic, **ALL triage decisions**, coordination, persistence
   - Python Service: **ONLY** NLP symptom extraction (no urgency decisions)
   - PostgreSQL: Single source of truth

3. **PostgreSQL as Source of Truth**
   - All emergency data persists in database **FIRST**
   - WebSockets distribute updates **SECOND** (best-effort)
   - Reconnecting clients reload state from REST API
   - No dual-write scenarios

4. **Transparent and Versioned Triage Rules**
   - Java rules engine with version tracking (e.g., "1.0.0")
   - Python extracts symptoms → Java decides urgency
   - Every rule has explicit test case
   - Rules are version-controlled and auditable
   - Clear disclaimer: "AUTOMATED PROTOTYPE, not clinically validated"

5. **Real-Time Coordination (Non-Blocking)**
   - WebSocket + STOMP for live updates
   - WebSocket failure does not prevent data updates
   - All dashboards subscribe to relevant sessions
   - Authenticated and session-authorized subscriptions
   - Reconnection loads current state from PostgreSQL via REST

6. **Security and Privacy**
   - JWT authentication with refresh token rotation
   - Role-based access control (RBAC)
   - Public registration ONLY for SENIOR and CAREGIVER roles
   - Staff sees minimum necessary information only
   - No PII in application logs (only audit logs)
   - WebSocket subscriptions require authorization
   - Audit logging of all data access

7. **Mobile-First Responsive with Offline Resilience**
   - PWA capabilities
   - Offline fallback for critical actions (SOS button)
   - Large touch targets for seniors
   - Voice input priority (with text fallback)

8. **Mock Integration Transparency**
   - All simulated features clearly labeled ("⚠️ SIMULATED DISPATCH")
   - Adapter pattern enables future real integrations
   - Dev profile uses mocks, prod throws UnsupportedOperationException
   - Never claim real 108/112 dispatch or live hospital integration

### 1.3 Technology Stack

#### Frontend
- **Framework:** React 19.3 + TypeScript 7.0
- **Build:** Vite 8.3
- **Routing:** React Router 7.18
- **State:** React Context + localStorage (cache)
- **WebSocket:** STOMP.js
- **Maps:** Leaflet + OpenStreetMap
- **Icons:** Lucide React
- **Testing:** Vitest + Playwright

#### Backend (Java)
- **Java Version:** 21 LTS
- **Framework:** Spring Boot 3.2.11
- **Security:** Spring Security + JWT (jjwt 0.12.6)
- **Database:** Spring Data JPA + Hibernate
- **Migrations:** Flyway
- **WebSocket:** Spring WebSocket + STOMP
- **Validation:** Jakarta Validation
- **Testing:** JUnit 5 + Mockito + Testcontainers
- **Build:** Maven 3.9+

#### Backend (Python)
- **Framework:** FastAPI 0.115+
- **NLP:** spaCy 3.7+ (en_core_web_sm, hi_core_news_sm)
- **Medical NER:** Custom trained models or BioBERT
- **HTTP Client:** httpx (async)
- **Testing:** pytest + pytest-asyncio
- **ASGI Server:** uvicorn

#### Database
- **RDBMS:** PostgreSQL 15+
- **Connection Pooling:** HikariCP
- **Caching:** Redis 7+ (optional, for session cache)
- **Migrations:** Flyway

#### Infrastructure
- **Containerization:** Docker + Docker Compose
- **Reverse Proxy:** Nginx
- **CI/CD:** GitHub Actions
- **Monitoring:** Prometheus + Grafana
- **Logging:** ELK Stack (Elasticsearch, Logstash, Kibana)
- **Deployment:** AWS/Azure/GCP or on-premise

---

## 2. SYSTEM COMPONENTS

### 2.1 High-Level Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                          CLIENT LAYER                                │
├─────────────────────────────────────────────────────────────────────┤
│  React Frontend (PWA)                                                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐              │
│  │   Senior     │  │  Emergency   │  │  Transport   │              │
│  │   Citizen    │  │  Response    │  │   Partner    │              │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘              │
│  ┌──────────────┐  ┌──────────────┐                                 │
│  │  Hospital    │  │  Caregiver   │                                 │
│  │  Operations  │  │  Dashboard   │                                 │
│  └──────┬───────┘  └──────┬───────┘                                 │
└─────────┼──────────────────┼──────────────────────────────────────┘
          │                  │
          │  HTTPS/WSS       │
          ▼                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                       API GATEWAY / NGINX                            │
│                   (Reverse Proxy + Load Balancer)                    │
└────────────────┬──────────────────────────────┬─────────────────────┘
                 │                              │
       ┌─────────▼────────────┐      ┌─────────▼─────────────┐
       │  Java Backend        │      │  Python NLP Service   │
       │  Spring Boot         │◄─────┤  FastAPI              │
       │                      │ REST │                       │
       │  ┌────────────────┐ │      │  ┌─────────────────┐  │
       │  │  Controllers   │ │      │  │  Symptom        │  │
       │  │  (REST API)    │ │      │  │  Extractor      │  │
       │  └────────────────┘ │      │  └─────────────────┘  │
       │  ┌────────────────┐ │      │  ┌─────────────────┐  │
       │  │  WebSocket     │ │      │  │  spaCy NLP      │  │
       │  │  Controller    │ │      │  │  Models         │  │
       │  └────────────────┘ │      │  └─────────────────┘  │
       │  ┌────────────────┐ │      └───────────────────────┘
       │  │  Services      │ │
       │  │  - Emergency   │ │
       │  │  - Triage      │ │
       │  │  - Hospital    │ │
       │  │  - Ambulance   │ │
       │  │  - User Auth   │ │
       │  └────────────────┘ │
       │  ┌────────────────┐ │
       │  │  Repositories  │ │
       │  │  (Spring Data) │ │
       │  └────────┬───────┘ │
       └───────────┼─────────┘
                   │
         ┌─────────▼────────────┐
         │  PostgreSQL 15+      │
         │  ┌─────────────────┐ │
         │  │  Tables:        │ │
         │  │  - users        │ │
         │  │  - roles        │ │
         │  │  - emergency    │ │
         │  │    _sessions    │ │
         │  │  - health       │ │
         │  │    _profiles    │ │
         │  │  - hospitals    │ │
         │  │  - ambulances   │ │
         │  │  - audit_logs   │ │
         │  └─────────────────┘ │
         └──────────────────────┘

┌──────────────────────────────────────────────────────┐
│         EXTERNAL INTEGRATIONS (Mock Adapters)        │
├──────────────────────────────────────────────────────┤
│  - SMS Gateway (Twilio)                              │
│  - Email Service (SendGrid)                          │
│  - Maps API (OpenStreetMap / Google Maps)            │
│  - Real Ambulance Dispatch (Future)                  │
│  - Hospital EMR Integration (Future)                 │
└──────────────────────────────────────────────────────┘
```

### 2.2 Component Descriptions

#### 2.2.1 React Frontend

**Responsibilities:**
- User interface for all roles
- Voice input capture (Web Speech API)
- Form validation and submission
- Real-time updates via WebSocket subscription
- Local state caching (optimistic UI)
- Offline fallback for critical actions
- Responsive design (mobile-first)

**Key Pages:**
- `/` - Public landing page
- `/login` - Authentication
- `/senior` - Senior citizen emergency submission
- `/emergency` - Emergency response coordinator dashboard
- `/transport` - Transport partner dashboard
- `/hospital` - Hospital operations dashboard
- `/caregiver` - Family caregiver notifications
- `/admin` - System administration

**Frontend Folder Structure:**
```
src/
├── api/
│   ├── client.ts                # Axios instance with interceptors
│   ├── auth.ts                  # Authentication API
│   ├── emergency.ts             # Emergency API
│   ├── hospital.ts              # Hospital API
│   ├── ambulance.ts             # Ambulance API
│   └── websocket.ts             # STOMP client setup
├── components/
│   ├── common/
│   │   ├── Button.tsx
│   │   ├── Card.tsx
│   │   ├── Badge.tsx
│   │   └── Modal.tsx
│   ├── emergency/
│   │   ├── SymptomInput.tsx
│   │   ├── VoiceRecorder.tsx
│   │   ├── TriageDisplay.tsx
│   │   └── StatusTimeline.tsx
│   └── layout/
│       ├── Header.tsx
│       ├── Sidebar.tsx
│       └── Footer.tsx
├── contexts/
│   ├── AuthContext.tsx          # JWT + user state
│   ├── EmergencyContext.tsx     # Emergency sessions state
│   └── WebSocketContext.tsx     # WebSocket connection
├── hooks/
│   ├── useAuth.ts
│   ├── useWebSocket.ts
│   ├── useEmergency.ts
│   └── useVoiceInput.ts
├── pages/
│   ├── LandingPage.tsx
│   ├── LoginPage.tsx
│   ├── SeniorCitizenHome.tsx
│   ├── EmergencyResponseDashboard.tsx
│   ├── TransportPartnerDashboard.tsx
│   ├── HospitalOperationsDashboard.tsx
│   ├── CaregiverDashboard.tsx
│   └── AdminDashboard.tsx
├── services/
│   ├── triageService.ts         # Client-side triage preview
│   ├── locationService.ts       # Geolocation
│   └── nlpService.ts            # Symptom highlighting
├── types/
│   └── index.ts                 # TypeScript interfaces
├── utils/
│   ├── validators.ts
│   ├── formatters.ts
│   └── constants.ts
├── App.tsx
└── main.tsx
```

#### 2.2.2 Java Backend (Spring Boot)

**Responsibilities:**
- REST API for all CRUD operations
- WebSocket server for real-time updates
- JWT authentication and authorization
- Business logic (triage rules, coordination)
- Database persistence (PostgreSQL)
- Integration with Python NLP service
- Audit logging
- Email/SMS notification triggering
- Hospital/ambulance matching logic

**Backend Package Structure:**
```
backend/src/main/java/com/aayush/backend/
├── AayushBackendApplication.java
├── config/
│   ├── SecurityConfig.java            # Spring Security + JWT
│   ├── WebSocketConfig.java           # STOMP configuration
│   ├── CorsConfig.java                # CORS settings
│   └── AsyncConfig.java               # Async processing
├── controller/
│   ├── rest/
│   │   ├── AuthController.java        # Login, register, refresh
│   │   ├── EmergencySessionController.java
│   │   ├── HealthProfileController.java
│   │   ├── HospitalController.java
│   │   ├── AmbulanceController.java
│   │   ├── CaregiverController.java
│   │   └── AdminController.java
│   └── websocket/
│       └── EmergencyWebSocketController.java
├── dto/
│   ├── request/
│   │   ├── LoginRequest.java
│   │   ├── RegisterRequest.java
│   │   ├── CreateEmergencySessionRequest.java
│   │   ├── UpdateSessionStatusRequest.java
│   │   ├── CreateHealthProfileRequest.java
│   │   └── LinkCaregiverRequest.java
│   ├── response/
│   │   ├── AuthResponse.java
│   │   ├── EmergencySessionResponse.java
│   │   ├── HealthProfileResponse.java
│   │   ├── HospitalResponse.java
│   │   └── AmbulanceResponse.java
│   └── websocket/
│       ├── SessionCreatedMessage.java
│       ├── StatusUpdatedMessage.java
│       └── NotificationMessage.java
├── entity/
│   ├── User.java
│   ├── Role.java
│   ├── EmergencySession.java
│   ├── HealthProfile.java
│   ├── MedicalHistory.java
│   ├── Medication.java
│   ├── Caregiver.java
│   ├── CaregiverPermission.java
│   ├── Hospital.java
│   ├── HospitalCapacity.java
│   ├── Ambulance.java
│   ├── AuditLog.java
│   └── Notification.java
├── repository/
│   ├── UserRepository.java
│   ├── RoleRepository.java
│   ├── EmergencySessionRepository.java
│   ├── HealthProfileRepository.java
│   ├── MedicalHistoryRepository.java
│   ├── CaregiverRepository.java
│   ├── HospitalRepository.java
│   ├── AmbulanceRepository.java
│   └── AuditLogRepository.java
├── service/
│   ├── auth/
│   │   ├── AuthService.java
│   │   ├── JwtService.java
│   │   └── UserDetailsServiceImpl.java
│   ├── emergency/
│   │   ├── EmergencySessionService.java
│   │   ├── TriageService.java           # Java rules engine
│   │   └── NlpIntegrationService.java   # Call Python
│   ├── health/
│   │   ├── HealthProfileService.java
│   │   ├── MedicalHistoryService.java
│   │   └── CaregiverService.java
│   ├── hospital/
│   │   ├── HospitalService.java
│   │   ├── HospitalMatchingService.java
│   │   └── AmbulanceService.java
│   ├── notification/
│   │   ├── NotificationService.java
│   │   ├── EmailService.java
│   │   └── SmsService.java
│   └── websocket/
│       └── WebSocketMessageService.java
├── security/
│   ├── JwtAuthenticationFilter.java
│   ├── JwtAuthenticationEntryPoint.java
│   └── SecurityUtils.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   ├── UnauthorizedException.java
│   ├── SessionIdConflictException.java
│   └── TriageException.java
└── util/
    ├── SessionIdGenerator.java
    ├── HandoverSummaryGenerator.java
    └── DistanceCalculator.java

backend/src/main/resources/
├── db/migration/
│   ├── V1__Initial_Schema.sql
│   ├── V2__Add_Health_Profiles.sql
│   ├── V3__Add_Caregivers.sql
│   ├── V4__Add_Hospitals_Ambulances.sql
│   └── V5__Add_Audit_Logs.sql
├── application.yml
├── application-dev.yml
├── application-prod.yml
└── data.sql                        # Seed data for development
```

#### 2.2.3 Python NLP Service (FastAPI)

**Responsibilities:**
- Symptom extraction from text
- Multilingual support (EN, HI, MR)
- Medical entity recognition (symptoms, duration, severity)
- Structured JSON output for Java backend
- No triage decisions (extract only, Java decides)

**Python Project Structure:**
```
nlp-service/
├── app/
│   ├── main.py                    # FastAPI application
│   ├── api/
│   │   └── routes/
│   │       ├── health.py          # Health check endpoint
│   │       └── extraction.py      # Symptom extraction endpoint
│   ├── models/
│   │   ├── request.py             # Pydantic request models
│   │   └── response.py            # Pydantic response models
│   ├── services/
│   │   ├── spacy_service.py       # spaCy NLP wrapper
│   │   ├── symptom_extractor.py   # Custom extraction logic
│   │   └── translator.py          # Language translation
│   ├── core/
│   │   ├── config.py              # Environment config
│   │   └── logging.py             # Logging setup
│   └── utils/
│       ├── validators.py
│       └── helpers.py
├── models/                        # Trained spaCy models
│   ├── en_core_web_sm/
│   ├── hi_core_news_sm/
│   └── medical_ner/               # Custom trained model
├── tests/
│   ├── test_extraction.py
│   └── test_api.py
├── requirements.txt
├── Dockerfile
└── README.md
```

---

## 3. DATABASE DESIGN

### 3.1 Entity Relationship Diagram (ER Diagram)

```
┌─────────────────┐
│     users       │
├─────────────────┤
│ id (PK)         │
│ username        │◄──────────┐
│ email           │           │
│ password_hash   │           │
│ mobile          │           │
│ email_verified  │           │
│ mobile_verified │           │
│ enabled         │           │
│ created_at      │           │
│ updated_at      │           │
└────────┬────────┘           │
         │                    │
         │ M                  │
         │                    │
         ▼ N                  │
┌─────────────────┐           │
│   user_roles    │           │
├─────────────────┤           │
│ user_id (PK,FK) │           │
│ role_id (PK,FK) │           │
└────────┬────────┘           │
         │                    │
         │ N                  │
         │                    │
         ▼ M                  │
┌─────────────────┐           │
│     roles       │           │
├─────────────────┤           │
│ id (PK)         │           │
│ name            │           │
│ description     │           │
└─────────────────┘           │
                              │
                              │
┌─────────────────────────────┴──────────────────┐
│            health_profiles                     │
├────────────────────────────────────────────────┤
│ id (PK)                                        │
│ user_id (FK) ──────────────────────────────────┘
│ blood_type                                     │
│ height_cm                                      │
│ weight_kg                                      │
│ chronic_conditions                             │
│ allergies                                      │
│ emergency_contact_name                         │
│ emergency_contact_phone                        │
│ created_at                                     │
│ updated_at                                     │
└──────┬───────────────────────────────┬─────────┘
       │                               │
       │ 1                             │ 1
       │                               │
       ▼ M                             ▼ M
┌──────────────────┐         ┌───────────────────┐
│ medical_history  │         │   medications     │
├──────────────────┤         ├───────────────────┤
│ id (PK)          │         │ id (PK)           │
│ profile_id (FK)  │         │ profile_id (FK)   │
│ condition_name   │         │ medication_name   │
│ diagnosed_date   │         │ dosage            │
│ treatment        │         │ frequency         │
│ notes            │         │ prescrib
ed_by        │
│ is_active        │         │ start_date        │
│ created_at       │         │ end_date          │
└──────────────────┘         │ notes             │
                             │ is_active         │
                             │ created_at        │
                             └───────────────────┘

┌──────────────────────────────────────────────────────────┐
│             caregivers                                    │
├──────────────────────────────────────────────────────────┤
│ id (PK)                                                   │
│ senior_user_id (FK → users.id)                           │
│ caregiver_user_id (FK → users.id)                        │
│ relationship                                              │
│ is_primary                                                │
│ consent_given                                             │
│ consent_date                                              │
│ created_at                                                │
└───────────────────────────────────────────┬──────────────┘
                                            │
                                            │ 1
                                            │
                                            ▼ M
                              ┌──────────────────────────────┐
                              │  caregiver_permissions       │
                              ├──────────────────────────────┤
                              │ id (PK)                      │
                              │ caregiver_id (FK)            │
                              │ permission_type              │
                              │ granted                      │
                              │ updated_at                   │
                              └──────────────────────────────┘

┌────────────────────────────────────────────────────────────┐
│           emergency_sessions                               │
├────────────────────────────────────────────────────────────┤
│ id (PK)                                                    │
│ session_id (UK)                                            │
│ user_id (FK → users.id)                                    │
│ health_profile_id (FK → health_profiles.id)                │
│ symptoms                                                   │
│ language                                                   │
│ nlp_extracted_data (JSONB)                                 │
│ urgency_level (RED/ORANGE/GREEN)                           │
│ matched_keywords                                           │
│ triage_reasoning                                           │
│ handover_summary                                           │
│ location_lat                                               │
│ location_lng                                               │
│ location_address                                           │
│ status                                                     │
│ hospital_id (FK → hospitals.id)                            │
│ ambulance_id (FK → ambulances.id)                          │
│ transport_accepted_at                                      │
│ arrived_at_patient_at                                      │
│ arrived_at_hospital_at                                     │
│ completed_at                                               │
│ created_at                                                 │
│ updated_at                                                 │
│ created_by                                                 │
│ last_modified_by                                           │
└─────────────────┬──────────────────────────────┬──────────┘
                  │                              │
                  │ M                            │ M
                  │                              │
                  ▼ 1                            ▼ 1
         ┌────────────────┐            ┌──────────────────┐
         │   hospitals    │            │   ambulances     │
         ├────────────────┤            ├──────────────────┤
         │ id (PK)        │            │ id (PK)          │
         │ name           │            │ vehicle_id (UK)  │
         │ address        │            │ hospital_id (FK) │
         │ lat            │            │ driver_name      │
         │ lng            │            │ driver_phone     │
         │ phone          │            │ zone             │
         │ email          │            │ is_available     │
         │ specialties    │            │ current_lat      │
         │ is_active      │            │ current_lng      │
         │ created_at     │            │ last_updated     │
         └────┬───────────┘            │ created_at       │
              │                        └──────────────────┘
              │ 1
              │
              ▼ M
    ┌──────────────────────┐
    │ hospital_capacities  │
    ├──────────────────────┤
    │ id (PK)              │
    │ hospital_id (FK)     │
    │ bed_type             │
    │ total_beds           │
    │ available_beds       │
    │ last_updated         │
    └──────────────────────┘

┌──────────────────────────────────────────┐
│           audit_logs                     │
├──────────────────────────────────────────┤
│ id (PK)                                  │
│ user_id (FK → users.id)                  │
│ action                                   │
│ entity_type                              │
│ entity_id                                │
│ old_value (JSONB)                        │
│ new_value (JSONB)                        │
│ ip_address                               │
│ user_agent                               │
│ created_at                               │
└──────────────────────────────────────────┘

┌──────────────────────────────────────────┐
│         notifications                    │
├──────────────────────────────────────────┤
│ id (PK)                                  │
│ user_id (FK → users.id)                  │
│ emergency_session_id (FK)                │
│ type (SMS/EMAIL/PUSH)                    │
│ message                                  │
│ sent_at                                  │
│ delivered_at                             │
│ status                                   │
│ created_at                               │
└──────────────────────────────────────────┘
```

### 3.2 Database Schema Specification

#### 3.2.1 Users Table

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    mobile VARCHAR(20),
    email_verified BOOLEAN DEFAULT FALSE,
    mobile_verified BOOLEAN DEFAULT FALSE,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
```

#### 3.2.2 Roles & Permissions

```sql
CREATE TABLE roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,  -- SENIOR, CAREGIVER, EMERGENCY_STAFF, TRANSPORT_STAFF, HOSPITAL_STAFF, ADMIN
    description TEXT
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id INTEGER NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- Seed roles
INSERT INTO roles (name, description) VALUES
('SENIOR', 'Senior citizen - can create emergency requests'),
('CAREGIVER', 'Family caregiver - receives notifications'),
('EMERGENCY_STAFF', 'Emergency response coordinator'),
('TRANSPORT_STAFF', 'Ambulance driver / transport provider'),
('HOSPITAL_STAFF', 'Hospital emergency staff'),
('ADMIN', 'System administrator');
```

#### 3.2.3 Health Profiles

**⚠️ CORRECTION APPLIED:** Health profiles are now OPTIONAL for emergency creation. Profile completeness tracked as percentage (0-100%).

```sql
CREATE TABLE health_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    blood_type VARCHAR(10),
    height_cm DECIMAL(5,2),
    weight_kg DECIMAL(5,2),
    chronic_conditions TEXT,  -- DEPRECATED: Use chronic_conditions table
    allergies TEXT,  -- DEPRECATED: Use allergies table
    emergency_contact_name VARCHAR(100),
    emergency_contact_phone VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_health_profiles_user_id ON health_profiles(user_id);
```

#### 3.2.4 Vital Readings (Time-Series)

**⚠️ NEW TABLE - CORRECTION APPLIED:** Vital readings are now time-series (never overwrite history). Each reading has source, timestamp, and verification status.

```sql
CREATE TABLE vital_readings (
    id BIGSERIAL PRIMARY KEY,
    profile_id BIGINT NOT NULL REFERENCES health_profiles(id) ON DELETE CASCADE,
    reading_type VARCHAR(50) NOT NULL,  -- BLOOD_PRESSURE, HEART_RATE, TEMPERATURE, BLOOD_GLUCOSE, WEIGHT, OXYGEN_SATURATION
    value_numeric DECIMAL(10,2),
    value_text VARCHAR(255),  -- For non-numeric like blood type
    unit VARCHAR(20),  -- mmHg, bpm, °C, kg, %, etc.
    measured_at TIMESTAMP NOT NULL,
    source VARCHAR(50) NOT NULL,  -- SELF_REPORTED, WEARABLE_DEVICE, HOSPITAL, CLINIC, CAREGIVER
    device_id VARCHAR(100),  -- Optional: device serial number
    verification_status VARCHAR(20) DEFAULT 'UNVERIFIED',  -- UNVERIFIED, VERIFIED, DISPUTED
    verified_by BIGINT REFERENCES users(id),
    verified_at TIMESTAMP,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_vital_readings_profile_type ON vital_readings(profile_id, reading_type, measured_at DESC);
CREATE INDEX idx_vital_readings_source ON vital_readings(source);
```

#### 3.2.5 Allergies (Structured)

**⚠️ NEW TABLE - CORRECTION APPLIED:** Allergies are now structured with severity, type, and verification status (not flat text).

```sql
CREATE TABLE allergies (
    id BIGSERIAL PRIMARY KEY,
    profile_id BIGINT NOT NULL REFERENCES health_profiles(id) ON DELETE CASCADE,
    allergen VARCHAR(255) NOT NULL,
    allergy_type VARCHAR(50) NOT NULL,  -- MEDICATION, FOOD, ENVIRONMENTAL, INSECT, OTHER
    severity VARCHAR(20) NOT NULL,  -- MILD, MODERATE, SEVERE, LIFE_THREATENING
    reaction TEXT,  -- Description of reaction
    diagnosed_date DATE,
    source VARCHAR(50) NOT NULL DEFAULT 'SELF_REPORTED',
    verification_status VARCHAR(20) DEFAULT 'UNVERIFIED',
    is_active BOOLEAN DEFAULT TRUE,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_allergies_profile_active ON allergies(profile_id, is_active);
CREATE INDEX idx_allergies_severity ON allergies(severity);
```

#### 3.2.6 Medications (with History)

**⚠️ CORRECTION APPLIED:** Medications now include route, verification status, and discontinuation tracking.

```sql
CREATE TABLE medications (
    id BIGSERIAL PRIMARY KEY,
    profile_id BIGINT NOT NULL REFERENCES health_profiles(id) ON DELETE CASCADE,
    medication_name VARCHAR(255) NOT NULL,
    dosage VARCHAR(100),
    frequency VARCHAR(100),  -- ONCE_DAILY, TWICE_DAILY, AS_NEEDED, etc.
    route VARCHAR(50),  -- ORAL, INJECTION, TOPICAL, INHALED, etc.
    prescribed_by VARCHAR(255),
    prescribed_date DATE,
    start_date DATE NOT NULL,
    end_date DATE,
    discontinuation_reason TEXT,
    source VARCHAR(50) NOT NULL DEFAULT 'SELF_REPORTED',
    verification_status VARCHAR(20) DEFAULT 'UNVERIFIED',
    is_active BOOLEAN DEFAULT TRUE,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_medications_profile_id ON medications(profile_id);
CREATE INDEX idx_medications_is_active ON medications(profile_id, is_active);
```

#### 3.2.7 Chronic Conditions (Structured)

**⚠️ NEW TABLE - CORRECTION APPLIED:** Chronic conditions now structured with ICD-10 codes, severity, and verification status.

```sql
CREATE TABLE chronic_conditions (
    id BIGSERIAL PRIMARY KEY,
    profile_id BIGINT NOT NULL REFERENCES health_profiles(id) ON DELETE CASCADE,
    condition_name VARCHAR(255) NOT NULL,
    icd_10_code VARCHAR(20),  -- Optional: standardized code
    diagnosed_date DATE,
    diagnosed_by VARCHAR(255),
    severity VARCHAR(20),  -- MILD, MODERATE, SEVERE, CONTROLLED, UNCONTROLLED
    current_treatment TEXT,
    source VARCHAR(50) NOT NULL DEFAULT 'SELF_REPORTED',
    verification_status VARCHAR(20) DEFAULT 'UNVERIFIED',
    is_active BOOLEAN DEFAULT TRUE,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_chronic_conditions_profile ON chronic_conditions(profile_id, is_active);
```

#### 3.2.6 Caregivers

```sql
CREATE TABLE caregivers (
    id BIGSERIAL PRIMARY KEY,
    senior_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    caregiver_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    relationship VARCHAR(50),  -- SPOUSE, SON, DAUGHTER, SIBLING, FRIEND, PROFESSIONAL
    is_primary BOOLEAN DEFAULT FALSE,
    consent_given BOOLEAN DEFAULT FALSE,
    consent_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(senior_user_id, caregiver_user_id)
);

CREATE INDEX idx_caregivers_senior_user_id ON caregivers(senior_user_id);
CREATE INDEX idx_caregivers_caregiver_user_id ON caregivers(caregiver_user_id);
```

#### 3.2.8 Caregivers

**⚠️ CORRECTION APPLIED:** Caregiver relationships now track consent with audit trail.

```sql
CREATE TABLE caregivers (
    id BIGSERIAL PRIMARY KEY,
    senior_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    caregiver_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    relationship VARCHAR(50),  -- SPOUSE, SON, DAUGHTER, SIBLING, FRIEND, PROFESSIONAL
    is_primary BOOLEAN DEFAULT FALSE,
    consent_given BOOLEAN DEFAULT FALSE,
    consent_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(senior_user_id, caregiver_user_id)
);

CREATE INDEX idx_caregivers_senior_user_id ON caregivers(senior_user_id);
CREATE INDEX idx_caregivers_caregiver_user_id ON caregivers(caregiver_user_id);
```

#### 3.2.9 Caregiver Permissions

**⚠️ CORRECTION APPLIED:** Granular permissions for caregivers (view profile, receive notifications, view location, call ambulance).

```sql
CREATE TABLE caregiver_permissions (
    id BIGSERIAL PRIMARY KEY,
    caregiver_id BIGINT NOT NULL REFERENCES caregivers(id) ON DELETE CASCADE,
    permission_type VARCHAR(50) NOT NULL,  -- VIEW_HEALTH_PROFILE, RECEIVE_NOTIFICATIONS, VIEW_LOCATION, CALL_AMBULANCE
    granted BOOLEAN DEFAULT FALSE,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_caregiver_permissions_caregiver_id ON caregiver_permissions(caregiver_id);
```

#### 3.2.10 Caregiver Permission History (Audit Trail)

**⚠️ NEW TABLE - CORRECTION APPLIED:** Audit trail for all caregiver permission changes.

```sql
CREATE TABLE caregiver_permission_history (
    id BIGSERIAL PRIMARY KEY,
    caregiver_id BIGINT NOT NULL REFERENCES caregivers(id) ON DELETE CASCADE,
    permission_type VARCHAR(50) NOT NULL,
    old_value BOOLEAN,
    new_value BOOLEAN NOT NULL,
    changed_by BIGINT REFERENCES users(id),
    change_reason TEXT,
    ip_address VARCHAR(45),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_caregiver_permission_history ON caregiver_permission_history(caregiver_id, created_at DESC);
```

#### 3.2.11 Refresh Tokens

**⚠️ NEW TABLE - CORRECTION APPLIED:** Refresh token rotation and revocation tracking for security.

```sql
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,  -- SHA-256 hash
    family_id UUID NOT NULL,  -- Links rotated tokens
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN DEFAULT FALSE,
    revoked_at TIMESTAMP,
    revoked_reason VARCHAR(100),
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_family_id ON refresh_tokens(family_id);
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
```

#### 3.2.12 Hospitals

```sql
CREATE TABLE hospitals (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address TEXT NOT NULL,
    lat DECIMAL(10,7) NOT NULL,
    lng DECIMAL(10,7) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100),
    specialties TEXT,  -- CSV or JSON array
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_hospitals_location ON hospitals USING GIST (
    ll_to_earth(lat, lng)
);  -- PostGIS or earthdistance extension for geo queries
```

#### 3.2.13 Hospital Capacities

```sql
CREATE TABLE hospital_capacities (
    id BIGSERIAL PRIMARY KEY,
    hospital_id BIGINT NOT NULL REFERENCES hospitals(id) ON DELETE CASCADE,
    bed_type VARCHAR(50) NOT NULL,  -- EMERGENCY, ICU, GENERAL
    total_beds INTEGER NOT NULL,
    available_beds INTEGER NOT NULL,
    last_updated TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_hospital_capacities_hospital_id ON hospital_capacities(hospital_id);
```

#### 3.2.14 Ambulances

```sql
CREATE TABLE ambulances (
    id BIGSERIAL PRIMARY KEY,
    vehicle_id VARCHAR(20) UNIQUE NOT NULL,  -- AMB-001
    hospital_id BIGINT REFERENCES hospitals(id) ON DELETE SET NULL,
    driver_name VARCHAR(100) NOT NULL,
    driver_phone VARCHAR(20) NOT NULL,
    zone VARCHAR(50),  -- Zone A, Zone B, etc.
    is_available BOOLEAN DEFAULT TRUE,
    current_lat DECIMAL(10,7),
    current_lng DECIMAL(10,7),
    last_updated TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ambulances_is_available ON ambulances(is_available);
CREATE INDEX idx_ambulances_zone ON ambulances(zone);
```

#### 3.2.15 Emergency Sessions (Enhanced)

**⚠️ CORRECTION APPLIED:** Emergency sessions now support NULL health_profile_id, include triage_method and triage_rules_version for transparency.

```sql
CREATE TABLE emergency_sessions (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(50) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    health_profile_id BIGINT REFERENCES health_profiles(id) ON DELETE RESTRICT,  -- ⚠️ CAN BE NULL
    
    -- Symptoms and NLP
    symptoms TEXT NOT NULL,
    language VARCHAR(5) NOT NULL,  -- en, hi, mr
    nlp_extracted_data JSONB,  -- From Python service
    
    -- Triage Assessment (Java rules)
    urgency_level VARCHAR(10) NOT NULL,  -- RED, ORANGE, GREEN
    triage_method VARCHAR(30) NOT NULL,  -- SOS_DIRECT, NLP_ENHANCED, KEYWORD_FALLBACK, HUMAN_REVIEW_REQUIRED
    triage_rules_version VARCHAR(10),  -- e.g., "1.0.0"
    matched_keywords TEXT,  -- CSV
    triage_reasoning TEXT NOT NULL,
    handover_summary TEXT NOT NULL,
    
    -- Location
    location_lat DECIMAL(10,7),
    location_lng DECIMAL(10,7),
    location_address TEXT,
    
    -- Status and Coordination
    status VARCHAR(30) NOT NULL,  -- CREATED, ACKNOWLEDGED, TRANSPORT_ASSIGNED, etc.
    hospital_id BIGINT REFERENCES hospitals(id) ON DELETE RESTRICT,
    ambulance_id BIGINT REFERENCES ambulances(id) ON DELETE RESTRICT,
    
    -- Timestamps
    transport_accepted_at TIMESTAMP,
    arrived_at_patient_at TIMESTAMP,
    arrived_at_hospital_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    -- Audit
    created_by VARCHAR(100),
    last_modified_by VARCHAR(100)
);

CREATE INDEX idx_emergency_sessions_session_id ON emergency_sessions(session_id);
CREATE INDEX idx_emergency_sessions_user_id ON emergency_sessions(user_id);
CREATE INDEX idx_emergency_sessions_status ON emergency_sessions(status);
CREATE INDEX idx_emergency_sessions_urgency ON emergency_sessions(urgency_level);
CREATE INDEX idx_emergency_sessions_triage_method ON emergency_sessions(triage_method);
CREATE INDEX idx_emergency_sessions_created_at ON emergency_sessions(created_at DESC);
CREATE INDEX idx_emergency_sessions_hospital_id ON emergency_sessions(hospital_id);
CREATE INDEX idx_emergency_sessions_ambulance_id ON emergency_sessions(ambulance_id);
CREATE INDEX idx_emergency_sessions_user_created ON emergency_sessions(user_id, created_at DESC);  -- For duplicate detection
```

#### 3.2.16 Audit Logs

```sql
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(50) NOT NULL,  -- CREATE, UPDATE, DELETE, LOGIN, LOGOUT
    entity_type VARCHAR(50) NOT NULL,  -- EMERGENCY_SESSION, HEALTH_PROFILE, etc.
    entity_id BIGINT,
    old_value JSONB,
    new_value JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);
```

#### 3.2.17 Notifications

```sql
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    emergency_session_id BIGINT REFERENCES emergency_sessions(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL,  -- SMS, EMAIL, PUSH
    message TEXT NOT NULL,
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    status VARCHAR(20) NOT NULL,  -- PENDING, SENT, DELIVERED, FAILED
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_emergency_session_id ON notifications(emergency_session_id);
CREATE INDEX idx_notifications_status ON notifications(status);
```

### 3.3 Flyway Migration Strategy

**⚠️ CORRECTION APPLIED:** Safe baseline-on-migrate strategy to preserve existing data.

**Migration Naming:**
- `V1__Baseline_Existing_Schema.sql` - Baseline existing emergency_sessions table (preserves data)
- `V2__Add_Users_And_Roles.sql` - Users, roles, user_roles, refresh_tokens
- `V3__Add_Health_Profiles.sql` - Health profiles, vital_readings, allergies, medications, chronic_conditions
- `V4__Add_Caregivers.sql` - Caregivers, caregiver_permissions, caregiver_permission_history
- `V5__Add_Hospitals_Ambulances.sql` - Hospitals, capacities, ambulances
- `V6__Update_Emergency_Sessions.sql` - Add triage_method, triage_rules_version columns
- `V7__Add_Audit_Notifications.sql` - Audit logs and notifications
- `V8__Add_Indexes.sql` - Additional performance indexes
- `V9__Seed_Data.sql` - Initial data for development (roles, demo hospitals)

**Flyway Configuration (application.yml):**
```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true  # ⚠️ CRITICAL: Preserves existing tables
    baseline-version: 0
    locations: classpath:db/migration
    validate-on-migrate: true
    table: flyway_schema_history
```

**Safety Protocol:**
1. Backup database before migration: `pg_dump -h localhost -U postgres -d aayush_db > backup_$(date +%Y%m%d).sql`
2. Test migrations in dev environment first
3. V1 migration checks if tables exist before creating (preserves data)
4. All migrations have corresponding rollback scripts
5. Monitor `flyway_schema_history` table for applied migrations

---

## 4. API SPECIFICATIONS

### 4.1 Authentication Endpoints

#### POST /api/auth/register
**Description:** Register a new user account

**⚠️ SECURITY:** Public registration ONLY allows SENIOR and CAREGIVER roles. Staff accounts must be created by admin.

**Request:**
```json
{
  "username": "ramesh_kumar",
  "email": "ramesh@example.com",
  "password": "SecurePass123!",
  "mobile": "+91-9876543210",
  "role": "SENIOR"  // ONLY "SENIOR" or "CAREGIVER" allowed for public registration
}
```

**Validation:**
- `role` must be "SENIOR" or "CAREGIVER" (others rejected with 403 Forbidden)
- EMERGENCY_STAFF, TRANSPORT_STAFF, HOSPITAL_STAFF, ADMIN must be created by admin via `/api/admin/users`

**Response (201 CREATED):**
```json
{
  "userId": 1,
  "username": "ramesh_kumar",
  "email": "ramesh@example.com",
  "roles": ["SENIOR"],
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 86400
}
```

**Response (403 FORBIDDEN) - Attempted Staff Registration:**
```json
{
  "timestamp": "2026-10-31T10:00:00Z",
  "status": 403,
  "error": "Forbidden",
  "message": "Public registration restricted to SENIOR and CAREGIVER roles. Staff accounts must be created by administrator.",
  "path": "/api/auth/register"
}
```

#### POST /api/auth/login
**Description:** Authenticate user and get JWT tokens

**Request:**
```json
{
  "username": "ramesh_kumar",
  "password": "SecurePass123!"
}
```

**Response (200 OK):**
```json
{
  "userId": 1,
  "username": "ramesh_kumar",
  "email": "ramesh@example.com",
  "roles": ["SENIOR"],
  "accessToken": "eyJhbGci...",
  "refreshToken": "eyJhbGci...",
  "expiresIn": 86400
}
```

#### POST /api/auth/refresh
**Description:** Refresh access token using refresh token

**⚠️ SECURITY:** Implements token rotation - old refresh token is revoked, new one issued

**Request:**
```json
{
  "refreshToken": "eyJhbGci..."
}
```

**Behavior:**
1. Validates refresh token (signature, expiration, revocation status)
2. **Detects token reuse** - if token already revoked, revokes entire token family
3. Revokes current refresh token
4. Issues new access token + new refresh token (same family ID)
5. Stores new refresh token in database

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGci...",  // New access token
  "refreshToken": "eyJhbGci...",  // New refresh token (old one revoked)
  "expiresIn": 86400
}
```

**Response (401 UNAUTHORIZED) - Token Reuse Detected:**
```json
{
  "timestamp": "2026-10-31T10:00:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Refresh token has been revoked. Token reuse detected - entire session terminated for security.",
  "path": "/api/auth/refresh"
}
```

**Security Notes:**
- Token reuse indicates potential attack (stolen token used after legitimate refresh)
- Entire token family revoked on reuse detection
- User must re-login with username/password

#### POST /api/auth/logout
**Description:** Invalidate refresh token

**Headers:** `Authorization: Bearer {accessToken}`

**Response (204 NO CONTENT)**

---

### 4.2 Emergency Session Endpoints

#### POST /api/emergency-sessions/sos ⚡ NEW: DIRECT SOS
**Description:** Create IMMEDIATE emergency session (life-threatening)

**Authentication:** Required (SENIOR role)

**Performance Requirement:** < 500ms (p95)

**Request:**
```json
{
  "symptoms": "Help"  // OPTIONAL - can be empty string
}
```

**Behavior:**
- Creates session in PostgreSQL immediately
- Sets `urgency_level = 'RED'` (assumed critical)
- Sets `triage_method = 'SOS_DIRECT'`
- Skips Python NLP call
- Skips health profile retrieval (background job)
- Skips location lookup (background job)
- Returns session ID in < 500ms

**Response (201 CREATED):**
```json
{
  "id": 1,
  "sessionId": "EMR-1730364123456-ABC123XYZ",
  "urgencyLevel": "RED",
  "triageMethod": "SOS_DIRECT",
  "status": "CREATED",
  "createdAt": "2026-10-31T10:35:23Z",
  "message": "Emergency SOS received. Help is on the way. Call 108/102/112 immediately if life-threatening."
}
```

**Idempotency:**
- Duplicate SOS within 5 minutes returns existing session (same sessionId)
- Prevents multiple emergency creations from panic clicking

---

#### POST /api/emergency-sessions (Standard Emergency)
**Description:** Create emergency session with enhanced symptom analysis

**Authentication:** Required (SENIOR role)

**Request:**
```json
{
  "symptoms": "I have severe chest pain and difficulty breathing for the past 20 minutes",
  "language": "en",
  "allergies": "Penicillin"  // OPTIONAL - used if profile incomplete
}
```

**Behavior:**
- Retrieves health profile (if exists, not required)
- Calls Python NLP with 3-second timeout
- Falls back to keyword triage if NLP fails/low confidence
- Java triage engine determines urgency
- Uses profile allergies if available, otherwise request allergies

**Response (201 CREATED):**
```json
{
  "id": 1,
  "sessionId": "EMR-1730364123456-ABC123XYZ",
  "userId": 1,
  "patientProfile": {
    "name": "Ramesh Kumar",
    "age": 72,
    "bloodType": "O+",
    "allergies": "Penicillin",
    "emergencyContact": "+91-9876543210",
    "profileCompleteness": 75  // Percentage (0-100)
  },
  "symptoms": "I have severe chest pain and difficulty breathing for the past 20 minutes",
  "language": "en",
  "nlpExtraction": {
    "nlpAvailable": true,
    "confidence": 0.87,
    "symptoms": ["chest pain", "difficulty breathing"],
    "duration": "20 minutes",
    "severity": "severe",
    "redFlags": ["chest pain", "difficulty breathing"],
    "negatedSymptoms": [],  // e.g., ["no fever"]
    "uncertainSymptoms": []  // e.g., ["maybe dizzy"]
  },
  "triageAssessment": {
    "urgencyLevel": "RED",
    "matchedKeywords": ["chest pain", "difficulty breathing"],
    "reasoning": "Critical symptoms detected that may indicate life-threatening condition",
    "rulesVersion": "1.0.0",
    "triageMethod": "NLP_ENHANCED",  // or KEYWORD_FALLBACK, SOS_DIRECT
    "disclaimer": "⚠️ This urgency classification is generated by an AUTOMATED SOFTWARE PROTOTYPE for demonstration purposes only. It is NOT a clinically validated medical triage system. For life-threatening emergencies, immediately call 108, 102, or 112."
  },
  "handoverSummary": "Emergency session EMR-1730364123456-ABC123XYZ: Ramesh Kumar, 72 years old, reports severe chest pain and difficulty breathing for 20 minutes. Known allergies: Penicillin. URGENT: RED priority - Critical symptoms detected.",
  "location": {
    "lat": 18.5204,
    "lng": 73.8567,
    "address": "MG Road, Pune, Maharashtra 411001"
  },
  "status": "CREATED",
  "mockDataIndicators": {
    "ambulanceDispatchMocked": true,
    "hospitalCapacityMocked": true,
    "etaMocked": true,
    "notificationsMocked": true,
    "mockDataWarning": "This system uses simulated data for demonstration. Do not rely on this for actual emergency coordination."
  },
  "createdAt": "2026-10-31T10:35:23Z",
  "updatedAt": "2026-10-31T10:35:23Z"
}
```

#### GET /api/emergency-sessions
**Description:** Get all emergency sessions (paginated, filtered)

**Authentication:** Required (EMERGENCY_STAFF, TRANSPORT_STAFF, HOSPITAL_STAFF, ADMIN)

**Query Parameters:**
- `page` (int, default: 0)
- `size` (int, default: 20, max: 100)
- `status` (string, optional): CREATED, ACKNOWLEDGED, TRANSPORT_ASSIGNED, etc.
- `urgency` (string, optional): RED, ORANGE, GREEN
- `sort` (string, default: createdAt,desc)

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": 1,
      "sessionId": "EMR-1730364123456-ABC123XYZ",
      "patientProfile": { "name": "Ramesh Kumar", "age": 72 },
      "urgencyLevel": "RED",
      "status": "CREATED",
      "createdAt": "2026-10-31T10:35:23Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

#### GET /api/emergency-sessions/{sessionId}
**Description:** Get emergency session by ID

**Authentication:** Required (user must own session, be caregiver, or staff)

**Response (200 OK):** Same as POST response

#### PATCH /api/emergency-sessions/{sessionId}/status
**Description:** Update emergency session status

**Authentication:** Required (EMERGENCY_STAFF, TRANSPORT_STAFF, HOSPITAL_STAFF)

**Request:**
```json
{
  "status": "ACKNOWLEDGED"
}
```

**Response (200 OK):** Full session object with updated status

#### PATCH /api/emergency-sessions/{sessionId}/assign-transport
**Description:** Assign ambulance to emergency session

**Authentication:** Required (EMERGENCY_STAFF, TRANSPORT_STAFF)

**Request:**
```json
{
  "ambulanceId": 5
}
```

**Response (200 OK):** Full session object with ambulance assigned

#### PATCH /api/emergency-sessions/{sessionId}/assign-hospital
**Description:** Assign hospital to emergency session

**Authentication:** Required (EMERGENCY_STAFF, HOSPITAL_STAFF)

**Request:**
```json
{
  "hospitalId": 3
}
```

**Response (200 OK):** Full session object with hospital assigned

---

### 4.3 Health Profile Endpoints

#### POST /api/health-profiles
**Description:** Create or update health profile for authenticated user

**Authentication:** Required (SENIOR role)

**Request:**
```json
{
  "bloodType": "O+",
  "heightCm": 168,
  "weightKg": 72,
  "chronicConditions": "Hypertension, Type 2 Diabetes",
  "allergies": "Penicillin, Sulfa drugs",
  "emergencyContactName": "Priya Kumar",
  "emergencyContactPhone": "+91-9876543211"
}
```

**Response (201 CREATED):**
```json
{
  "id": 1,
  "userId": 1,
  "bloodType": "O+",
  "heightCm": 168,
  "weightKg": 72,
  "chronicConditions": "Hypertension, Type 2 Diabetes",
  "allergies": "Penicillin, Sulfa drugs",
  "emergencyContactName": "Priya Kumar",
  "emergencyContactPhone": "+91-9876543211",
  "createdAt": "2026-10-31T10:00:00Z",
  "updatedAt": "2026-10-31T10:00:00Z"
}
```

#### GET /api/health-profiles/me
**Description:** Get authenticated user's health profile

**Authentication:** Required

**Response (200 OK):** Same as POST response

---

### 4.4 Hospital Endpoints

#### GET /api/hospitals
**Description:** Get all hospitals (paginated)

**Authentication:** Required

**Query Parameters:**
- `lat` (decimal, optional): Latitude for nearest search
- `lng` (decimal, optional): Longitude for nearest search
- `radiusKm` (decimal, default: 50): Search radius
- `specialty` (string, optional): Filter by specialty
- `page`, `size`, `sort`

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": 1,
      "name": "City General Hospital",
      "address": "MG Road, Pune 411001",
      "lat": 18.5204,
      "lng": 73.8567,
      "phone": "+91-20-12345678",
      "specialties": ["Emergency", "Cardiology", "ICU"],
      "capacities": [
        { "bedType": "EMERGENCY", "totalBeds": 12, "availableBeds": 7 },
        { "bedType": "ICU", "totalBeds": 8, "availableBeds": 3 }
      ],
      "distanceKm": 2.5
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 15,
  "totalPages": 1
}
```

#### POST /api/hospitals (Admin only)
**Description:** Create new hospital

#### PUT /api/hospitals/{id}/capacities
**Description:** Update hospital bed capacities

---

### 4.5 Python NLP Service API (Called by Java Backend)

#### POST /api/v1/extract-symptoms
**Description:** Extract symptoms and entities from text

**Request:**
```json
{
  "text": "I have severe chest pain and difficulty breathing for the past 20 minutes",
  "language": "en"
}
```

**Response (200 OK):**
```json
{
  "originalText": "I have severe chest pain and difficulty breathing for the past 20 minutes",
  "language": "en",
  "entities": {
    "symptoms": ["chest pain", "difficulty breathing"],
    "bodyParts": ["chest"],
    "severity": ["severe"],
    "duration": "20 minutes",
    "onset": "past"
  },
  "extractedPhrases": [
    {
      "text": "chest pain",
      "category": "symptom",
      "startIndex": 14,
      "endIndex": 24,
      "confidence": 0.95
    },
    {
      "text": "difficulty breathing",
      "category": "symptom",
      "startIndex": 29,
      "endIndex": 49,
      "confidence": 0.92
    }
  ],
  "structuredSummary": {
    "primarySymptoms": ["chest pain", "difficulty breathing"],
    "redFlags": ["chest pain", "difficulty breathing"],
    "duration": "20 minutes",
    "severity": "severe"
  },
  "confidence": 0.93
}
```

---

## 5. WEBSOCKET EVENTS

### 5.1 WebSocket Configuration

**Endpoint:** `wss://aayush.example.com/ws`

**Protocol:** STOMP over WebSocket

**Authentication:** JWT token in `Authorization` header

### 5.2 Message Destinations

#### Subscribe Destinations (Client → Server)

```
/user/queue/notifications        # Personal notifications for authenticated user
/topic/emergency.{sessionId}     # Updates for specific emergency session
/topic/emergency.new             # New emergency sessions (EMERGENCY_STAFF only)
/topic/transport.requests        # Transport requests (TRANSPORT_STAFF only)
/topic/hospital.incoming         # Incoming patients (HOSPITAL_STAFF only)
```

#### Publish Destinations (Server → Client)

**Session Created:**
```json
{
  "type": "SESSION_CREATED",
  "sessionId": "EMR-1730364123456-ABC123XYZ",
  "urgencyLevel": "RED",
  "patientName": "Ramesh Kumar",
  "location": { "lat": 18.5204, "lng": 73.8567 },
  "createdAt": "2026-10-31T10:35:23Z"
}
```

**Status Updated:**
```json
{
  "type": "STATUS_UPDATED",
  "sessionId": "EMR-1730364123456-ABC123XYZ",
  "oldStatus": "CREATED",
  "newStatus": "ACKNOWLEDGED",
  "updatedBy": "emergency_coordinator_1",
  "updatedAt": "2026-10-31T10:36:00Z"
}
```

**Transport Assigned:**
```json
{
  "type": "TRANSPORT_ASSIGNED",
  "sessionId": "EMR-1730364123456-ABC123XYZ",
  "ambulanceId": 5,
  "vehicleId": "AMB-005",
  "driverName": "Rajesh Kumar",
  "estimatedArrival": "2026-10-31T10:45:00Z",
  "assignedAt": "2026-10-31T10:37:00Z"
}
```

**Hospital Assigned:**
```json
{
  "type": "HOSPITAL_ASSIGNED",
  "sessionId": "EMR-1730364123456-ABC123XYZ",
  "hospitalId": 3,
  "hospitalName": "City General Hospital",
  "distanceKm": 2.5,
  "estimatedArrival": "2026-10-31T10:50:00Z",
  "assignedAt": "2026-10-31T10:38:00Z"
}
```

**Caregiver Notification:**
```json
{
  "type": "CAREGIVER_NOTIFICATION",
  "sessionId": "EMR-1730364123456-ABC123XYZ",
  "message": "Emergency alert: Ramesh Kumar has requested medical assistance. Status: ACKNOWLEDGED. Session ID: EMR-1730364123456-ABC123XYZ",
  "urgencyLevel": "RED",
  "createdAt": "2026-10-31T10:35:30Z"
}
```

### 5.3 Frontend WebSocket Integration

```typescript
// src/api/websocket.ts
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

class WebSocketService {
  private client: Client;
  
  connect(accessToken: string) {
    this.client = new Client({
      webSocketFactory: () => new SockJS('https://api.aayush.example.com/ws'),
      connectHeaders: {
        Authorization: `Bearer ${accessToken}`
      },
      onConnect: () => {
        console.log('WebSocket connected');
        this.subscribeToTopics();
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame);
      }
    });
    
    this.client.activate();
  }
  
  subscribeToTopics() {
    // Subscribe to personal notifications
    this.client.subscribe('/user/queue/notifications', (message) => {
      const notification = JSON.parse(message.body);
      this.handleNotification(notification);
    });
    
    // Subscribe to emergency session updates
    this.client.subscribe('/topic/emergency.new', (message) => {
      const event = JSON.parse(message.body);
      this.handleEmergencyEvent(event);
    });
  }
  
  subscribeToSession(sessionId: string, callback: (event: any) => void) {
    this.client.subscribe(`/topic/emergency.${sessionId}`, (message) => {
      const event = JSON.parse(message.body);
      callback(event);
    });
  }
  
  disconnect() {
    if (this.client) {
      this.client.deactivate();
    }
  }
}

export const wsService = new WebSocketService();
```

---

## 6. AUTHENTICATION & AUTHORIZATION

### 6.1 JWT Token Structure

**Access Token (Expires in 24 hours):**
```json
{
  "sub": "ramesh_kumar",
  "userId": 1,
  "email": "ramesh@example.com",
  "roles": ["SENIOR"],
  "iat": 1730364000,
  "exp": 1730450400
}
```

**Refresh Token (Expires in 7 days):**
```json
{
  "sub": "ramesh_kumar",
  "userId": 1,
  "type": "refresh",
  "iat": 1730364000,
  "exp": 1730968800
}
```

### 6.2 Role-Based Access Control (RBAC)

| Endpoint | SENIOR | CAREGIVER | EMERGENCY_STAFF | TRANSPORT_STAFF | HOSPITAL_STAFF | ADMIN |
|----------|--------|-----------|-----------------|-----------------|----------------|-------|
| POST /api/auth/register | ✅ | ✅ | ❌ | ❌ | ❌ | ✅ |
| POST /api/emergency-sessions | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ |
| GET /api/emergency-sessions | Own | Linked | ✅ | ✅ | ✅ | ✅ |
| PATCH /api/emergency-sessions/{id}/status | ❌ | ❌ | ✅ | ✅ | ✅ | ✅ |
| POST /api/health-profiles | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ |
| GET /api/health-profiles/{id} | Own | With consent | ✅ | ✅ | ✅ | ✅ |
| GET /api/hospitals | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| POST /api/hospitals | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |
| POST /api/caregivers | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ |

### 6.3 Security Implementation

**JwtAuthenticationFilter.java:**
```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                String username = jwtService.extractUsername(token);
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    if (jwtService.isTokenValid(token, userDetails)) {
                        UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (Exception e) {
                logger.error("JWT validation failed", e);
            }
        }
        filterChain.doFilter(request, response);
    }
}
```

**Method-Level Security:**
```java
@RestController
@RequestMapping("/api/emergency-sessions")
public class EmergencySessionController {
    
    @PreAuthorize("hasRole('SENIOR')")
    @PostMapping
    public ResponseEntity<EmergencySessionResponse> createSession(@Valid @RequestBody CreateEmergencySessionRequest request) {
        // ...
    }
    
    @PreAuthorize("hasAnyRole('EMERGENCY_STAFF', 'TRANSPORT_STAFF', 'HOSPITAL_STAFF', 'ADMIN')")
    @PatchMapping("/{sessionId}/status")
    public ResponseEntity<EmergencySessionResponse> updateStatus(@PathVariable String sessionId, 
                                                                 @Valid @RequestBody UpdateSessionStatusRequest request) {
        // ...
    }
}
```

---

## 7. PYTHON NLP SERVICE

**⚠️ CRITICAL CONSTRAINTS:**
- Generic spaCy models do NOT recognize medical symptoms reliably
- Marathi NLP is NOT implemented (no verified model available)
- Python extracts symptom facts ONLY - no urgency decisions
- Java triage engine makes ALL urgency decisions
- NLP failure must NOT silently default to GREEN

### 7.1 Service Architecture

**Framework:** FastAPI for async performance

**NLP Pipeline:**
```
Input Text (EN/HI/MR)
    ↓
Language Detection (if not specified)
    ↓
Check Model Availability
    ├─ EN: Medical NER (if available) or Generic
    ├─ HI: Generic spaCy model
    └─ MR: FALLBACK (no model) → return nlp_available=false
    ↓
spaCy NLP Processing (if available)
    ↓
Negation Detection ("no chest pain")
    ↓
Uncertainty Detection ("maybe fever")
    ↓
Medical Entity Recognition (best-effort)
    ↓
Confidence Calculation (dynamic, NOT hardcoded)
    ↓
Structured JSON Output
```

**Critical Rules:**
1. **Never hardcode confidence values** - calculate dynamically based on model + context
2. **Return symptom facts only** - no urgency levels (RED/ORANGE/GREEN)
3. **Explicitly flag negated/uncertain symptoms**
4. **Return `nlp_available=false` for unsupported languages**
5. **Include fallback_reason when NLP unavailable**

### 7.2 FastAPI Implementation

```python
# app/main.py
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List, Optional
import spacy
import logging

app = FastAPI(title="AAYUSH NLP Service", version="1.0.0")
logger = logging.getLogger(__name__)

# Load spaCy models (with realistic availability)
class NLPModels:
    def __init__(self):
        # English: Try medical NER, fallback to generic
        try:
            self.nlp_en = spacy.load("en_ner_bc5cdr_md")  # BioBERT medical NER
            logger.info("English medical NER model loaded")
            self.en_model_type = "MEDICAL_NER"
        except:
            try:
                self.nlp_en = spacy.load("en_core_web_sm")
                logger.warning("Medical NER not available, using generic English model")
                self.en_model_type = "GENERIC"
            except:
                self.nlp_en = None
                self.en_model_type = "UNAVAILABLE"
                logger.error("No English NLP model available")
        
        # Hindi: Generic model only (no medical model available)
        try:
            self.nlp_hi = spacy.load("hi_core_news_sm")
            logger.info("Hindi generic model loaded (NOT medical-specific)")
            self.hi_model_type = "GENERIC"
        except:
            self.nlp_hi = None
            self.hi_model_type = "UNAVAILABLE"
            logger.warning("Hindi NLP model not available")
        
        # Marathi: NOT AVAILABLE
        self.nlp_mr = None
        self.mr_model_type = "UNAVAILABLE"
        logger.warning("Marathi NLP not implemented - no verified model available")
        
        # Negation patterns by language
        self.negation_patterns = {
            'en': ['no', 'not', 'never', 'without', 'lack of', "don't", "doesn't", "didn't", "cannot", "can't"],
            'hi': ['नहीं', 'बिना', 'कभी नहीं'],
            'mr': []  # Not available
        }
        
        # Uncertainty patterns
        self.uncertainty_patterns = {
            'en': ['maybe', 'might', 'possibly', 'perhaps', 'uncertain', 'not sure', 'think', 'probably'],
            'hi': ['शायद', 'हो सकता है', 'संभवतः'],
            'mr': []
        }

models = NLPModels()

class SymptomExtractionRequest(BaseModel):
    text: str
    language: str = "en"  # en, hi, mr, or "auto"

class ExtractedEntity(BaseModel):
    text: str
    label: str  # SYMPTOM, BODY_PART, DURATION, SEVERITY, etc.
    start: int
    end: int
    negated: bool = False
    uncertain: bool = False
    confidence: float  # NEVER hardcoded - calculated dynamically

class SymptomExtractionResponse(BaseModel):
    originalText: str
    language: str
    nlp_available: bool  # NEW: Explicitly flag if NLP worked
    fallback_reason: Optional[str]  # NEW: Explain why NLP unavailable
    model_type: str  # MEDICAL_NER, GENERIC, UNAVAILABLE
    entities: List[ExtractedEntity]
    extractedPhrases: List[dict]  # Backward compatibility
    structuredSummary: dict
    confidence: float  # Overall extraction confidence
    warning: Optional[str]  # NEW: Warnings about limitations

@app.post("/api/v1/extract-symptoms", response_model=SymptomExtractionResponse)
async def extract_symptoms(request: SymptomExtractionRequest):
    # Detect language if auto
    if request.language == "auto":
        request.language = detect_language(request.text)
        logger.info(f"Auto-detected language: {request.language}")
    
    # Check if language is supported
    if request.language == "mr":
        logger.warning("Marathi NLP requested but not implemented")
        return SymptomExtractionResponse(
            originalText=request.text,
            language="mr",
            nlp_available=False,
            fallback_reason="Marathi NLP model not available. System will use keyword-based fallback.",
            model_type="UNAVAILABLE",
            entities=[],
            extractedPhrases=[],
            structuredSummary={},
            confidence=0.0,  # No confidence - no extraction
            warning="Marathi language support is limited to keyword matching in Java backend."
        )
    
    # Get appropriate model
    nlp = models.nlp_en if request.language == "en" else models.nlp_hi
    model_type = models.en_model_type if request.language == "en" else models.hi_model_type
    
    if nlp is None:
        logger.error(f"NLP model unavailable for language: {request.language}")
        return SymptomExtractionResponse(
            originalText=request.text,
            language=request.language,
            nlp_available=False,
            fallback_reason=f"{request.language.upper()} NLP model not loaded. Check model installation.",
            model_type="UNAVAILABLE",
            entities=[],
            extractedPhrases=[],
            structuredSummary={},
            confidence=0.0,
            warning=f"NLP extraction failed. Java backend will use keyword-based triage."
        )
    
    # Process text with spaCy
    doc = nlp(request.text)
    
    # Extract entities with negation/uncertainty detection
    entities = []
    for ent in doc.ents:
        is_negated = is_entity_negated(ent, doc, request.language)
        is_uncertain = is_entity_uncertain(ent, doc, request.language)
        
        # Calculate confidence dynamically
        base_confidence = ent._.get("confidence", 0.7)  # spaCy model confidence
        confidence = calculate_confidence(base_confidence, is_negated, is_uncertain, model_type)
        
        entities.append(ExtractedEntity(
            text=ent.text,
            label=ent.label_,
            start=ent.start_char,
            end=ent.end_char,
            negated=is_negated,
            uncertain=is_uncertain,
            confidence=confidence  # DYNAMIC, not hardcoded
        ))
    
    # Calculate overall confidence
    overall_confidence = calculate_overall_confidence(entities, model_type)
    
    # Generate warning for generic models
    warning = None
    if model_type == "GENERIC":
        warning = f"Using generic {request.language.upper()} model (not medical-specific). Extraction accuracy may be limited."
    
    return SymptomExtractionResponse(
        originalText=request.text,
        language=request.language,
        nlp_available=True,
        fallback_reason=None,
        model_type=model_type,
        entities=entities,
        extractedPhrases=[],  # Populated from entities
        structuredSummary={
            "primarySymptoms": [e.text for e in entities if e.label_ == "SYMPTOM" and not e.negated],
            "negatedSymptoms": [e.text for e in entities if e.negated],
            "uncertainSymptoms": [e.text for e in entities if e.uncertain],
            "bodyParts": [e.text for e in entities if e.label_ == "BODY_PART"],
            "duration": extract_duration(doc),
            "severity": extract_severity(doc)
        },
        confidence=overall_confidence,
        warning=warning
    )

def is_entity_negated(entity, doc, language):
    """Detect if entity is negated in context"""
    negation_words = models.negation_patterns.get(language, [])
    
    # Check 3 words before entity
    start = max(0, entity.start - 3)
    context = doc[start:entity.start]
    
    for token in context:
        if token.text.lower() in negation_words:
            return True
    
    return False

def is_entity_uncertain(entity, doc, language):
    """Detect if entity expresses uncertainty"""
    uncertainty_words = models.uncertainty_patterns.get(language, [])
    
    # Check 5 words around entity
    start = max(0, entity.start - 5)
    end = min(len(doc), entity.end + 5)
    context = doc[start:end]
    
    for token in context:
        if token.text.lower() in uncertainty_words:
            return True
    
    return False

def calculate_confidence(base_confidence, is_negated, is_uncertain, model_type):
    """Calculate entity confidence dynamically"""
    confidence = base_confidence
    
    # Reduce confidence for negation/uncertainty
    if is_negated:
        confidence *= 0.5  # Negated = less confident
    if is_uncertain:
        confidence *= 0.7  # Uncertain = less confident
    
    # Reduce confidence for generic models
    if model_type == "GENERIC":
        confidence *= 0.8  # Generic model less confident for medical entities
    
    return round(confidence, 2)

def calculate_overall_confidence(entities, model_type):
    """Calculate overall extraction confidence"""
    if not entities:
        return 0.0
    
    # Average entity confidences
    avg_confidence = sum(e.confidence for e in entities) / len(entities)
    
    # Adjust for model type
    if model_type == "GENERIC":
        avg_confidence *= 0.9
    elif model_type == "UNAVAILABLE":
        avg_confidence = 0.0
    
    return round(avg_confidence, 2)

def detect_language(text):
    """Simple language detection"""
    # Check for Hindi/Marathi characters
    if any('\u0900' <= c <= '\u097F' for c in text):
        # Basic Hindi/Marathi distinction (simplified)
        return 'hi'  # Default to Hindi for Devanagari
    return 'en'

def extract_duration(doc):
    """Extract duration mentions"""
    # Pattern matching for durations
    import re
    duration_pattern = r'(\d+)\s*(minute|hour|day|week)s?'
    match = re.search(duration_pattern, doc.text, re.IGNORECASE)
    return match.group(0) if match else None

def extract_severity(doc):
    """Extract severity indicators"""
    severity_keywords = ['severe', 'mild', 'moderate', 'extreme', 'intense', 'slight']
    for token in doc:
        if token.text.lower() in severity_keywords:
            return token.text.lower()
    return None

@app.get("/health")
async def health_check():
    return {
        "status": "healthy",
        "service": "AAYUSH NLP",
        "models": {
            "english": models.en_model_type,
            "hindi": models.hi_model_type,
            "marathi": models.mr_model_type
        }
    }
```

### 7.3 Java-Python Integration

**⚠️ CRITICAL: NLP failure must NEVER silently default to GREEN urgency**

```java
@Service
public class NlpIntegrationService {
    
    @Value("${aayush.nlp.service.url}")
    private String nlpServiceUrl;
    
    @Value("${aayush.nlp.timeout.seconds:3}")
    private int nlpTimeoutSeconds;
    
    private final RestTemplate restTemplate;
    
    public NlpExtractionDTO extractSymptoms(String text, String language) {
        try {
            String url = nlpServiceUrl + "/api/v1/extract-symptoms";
            
            Map<String, String> request = Map.of(
                "text", text,
                "language", language
            );
            
            // 3-second timeout (configurable)
            ResponseEntity<NlpExtractionDTO> response = restTemplate.postForEntity(
                url, request, NlpExtractionDTO.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                NlpExtractionDTO result = response.getBody();
                
                // Check if NLP was actually available
                if (!result.isNlpAvailable()) {
                    log.warn("NLP unavailable for language {}: {}", 
                             language, result.getFallbackReason());
                    return null;  // Trigger keyword fallback
                }
                
                // Check confidence threshold
                if (result.getConfidence() < 0.5) {
                    log.warn("NLP confidence too low ({}) for language {}, using keyword fallback",
                             result.getConfidence(), language);
                    return null;  // Trigger keyword fallback
                }
                
                // Log warnings if present
                if (result.getWarning() != null) {
                    log.warn("NLP warning: {}", result.getWarning());
                }
                
                return result;
            } else {
                log.warn("NLP service returned non-2xx status: {}", response.getStatusCode());
                return null;  // Trigger keyword fallback
            }
        } catch (RestClientException e) {
            log.error("Failed to call NLP service (timeout or connection error)", e);
            return null;  // Trigger keyword fallback - DO NOT fail emergency creation
        } catch (Exception e) {
            log.error("Unexpected error calling NLP service", e);
            return null;  // Trigger keyword fallback
        }
    }
}
```

**Usage in Emergency Service:**
```java
@Service
public class EmergencySessionService {
    
    private final NlpIntegrationService nlpService;
    private final TriageService triageService;
    
    public EmergencySessionResponse createEmergency(CreateEmergencyRequest request) {
        NlpExtractionDTO nlpResult = null;
        TriageMethod triageMethod;
        
        try {
            // Attempt NLP extraction (3-second timeout)
            nlpResult = nlpService.extractSymptoms(
                request.getSymptoms(),
                request.getLanguage()
            );
            
            if (nlpResult != null && nlpResult.getConfidence() > 0.5) {
                triageMethod = TriageMethod.NLP_ENHANCED;
                log.info("Using NLP-enhanced triage (confidence: {})", nlpResult.getConfidence());
            } else {
                triageMethod = TriageMethod.KEYWORD_FALLBACK;
                log.warn("Using keyword fallback triage (NLP unavailable or low confidence)");
            }
        } catch (Exception e) {
            log.error("NLP extraction failed completely, using keyword fallback", e);
            triageMethod = TriageMethod.KEYWORD_FALLBACK;
        }
        
        // Java triage engine makes FINAL decision (NOT Python)
        TriageAssessment triage = triageService.assess(
            request.getSymptoms(),
            nlpResult,  // May be null - that's OK
            triageMethod
        );
        
        // CRITICAL: Never allow UNKNOWN to remain - escalate to ORANGE
        if (triage.getUrgencyLevel() == UrgencyLevel.UNKNOWN) {
            log.warn("Triage resulted in UNKNOWN, escalating to ORANGE for human review");
            triage.setUrgencyLevel(UrgencyLevel.ORANGE);
            triage.setReasoning("Unable to automatically classify symptoms. Requires human clinical review. " +
                               "⚠️ This is a software prototype, not a validated medical triage system.");
            triageMethod = TriageMethod.HUMAN_REVIEW_REQUIRED;
        }
        
        // Continue with session creation...
        EmergencySession session = EmergencySession.builder()
            .sessionId(generateSessionId())
            .symptoms(request.getSymptoms())
            .urgencyLevel(triage.getUrgencyLevel())
            .triageMethod(triageMethod)
            .triageRulesVersion(triage.getRulesVersion())
            .triageReasoning(triage.getReasoning())
            .nlpExtractedData(nlpResult != null ? toJson(nlpResult) : null)
            .build();
        
        // Persist to PostgreSQL
        EmergencySession saved = repository.save(session);
        
        return mapToResponse(saved);
    }
}
```

**Key Safety Features:**
1. **NLP timeout**: 3 seconds (configurable)
2. **NLP failure**: Returns null → triggers keyword fallback
3. **Low confidence**: < 0.5 → triggers keyword fallback
4. **Language unavailable**: `nlpAvailable=false` → triggers keyword fallback
5. **UNKNOWN urgency**: Escalates to ORANGE (human review), never GREEN
6. **Emergency creation never blocked**: Always succeeds even if NLP/triage fails

---

## 8. EMERGENCY LIFECYCLE

### 8.1 Status State Machine

```
CREATED
   ↓
ACKNOWLEDGED (Emergency staff reviews)
   ↓
TRANSPORT_ASSIGNED (Ambulance assigned)
   ↓
EN_ROUTE (Ambulance traveling to patient)
   ↓
ARRIVED_AT_PATIENT (Ambulance at patient location)
   ↓
PATIENT_PICKED_UP (Patient loaded in ambulance)
   ↓
EN_ROUTE_TO_HOSPITAL (Traveling to hospital)
   ↓
AT_HOSPITAL (Arrived at hospital)
   ↓
COMPLETED (Emergency resolved)
```

**Valid Transitions:**
- CREATED → ACKNOWLEDGED
- ACKNOWLEDGED → TRANSPORT_ASSIGNED
- TRANSPORT_ASSIGNED → EN_ROUTE
- EN_ROUTE → ARRIVED_AT_PATIENT
- ARRIVED_AT_PATIENT → PATIENT_PICKED_UP
- PATIENT_PICKED_UP → EN_ROUTE_TO_HOSPITAL
- EN_ROUTE_TO_HOSPITAL → AT_HOSPITAL
- AT_HOSPITAL → COMPLETED

**Invalid Transitions:**
- CREATED → COMPLETED (must go through workflow)
- AT_HOSPITAL → EN_ROUTE (cannot go backwards)

### 8.2 Automatic Notifications

**Trigger Events:**
1. **Session Created (CREATED)**
   - Notify: Emergency staff, linked caregivers
   - Method: WebSocket, SMS, Email

2. **Transport Assigned (TRANSPORT_ASSIGNED)**
   - Notify: Patient (senior), caregivers, hospital
   - Method: WebSocket, SMS
   - Include: Ambulance details, ETA

3. **Ambulance Arrived (ARRIVED_AT_PATIENT)**
   - Notify: Caregivers, hospital
   - Method: WebSocket, SMS

4. **En Route to Hospital (EN_ROUTE_TO_HOSPITAL)**
   - Notify: Hospital staff, caregivers
   - Method: WebSocket
   - Include: ETA, patient handover summary

5. **At Hospital (AT_HOSPITAL)**
   - Notify: Caregivers
   - Method: SMS, Email

6. **Completed (COMPLETED)**
   - Notify: Caregivers, patient (if applicable)
   - Method: SMS, Email

---

## 9. INTEGRATION ARCHITECTURE

### 9.1 External Service Integration

**Mock Adapters (Initial Implementation):**

All external integrations will use **adapter pattern** to allow swapping mock implementations with real services:

```java
public interface SmsService {
    void sendSms(String phoneNumber, String message);
}

@Service
@Profile("dev")
public class MockSmsService implements SmsService {
    @Override
    public void sendSms(String phoneNumber, String message) {
        log.info("MOCK SMS to {}: {}", phoneNumber, message);
    }
}

@Service
@Profile("prod")
public class TwilioSmsService implements SmsService {
    @Override
    public void sendSms(String phoneNumber, String message) {
        // Real Twilio integration
    }
}
```

**External Services:**
1. **SMS Gateway** (Twilio, AWS SNS)
2. **Email Service** (SendGrid, AWS SES)
3. **Maps API** (Google Maps, OpenStreetMap)
4. **Push Notifications** (Firebase Cloud Messaging)
5. **Payment Gateway** (Future: for subscription features)

### 9.2 Integration Points

| Service | Purpose | Mock in Dev | Production Integration |
|---------|---------|-------------|------------------------|
| Python NLP | Symptom extraction | ❌ Real service | ✅ Real service |
| PostgreSQL | Data persistence | ✅ Real database | ✅ Real database |
| SMS Gateway | Caregiver notifications | ✅ Mock/logs | Twilio / AWS SNS |
| Email Service | Notifications, verification | ✅ Mock/logs | SendGrid / AWS SES |
| Maps API | Geocoding, nearest hospital | ✅ Mock coords | Google Maps / OSM |
| WebSocket | Real-time updates | ✅ Real WebSocket | ✅ Real WebSocket |
| Redis (Optional) | Session cache | ❌ Optional | ✅ Recommended |

---

## 10. DEPLOYMENT ARCHITECTURE

### 10.1 Docker Compose (Local Development)

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: aayush_db
      POSTGRES_USER: aayush
      POSTGRES_PASSWORD: aayush_secure_pass
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  nlp-service:
    build: ./nlp-service
    ports:
      - "8000:8000"
    environment:
      - FASTAPI_ENV=development

  backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=dev
      - DB_HOST=postgres
      - DB_PORT=5432
      - DB_NAME=aayush_db
      - DB_USERNAME=aayush
      - DB_PASSWORD=aayush_secure_pass
      - NLP_SERVICE_URL=http://nlp-service:8000
      - REDIS_HOST=redis
      - REDIS_PORT=6379
    depends_on:
      - postgres
      - redis
      - nlp-service

  frontend:
    build: ./frontend
    ports:
      - "5173:5173"
    environment:
      - VITE_API_URL=http://localhost:8080/api
      - VITE_WS_URL=ws://localhost:8080/ws
    depends_on:
      - backend

volumes:
  postgres_data:
```

### 10.2 Production Deployment (AWS Example)

```
                                  ┌──────────────────┐
                                  │  Route 53 DNS    │
                                  └────────┬─────────┘
                                           │
                                  ┌────────▼─────────┐
                                  │  CloudFront CDN  │
                                  │  (Frontend)      │
                                  └────────┬─────────┘
                                           │
                    ┌──────────────────────┴──────────────────────┐
                    │                                              │
           ┌────────▼─────────┐                      ┌────────────▼──────────┐
           │  S3 Bucket        │                      │  Application Load     │
           │  (React Build)    │                      │  Balancer (ALB)       │
           └───────────────────┘                      └────────────┬──────────┘
                                                                   │
                                                      ┌────────────┴──────────────┐
                                                      │                           │
                                            ┌─────────▼────────┐    ┌────────────▼────────┐
                                            │  ECS/Fargate     │    │  ECS/Fargate        │
                                            │  (Java Backend)  │    │  (Python NLP)       │
                                            └─────────┬────────┘    └─────────────────────┘
                                                      │
                                      ┌───────────────┴───────────────┐
                                      │                               │
                            ┌─────────▼────────┐          ┌──────────▼─────────┐
                            │  RDS PostgreSQL  │          │  ElastiCache       │
                            │  (Multi-AZ)      │          │  (Redis)           │
                            └──────────────────┘          └────────────────────┘
```

**AWS Services Used:**
- **Route 53:** DNS management
- **CloudFront:** CDN for frontend
- **S3:** Static frontend hosting
- **ALB:** Load balancing
- **ECS/Fargate:** Container orchestration (serverless)
- **RDS PostgreSQL:** Managed database (Multi-AZ)
- **ElastiCache:** Redis caching
- **SES/SNS:** Email/SMS notifications
- **CloudWatch:** Monitoring and logging
- **Secrets Manager:** Secure credential storage

---

## 11. SECURITY ARCHITECTURE

### 11.1 Security Layers

**1. Network Security:**
- HTTPS/TLS 1.3 only
- HSTS headers
- WAF (Web Application Firewall) rules
- DDoS protection (CloudFlare/AWS Shield)

**2. Application Security:**
- JWT authentication
- CSRF protection
- XSS protection (Content Security Policy)
- SQL injection prevention (JPA parameterized queries)
- Input validation (Jakarta Validation)
- Rate limiting (10 requests/minute for login, 100/minute for APIs)

**3. Data Security:**
- Encryption at rest (database)
- Encryption in transit (TLS)
- Password hashing (BCrypt strength 12)
- Sensitive data masking in logs
- PII redaction in audit logs

**4. Access Control:**
- Role-based access control (RBAC)
- Caregiver consent requirements
- Audit logging of all data access
- Session timeout (24 hours)
- Automatic logout on inactivity (30 minutes)

### 11.2 OWASP Top 10 Mitigations

| Threat | Mitigation |
|--------|------------|
| Injection | JPA parameterized queries, input validation |
| Broken Authentication | JWT, BCrypt, MFA (future) |
| Sensitive Data Exposure | TLS, encryption at rest, data masking |
| XML External Entities | Not using XML parsing |
| Broken Access Control | RBAC, @PreAuthorize annotations |
| Security Misconfiguration | Spring Security defaults, security headers |
| XSS | CSP headers, React DOM escaping |
| Insecure Deserialization | JSON only, validated DTOs |
| Using Components with Known Vulnerabilities | Dependabot, regular updates |
| Insufficient Logging | Audit logs, CloudWatch logging |

---

## 12. TESTING STRATEGY

### 12.1 Test Pyramid

```
                    /\
                   /  \
                  / E2E\ (10%)
                 /______\
                /        \
               /Integration\ (30%)
              /____________\
             /              \
            /   Unit Tests   \ (60%)
           /__________________\
```

### 12.2 Testing Levels

**Unit Tests (60% coverage target):**
- Service layer logic
- Utility functions
- DTO validation
- Triage rules engine

**Integration Tests (30% coverage target):**
- Controller + Service + Repository
- Testcontainers for PostgreSQL
- WebSocket message flow
- Python NLP service integration

**E2E Tests (10% coverage target):**
- Playwright for frontend
- Complete user flows:
  - Senior submits emergency
  - Transport accepts and updates
  - Hospital acknowledges
- Cross-browser testing (Chrome, Firefox, Safari)

**Performance Tests:**
- JMeter scenarios
- 100 concurrent users
- 1000 emergency sessions
- API response time < 200ms (p95)
- WebSocket latency < 50ms

**Security Tests:**
- OWASP ZAP automated scan
- SQL injection attempts
- XSS attempts
- Authentication bypass tests
- Rate limiting validation

---

## 13. IMPLEMENTATION ROADMAP

**⚠️ CORRECTION APPLIED:** Revised 8-week safety-first roadmap (was 12 weeks). Prioritizes security and core emergency features before enhancements.

### 13.1 Safety-First Implementation Approach

**Key Principles:**
1. **Backend foundation + security FIRST** before frontend enhancements
2. **Direct SOS path** implemented early (Week 2) - never block life-threatening emergencies
3. **Database safety** via baseline-on-migrate and backups
4. **Authentication restrictions** (SENIOR/CAREGIVER public registration only)
5. **PostgreSQL-first** updates (WebSocket is enhancement, not requirement)
6. **NLP as enhancement** with keyword fallback (not blocker)

---

### Phase 1: Backend Foundation & Security (Week 1)

**Goal:** Secure, persistent backend that never blocks emergency creation

#### Week 1: Database, Authentication & Health Profiles

**Tasks:**
1. **Database Migration Safety** (Day 1-2)
   - ✅ Backup existing PostgreSQL database
   - ✅ Inspect current schema (emergency_sessions table)
   - ✅ Create Flyway V1 migration with baseline-on-migrate
   - ✅ Test migrations in dev environment
   - ✅ Add rollback scripts for all migrations
   - **Deliverable:** Flyway migrations preserving existing data

2. **JWT Authentication Foundation** (Day 2-4)
   - ✅ User entity with password hashing (BCrypt strength 12)
   - ✅ Role entity (SENIOR, CAREGIVER, EMERGENCY_STAFF, TRANSPORT_STAFF, HOSPITAL_STAFF, ADMIN)
   - ✅ JWT token generation and validation service
   - ✅ Refresh token rotation (revoke old, issue new)
   - ✅ Token reuse detection (revoke entire family)
   - ✅ `/api/auth/register` endpoint - **ONLY allows SENIOR/CAREGIVER roles**
   - ✅ `/api/auth/login` endpoint
   - ✅ `/api/auth/refresh` endpoint with rotation
   - ✅ `/api/auth/logout` endpoint (revoke refresh token)
   - ✅ `/api/admin/users` endpoint (create staff accounts)
   - ✅ JwtAuthenticationFilter for all protected endpoints
   - **Deliverable:** Secure authentication, public registration restricted

3. **Health Profile Module** (Day 4-5)
   - ✅ Time-series vital_readings table
   - ✅ Structured allergies table (with severity)
   - ✅ Medications table (with route, start/end dates)
   - ✅ Chronic_conditions table (with ICD-10 codes)
   - ✅ Health profile CRUD endpoints
   - ✅ Emergency creation supports NULL health_profile_id
   - **Deliverable:** Health profiles optional for emergencies

**Acceptance Criteria:**
- [ ] Only SENIOR/CAREGIVER can self-register (staff registration returns 403)
- [ ] Refresh token reuse detection revokes entire token family
- [ ] Emergency session creation succeeds with zero profile data
- [ ] Flyway migrations run without data loss
- [ ] All endpoints require authentication (except /api/auth/*)

**Testing:**
- Unit tests: Authentication service, triage rules
- Integration tests: Registration restrictions, token rotation
- Security tests: Attempted staff registration fails

---

### Phase 2: Emergency Creation & Direct SOS (Week 2)

**Goal:** Core emergency workflows with safety-first SOS path

#### Week 2: SOS, Standard Emergency & Authorization

**Tasks:**
1. **Direct SOS Workflow** (Day 1-2)
   - ✅ `/api/emergency-sessions/sos` endpoint
   - ✅ < 500ms response time (p95) - no NLP, profile, or location lookup
   - ✅ Sets urgency_level = RED, triage_method = SOS_DIRECT
   - ✅ Duplicate request detection (5-minute window)
   - ✅ Background jobs for location/profile enhancement
   - **Deliverable:** Life-saving SOS button works immediately

2. **Standard Emergency Creation** (Day 2-3)
   - ✅ Java triage rules engine (versioned, e.g., "1.0.0")
   - ✅ Keyword-based triage as fallback
   - ✅ Health profile integration (optional - uses request data if missing)
   - ✅ Triage disclaimer text in response
   - ✅ Unknown symptoms escalate to ORANGE (never GREEN)
   - ✅ Set triage_method: KEYWORD_FALLBACK or HUMAN_REVIEW_REQUIRED
   - **Deliverable:** Standard emergency path with keyword triage

3. **Authorization & Data Access Control** (Day 3-5)
   - ✅ @PreAuthorize annotations on all endpoints
   - ✅ Role-based data filtering (transport sees minimum necessary)
   - ✅ Patient/caregiver can access own sessions
   - ✅ Emergency sessions return different fields per role
   - ✅ Transport staff cannot access full medical history
   - ✅ No PII in application logs (only audit logs)
   - **Deliverable:** Minimum necessary information per role

**Acceptance Criteria:**
- [ ] SOS endpoint responds in < 500ms with minimal data
- [ ] SOS works with null location, null profile, empty symptoms
- [ ] Duplicate SOS within 5 minutes returns existing session
- [ ] Unknown symptoms result in ORANGE urgency (not GREEN)
- [ ] Transport staff GET returns filtered response (no full profile)
- [ ] Triage disclaimer displayed on every assessment

**Testing:**
- Performance tests: SOS < 500ms under load
- Security tests: Unauthorized role access blocked
- Integration tests: Emergency creation with/without profile

---

### Phase 3: Python NLP & Java Triage Integration (Week 3)

**Goal:** Enhanced symptom extraction with safe fallbacks

#### Week 3: Python NLP Service + Java Integration

**Tasks:**
1. **Python FastAPI Service** (Day 1-3)
   - ✅ FastAPI project structure
   - ✅ English: Medical NER model (if available) or generic with warning
   - ✅ Hindi: Generic spaCy model (NOT medical-specific)
   - ✅ Marathi: Return `nlp_available=false` with fallback_reason
   - ✅ Negation detection ("no chest pain" flagged)
   - ✅ Uncertainty detection ("maybe fever" flagged)
   - ✅ Dynamic confidence calculation (never hardcoded)
   - ✅ `/api/v1/extract-symptoms` endpoint
   - ✅ `/health` endpoint with model availability
   - ✅ Dockerfile for Python service
   - **Deliverable:** Python NLP service with realistic constraints

2. **Java-Python Integration** (Day 3-4)
   - ✅ NlpIntegrationService with 3-second timeout
   - ✅ Low confidence (< 0.5) triggers keyword fallback
   - ✅ NLP failure triggers keyword fallback (doesn't block)
   - ✅ Set triage_method = NLP_ENHANCED when successful
   - ✅ Java makes ALL triage decisions (Python extracts only)
   - ✅ Store nlp_extracted_data in JSONB column
   - **Deliverable:** NLP-enhanced triage with safe fallback

3. **Docker Compose** (Day 5)
   - ✅ PostgreSQL container
   - ✅ Redis container (optional caching)
   - ✅ Python NLP service container
   - ✅ Java backend container
   - ✅ React frontend container
   - ✅ Volume mounts for development
   - **Deliverable:** Full local development environment

**Acceptance Criteria:**
- [ ] Marathi request returns nlp_available=false (not error)
- [ ] "No chest pain" detected as negated symptom
- [ ] NLP confidence calculated dynamically (not 0.85 hardcoded)
- [ ] NLP timeout/failure falls back to keywords (emergency still created)
- [ ] Docker Compose starts all services successfully
- [ ] Python service health check returns model availability

**Testing:**
- Unit tests: Negation detection, uncertainty detection
- Integration tests: Java-Python communication, timeout handling
- E2E tests: Full emergency flow with NLP

---

### Phase 4: Hospital & Transport Coordination (Week 4)

**Goal:** Resource management with clear mock labeling

#### Week 4: Hospitals, Ambulances & Mock Adapters

**Tasks:**
1. **Hospital Management** (Day 1-2)
   - ✅ Hospital CRUD operations (admin only)
   - ✅ Hospital capacity tracking (with staleness indicator)
   - ✅ Nearest hospital finder (location-based, radius query)
   - ✅ Hospital assignment to emergency session
   - ✅ Mock capacity updates (labeled as simulated)
   - **Deliverable:** Real hospital entities with capacity tracking

2. **Ambulance Management** (Day 2-3)
   - ✅ Ambulance CRUD operations (admin only)
   - ✅ Mock dispatch service (`@Profile("dev")`)
   - ✅ Real dispatch service interface (throws UnsupportedOperationException in prod)
   - ✅ Ambulance assignment to emergency session
   - ✅ Mock dispatch response includes `isMock` flag
   - ✅ Clear "⚠️ SIMULATED DISPATCH" warnings in responses
   - **Deliverable:** Mock dispatch clearly labeled

3. **Caregiver Management** (Day 3-5)
   - ✅ Caregiver entity with consent tracking
   - ✅ Caregiver_permissions table (granular permissions)
   - ✅ Caregiver_permission_history (audit trail)
   - ✅ Link caregiver to senior endpoint
   - ✅ Revoke caregiver consent endpoint
   - ✅ Caregivers receive emergency notifications (mock)
   - **Deliverable:** Caregiver relationships with audit trail

**Acceptance Criteria:**
- [ ] Nearest hospital found within 50km radius
- [ ] Mock ambulance dispatch includes "isMock: true" flag
- [ ] Mock dispatch response has warning text
- [ ] Caregiver permission change creates audit log entry
- [ ] Hospital capacity shows "last updated" timestamp
- [ ] Admin can create/update hospitals and ambulances

**Testing:**
- Integration tests: Hospital nearest search
- Unit tests: Mock dispatch labeling
- Security tests: Only admin can create hospitals

---

### Phase 5-6: Secure WebSocket Integration (Weeks 5-6)

**Goal:** Real-time coordination with PostgreSQL as source of truth

#### Week 5-6: WebSocket Security & PostgreSQL-First Updates

**Tasks:**
1. **WebSocket Security** (Day 1-3)
   - ✅ Spring WebSocket + STOMP configuration
   - ✅ JWT authentication for WebSocket connections
   - ✅ Role-based subscription authorization
   - ✅ Session-level access control (cannot subscribe to unauthorized sessions)
   - ✅ CSRF protection for WebSocket
   - ✅ WebSocket authorization interceptor
   - **Deliverable:** Authenticated, authorized WebSocket subscriptions

2. **PostgreSQL-First Updates** (Day 3-5)
   - ✅ All status updates save to PostgreSQL FIRST
   - ✅ WebSocket broadcast is best-effort (logged warning on failure)
   - ✅ WebSocket failure doesn't prevent database updates
   - ✅ Reconnection loads current state from REST API
   - ✅ Status update events broadcast to subscribed clients
   - **Deliverable:** WebSocket as enhancement, not requirement

3. **Testing & Bug Fixes** (Day 6-10)
   - ✅ Comprehensive triage rule tests (all RED/ORANGE/GREEN scenarios)
   - ✅ Security authorization tests (role restrictions)
   - ✅ Integration tests with Testcontainers (PostgreSQL)
   - ✅ WebSocket subscription tests (authorized/unauthorized)
   - ✅ Performance tests (SOS < 500ms, API < 200ms)
   - ✅ Bug fixes from testing
   - **Deliverable:** 80%+ code coverage, all tests passing

**Acceptance Criteria:**
- [ ] PostgreSQL update succeeds even if WebSocket broadcast fails
- [ ] Reconnecting client reloads state from REST API
- [ ] User cannot subscribe to unauthorized session topic
- [ ] WebSocket connection requires valid JWT token
- [ ] All triage rules have passing test cases
- [ ] Testcontainers integration tests passing

**Testing:**
- Integration tests: WebSocket connection, subscription, message flow
- Security tests: Unauthorized subscription attempts
- Performance tests: Concurrent WebSocket connections

---

### Phase 6-7: Frontend Integration & Authentication UI (Weeks 6-7)

**Goal:** Connect frontend to real backend APIs

#### Week 6-7: Frontend Integration

**Tasks:**
1. **Authentication UI** (Day 1-3)
   - ✅ Login form with validation
   - ✅ Register form (SENIOR/CAREGIVER only - no role selector)
   - ✅ JWT token storage (httpOnly cookie or localStorage)
   - ✅ Token refresh on 401 responses
   - ✅ Auto-logout on token expiration
   - ✅ Role-based navigation (hide staff dashboards)
   - ✅ Show error if staff attempts public registration
   - **Deliverable:** Authentication UI with role restrictions

2. **Emergency Submission Updates** (Day 3-5)
   - ✅ Prominent "Emergency SOS" button (red, large)
   - ✅ Standard symptom form (separate from SOS)
   - ✅ Profile completeness indicator (0-100%)
   - ✅ Mock data warnings on all simulated features
   - ✅ Triage disclaimer prominently displayed
   - ✅ Replace localStorage with backend API calls
   - **Deliverable:** Frontend uses real backend APIs

3. **Real-Time Updates** (Day 5-8)
   - ✅ WebSocket connection with JWT authentication
   - ✅ Auto-reconnect with exponential backoff
   - ✅ State reload from REST API on reconnection
   - ✅ Session-specific topic subscriptions
   - ✅ Real-time status updates across dashboards
   - **Deliverable:** Real-time coordination via WebSocket

4. **Testing & Polish** (Day 9-14)
   - ✅ E2E tests with Playwright (full emergency workflow)
   - ✅ Cross-browser testing (Chrome, Firefox, Safari)
   - ✅ Mobile responsiveness testing
   - ✅ Accessibility testing (axe-core)
   - ✅ Bug fixes and UI polish
   - **Deliverable:** Production-ready frontend

**Acceptance Criteria:**
- [ ] Staff role selection NOT shown on public registration form
- [ ] SOS button creates emergency in < 500ms
- [ ] WebSocket reconnection reloads latest state
- [ ] All mock features have warning labels
- [ ] Triage disclaimer visible on every assessment
- [ ] E2E test covers full emergency workflow

**Testing:**
- E2E tests: Emergency creation, transport assignment, hospital handover
- Accessibility tests: WCAG 2.1 AA compliance
- Cross-browser tests: Chrome, Firefox, Safari, Edge

---

### Phase 7-8: Testing & Deployment (Weeks 7-8)

**Goal:** Production-ready deployment with monitoring

#### Week 7-8: Testing, Security & Production Deployment

**Tasks:**
1. **Comprehensive Testing** (Day 1-4)
   - ✅ All triage rules have explicit test cases
   - ✅ Security authorization tests (all roles)
   - ✅ Token rotation and reuse detection tests
   - ✅ Load testing (100 concurrent users)
   - ✅ API performance tests (< 200ms p95)
   - ✅ WebSocket latency tests (< 50ms)
   - ✅ Target 80% code coverage
   - **Deliverable:** Comprehensive test suite

2. **Security Hardening** (Day 4-7)
   - ✅ OWASP ZAP automated security scan
   - ✅ SQL injection tests (should be blocked by JPA)
   - ✅ XSS attempt tests (CSP headers)
   - ✅ Rate limiting (10 login attempts/minute, 100 API calls/minute)
   - ✅ HTTPS enforcement (HSTS headers)
   - ✅ Security headers (X-Frame-Options, X-Content-Type-Options)
   - ✅ No PII in logs verification
   - **Deliverable:** Security scan passed, hardened configuration

3. **CI/CD Pipeline** (Day 7-10)
   - ✅ GitHub Actions workflow (or GitLab CI)
   - ✅ Automated tests on every commit
   - ✅ Build frontend and backend
   - ✅ Docker image building
   - ✅ Automated deployment to staging
   - ✅ Manual approval for production deployment
   - **Deliverable:** Automated build and deployment

4. **Production Deployment** (Day 11-14)
   - ✅ Production environment setup (AWS/Azure/GCP)
   - ✅ PostgreSQL RDS/managed database
   - ✅ Redis ElastiCache (optional)
   - ✅ Load balancer configuration
   - ✅ SSL/TLS certificates
   - ✅ Monitoring (Prometheus + Grafana)
   - ✅ Logging (CloudWatch or ELK)
   - ✅ Backup and disaster recovery plan
   - ✅ Production deployment guide
   - ✅ User documentation
   - **Deliverable:** Live production deployment

**Final Acceptance Criteria:**
- [ ] Deployed to production with HTTPS
- [ ] Monitoring dashboards operational (uptime, response times)
- [ ] Load tested (100+ concurrent users, no degradation)
- [ ] Security scan passed (no critical vulnerabilities)
- [ ] 80%+ code coverage across backend and frontend
- [ ] CI/CD pipeline builds and deploys automatically
- [ ] User documentation complete
- [ ] Emergency workflows tested end-to-end in production

**Testing:**
- Load tests: 100+ concurrent users, 1000+ sessions
- Security tests: OWASP Top 10 validation
- Disaster recovery test: Restore from backup

---

## 13.2 Milestone Summary

| Milestone | Duration | Key Deliverables | Status |
|-----------|----------|------------------|--------|
| **Phase 1: Backend Foundation** | Week 1 | JWT auth, Flyway migrations, health profiles | ⏳ To Do |
| **Phase 2: Emergency Creation** | Week 2 | Direct SOS, standard emergency, authorization | ⏳ To Do |
| **Phase 3: Python NLP** | Week 3 | NLP service, Java integration, Docker Compose | ⏳ To Do |
| **Phase 4: Hospital/Transport** | Week 4 | Hospitals, ambulances, caregivers, mock adapters | ⏳ To Do |
| **Phase 5-6: WebSocket** | Weeks 5-6 | WebSocket security, PostgreSQL-first, testing | ⏳ To Do |
| **Phase 6-7: Frontend** | Weeks 6-7 | Auth UI, SOS button, real-time updates, E2E tests | ⏳ To Do |
| **Phase 7-8: Production** | Weeks 7-8 | Security hardening, CI/CD, monitoring, deployment | ⏳ To Do |

**Total Duration:** 8 weeks (320 hours for 1 full-stack developer)

**Critical Path:**
1. Authentication (Week 1) → blocks all other work
2. Direct SOS (Week 2) → safety-critical feature
3. Python NLP (Week 3) → enhances triage
4. WebSocket (Weeks 5-6) → enables real-time coordination
5. Frontend integration (Weeks 6-7) → connects all pieces
6. Production deployment (Weeks 7-8) → go-live

---

## 13.3 Risk Mitigation During Implementation

### High-Risk Items
1. **Database migration data loss** → Mitigation: Baseline-on-migrate, backups, test in dev first
2. **NLP service delays emergency creation** → Mitigation: 3-second timeout, keyword fallback, SOS bypasses NLP
3. **Authentication blocks existing demo users** → Mitigation: Seed admin account, migration preserves sessions
4. **WebSocket scalability issues** → Mitigation: PostgreSQL-first (WebSocket optional), load testing early

### Medium-Risk Items
5. **Frontend-backend integration bugs** → Mitigation: Integration tests, E2E tests, staging environment
6. **Performance degradation under load** → Mitigation: Load testing Week 5-6, caching, database indexes

---

## 13.4 Definition of Done

**Feature Complete Checklist:**
- [ ] All acceptance criteria met
- [ ] Unit tests written and passing (80%+ coverage)
- [ ] Integration tests passing
- [ ] E2E test covering happy path
- [ ] Security tested (no critical vulnerabilities)
- [ ] Code reviewed
- [ ] Documentation updated
- [ ] Deployed to staging and tested
- [ ] Performance validated (< 200ms API, < 500ms SOS)

**Production Ready Checklist:**
- [ ] All 8 milestones complete
- [ ] Security scan passed (OWASP ZAP)
- [ ] Load tested (100+ concurrent users)
- [ ] Monitoring operational
- [ ] Backup/restore tested
- [ ] User documentation complete
- [ ] Deployed to production with HTTPS
- [ ] Post-deployment smoke tests passed

---

## 14. APPENDICES

### Appendix A: Environment Variables

```bash
# Backend (Java)
DB_HOST=localhost
DB_PORT=5432
DB_NAME=aayush_db
DB_USERNAME=aayush
DB_PASSWORD=***  # ⚠️ Use strong password, not default

JWT_SECRET=***  # ⚠️ CRITICAL: Generate secure 256-bit key
JWT_EXPIRATION_MS=86400000  # 24 hours
JWT_REFRESH_EXPIRATION_MS=604800000  # 7 days

NLP_SERVICE_URL=http://localhost:8000

REDIS_HOST=localhost
REDIS_PORT=6379

SMS_PROVIDER=twilio
TWILIO_ACCOUNT_SID=***
TWILIO_AUTH_TOKEN=***
TWILIO_PHONE_NUMBER=***

EMAIL_PROVIDER=sendgrid
SENDGRID_API_KEY=***
SENDGRID_FROM_EMAIL=noreply@aayush.example.com

MAPS_API_KEY=***

# Frontend
VITE_API_URL=http://localhost:8080/api
VITE_WS_URL=ws://localhost:8080/ws
VITE_MAPS_API_KEY=***
```

### Appendix B: Glossary

- **EMR:** Emergency Medical Record (session ID prefix)
- **NLP:** Natural Language Processing
- **PWA:** Progressive Web App
- **RBAC:** Role-Based Access Control
- **STOMP:** Simple Text Oriented Messaging Protocol
- **JWT:** JSON Web Token
- **HIPAA:** Health Insurance Portability and Accountability Act
- **GDPR:** General Data Protection Regulation
- **TLS:** Transport Layer Security
- **SOS:** Emergency distress signal (from Morse code)
- **RED/ORANGE/GREEN:** Triage urgency levels (critical/urgent/stable)
- **Triage:** Process of determining priority of treatment based on severity
- **BCrypt:** Password hashing algorithm
- **Flyway:** Database migration tool
- **Testcontainers:** Library for integration testing with Docker containers
- **Idempotency:** Property where multiple identical requests have same effect as single request

### Appendix C: Architecture Decisions Reference

**For detailed rationale behind all corrections, see:**
- **docs/ARCHITECTURE_DECISIONS.md** - 9 critical decisions with full explanations
- **docs/ARCHITECTURE_CORRECTIONS_SUMMARY.md** - Executive summary of changes

**Key Decision Summary:**
1. **Direct SOS Workflow** - < 500ms, bypasses NLP/profile/location
2. **Time-Series Vital Readings** - Never overwrite history
3. **Public Registration Restrictions** - Only SENIOR/CAREGIVER allowed
4. **Refresh Token Rotation** - Revoke old, issue new, detect reuse
5. **Python NLP Constraints** - No Marathi, generic models, no triage decisions
6. **Triage Rules Versioning** - Unknown symptoms escalate to ORANGE
7. **Database Migration Safety** - Baseline-on-migrate preserves data
8. **PostgreSQL-First Updates** - WebSocket is best-effort broadcast
9. **Mock Integration Labeling** - Clear warnings on all simulated features

### Appendix D: Unresolved Decisions Requiring Client Approval

**See docs/ARCHITECTURE_DECISIONS.md Section "UNRESOLVED DECISIONS" for:**
1. Medical disclaimer placement
2. Caregiver consent granularity
3. Emergency session expiry policy
4. Python NLP timeout value
5. Triage rules update strategy
6. Minimum profile completeness requirements
7. WebSocket connection limit per user
8. Hospital capacity staleness threshold
9. Audit log retention period
10. Emergency numbers display frequency

**Recommendations provided - awaiting client decisions before finalizing implementation.**

---

**END OF SYSTEM ARCHITECTURE DOCUMENT**

**Document Version:** 2.0 (CORRECTED)  
**Last Updated:** October 31, 2026  
**Status:** Architecture Design - Corrections Applied, Ready for Implementation  
**Next Steps:** Client approval of unresolved decisions, then begin Week 1 implementation

---
- **CSP:** Content Security Policy

---

**END OF SYSTEM ARCHITECTURE DOCUMENT**

*This document serves as the comprehensive architectural blueprint for transforming AAYUSH from a demonstration application to an enterprise-grade production system. All implementation must follow this architecture to ensure consistency, security, and maintainability.*

**Next Steps:**
1. Review and approve this architecture with stakeholders
2. Set up development environment (Docker Compose)
3. Begin Milestone 1 implementation
4. Schedule weekly architecture review meetings

---

**Document Control:**
- **Author:** Architecture Team
- **Reviewers:** [To be assigned]
- **Approval:** [To be signed]
- **Next Review Date:** [30 days from approval]
