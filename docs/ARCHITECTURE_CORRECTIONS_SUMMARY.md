# ARCHITECTURE CORRECTIONS SUMMARY
## Changes Made to AAYUSH Documentation

**Date:** October 31, 2026  
**Version:** 2.0  
**Status:** Corrected Architecture - Ready for Implementation

---

## OVERVIEW

This document summarizes all critical corrections made to the AAYUSH architecture documentation based on the comprehensive review. All changes prioritize **safety, security, and realistic technical constraints**.

---

## DOCUMENTS UPDATED

1. ✅ **docs/ARCHITECTURE_DECISIONS.md** - NEW (9 decisions + 10 unresolved questions)
2. ✅ **docs/SYSTEM_ARCHITECTURE.md** - CORRECTED (sections 1, 4, 6, 7)
3. ⏳ **docs/PROJECT_AUDIT.md** - PENDING UPDATES
4. ⏳ **docs/AUDIT_SUMMARY.md** - PENDING UPDATES

---

## CRITICAL CORRECTIONS MADE

### 1. **Direct SOS Workflow** ✅ ADDED

**Problem:** Original design required NLP extraction, profile lookup, and location for all emergencies, causing potential delays.

**Solution:**
- Added `/api/emergency-sessions/sos` endpoint
- Response time < 500ms (p95)
- Skips NLP, profile, location lookup
- Assumes RED urgency
- Sets `triage_method = 'SOS_DIRECT'`
- Background jobs enhance session asynchronously
- Duplicate detection (5-minute window)

**Files Updated:**
- `docs/ARCHITECTURE_DECISIONS.md` - Decision 1
- `docs/SYSTEM_ARCHITECTURE.md` - Section 4.2 (new endpoint)

**Tests Required:**
- SOS with minimal data (< 500ms)
- SOS with empty symptoms succeeds
- Duplicate SOS returns existing session

---

### 2. **Health Profile Data Model** ✅ CORRECTED

**Problem:** Single-value vital readings (overwrites history), no source tracking, flat text allergies.

**Solution:**
- Time-series `vital_readings` table (never overwrite)
- Structured `allergies`, `medications`, `chronic_conditions` tables
- Add `source`, `verification_status` to all medical data
- `caregiver_permission_history` audit table
- Emergency creation supports null `health_profile_id`

**Files Updated:**
- `docs/ARCHITECTURE_DECISIONS.md` - Decision 2
- `docs/SYSTEM_ARCHITECTURE.md` - Section 3 (database schema)

**Tests Required:**
- Emergency created without profile
- Vital reading stored with timestamp/source
- Caregiver permission change creates audit log

---

### 3. **Security & Authorization** ✅ CORRECTED

**Problem:** Public registration allows any role, no refresh token rotation, no WebSocket auth.

**Solution:**
- Public registration ONLY for SENIOR and CAREGIVER
- Staff accounts created by admin via `/api/admin/users`
- Refresh token rotation (revoke old, issue new)
- Token reuse detection → revoke entire family
- WebSocket subscription authorization (role + session-level)
- Minimum necessary information for staff (transport ≠ full medical history)
- No PII in application logs

**Files Updated:**
- `docs/ARCHITECTURE_DECISIONS.md` - Decision 3
- `docs/SYSTEM_ARCHITECTURE.md` - Sections 4.1, 6

**Tests Required:**
- Staff role registration fails (403 Forbidden)
- Refresh token reuse revokes family
- WebSocket subscription to unauthorized session fails
- Transport staff cannot access full profile

---

### 4. **Python NLP Constraints** ✅ CORRECTED

**Problem:** Assumed generic spaCy models recognize medical symptoms, claimed Marathi support, hardcoded confidence.

**Solution:**
- English: Medical NER if available, else generic (with warning)
- Hindi: Generic model only (NOT medical-specific)
- Marathi: NOT AVAILABLE → returns `nlp_available=false`
- Negation detection ("no chest pain" ≠ positive symptom)
- Uncertainty detection ("maybe fever" flagged)
- Dynamic confidence calculation (never hardcoded)
- Python returns symptom facts ONLY (no urgency decisions)

**Files Updated:**
- `docs/ARCHITECTURE_DECISIONS.md` - Decision 4
- `docs/SYSTEM_ARCHITECTURE.md` - Section 7

**Tests Required:**
- Marathi returns fallback (nlp_available=false)
- "No chest pain" detected as negated
- Confidence calculated dynamically
- NLP failure doesn't block emergency

---

### 5. **Triage Rules Engine** ✅ CORRECTED

**Problem:** No versioning, no test cases, unknown symptoms defaulted to GREEN.

**Solution:**
- Versioned rules (e.g., "1.0.0")
- Every rule has explicit test case
- Unknown/uncertain symptoms escalate to ORANGE (not GREEN)
- Java makes ALL triage decisions (Python extracts only)
- Clear disclaimer: "AUTOMATED PROTOTYPE, not clinically validated"
- NLP failure triggers keyword fallback

**Files Updated:**
- `docs/ARCHITECTURE_DECISIONS.md` - Decision 5
- `docs/SYSTEM_ARCHITECTURE.md` - Sections 1.2, 7.3

**Tests Required:**
- Unknown symptoms → ORANGE
- Negated symptoms don't trigger false positives
- All triage rules have passing tests
- Triage disclaimer displayed

---

### 6. **Database Migration Strategy** ✅ CORRECTED

**Problem:** Proposed blindly recreating tables, no backup strategy.

**Solution:**
- Pre-migration backup (pg_dump)
- Inspect existing schema first
- Flyway baseline-on-migrate (preserve data)
- V1 migration adds columns to existing table (doesn't recreate)
- V2+ migrations create new tables
- Rollback scripts for each migration
- Test in dev environment first

**Files Updated:**
- `docs/ARCHITECTURE_DECISIONS.md` - Decision 6
- `docs/SYSTEM_ARCHITECTURE.md` - Section 3.3

**Tests Required:**
- Migration preserves existing data
- Rollback procedure works
- V1 adds columns without data loss

---

### 7. **Real-Time Coordination** ✅ CORRECTED

**Problem:** WebSocket as primary data source, no reconnection state reload.

**Solution:**
- PostgreSQL FIRST, WebSocket SECOND (best-effort)
- WebSocket failure doesn't prevent updates
- Reconnecting clients reload state from REST API
- Authenticated WebSocket connections (JWT)
- Session-level subscription authorization
- Log warnings on WebSocket failure (don't fail request)

**Files Updated:**
- `docs/ARCHITECTURE_DECISIONS.md` - Decision 7
- `docs/SYSTEM_ARCHITECTURE.md` - Sections 1.2, 5

**Tests Required:**
- PostgreSQL update succeeds even if WebSocket fails
- Reconnection loads latest state from REST
- Unauthorized subscription fails

---

### 8. **Mock Integration Labeling** ✅ CORRECTED

**Problem:** Claims "real ambulance dispatch" without clear mock labeling.

**Solution:**
- All mocks have `@Profile("dev")` annotation
- Mock responses include `isMock` flag
- Frontend displays "⚠️ SIMULATED DISPATCH" warnings
- `mockDataIndicators` in API responses
- Real implementations throw `UnsupportedOperationException` in prod
- Never claim 108/112 dispatch or live hospital integration

**Files Updated:**
- `docs/ARCHITECTURE_DECISIONS.md` - Decision 8
- `docs/SYSTEM_ARCHITECTURE.md` - Sections 1.2, 4.2

**Tests Required:**
- All mock data labeled clearly
- Mock ambulance dispatch includes warning
- API documentation labels mock vs real

---

### 9. **Implementation Roadmap** ✅ REORDERED

**Problem:** Original order attempted features before foundation was secure.

**Solution - Revised Order:**
1. **Week 1:** Database migration safety + Authentication + Health profiles
2. **Week 2:** Emergency creation + Direct SOS + Authorization
3. **Week 3:** Python NLP + Java triage integration + Safe fallbacks
4. **Week 4:** Hospital/transport + Mock adapters + Caregiver permissions
5. **Week 5-6:** WebSocket security + PostgreSQL-first updates
6. **Week 6-7:** Frontend integration + Authentication UI
7. **Week 7-8:** Testing (unit, integration, E2E, security) + Deployment

**Rationale:**
- Safety-critical features first (SOS, authentication)
- Build secure foundation before enhancements
- NLP is enhancement, not blocker
- WebSocket after PostgreSQL persistence works

**Files Updated:**
- `docs/ARCHITECTURE_DECISIONS.md` - Decision 9
- `docs/SYSTEM_ARCHITECTURE.md` - Section 13 (attempted update)

---

## UNRESOLVED DECISIONS (REQUIRE CLIENT INPUT)

The following 10 decisions need your input before finalizing:

1. **Medical Disclaimer Placement** - Every API response, only frontend, or both?
2. **Caregiver Consent Granularity** - All-or-nothing, granular permissions, or tiered?
3. **Emergency Session Expiry** - Never delete, archive after 90 days, or soft delete after 30 days?
4. **Python NLP Timeout** - 3 seconds, 5 seconds, or 10 seconds?
5. **Triage Rules Update Strategy** - Code deployment only, database-driven, or hybrid?
6. **Minimum Profile Completeness** - No minimum, require basics, or encourage but not require?
7. **WebSocket Connection Limit** - Unlimited, 3 per user, or 1 per user?
8. **Hospital Capacity Staleness** - 5 minutes, 30 minutes, or 24 hours warning threshold?
9. **Audit Log Retention** - Forever, 1 year, or 7 years?
10. **Emergency Numbers Display** - Every page, only on emergency screens, or only when SOS pressed?

**Recommendations provided in `docs/ARCHITECTURE_DECISIONS.md` - awaiting your approval.**

---

## KEY ARCHITECTURAL PRINCIPLES (REVISED)

1. **Safety First**
   - Never block emergency assistance due to missing data or failed services
   - Direct SOS path bypasses all non-critical dependencies
   - Unknown urgency escalates to ORANGE (human review), never GREEN

2. **Separation of Concerns**
   - Python: Symptom extraction ONLY (no triage decisions)
   - Java: ALL triage decisions and business logic
   - PostgreSQL: Single source of truth
   - WebSocket: Best-effort real-time updates (not critical path)

3. **Security by Design**
   - Public registration restricted to SENIOR/CAREGIVER only
   - Refresh token rotation prevents token theft
   - WebSocket requires authentication and authorization
   - Staff sees minimum necessary information only
   - No PII in application logs

4. **Realistic Technical Constraints**
   - Generic spaCy models (no medical NER assumed)
   - Marathi NLP not available (fallback to keywords)
   - NLP timeout (3 seconds) with fallback
   - Mock integrations clearly labeled

5. **Audit and Transparency**
   - Triage rules versioned and tested
   - All medical data has source and verification status
   - Caregiver permission changes audited
   - Clear disclaimers: "PROTOTYPE, not clinically validated"

---

## TESTING REQUIREMENTS ADDED

### Unit Tests (60% coverage)
- ✅ Every triage rule has test case
- ✅ Negation detection tests
- ✅ Duplicate request detection
- ✅ Authorization tests (role restrictions)

### Integration Tests (30% coverage)
- ✅ Testcontainers for PostgreSQL
- ✅ NLP service integration with fallback
- ✅ WebSocket authorization
- ✅ Refresh token rotation

### E2E Tests (10% coverage)
- ✅ SOS workflow (< 500ms)
- ✅ Standard emergency workflow
- ✅ Real-time updates across tabs
- ✅ Reconnection state reload

### Security Tests
- ✅ Staff registration attempt (should fail)
- ✅ Token reuse detection
- ✅ Unauthorized WebSocket subscription
- ✅ SQL injection attempts
- ✅ OWASP ZAP scan

---

## API CHANGES SUMMARY

### New Endpoints
- `POST /api/emergency-sessions/sos` - Direct SOS (< 500ms)
- `POST /api/admin/users` - Create staff accounts (admin only)

### Modified Endpoints
- `POST /api/auth/register` - Now restricts role to SENIOR/CAREGIVER
- `POST /api/auth/refresh` - Now implements token rotation
- `POST /api/emergency-sessions` - Now supports null profile, includes mockDataIndicators
- `GET /api/emergency-sessions/{id}` - Now returns role-filtered data

### New Response Fields
- `triageMethod` - SOS_DIRECT, NLP_ENHANCED, KEYWORD_FALLBACK, HUMAN_REVIEW_REQUIRED
- `triageRulesVersion` - e.g., "1.0.0"
- `mockDataIndicators` - Flags for which data is simulated
- `nlpAvailable` - Boolean (false for Marathi)
- `fallbackReason` - String explaining why NLP unavailable
- `profileCompleteness` - Percentage (0-100)

---

## DATABASE SCHEMA CHANGES

### New Tables
- `refresh_tokens` - Token rotation tracking
- `vital_readings` - Time-series vitals (never overwrite)
- `allergies` - Structured with severity
- `medications` - With start/end dates
- `chronic_conditions` - With ICD-10 codes
- `caregiver_permission_history` - Audit trail

### Modified Tables
- `emergency_sessions` - Add columns:
  - `triage_method` VARCHAR(30)
  - `triage_rules_version` VARCHAR(10)
  - `nlp_extracted_data` JSONB

### Removed Assumptions
- ❌ Health profile is NOT required for emergency creation
- ❌ Vital readings are NOT single-value (now time-series)
- ❌ Allergies are NOT flat text (now structured)

---

## NEXT STEPS

### For You (Client):
1. **Read** `docs/ARCHITECTURE_DECISIONS.md`
2. **Decide** on 10 unresolved questions
3. **Approve** architecture corrections
4. **Prioritize** Phase 1 implementation

### For Development Team:
1. **Review** all corrected documentation
2. **Set up** dev environment (Docker Compose)
3. **Begin** Phase 1: Database migration safety + Authentication
4. **Test** each correction before moving to next phase

---

## FILES TO UPDATE NEXT

### High Priority:
- ⏳ `docs/PROJECT_AUDIT.md` - Update sections 2, 6, 7, 11 with corrections
- ⏳ `docs/AUDIT_SUMMARY.md` - Update current state, timeline, risks
- ⏳ `docs/SYSTEM_ARCHITECTURE.md` - Complete section 13 (roadmap), section 3 (full database schema)

### Medium Priority:
- ⏳ `backend/src/main/resources/db/migration/` - Write actual Flyway scripts
- ⏳ `backend/src/main/java/.../dto/` - Add new DTOs (NlpExtractionDTO updates, mockDataIndicators)
- ⏳ Backend test files - Write triage rule tests

### Low Priority (Future):
- Frontend component updates (after Phase 1 complete)
- Docker Compose configuration
- CI/CD pipeline setup

---

## SUMMARY

**Total Corrections Made:** 9 major decisions  
**Documents Updated:** 2 of 4 (50% complete)  
**New Files Created:** 2 (ARCHITECTURE_DECISIONS.md, this summary)  
**Unresolved Decisions:** 10 (awaiting client input)  
**Implementation Readiness:** 70% (needs decision approval + remaining doc updates)

**Critical Insight:** The original architecture was technically sound but lacked safety-first design (SOS path), realistic NLP constraints (Marathi unavailable), and security hardening (token rotation, role restrictions). All corrections prioritize **user safety**, **technical honesty**, and **secure defaults**.

---

**Document Status:** ✅ Complete  
**Next Action:** Client review of unresolved decisions in ARCHITECTURE_DECISIONS.md  
**Estimated Time to Finalize:** 1-2 hours (after client decisions)

---

**END OF CORRECTIONS SUMMARY**
