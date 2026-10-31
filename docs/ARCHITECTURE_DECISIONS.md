# AAYUSH ARCHITECTURE DECISIONS
## Critical Design Corrections Before Implementation

**Document Version:** 2.0  
**Last Updated:** October 31, 2026  
**Status:** Architecture Review - Corrected Design  
**Purpose:** Document critical architecture decisions and corrections from initial audit

---

## OVERVIEW

This document captures critical architecture corrections made after the initial audit. Each decision addresses a specific gap or risk in the original design and explains the reasoning, affected components, and acceptance criteria.

**Key Principle:** Safety first. Never block emergency assistance due to incomplete data, failed integrations, or technical issues.

---

## DECISION 1: DIRECT SOS WORKFLOW

### Original Problem
Initial design assumed all emergency requests would:
1. Wait for Python NLP extraction
2. Require complete health profile
3. Perform location lookup before session creation
4. Have no duplicate-request handling

**Risk:** Life-threatening delays, system dependency failures blocking critical requests.

### Revised Design

**Two Emergency Creation Paths:**

#### Path A: Direct SOS (Immediate)
- **Trigger:** User presses "Emergency SOS" button
- **Behavior:**
  - Creates emergency session IMMEDIATELY in PostgreSQL
  - Sets `urgency_level = 'RED'` (assumed critical)
  - Sets `triage_method = 'SOS_DIRECT'`
  - Stores raw symptom text (if provided)
  - Skips Python NLP call
  - Skips profile retrieval
  - Skips location lookup (accepts null, uses later background job)
  - Returns session ID in < 500ms
- **Async Processing:**
  - Location lookup (background)
  - Profile retrieval (background, links if available)
  - NLP extraction (background, enhances handover summary)

#### Path B: Standard Emergency (Enhanced)
- **Trigger:** User completes symptom form and reviews
- **Behavior:**
  - Calls Python NLP (with 3-second timeout)
  - Retrieves health profile (if exists)
  - Gets current location
  - Java triage engine determines urgency
  - Sets `triage_method = 'NLP_ENHANCED'` or `'KEYWORD_FALLBACK'`
  - Creates session with full context
- **Fallback:** If NLP fails, uses keyword-based triage

#### Idempotency Handling
```java
// Prevent duplicate submissions within 5 minutes
@Service
public class EmergencySessionService {
    
    public EmergencySessionResponse createEmergency(CreateEmergencyRequest request) {
        // Check for recent duplicate
        Optional<EmergencySession> recentSession = repository
            .findByUserIdAndCreatedAtAfter(
                request.getUserId(), 
                LocalDateTime.now().minusMinutes(5)
            )
            .stream()
            .filter(s -> s.getStatus() != SessionStatus.COMPLETED)
            .findFirst();
        
        if (recentSession.isPresent()) {
            log.info("Returning existing session {} for user {}", 
                     recentSession.get().getSessionId(), request.getUserId());
            return mapToResponse(recentSession.get());
        }
        
        // Create new session
        return createNewSession(request);
    }
}
```

### Affected Components
- **Backend:**
  - `EmergencySessionController.java` - Add `/api/emergency-sessions/sos` endpoint
  - `EmergencySessionService.java` - Implement both paths
  - `TriageService.java` - Support SOS direct bypass
  - `AsyncTaskService.java` - Background enhancement jobs
  
- **Database:**
  - `emergency_sessions` table - Add `triage_method` enum column
  - Add index on `(user_id, created_at)` for duplicate detection

- **Frontend:**
  - Add prominent "Emergency SOS" button (red, large)
  - Distinguish from "Describe Symptoms" flow
  - Show immediate confirmation with session ID

### Acceptance Criteria
- [ ] SOS endpoint returns session ID in < 500ms (p95)
- [ ] SOS works with null location, null profile, empty symptoms
- [ ] Duplicate request within 5 minutes returns existing session
- [ ] Background jobs enhance session without blocking response
- [ ] Frontend shows clear distinction between SOS and standard path

### Test Cases
```java
@Test
void testDirectSOS_WithMinimalData_CreatesSession() {
    // Given: User with no profile, no location
    SOSRequest request = SOSRequest.builder()
        .userId(1L)
        .symptoms("Help") // Optional
        .build();
    
    // When: Direct SOS created
    long startTime = System.currentTimeMillis();
    EmergencySessionResponse response = service.createSOSEmergency(request);
    long duration = System.currentTimeMillis() - startTime;
    
    // Then: Session created quickly with RED urgency
    assertNotNull(response.getSessionId());
    assertEquals("RED", response.getUrgencyLevel());
    assertEquals("SOS_DIRECT", response.getTriageMethod());
    assertTrue(duration < 500, "SOS creation took " + duration + "ms");
}

@Test
void testDuplicateSOS_Within5Minutes_ReturnsSameSession() {
    // Given: User creates SOS
    EmergencySessionResponse first = service.createSOSEmergency(request);
    
    // When: User presses SOS again 2 minutes later
    EmergencySessionResponse second = service.createSOSEmergency(request);
    
    // Then: Same session returned
    assertEquals(first.getSessionId(), second.getSessionId());
}
```

---

## DECISION 2: HEALTH PROFILE DATA MODEL

### Original Problem
Initial design had:
- Single current value for vitals (overwrites history)
- No source tracking (who entered? device? manual?)
- No verification status (self-reported vs verified)
- Allergies as flat text field
- No audit trail for caregiver permission changes

**Risk:** Loss of vital medical history, inability to track data provenance, no consent audit trail.

### Revised Design

#### Vital Readings (Time-Series)
```sql
CREATE TABLE vital_readings (
    id BIGSERIAL PRIMARY KEY,
    profile_id BIGINT NOT NULL REFERENCES health_profiles(id),
    reading_type VARCHAR(50) NOT NULL,  -- BLOOD_PRESSURE, HEART_RATE, TEMPERATURE, BLOOD_GLUCOSE, WEIGHT, etc.
    value_numeric DECIMAL(10,2),
    value_text VARCHAR(255),  -- For non-numeric like blood type
    unit VARCHAR(20),  -- mmHg, bpm, °C, kg, etc.
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
```

#### Allergies (Structured)
```sql
CREATE TABLE allergies (
    id BIGSERIAL PRIMARY KEY,
    profile_id BIGINT NOT NULL REFERENCES health_profiles(id),
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
```

#### Medications (with History)
```sql
CREATE TABLE medications (
    id BIGSERIAL PRIMARY KEY,
    profile_id BIGINT NOT NULL REFERENCES health_profiles(id),
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
```

#### Chronic Conditions (Structured)
```sql
CREATE TABLE chronic_conditions (
    id BIGSERIAL PRIMARY KEY,
    profile_id BIGINT NOT NULL REFERENCES health_profiles(id),
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
```

#### Caregiver Permission Audit
```sql
CREATE TABLE caregiver_permission_history (
    id BIGSERIAL PRIMARY KEY,
    caregiver_id BIGINT NOT NULL REFERENCES caregivers(id),
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

#### Incomplete Profile Support
```sql
-- Health profile is OPTIONAL for emergency creation
-- emergency_sessions.health_profile_id can be NULL

-- Service method supports missing profile
public EmergencySessionResponse createEmergency(CreateEmergencyRequest request) {
    HealthProfile profile = healthProfileService.findByUserId(request.getUserId())
        .orElse(null);  // OK to be null
    
    // Use profile data if available, otherwise use request data
    String allergies = profile != null ? getAllergiesText(profile) : request.getAllergies();
    // ... continue with emergency creation
}
```

### Affected Components
- **Database:**
  - Replace `health_profiles` single-value fields with time-series tables
  - Add 5 new tables: `vital_readings`, `allergies`, `medications`, `chronic_conditions`, `caregiver_permission_history`
  - Add `verification_status` and `source` to all medical data
  
- **Backend:**
  - `VitalReadingRepository.java`, `VitalReadingService.java`
  - `AllergyRepository.java`, `AllergyService.java`
  - Update `EmergencySessionService.java` to handle null profile

- **Frontend:**
  - Vitals entry with date/time picker
  - Allergies multi-select with severity
  - Medications with start/end dates
  - Profile completion percentage indicator

### Acceptance Criteria
- [ ] Vital readings stored with timestamp, never overwritten
- [ ] Each medical record has source and verification status
- [ ] Caregiver permission changes create audit log entries
- [ ] Emergency can be created with zero profile data
- [ ] Profile completion shown as percentage (0-100%)

### Test Cases
```java
@Test
void testEmergencyCreation_WithoutProfile_Succeeds() {
    // Given: User with no health profile
    when(healthProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());
    
    // When: Emergency created
    CreateEmergencyRequest request = CreateEmergencyRequest.builder()
        .userId(1L)
        .symptoms("Chest pain")
        .allergies("Unknown")  // Provided in request
        .build();
    
    EmergencySessionResponse response = service.createEmergency(request);
    
    // Then: Session created with request data
    assertNotNull(response.getSessionId());
    assertEquals("Unknown", response.getAllergies());
}
```

---

## DECISION 3: SECURITY AND AUTHORIZATION

### Original Problem
- Public registration allows any role selection
- No refresh token rotation or revocation
- No WebSocket subscription authorization
- Patient data exposed to all staff
- Medical data logged in plaintext

**Risk:** Privilege escalation, unauthorized data access, PII exposure, inability to revoke access.

### Revised Design

#### Registration Security
```java
@PostMapping("/api/auth/register")
public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
    // ONLY allow public registration for SENIOR and CAREGIVER roles
    if (!request.getRole().equals("SENIOR") && !request.getRole().equals("CAREGIVER")) {
        throw new UnauthorizedException("Public registration restricted to SENIOR and CAREGIVER roles");
    }
    
    // Admin must create EMERGENCY_STAFF, TRANSPORT_STAFF, HOSPITAL_STAFF accounts
    User user = authService.register(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(createAuthResponse(user));
}
```

#### Refresh Token Rotation
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
```

**Rotation Logic:**
```java
public AuthResponse refreshAccessToken(String refreshToken) {
    // Validate and decode refresh token
    RefreshToken storedToken = refreshTokenRepository.findByTokenHash(hash(refreshToken))
        .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
    
    // Check if revoked
    if (storedToken.isRevoked()) {
        // Token reuse detected - revoke entire family
        refreshTokenRepository.revokeTokenFamily(storedToken.getFamilyId(), "Token reuse detected");
        throw new UnauthorizedException("Refresh token has been revoked");
    }
    
    // Check expiration
    if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
        throw new UnauthorizedException("Refresh token expired");
    }
    
    // Revoke current token
    storedToken.setRevoked(true);
    storedToken.setRevokedAt(LocalDateTime.now());
    refreshTokenRepository.save(storedToken);
    
    // Issue new tokens
    String newAccessToken = jwtService.generateAccessToken(storedToken.getUserId());
    String newRefreshToken = jwtService.generateRefreshToken(storedToken.getUserId(), storedToken.getFamilyId());
    
    // Store new refresh token
    refreshTokenRepository.save(new RefreshToken(
        storedToken.getUserId(),
        hash(newRefreshToken),
        storedToken.getFamilyId(),
        LocalDateTime.now().plusDays(7)
    ));
    
    return new AuthResponse(newAccessToken, newRefreshToken);
}
```

#### Data Access Control
```java
@PreAuthorize("hasAnyRole('EMERGENCY_STAFF', 'TRANSPORT_STAFF', 'HOSPITAL_STAFF', 'ADMIN') " +
              "or @emergencySessionService.isPatientOrCaregiver(#sessionId, authentication.principal.userId)")
@GetMapping("/api/emergency-sessions/{sessionId}")
public ResponseEntity<EmergencySessionResponse> getSession(@PathVariable String sessionId) {
    // Staff receives MINIMUM necessary information
    EmergencySessionResponse response = emergencySessionService.getSession(sessionId);
    
    User currentUser = getCurrentUser();
    if (currentUser.hasRole("TRANSPORT_STAFF")) {
        // Transport sees: location, urgency, contact - NOT detailed medical history
        return ResponseEntity.ok(response.withFieldsFor(Role.TRANSPORT_STAFF));
    } else if (currentUser.hasRole("HOSPITAL_STAFF")) {
        // Hospital sees: symptoms, allergies, meds, urgency - NOT full profile
        return ResponseEntity.ok(response.withFieldsFor(Role.HOSPITAL_STAFF));
    }
    
    // Patient, caregiver, emergency staff see full details
    return ResponseEntity.ok(response);
}
```

#### WebSocket Authorization
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
                
                if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    String destination = accessor.getDestination();
                    
                    // Validate subscription authorization
                    if (destination.startsWith("/topic/emergency.")) {
                        String sessionId = extractSessionId(destination);
                        User user = getCurrentUser(accessor);
                        
                        if (!canAccessSession(user, sessionId)) {
                            throw new AccessDeniedException("Not authorized for session " + sessionId);
                        }
                    }
                }
                
                return message;
            }
        });
    }
}
```

#### Logging Security
```java
@Slf4j
public class EmergencySessionService {
    
    public EmergencySessionResponse createEmergency(CreateEmergencyRequest request) {
        // NEVER log full request - contains PII
        log.info("Creating emergency session for user {} with urgency {}", 
                 request.getUserId(),  // OK - user ID
                 assessedUrgency);      // OK - urgency level
        // NOT logged: symptoms, allergies, medical history
        
        // Audit log has full details but restricted access
        auditLogService.logEmergencyCreation(session, MaskingLevel.FULL_DETAILS);
    }
}
```

### Affected Components
- **Database:**
  - Add `refresh_tokens` table
  - Add `role_restrictions` to track which roles can be publicly registered
  
- **Backend:**
  - `AuthController.java` - Restrict public registration
  - `JwtService.java` - Implement token rotation
  - `RefreshTokenRepository.java`, `RefreshTokenService.java`
  - `WebSocketConfig.java` - Add subscription authorization
  - All services - Remove PII from logs
  - `EmergencySessionResponse.java` - Add `withFieldsFor(Role)` method

- **Frontend:**
  - Registration form removes role selection for non-admin
  - Implement refresh token rotation on 401 responses
  - WebSocket reconnect with fresh token

### Acceptance Criteria
- [ ] Public registration only allows SENIOR and CAREGIVER roles
- [ ] Refresh token rotation works correctly
- [ ] Token reuse detection revokes entire token family
- [ ] Logout revokes refresh token
- [ ] Transport staff cannot access full medical history
- [ ] WebSocket subscription fails if user not authorized
- [ ] No PII in application logs (only audit logs)

### Test Cases
```java
@Test
void testPublicRegistration_AsStaffRole_Rejected() {
    RegisterRequest request = RegisterRequest.builder()
        .username("hacker")
        .email("hacker@example.com")
        .password("password")
        .role("EMERGENCY_STAFF")  // Attempt privilege escalation
        .build();
    
    assertThrows(UnauthorizedException.class, () -> {
        authController.register(request);
    });
}

@Test
void testRefreshToken_Reuse_RevokesFamily() {
    // Given: User refreshes token
    AuthResponse first = authService.refreshAccessToken(refreshToken);
    
    // When: Attacker reuses old refresh token
    // Then: Entire token family revoked
    assertThrows(UnauthorizedException.class, () -> {
        authService.refreshAccessToken(refreshToken);  // Old token
    });
    
    // And: New token also revoked
    assertThrows(UnauthorizedException.class, () -> {
        authService.refreshAccessToken(first.getRefreshToken());
    });
}
```

---

## DECISION 4: PYTHON NLP CONSTRAINTS

### Original Problem
- Assumed generic spaCy models recognize medical symptoms
- Claimed Marathi NLP without verified model
- No handling of negation ("no chest pain")
- No handling of uncertainty ("might be fever")
- Hardcoded confidence = 0.85
- NLP failure defaulted to GREEN urgency

**Risk:** Misclassification of emergencies, false confidence in extraction accuracy, safety-critical failures.

### Revised Design

#### Realistic NLP Capabilities
```python
# app/services/spacy_service.py
class SpacyNLPService:
    def __init__(self):
        # English: Use medical NER model if available, else generic
        try:
            self.nlp_en = spacy.load("en_ner_bc5cdr_md")  # Medical entities
        except:
            self.nlp_en = spacy.load("en_core_web_sm")  # Generic fallback
            logger.warning("Medical NER model not available, using generic model")
        
        # Hindi: Generic model only
        try:
            self.nlp_hi = spacy.load("hi_core_news_sm")
            logger.info("Hindi NLP model loaded (generic, not medical)")
        except:
            self.nlp_hi = None
            logger.warning("Hindi NLP model not available")
        
        # Marathi: NOT AVAILABLE
        self.nlp_mr = None
        logger.warning("Marathi NLP not implemented - will use keyword fallback")
        
        # Negation detection
        self.negation_patterns = {
            'en': ['no', 'not', 'never', 'without', 'lack of', "don't", "doesn't", "didn't"],
            'hi': ['नहीं', 'बिना', 'कभी नहीं'],
            'mr': []  # Not available
        }
        
        # Uncertainty patterns
        self.uncertainty_patterns = {
            'en': ['maybe', 'might', 'possibly', 'perhaps', 'uncertain', 'not sure'],
            'hi': ['शायद', 'हो सकता है'],
            'mr': []
        }
```

#### Language Detection
```python
@app.post("/api/v1/extract-symptoms")
async def extract_symptoms(request: SymptomExtractionRequest):
    # Detect language if not specified
    if not request.language or request.language == "auto":
        request.language = detect_language(request.text)
        logger.info(f"Detected language: {request.language}")
    
    # Check if language is supported
    if request.language == "mr":
        logger.warning("Marathi NLP not implemented, returning fallback")
        return SymptomExtractionResponse(
            originalText=request.text,
            language="mr",
            nlp_available=False,
            fallback_reason="Marathi NLP model not available",
            entities={},
            extractedPhrases=[],
            structuredSummary={},
            confidence=0.0  # No confidence - no extraction
        )
```

#### Negation and Uncertainty Handling
```python
def extract_entities(self, doc, language: str):
    entities = []
    
    for ent in doc.ents:
        # Check for negation in surrounding context
        is_negated = self.is_negated(ent, doc, language)
        
        # Check for uncertainty
        is_uncertain = self.is_uncertain(ent, doc, language)
        
        # Calculate confidence based on model + context
        confidence = self.calculate_confidence(ent, is_negated, is_uncertain)
        
        entities.append({
            "text": ent.text,
            "label": ent.label_,
            "start": ent.start_char,
            "end": ent.end_char,
            "negated": is_negated,
            "uncertain": is_uncertain,
            "confidence": confidence  # NEVER hardcoded
        })
    
    return entities
```

#### Safe Fallback on NLP Failure
```java
@Service
public class EmergencySessionService {
    
    public EmergencySessionResponse createEmergency(CreateEmergencyRequest request) {
        NlpExtractionDTO nlpResult = null;
        TriageMethod triageMethod;
        
        try {
            // Attempt NLP extraction (3-second timeout)
            nlpResult = nlpIntegrationService.extractSymptoms(
                request.getSymptoms(),
                request.getLanguage()
            );
            
            if (nlpResult != null && nlpResult.getConfidence() > 0.5) {
                triageMethod = TriageMethod.NLP_ENHANCED;
            } else {
                log.warn("NLP confidence too low ({}), using keyword fallback", 
                         nlpResult != null ? nlpResult.getConfidence() : "null");
                triageMethod = TriageMethod.KEYWORD_FALLBACK;
            }
        } catch (Exception e) {
            log.error("NLP service failed, using keyword fallback", e);
            triageMethod = TriageMethod.KEYWORD_FALLBACK;
        }
        
        // Java triage engine makes final decision
        TriageAssessment triage = triageService.assess(
            request.getSymptoms(),
            nlpResult,  // May be null
            triageMethod
        );
        
        // NEVER default to GREEN on failure - use ORANGE (escalate for human review)
        if (triage.getUrgencyLevel() == UrgencyLevel.UNKNOWN) {
            log.warn("Triage resulted in UNKNOWN, escalating to ORANGE");
            triage.setUrgencyLevel(UrgencyLevel.ORANGE);
            triage.setReasoning("Unable to automatically classify - requires human review");
        }
        
        // Continue with session creation...
    }
}
```

### Affected Components
- **Python Service:**
  - `spacy_service.py` - Add negation/uncertainty detection
  - `language_detector.py` - Implement language detection
  - `confidence_calculator.py` - Dynamic confidence scoring
  - `models/response.py` - Add `nlp_available`, `fallback_reason` fields
  
- **Java Backend:**
  - `NlpIntegrationService.java` - Handle null responses, low confidence
  - `TriageService.java` - Never default to GREEN on failure
  - Add `TriageMethod` enum: `SOS_DIRECT`, `NLP_ENHANCED`, `KEYWORD_FALLBACK`, `HUMAN_REVIEW`
  
- **Database:**
  - `emergency_sessions.triage_method` - Store which method was used

### Acceptance Criteria
- [ ] Marathi input returns `nlp_available=false` with clear reason
- [ ] "No chest pain" detected as negated symptom
- [ ] "Maybe fever" detected as uncertain symptom
- [ ] Confidence calculated dynamically (not hardcoded)
- [ ] NLP failure triggers keyword fallback, not GREEN urgency
- [ ] UNKNOWN urgency escalates to ORANGE for human review

### Test Cases
```python
def test_marathi_nlp_returns_fallback():
    request = SymptomExtractionRequest(
        text="मला छातीत दुखत आहे",  # Marathi
        language="mr"
    )
    
    response = extract_symptoms(request)
    
    assert response.nlp_available == False
    assert "Marathi" in response.fallback_reason
    assert response.confidence == 0.0

def test_negation_detection():
    request = SymptomExtractionRequest(
        text="No chest pain, but feeling dizzy",
        language="en"
    )
    
    response = extract_symptoms(request)
    
    chest_pain_entity = [e for e in response.entities if "chest pain" in e['text'].lower()][0]
    assert chest_pain_entity['negated'] == True
```

---

## DECISION 5: TRIAGE RULES ENGINE

### Original Problem
- Triage rules not versioned
- No test cases for rules
- Unknown symptoms defaulted to GREEN
- No distinction between prototype and clinical validation

**Risk:** Incorrect urgency classification, untraceable rule changes, false safety assurance.

### Revised Design

#### Versioned Triage Rules
```java
@Component
public class TriageRulesEngine {
    
    private static final String RULES_VERSION = "1.0.0";
    
    // RED level rules (version 1.0.0)
    private static final List<TriageRule> RED_RULES = List.of(
        new TriageRule("chest pain", "cardiac", List.of("chest pain", "heart attack", "cardiac arrest")),
        new TriageRule("breathing", "respiratory", List.of("not breathing", "stopped breathing", "no breath")),
        new TriageRule("consciousness", "neurological", List.of("unconscious", "unresponsive", "collapsed")),
        new TriageRule("severe bleeding", "trauma", List.of("severe bleeding", "heavy bleeding", "blood loss"))
        // ... more rules
    );
    
    public TriageAssessment assess(String symptoms, NlpExtractionDTO nlp) {
        log.info("Assessing triage using rules version {}", RULES_VERSION);
        
        TriageAssessment assessment = new TriageAssessment();
        assessment.setRulesVersion(RULES_VERSION);
        assessment.setAssessmentMethod("AUTOMATED_PROTOTYPE");  // NOT "CLINICAL_VALIDATED"
        
        // Apply rules...
        UrgencyLevel level = applyRules(symptoms, nlp);
        
        // NEVER default to GREEN - unknown = ORANGE (requires review)
        if (level == UrgencyLevel.UNKNOWN) {
            level = UrgencyLevel.ORANGE;
            assessment.setReasoning("Unable to classify symptoms automatically. Requires human clinical review. " +
                                   "⚠️ This is a software prototype, not a validated medical triage system.");
        }
        
        assessment.setUrgencyLevel(level);
        return assessment;
    }
}
```

#### Test-Driven Triage Rules
```java
@SpringBootTest
public class TriageRulesEngineTest {
    
    @Autowired
    private TriageRulesEngine triageEngine;
    
    @Test
    @DisplayName("RED: Chest pain should classify as RED urgency")
    void testChestPain_ClassifiedRed() {
        TriageAssessment result = triageEngine.assess("Severe chest pain for 20 minutes", null);
        
        assertEquals(UrgencyLevel.RED, result.getUrgencyLevel());
        assertTrue(result.getMatchedKeywords().contains("chest pain"));
        assertEquals("1.0.0", result.getRulesVersion());
    }
    
    @Test
    @DisplayName("ORANGE: High fever should classify as ORANGE urgency")
    void testHighFever_ClassifiedOrange() {
        TriageAssessment result = triageEngine.assess("Very high fever and severe headache", null);
        
        assertEquals(UrgencyLevel.ORANGE, result.getUrgencyLevel());
    }
    
    @Test
    @DisplayName("UNKNOWN: Vague symptoms should escalate to ORANGE")
    void testVagueSymptoms_EscalateToOrange() {
        TriageAssessment result = triageEngine.assess("Not feeling well", null);
        
        assertEquals(UrgencyLevel.ORANGE, result.getUrgencyLevel());
        assertTrue(result.getReasoning().contains("requires human clinical review"));
    }
    
    @Test
    @DisplayName("NEGATION: 'No chest pain' should not trigger RED")
    void testNegation_NoFalsePositive() {
        NlpExtractionDTO nlp = new NlpExtractionDTO();
        nlp.addEntity(new Entity("chest pain", true, false, 0.9));  // negated=true
        
        TriageAssessment result = triageEngine.assess("No chest pain, just tired", nlp);
        
        assertNotEquals(UrgencyLevel.RED, result.getUrgencyLevel());
    }
}
```

#### Disclaimer Requirements
```java
public class TriageAssessment {
    private UrgencyLevel urgencyLevel;
    private String reasoning;
    private String rulesVersion;
    private String assessmentMethod;  // AUTOMATED_PROTOTYPE, HUMAN_REVIEW, CLINICAL_VALIDATED
    
    public String getDisclaimerText() {
        return "⚠️ IMPORTANT DISCLAIMER:\n" +
               "This urgency classification is generated by an AUTOMATED SOFTWARE PROTOTYPE for demonstration purposes only.\n" +
               "It is NOT a clinically validated medical triage system.\n" +
               "It does NOT replace professional medical assessment.\n" +
               "For life-threatening emergencies, immediately call 108, 102, or 112.\n" +
               "Rules version: " + rulesVersion + " | Method: " + assessmentMethod;
    }
}
```

### Affected Components
- **Backend:**
  - `TriageRulesEngine.java` - Add version tracking, test cases
  - `TriageAssessment.java` - Add disclaimer method, rules version
  - `TriageRule.java` - Structured rule definition
  
- **Database:**
  - `emergency_sessions.triage_rules_version` - Track which version was used
  - `emergency_sessions.triage_method` - AUTOMATED_PROTOTYPE vs HUMAN_REVIEW
  
- **Frontend:**
  - Display disclaimer prominently with every triage result
  - Never hide or minimize warning text

### Acceptance Criteria
- [ ] Each triage rule has explicit test case
- [ ] Rules version tracked in database
- [ ] Unknown symptoms escalate to ORANGE (never GREEN)
- [ ] Disclaimer displayed on every triage result
- [ ] Negated symptoms don't trigger false positives

---

## DECISION 6: DATABASE MIGRATION STRATEGY

### Original Problem
- Proposed blindly recreating tables with Flyway
- No inspection of existing schema
- No data backup strategy
- Risk of data loss

**Risk:** Destroying existing emergency sessions, unrecoverable data loss.

### Revised Design

#### Pre-Migration Assessment
```bash
# Step 1: Inspect existing schema
psql -h localhost -U postgres -d aayush_db -c "\dt"
psql -h localhost -U postgres -d aayush_db -c "\d emergency_sessions"

# Step 2: Backup existing data
pg_dump -h localhost -U postgres -d aayush_db > backup_pre_flyway_$(date +%Y%m%d_%H%M%S).sql

# Step 3: Export existing sessions to JSON (if any)
psql -h localhost -U postgres -d aayush_db -c "COPY (SELECT row_to_json(t) FROM emergency_sessions t) TO '/tmp/emergency_sessions_backup.json'"
```

#### Flyway Migration Strategy
```sql
-- V1__Baseline_Existing_Schema.sql
-- This migration captures the EXISTING schema state

-- Check if emergency_sessions already exists
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'emergency_sessions') THEN
        RAISE NOTICE 'emergency_sessions table already exists - preserving data';
        
        -- Add any missing columns to existing table
        IF NOT EXISTS (SELECT FROM information_schema.columns 
                       WHERE table_name = 'emergency_sessions' AND column_name = 'triage_method') THEN
            ALTER TABLE emergency_sessions 
            ADD COLUMN triage_method VARCHAR(30) DEFAULT 'KEYWORD_FALLBACK';
        END IF;
        
        IF NOT EXISTS (SELECT FROM information_schema.columns 
                       WHERE table_name = 'emergency_sessions' AND column_name = 'nlp_extracted_data') THEN
            ALTER TABLE emergency_sessions 
            ADD COLUMN nlp_extracted_data JSONB;
        END IF;
    ELSE
        -- Create table if it doesn't exist
        CREATE TABLE emergency_sessions (
            id BIGSERIAL PRIMARY KEY,
            session_id VARCHAR(50) UNIQUE NOT NULL,
            -- ... full schema
        );
    END IF;
END $$;
```

```sql
-- V2__Add_Users_And_Roles.sql
-- Creates new tables (won't affect existing emergency_sessions)

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    -- ... schema
);

CREATE TABLE IF NOT EXISTS roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL
);

-- Insert default roles
INSERT INTO roles (name, description) VALUES
('SENIOR', 'Senior citizen'),
('CAREGIVER', 'Family caregiver'),
('EMERGENCY_STAFF', 'Emergency response coordinator'),
('TRANSPORT_STAFF', 'Ambulance driver / transport provider'),
('HOSPITAL_STAFF', 'Hospital emergency staff'),
('ADMIN', 'System administrator')
ON CONFLICT (name) DO NOTHING;  -- Skip if already exists
```

#### Migration Rollback Plan
```sql
-- Each migration has corresponding rollback
-- V1__Baseline_Existing_Schema__ROLLBACK.sql (if needed)
-- V2__Add_Users_And_Roles__ROLLBACK.sql

-- Rollback V2
DROP TABLE IF EXISTS user_roles CASCADE;
DROP TABLE IF EXISTS refresh_tokens CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS roles CASCADE;
```

#### Flyway Configuration
```yaml
# application.yml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true  # Important: marks existing DB as baseline
    baseline-version: 0
    validate-on-migrate: true
    out-of-order: false
    locations: classpath:db/migration
    table: flyway_schema_history
```

### Affected Components
- **Backend:**
  - `src/main/resources/db/migration/` - Create migration scripts
  - `V1__Baseline_Existing_Schema.sql`
  - `V2__Add_Users_And_Roles.sql`
  - `V3__Add_Health_Profiles.sql`
  - `V4__Add_Caregivers.sql`
  - `V5__Add_Hospitals_Ambulances.sql`
  - `V6__Add_Vital_Readings.sql`
  - `V7__Add_Audit_Logs.sql`
  - `V8__Add_Indexes.sql`

- **DevOps:**
  - Pre-migration backup script
  - Post-migration validation script
  - Rollback procedure documentation

### Acceptance Criteria
- [ ] Existing emergency_sessions data preserved
- [ ] Flyway baseline created without errors
- [ ] All new tables created successfully
- [ ] Indexes added without locking production traffic
- [ ] Backup verified and restorable
- [ ] Rollback procedure tested in dev environment

### Test Cases
```bash
# Test migration in dev environment
# 1. Create test data
psql -h localhost -U postgres -d aayush_db_test -c "
INSERT INTO emergency_sessions (session_id, patient_name, symptoms, urgency_level, status, created_at)
VALUES ('TEST-001', 'Test Patient', 'Test symptoms', 'RED', 'CREATED', NOW());
"

# 2. Run Flyway migration
./mvnw flyway:migrate

# 3. Verify data preserved
psql -h localhost -U postgres -d aayush_db_test -c "
SELECT session_id, patient_name FROM emergency_sessions WHERE session_id = 'TEST-001';
"

# 4. Verify new tables created
psql -h localhost -U postgres -d aayush_db_test -c "\dt"

# 5. Test rollback
./mvnw flyway:undo
```

---

## DECISION 7: REAL-TIME COORDINATION

### Original Problem
- WebSocket as primary data source
- No reconnection state reload
- No subscription authorization

**Risk:** Data inconsistency, lost updates during disconnection, unauthorized access.

### Revised Design

#### PostgreSQL as Source of Truth
```java
@Service
public class EmergencySessionService {
    
    @Transactional
    public EmergencySessionResponse updateStatus(String sessionId, SessionStatus newStatus) {
        // 1. UPDATE POSTGRESQL FIRST
        EmergencySession session = repository.findBySessionId(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        
        SessionStatus oldStatus = session.getStatus();
        session.setStatus(newStatus);
        session.setUpdatedAt(LocalDateTime.now());
        EmergencySession updated = repository.save(session);  // PERSISTED
        
        // 2. THEN BROADCAST VIA WEBSOCKET (best effort)
        try {
            websocketService.broadcastStatusUpdate(sessionId, oldStatus, newStatus);
        } catch (Exception e) {
            log.warn("WebSocket broadcast failed for session {}, clients will sync on reconnect", sessionId, e);
            // Don't fail the request - data is in PostgreSQL
        }
        
        return mapToResponse(updated);
    }
}
```

#### Reconnection State Reload
```typescript
// Frontend: WebSocket reconnection handling
class WebSocketService {
    private reconnectAttempts = 0;
    private maxReconnectAttempts = 5;
    
    connect() {
        this.client.onConnect = () => {
            console.log('WebSocket connected');
            this.reconnectAttempts = 0;
            
            // RELOAD CURRENT STATE FROM REST API
            this.reloadCurrentState();
            
            // Then subscribe to updates
            this.subscribeToTopics();
        };
        
        this.client.onDisconnect = () => {
            console.log('WebSocket disconnected');
            
            if (this.reconnectAttempts < this.maxReconnectAttempts) {
                this.reconnectAttempts++;
                setTimeout(() => this.connect(), 2000 * this.reconnectAttempts);
            }
        };
    }
    
    async reloadCurrentState() {
        // Fetch latest data from REST API
        if (this.currentSessionId) {
            const session = await api.getEmergencySession(this.currentSessionId);
            store.dispatch(updateSessionFromServer(session));
        }
        
        // Reload active sessions list
        const activeSessions = await api.getActiveSessions();
        store.dispatch(updateActiveSessionsFromServer(activeSessions));
    }
}
```

#### Subscription Authorization
```java
@Configuration
public class WebSocketSecurityConfig extends AbstractSecurityWebSocketMessageBrokerConfigurer {
    
    @Override
    protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
        messages
            .nullDestMatcher().authenticated()  // Must be authenticated
            .simpSubscribeDestMatchers("/topic/emergency.*").hasAnyRole("EMERGENCY_STAFF", "SENIOR", "CAREGIVER")
            .simpSubscribeDestMatchers("/topic/transport.*").hasRole("TRANSPORT_STAFF")
            .simpSubscribeDestMatchers("/topic/hospital.*").hasRole("HOSPITAL_STAFF")
            .simpSubscribeDestMatchers("/user/queue/**").authenticated()
            .anyMessage().denyAll();
    }
    
    @Override
    protected boolean sameOriginDisabled() {
        return true;  // CSRF protection for WebSocket
    }
}
```

#### Session-Level Authorization
```java
@Component
public class WebSocketAuthorizationInterceptor implements ChannelInterceptor {
    
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            
            if (destination != null && destination.startsWith("/topic/emergency.")) {
                String sessionId = extractSessionId(destination);  // e.g., /topic/emergency.EMR-123
                
                User user = (User) accessor.getUser();
                
                // Check if user can access this specific session
                if (!emergencySessionService.canAccessSession(user.getId(), sessionId)) {
                    throw new AccessDeniedException(
                        "User " + user.getId() + " not authorized for session " + sessionId
                    );
                }
            }
        }
        
        return message;
    }
    
    private String extractSessionId(String destination) {
        // /topic/emergency.EMR-12345 -> EMR-12345
        return destination.substring(destination.lastIndexOf('.') + 1);
    }
}
```

### Affected Components
- **Backend:**
  - `EmergencySessionService.java` - PostgreSQL first, WebSocket second
  - `WebSocketSecurityConfig.java` - Role-based subscription auth
  - `WebSocketAuthorizationInterceptor.java` - Session-level auth
  
- **Frontend:**
  - `websocket.ts` - Add reconnection state reload
  - `api/client.ts` - Add session/list reload methods

### Acceptance Criteria
- [ ] PostgreSQL update succeeds even if WebSocket broadcast fails
- [ ] Reconnecting client loads latest state from REST API
- [ ] User cannot subscribe to unauthorized session topics
- [ ] WebSocket CSRF protection enabled

---

## DECISION 8: MOCK INTEGRATION LABELING

### Original Problem
- Documentation claimed "real ambulance dispatch"
- No clear distinction between mock and real integrations

**Risk:** False expectations, legal liability, user confusion.

### Revised Design

#### Clear Mock Labeling
```java
@Service
@Profile("dev")
public class MockAmbulanceService implements AmbulanceDispatchService {
    
    @Override
    public DispatchResponse dispatch(String sessionId, Location location) {
        log.info("🎭 MOCK: Simulating ambulance dispatch for session {}", sessionId);
        
        // Return simulated response
        return DispatchResponse.builder()
            .success(true)
            .message("⚠️ SIMULATED DISPATCH - No real ambulance was contacted")
            .ambulanceId("AMB-MOCK-" + System.currentTimeMillis())
            .estimatedArrivalMinutes(15)
            .isMock(true)  // Flag for frontend
            .build();
    }
}

@Service
@Profile("prod")
public class RealAmbulanceService implements AmbulanceDispatchService {
    
    @Override
    public DispatchResponse dispatch(String sessionId, Location location) {
        // Real integration with ambulance provider API
        // NOT IMPLEMENTED YET
        throw new UnsupportedOperationException("Real ambulance dispatch not yet implemented");
    }
}
```

#### Frontend Mock Indicators
```typescript
// Emergency response display
function EmergencyStatus({ session }: Props) {
    return (
        <div className="emergency-status">
            <h2>Emergency Status</h2>
            <p>Session ID: {session.sessionId}</p>
            <p>Urgency: <UrgencyBadge level={session.urgency} /></p>
            
            {session.ambulanceAssigned && (
                <div className="alert alert-warning">
                    <AlertTriangle size={20} />
                    <strong>⚠️ SIMULATED AMBULANCE ASSIGNMENT</strong>
                    <p>
                        This is a demonstration system. No real ambulance has been dispatched.
                        For actual emergencies, call 108, 102, or 112 immediately.
                    </p>
                    <p className="mock-details">
                        Mock Ambulance ID: {session.ambulance.id}<br />
                        Mock ETA: {session.ambulance.eta} minutes (simulated)
                    </p>
                </div>
            )}
            
            {session.hospitalAssigned && (
                <div className="alert alert-info">
                    <Info size={20} />
                    <strong>Hospital Information (Live Data)</strong>
                    <p>
                        Hospital: {session.hospital.name}<br />
                        Distance: {session.hospital.distance} km<br />
                        <small>⚠️ Bed availability is sample data for demonstration</small>
                    </p>
                </div>
            )}
        </div>
    );
}
```

#### API Response Mocking Flags
```java
public class EmergencySessionResponse {
    private String sessionId;
    private UrgencyLevel urgencyLevel;
    
    // ... other fields
    
    // Mock flags for frontend
    private MockDataIndicators mockData;
    
    @Data
    @Builder
    public static class MockDataIndicators {
        private boolean ambulanceDispatchMocked;
        private boolean hospitalCapacityMocked;
        private boolean etaMocked;
        private boolean notificationsMocked;
        private String mockDataWarning = "This system uses simulated data for demonstration. Do not rely on this for actual emergency coordination.";
    }
}
```

### Affected Components
- **Backend:**
  - All service interfaces use adapter pattern
  - Mock implementations for dev profile
  - Real implementations throw UnsupportedOperationException in prod until implemented
  
- **Frontend:**
  - All mock data has warning badges
  - Never claim "ambulance dispatched" - say "simulated dispatch"
  - Display 108/112 emergency numbers prominently

### Acceptance Criteria
- [ ] Every mock integration has `@Profile("dev")` annotation
- [ ] Every mock response includes `isMock` flag
- [ ] Frontend displays warning for all mocked features
- [ ] API documentation clearly labels mock vs real endpoints
- [ ] Logs prefix with "🎭 MOCK:" for all simulated actions

---

## DECISION 9: REVISED IMPLEMENTATION ROADMAP

### Original Problem
- Implementation order didn't prioritize safety-critical features
- Attempted features before foundation was secure

### Revised Order

#### Phase 1: Backend Foundation (Week 1)
**Priority: Safety and Security**

1. **Database Migration Safety**
   - Backup existing database
   - Inspect schema
   - Create Flyway baseline
   - Migrate incrementally
   - **Acceptance:** No data loss, rollback tested

2. **Authentication Foundation**
   - JWT token generation and validation
   - Refresh token rotation
   - Public registration restrictions (SENIOR/CAREGIVER only)
   - Password hashing (BCrypt)
   - **Acceptance:** Only SENIOR/CAREGIVER can self-register

3. **Health Profile Module**
   - Time-series vital readings
   - Structured allergies, medications, conditions
   - Support null/incomplete profiles
   - **Acceptance:** Emergency creation works with zero profile data

#### Phase 2: Emergency Creation & SOS (Week 2)
**Priority: Core Emergency Workflow**

4. **Direct SOS Workflow**
   - `/api/emergency-sessions/sos` endpoint
   - < 500ms response time
   - No dependencies on NLP, profile, or location
   - Duplicate request handling
   - **Acceptance:** SOS creates RED session in < 500ms

5. **Standard Emergency Creation**
   - Java triage rules engine (versioned)
   - Keyword-based fallback
   - Health profile integration (optional)
   - **Acceptance:** Emergency created with/without profile

6. **Authorization & Data Access**
   - Role-based endpoint protection
   - Minimum necessary information for staff
   - Patient/caregiver data access control
   - **Acceptance:** Transport can't see full medical history

#### Phase 3: Python NLP & Java Triage (Week 3)
**Priority: Enhanced Symptom Extraction**

7. **Python FastAPI Service**
   - English NLP (medical NER if available, else generic)
   - Hindi NLP (generic model)
   - Marathi fallback (keyword only, no model)
   - Negation and uncertainty detection
   - Dynamic confidence calculation
   - **Acceptance:** Marathi returns fallback, negation detected

8. **Java Triage Integration**
   - Call Python with 3-second timeout
   - Use NLP output if confidence > 0.5
   - Keyword fallback if NLP fails/low confidence
   - Unknown symptoms escalate to ORANGE (never GREEN)
   - **Acceptance:** NLP failure doesn't block emergency creation

#### Phase 4: Hospital & Transport Coordination (Week 4)
**Priority: Resource Coordination**

9. **Hospital Management**
   - Hospital CRUD operations (admin only)
   - Nearest hospital finder (location-based)
   - Bed capacity tracking (with staleness indicator)
   - **Acceptance:** Nearest hospital found within 50km

10. **Ambulance Management (Mock Adapter)**
    - Ambulance fleet CRUD (admin only)
    - Mock dispatch service (dev profile)
    - Clear "SIMULATED DISPATCH" warnings
    - Real adapter interface (for future integration)
    - **Acceptance:** Mock dispatch labeled clearly

#### Phase 5: Secure WebSocket Integration (Week 5-6)
**Priority: Real-Time Coordination**

11. **WebSocket Security**
    - Role-based subscription authorization
    - Session-level access control
    - JWT authentication for WebSocket connections
    - **Acceptance:** Unauthorized subscription fails

12. **PostgreSQL-First Updates**
    - All updates persist to PostgreSQL first
    - WebSocket broadcast is best-effort
    - Reconnection loads state from REST API
    - **Acceptance:** WebSocket failure doesn't prevent updates

#### Phase 6: Frontend Integration (Week 6-7)
**Priority: User Experience**

13. **Authentication UI**
    - Login/register forms (SENIOR/CAREGIVER only)
    - JWT token storage and refresh
    - Role-based navigation
    - **Acceptance:** Staff registration blocked on public form

14. **Emergency Submission**
    - Prominent SOS button
    - Standard symptom form
    - Profile completion indicator
    - Clear mock data warnings
    - **Acceptance:** Both SOS and standard paths work

15. **Real-Time Updates**
    - WebSocket connection with auto-reconnect
    - State reload on reconnection
    - Session-specific subscriptions
    - **Acceptance:** Reconnection loads latest state

#### Phase 7: Testing & Deployment (Week 7-8)
**Priority: Quality Assurance**

16. **Comprehensive Testing**
    - Triage rules test cases
    - Security authorization tests
    - Integration tests (Testcontainers)
    - E2E tests (Playwright)
    - Load testing (100+ concurrent users)
    - **Acceptance:** 80% code coverage

17. **Production Deployment**
    - Docker Compose configuration
    - Environment-specific configs
    - CI/CD pipeline
    - Monitoring and logging
    - **Acceptance:** Successful prod deployment

---

## UNRESOLVED DECISIONS

The following decisions require your input before finalizing the architecture:

### 1. **Medical Disclaimer Placement**
**Question:** Where should medical disclaimers appear?
- **Options:**
  a) Every API response (verbose but clear)
  b) Only in frontend UI
  c) Both (redundant but safest)
- **Recommendation:** Both - backend includes disclaimer text, frontend displays prominently
- **Your Decision:** ?

### 2. **Caregiver Consent Granularity**
**Question:** What permission levels should caregivers have?
- **Options:**
  a) All-or-nothing (full access or none)
  b) Granular (can view profile, can receive notifications, can see location, can call ambulance)
  c) Tiered (basic, standard, full)
- **Recommendation:** Granular permissions with explicit consent for each
- **Your Decision:** ?

### 3. **Emergency Session Expiry**
**Question:** Should old emergency sessions auto-archive?
- **Options:**
  a) Never delete (audit trail)
  b) Archive after 90 days (move to cold storage)
  c) Soft delete after 30 days (status = ARCHIVED)
- **Recommendation:** Soft delete after 30 days, hard delete never (legal/audit requirement)
- **Your Decision:** ?

### 4. **Python NLP Timeout**
**Question:** What timeout for Python NLP calls?
- **Options:**
  a) 3 seconds (fast but may timeout on slow servers)
  b) 5 seconds (safer but delays emergency creation)
  c) 10 seconds (very safe but poor UX)
- **Recommendation:** 3 seconds for standard flow, 0 seconds (skip) for SOS
- **Your Decision:** ?

### 5. **Triage Rules Update Strategy**
**Question:** How to update triage rules in production?
- **Options:**
  a) Code deployment only (requires redeploy)
  b) Database-driven rules (editable via admin UI)
  c) Hybrid (code for core rules, database for adjustments)
- **Recommendation:** Code deployment only (better versioning, less risk)
- **Your Decision:** ?

### 6. **Minimum Profile Completeness for Standard Flow**
**Question:** Should standard emergency creation require minimum profile data?
- **Options:**
  a) No minimum (same as SOS)
  b) Require at least name, age, emergency contact
  c) Encourage but don't require
- **Recommendation:** Encourage but don't require (user can skip, system warns)
- **Your Decision:** ?

### 7. **WebSocket Connection Limit**
**Question:** How many concurrent WebSocket connections per user?
- **Options:**
  a) Unlimited (user can open many tabs)
  b) 3 connections per user (limit tabs)
  c) 1 connection per user (single session)
- **Recommendation:** 3 connections (allow multiple devices)
- **Your Decision:** ?

### 8. **Hospital Capacity Data Staleness**
**Question:** How old can hospital capacity data be before warning?
- **Options:**
  a) 5 minutes (very fresh but frequent updates required)
  b) 30 minutes (reasonable for demo)
  c) 24 hours (stale but low update burden)
- **Recommendation:** 30 minutes with "Last updated: X minutes ago" indicator
- **Your Decision:** ?

### 9. **Audit Log Retention**
**Question:** How long to keep audit logs?
- **Options:**
  a) Forever (unlimited growth)
  b) 1 year (legal minimum)
  c) 7 years (HIPAA compliance)
- **Recommendation:** 7 years if handling real PHI, 1 year for demo
- **Your Decision:** ?

### 10. **Emergency Numbers Display**
**Question:** When to show emergency numbers (108/112)?
- **Options:**
  a) On every page (always visible)
  b) Only on emergency submission screens
  c) Only when SOS button is pressed
- **Recommendation:** On every page in header/footer (always accessible)
- **Your Decision:** ?

---

## NEXT STEPS

1. **Review this document** and provide decisions for unresolved items
2. **Approve architecture corrections** before implementation
3. **Prioritize Phase 1** (Backend Foundation + Authentication)
4. **Begin implementation** only after approval

---

**Document Control:**
- **Created:** October 31, 2026
- **Author:** Architecture Team (Post-Review Revision)
- **Status:** Awaiting Client Review
- **Next Update:** After unresolved decisions finalized

---

**END OF ARCHITECTURE DECISIONS DOCUMENT**
