# Authentication Implementation Plan

**Project**: AAYUSH - Senior-Friendly Emergency Assistance Platform  
**Version**: 1.0  
**Date**: 2024-11-01  
**Status**: Planning - Task 1.1 Complete

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Existing Implementation Audit](#existing-implementation-audit)
3. [User Document Schema](#user-document-schema)
4. [API Endpoint Contracts](#api-endpoint-contracts)
5. [JWT Token Lifecycle](#jwt-token-lifecycle)
6. [Role-Based Authorization Design](#role-based-authorization-design)
7. [Security Architecture](#security-architecture)
8. [Frontend Integration Requirements](#frontend-integration-requirements)
9. [Testing Strategy](#testing-strategy)
10. [Implementation Tasks](#implementation-tasks)
11. [Environment Configuration](#environment-configuration)
12. [Unresolved Decisions](#unresolved-decisions)

---

## Executive Summary

This document defines the complete authentication and authorization implementation plan for the AAYUSH platform. The system will support secure user registration, login, JWT-based authentication, role-based authorization, and token lifecycle management.

**Core Requirements**:
- Secure user registration with BCrypt password hashing
- JWT-based stateless authentication with access and refresh tokens
- Role-based authorization (SENIOR, CAREGIVER, ADMIN)
- Email and mobile normalization with duplicate prevention
- Token refresh and revocation capabilities
- Security best practices including rate limiting and input validation

**Technology Stack**:
- Backend: Java 21, Spring Boot 3.2.11, Spring Security 6.x
- Database: MongoDB 7.0
- JWT Library: jjwt 0.12.6 (already present)
- Password Hashing: BCrypt (Spring Security built-in)

---

## Existing Implementation Audit

### Current Security Status

**SecurityConfig.java** (Current State):
```java
// Security is DISABLED - All endpoints use .anyRequest().permitAll()
// Comment: "For Milestone 3: Security is disabled to focus on core functionality"
```

**Current Configuration**:
- Spring Security dependency: Present in pom.xml
- JWT dependencies: jjwt-api 0.12.6, jjwt-impl, jjwt-jackson (Present)
- Security Config: Exists but explicitly disables authentication
- Session Management: STATELESS (already configured correctly)
- CORS: Configured for localhost:5173, 5174, 3000

**Existing Endpoints** (All Currently Public):
- `POST /api/emergency-sessions` - Create emergency session
- `GET /api/emergency-sessions` - List all sessions
- `GET /api/emergency-sessions/{sessionId}` - Get session by ID
- `PATCH /api/emergency-sessions/{sessionId}/status` - Update session status
- `GET /api/health` - Health check

### Package Structure

```
com.aayush.backend/
├── config/
│   ├── MongoConfig.java              (Auditing enabled)
│   └── SecurityConfig.java           (Security DISABLED)
├── controller/
│   ├── EmergencySessionController.java
│   └── HealthController.java
├── document/
│   └── EmergencySession.java         (MongoDB document, String ID)
├── dto/
│   ├── CreateEmergencySessionRequest.java
│   ├── EmergencySessionResponse.java
│   ├── PatientProfileDTO.java
│   └── UpdateSessionStatusRequest.java
├── exception/
│   ├── GlobalExceptionHandler.java   (Validation, 404, Conflict)
│   ├── ResourceNotFoundException.java
│   └── SessionIdConflictException.java
├── repository/
│   └── EmergencySessionRepository.java (MongoRepository<EmergencySession, String>)
└── service/
    ├── EmergencySessionService.java
    └── TriageService.java
```

### Existing Patterns to Follow

**DTO Validation Pattern**:
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestDTO {
    @NotBlank(message = "Field is required")
    @Size(min = X, max = Y, message = "...")
    @Pattern(regexp = "...", message = "...")
    private String field;
}
```

**Repository Pattern**:
```java
@Repository
public interface EmergencySessionRepository extends MongoRepository<EmergencySession, String> {
    Optional<EmergencySession> findBySessionId(String sessionId);
    boolean existsBySessionId(String sessionId);
}
```

**Exception Handling** (GlobalExceptionHandler):
- `@Valid` validation errors → 400 Bad Request with field details
- `ResourceNotFoundException` → 404 Not Found
- Custom exceptions → Appropriate HTTP status

**MongoDB Document Pattern**:
```java
@Document(collection = "emergency_sessions")
@Data
public class EmergencySession {
    @Id
    private String id;  // MongoDB ObjectId
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

### Frontend Authentication Status

**Current Frontend State**:
- No authentication implemented
- Direct navigation to role-based dashboards
- No login/registration screens
- API calls made without authentication headers
- localStorage used for session data (no user accounts)

**Frontend Files**:
- `src/pages/RoleSelection.tsx` - Role selection (no auth)
- `src/pages/DemoAccess.tsx` - Demo access (no auth)
- `src/pages/SeniorCitizenHome.tsx` - Emergency request form
- `src/services/sessionService.ts` - Local storage session management

### Test Infrastructure

**Test Profile** (`application-test.properties`):
```properties
spring.data.mongodb.uri=mongodb://localhost:27017/aayush_test
```

**Test Patterns**:
- `@SpringBootTest` + `@AutoConfigureMockMvc` for integration tests
- `@ActiveProfiles("test")` to use test database
- `@AfterEach` cleanup with `repository.deleteAll()`
- MockMvc for HTTP request/response testing
- ObjectMapper for JSON serialization

**Existing Test Results**:
- EmergencySessionControllerTest: 7/7 passing
- HealthControllerTest: 1/1 passing
- TriageServiceTest: 6/6 passing
- **Total: 14/14 passing**

---

## User Document Schema

### User MongoDB Document

```java
package com.aayush.backend.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    private String id;  // MongoDB ObjectId
    
    @Indexed(unique = true)
    private String email;  // Normalized: lowercase, trimmed
    
    @Indexed(unique = true)
    private String mobile;  // Normalized: digits only (e.g., "919876543210")
    
    private String fullName;
    
    private String passwordHash;  // BCrypt hash
    
    private UserRole role;  // SENIOR, CAREGIVER, ADMIN
    
    private AccountStatus status;  // ACTIVE, SUSPENDED, DELETED
    
    private boolean emailVerified;
    
    private boolean mobileVerified;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    // Optional fields for future extensions
    private LocalDateTime lastLoginAt;
    private String preferredLanguage;  // en, hi, mr
}
```

### Enumerations

```java
package com.aayush.backend.document;

public enum UserRole {
    SENIOR,      // Senior citizen requesting emergency assistance
    CAREGIVER,   // Family member or caregiver
    ADMIN        // System administrator (not self-registerable)
}
```

```java
package com.aayush.backend.document;

public enum AccountStatus {
    ACTIVE,      // Account is active and can authenticate
    SUSPENDED,   // Account temporarily suspended (admin action)
    DELETED      // Soft-deleted account
}
```

### MongoDB Indexes

**Required Unique Indexes**:
```javascript
db.users.createIndex({ "email": 1 }, { unique: true, sparse: true })
db.users.createIndex({ "mobile": 1 }, { unique: true, sparse: true })
```

**Query Optimization Indexes**:
```javascript
db.users.createIndex({ "role": 1, "status": 1 })
db.users.createIndex({ "createdAt": -1 })
```

### Refresh Token Document

```java
package com.aayush.backend.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "refresh_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    private String id;
    
    @Indexed(unique = true)
    private String token;  // Hashed refresh token
    
    @Indexed
    private String userId;  // Reference to User document
    
    private LocalDateTime issuedAt;
    
    @Indexed(expireAfterSeconds = 0)
    private LocalDateTime expiresAt;  // TTL index for auto-deletion
    
    private boolean revoked;
    
    @Indexed
    private LocalDateTime revokedAt;
}
```

**Refresh Token Indexes**:
```javascript
db.refresh_tokens.createIndex({ "token": 1 }, { unique: true })
db.refresh_tokens.createIndex({ "userId": 1 })
db.refresh_tokens.createIndex({ "expiresAt": 1 }, { expireAfterSeconds: 0 })
```

### Normalization Rules

**Email Normalization**:
```java
public static String normalizeEmail(String email) {
    if (email == null) return null;
    return email.trim().toLowerCase();
}
```

**Mobile Normalization**:
```java
public static String normalizeMobile(String mobile) {
    if (mobile == null) return null;
    // Remove all non-digits: +91-9876543210 → 919876543210
    String digits = mobile.replaceAll("\\D", "");
    // Ensure it starts with country code (91 for India)
    if (digits.length() == 10) {
        digits = "91" + digits;
    }
    return digits;
}
```

---

## API Endpoint Contracts

### Base Path
All authentication endpoints: `/api/auth`

---

### 1. User Registration

**Endpoint**: `POST /api/auth/register`

**Request Headers**:
```
Content-Type: application/json
```

**Request Body**:
```json
{
  "fullName": "Ramesh Kumar",
  "email": "ramesh.kumar@example.com",
  "mobile": "+91-9876543210",
  "password": "SecurePassword123!",
  "role": "SENIOR"
}
```

**Validation Rules**:
- `fullName`: Required, 2-100 characters
- `email`: Required, valid email format, unique
- `mobile`: Required, valid phone format, unique
- `password`: Required, 8-100 characters, must contain uppercase, lowercase, digit, special char
- `role`: Required, must be "SENIOR" or "CAREGIVER" only (ADMIN not allowed)

**Success Response** (201 Created):
```json
{
  "id": "67234abc123def456789",
  "fullName": "Ramesh Kumar",
  "email": "ramesh.kumar@example.com",
  "mobile": "919876543210",
  "role": "SENIOR",
  "status": "ACTIVE",
  "emailVerified": false,
  "mobileVerified": false,
  "createdAt": "2024-11-01T10:30:00Z"
}
```

**Error Responses**:

*400 Bad Request - Validation Error*:
```json
{
  "timestamp": "2024-11-01T10:30:00Z",
  "status": 400,
  "error": "Validation Failed",
  "message": "Invalid input data",
  "validationErrors": {
    "email": "Email is already registered",
    "password": "Password must contain at least one uppercase letter"
  }
}
```

*409 Conflict - Duplicate Account*:
```json
{
  "timestamp": "2024-11-01T10:30:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "User with this email or mobile already exists"
}
```

---

### 2. User Login

**Endpoint**: `POST /api/auth/login`

**Request Headers**:
```
Content-Type: application/json
```

**Request Body**:
```json
{
  "identifier": "ramesh.kumar@example.com",
  "password": "SecurePassword123!"
}
```

**Notes**:
- `identifier` can be either email or mobile number
- Both email and mobile will be normalized and checked

**Success Response** (200 OK):
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "a7f3c2e1b9d8f4a6c3e2b1d9f8a7c6e5b4d3a2c1...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": "67234abc123def456789",
    "fullName": "Ramesh Kumar",
    "email": "ramesh.kumar@example.com",
    "mobile": "919876543210",
    "role": "SENIOR",
    "status": "ACTIVE",
    "emailVerified": false,
    "mobileVerified": false
  }
}
```

**Error Responses**:

*401 Unauthorized - Invalid Credentials*:
```json
{
  "timestamp": "2024-11-01T10:30:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid credentials"
}
```

*403 Forbidden - Account Suspended*:
```json
{
  "timestamp": "2024-11-01T10:30:00Z",
  "status": 403,
  "error": "Forbidden",
  "message": "Account is suspended. Please contact support."
}
```

---

### 3. Refresh Access Token

**Endpoint**: `POST /api/auth/refresh`

**Request Headers**:
```
Content-Type: application/json
```

**Request Body**:
```json
{
  "refreshToken": "a7f3c2e1b9d8f4a6c3e2b1d9f8a7c6e5b4d3a2c1..."
}
```

**Success Response** (200 OK):
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "b8g4d3f2c0e9g5b7d4f3c2e0g9b8d7f6c5e4d3f2...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

**Notes**:
- Old refresh token is revoked
- New refresh token issued (token rotation)

**Error Responses**:

*401 Unauthorized - Invalid/Expired Token*:
```json
{
  "timestamp": "2024-11-01T10:30:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid or expired refresh token"
}
```

---

### 4. Logout

**Endpoint**: `POST /api/auth/logout`

**Request Headers**:
```
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Request Body**:
```json
{
  "refreshToken": "a7f3c2e1b9d8f4a6c3e2b1d9f8a7c6e5b4d3a2c1..."
}
```

**Success Response** (200 OK):
```json
{
  "message": "Logged out successfully"
}
```

**Notes**:
- Revokes the provided refresh token
- Access token remains valid until expiry (short-lived by design)

---

### 5. Get Current User

**Endpoint**: `GET /api/auth/me`

**Request Headers**:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Success Response** (200 OK):
```json
{
  "id": "67234abc123def456789",
  "fullName": "Ramesh Kumar",
  "email": "ramesh.kumar@example.com",
  "mobile": "919876543210",
  "role": "SENIOR",
  "status": "ACTIVE",
  "emailVerified": false,
  "mobileVerified": false,
  "createdAt": "2024-11-01T10:00:00Z",
  "lastLoginAt": "2024-11-01T10:30:00Z"
}
```

**Error Responses**:

*401 Unauthorized - Missing/Invalid Token*:
```json
{
  "timestamp": "2024-11-01T10:30:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Authentication required"
}
```

---

## JWT Token Lifecycle

### Access Token

**Purpose**: Short-lived token for API authentication

**Claims**:
```json
{
  "sub": "67234abc123def456789",
  "email": "ramesh.kumar@example.com",
  "role": "SENIOR",
  "type": "ACCESS",
  "iat": 1698832200,
  "exp": 1698833100
}
```

**Properties**:
- **Expiration**: 15 minutes (900 seconds)
- **Algorithm**: HS256 (HMAC with SHA-256)
- **Signing Key**: Environment variable `JWT_SECRET_KEY`
- **Storage**: Frontend stores in memory or short-lived httpOnly cookie

**Validation**:
- Signature verification
- Expiration check
- Type claim must be "ACCESS"
- User status check (ACTIVE)

### Refresh Token

**Purpose**: Long-lived token for obtaining new access tokens

**Properties**:
- **Format**: Cryptographically secure random string (64 bytes, hex-encoded)
- **Expiration**: 7 days (604800 seconds)
- **Storage**: 
  - Backend: Hashed in MongoDB `refresh_tokens` collection
  - Frontend: httpOnly, secure cookie (recommended) or localStorage
- **Rotation**: New refresh token issued on each refresh request

**Lifecycle**:
1. **Issuance**: Generated on login, stored hashed in database
2. **Usage**: Presented to `/api/auth/refresh` endpoint
3. **Validation**: 
   - Hash and lookup in database
   - Check expiration
   - Check revoked status
   - Verify user status
4. **Rotation**: Old token revoked, new token issued
5. **Revocation**: Marked as revoked on logout or security event

### Token Generation

**Access Token Generation**:
```java
public String generateAccessToken(User user) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + 900_000); // 15 minutes
    
    return Jwts.builder()
        .setSubject(user.getId())
        .claim("email", user.getEmail())
        .claim("role", user.getRole().name())
        .claim("type", "ACCESS")
        .setIssuedAt(now)
        .setExpiration(expiryDate)
        .signWith(getSigningKey(), SignatureAlgorithm.HS256)
        .compact();
}
```

**Refresh Token Generation**:
```java
public String generateRefreshToken() {
    SecureRandom secureRandom = new SecureRandom();
    byte[] randomBytes = new byte[64];
    secureRandom.nextBytes(randomBytes);
    return Hex.encodeHexString(randomBytes);
}

public String hashToken(String token) {
    return BCrypt.hashpw(token, BCrypt.gensalt(12));
}
```

### Token Validation

**JWT Filter** (JwtAuthenticationFilter):
```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) {
        String token = extractTokenFromRequest(request);
        
        if (token != null && jwtTokenProvider.validateAccessToken(token)) {
            String userId = jwtTokenProvider.getUserIdFromToken(token);
            UserDetails userDetails = userDetailsService.loadUserById(userId);
            
            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()
                );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        
        filterChain.doFilter(request, response);
    }
}
```

### Security Considerations

**Token Storage**:
- **Access Token**: Memory (recommended) or sessionStorage
- **Refresh Token**: httpOnly, secure, sameSite=strict cookie (recommended)

**Token Rotation**:
- Refresh token rotation prevents replay attacks
- Old refresh token immediately revoked when new one issued

**Token Revocation**:
- Refresh tokens stored in database, can be revoked
- Access tokens cannot be revoked (short expiry mitigates risk)

**Replay Detection**:
- Used refresh tokens immediately revoked
- Attempt to reuse revoked token triggers security alert

---

## Role-Based Authorization Design

### Role Hierarchy

```
ADMIN
  ├── Full system access
  ├── User management
  ├── System configuration
  └── Access to all resources

CAREGIVER
  ├── Create emergency sessions on behalf of senior
  ├── View sessions they created
  ├── Update patient profiles
  └── Receive notifications

SENIOR
  ├── Create own emergency sessions
  ├── View own sessions
  ├── Update own profile
  └── Manage own account
```

### Authorization Rules

**Emergency Sessions**:
- **Create**: SENIOR (own), CAREGIVER (on behalf)
- **Read**: 
  - Own sessions: SENIOR, CAREGIVER (created by them)
  - All sessions: ADMIN, Emergency Response (future role)
- **Update Status**: Emergency Response (future), Transport Partner (future), Hospital (future)
- **Delete**: ADMIN only

**User Management**:
- **Register**: Public endpoint (SENIOR, CAREGIVER only)
- **View Own Profile**: Authenticated users
- **Update Own Profile**: Authenticated users
- **View All Users**: ADMIN only
- **Suspend/Delete Users**: ADMIN only

### Method-Level Authorization

**Controller Method Annotations**:
```java
@PreAuthorize("hasRole('SENIOR')")
public EmergencySessionResponse createOwnSession(CreateEmergencySessionRequest request) {
    // Only SENIOR role can create
}

@PreAuthorize("hasAnyRole('SENIOR', 'CAREGIVER')")
public List<EmergencySessionResponse> getOwnSessions() {
    // SENIOR or CAREGIVER can view
}

@PreAuthorize("hasRole('ADMIN')")
public List<User> getAllUsers() {
    // Only ADMIN
}
```

### Resource Ownership Verification

**Service Layer Checks**:
```java
public EmergencySession getSessionById(String sessionId, String authenticatedUserId) {
    EmergencySession session = repository.findById(sessionId)
        .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
    
    // Verify ownership
    if (!session.getCreatedByUserId().equals(authenticatedUserId)) {
        throw new ForbiddenException("Access denied to this session");
    }
    
    return session;
}
```

### Future Authorization Requirements

**WebSocket Authentication** (Future Phase):
- Real-time session updates require authenticated WebSocket connections
- JWT token passed in WebSocket handshake header

**Emergency Response Role** (Future Phase):
- New role for emergency operators
- Can acknowledge and assign transport to any session
- Can view all active sessions

**Transport Partner Role** (Future Phase):
- Can accept transport assignments
- Can update session status (EN_ROUTE, ARRIVED, etc.)
- Can view assigned sessions only

**Hospital Operations Role** (Future Phase):
- Can view incoming patient sessions
- Can acknowledge patient handover
- Can update hospital-related status

---

## Security Architecture

### Password Security

**Hashing Algorithm**: BCrypt with strength 12

**Password Policy**:
- Minimum 8 characters
- Maximum 100 characters
- Must contain at least one uppercase letter
- Must contain at least one lowercase letter
- Must contain at least one digit
- Must contain at least one special character

**Validation Regex**:
```java
@Pattern(
    regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,100}$",
    message = "Password must be 8-100 characters with uppercase, lowercase, digit, and special character"
)
```

**Password Hashing**:
```java
public String hashPassword(String plainPassword) {
    return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
}

public boolean verifyPassword(String plainPassword, String hashedPassword) {
    return BCrypt.checkpw(plainPassword, hashedPassword);
}
```

### Input Validation

**Email Validation**:
```java
@Email(message = "Invalid email format")
@NotBlank(message = "Email is required")
@Size(max = 255)
private String email;
```

**Mobile Validation**:
```java
@NotBlank(message = "Mobile number is required")
@Pattern(
    regexp = "^\\+?[0-9\\-\\s()]{10,20}$",
    message = "Invalid mobile number format"
)
private String mobile;
```

### Rate Limiting

**Strategy**: In-memory rate limiting per IP address (Phase 1)

**Login Endpoint**:
- 5 attempts per IP per 15 minutes
- After 5 failed attempts: 15-minute lockout
- 429 Too Many Requests response

**Registration Endpoint**:
- 3 registrations per IP per hour
- Prevents automated account creation

**Implementation** (Future Task):
```java
@Component
public class RateLimitingInterceptor implements HandlerInterceptor {
    
    private final LoadingCache<String, AtomicInteger> attemptsCache;
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                             HttpServletResponse response, 
                             Object handler) {
        String clientIp = getClientIp(request);
        String key = clientIp + ":" + request.getRequestURI();
        
        AtomicInteger attempts = attemptsCache.get(key);
        if (attempts.incrementAndGet() > getLimit(request.getRequestURI())) {
            response.setStatus(429);
            return false;
        }
        
        return true;
    }
}
```

### CORS Configuration

**Current Configuration** (SecurityConfig):
```java
corsConfigurer.addMapping("/api/**")
    .allowedOrigins("http://localhost:5173", "http://localhost:5174", "http://localhost:3000")
    .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
    .allowedHeaders("*")
    .allowCredentials(true);
```

**Production Configuration** (Future):
- Replace localhost origins with actual domain
- Enable `allowCredentials` for httpOnly cookies
- Specify exact allowed headers

### Security Headers

**Required Headers**:
```java
http.headers()
    .contentSecurityPolicy("default-src 'self'")
    .xssProtection()
    .contentTypeOptions()
    .frameOptions().deny()
    .httpStrictTransportSecurity();
```

### Logging and Monitoring

**Security Events to Log**:
- Successful login (userId, IP, timestamp)
- Failed login attempt (identifier, IP, timestamp)
- Account registration (userId, IP, timestamp)
- Token refresh (userId, IP, timestamp)
- Logout (userId, timestamp)
- Rate limit exceeded (IP, endpoint, timestamp)
- Suspicious activity (IP, pattern, timestamp)

**Log Format**:
```
[SECURITY] 2024-11-01T10:30:00Z | LOGIN_SUCCESS | userId=67234abc | ip=192.168.1.100
[SECURITY] 2024-11-01T10:30:15Z | LOGIN_FAILED | identifier=unknown@example.com | ip=192.168.1.100
```

**Never Log**:
- Plain-text passwords
- JWT tokens
- Refresh tokens
- Password hashes

### Threat Mitigation

**SQL Injection**: N/A (MongoDB, parameterized queries)

**NoSQL Injection**: 
- Input validation with Jakarta Bean Validation
- MongoDB driver parameterized queries

**XSS (Cross-Site Scripting)**:
- Frontend: React automatic escaping
- Backend: Content-Type validation

**CSRF (Cross-Site Request Forgery)**:
- Stateless JWT authentication (no session cookies)
- If using refresh token cookies: SameSite=Strict attribute

**Brute Force**:
- Rate limiting on login endpoint
- Account lockout after repeated failures (future)

**Token Replay**:
- Refresh token rotation
- Revoked token tracking

**Man-in-the-Middle**:
- HTTPS required in production
- Secure cookie flag for refresh tokens

---

## Frontend Integration Requirements

### Authentication State Management

**React Context** (AuthContext):
```typescript
interface AuthContextType {
  user: User | null;
  accessToken: string | null;
  isAuthenticated: boolean;
  login: (identifier: string, password: string) => Promise<void>;
  register: (data: RegisterRequest) => Promise<void>;
  logout: () => Promise<void>;
  refreshAccessToken: () => Promise<void>;
}
```

### API Client Configuration

**Axios Interceptor**:
```typescript
axios.interceptors.request.use(
  (config) => {
    const token = getAccessToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

axios.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401 && !error.config._retry) {
      error.config._retry = true;
      
      try {
        await refreshAccessToken();
        const newToken = getAccessToken();
        error.config.headers.Authorization = `Bearer ${newToken}`;
        return axios.request(error.config);
      } catch (refreshError) {
        logout();
        return Promise.reject(refreshError);
      }
    }
    return Promise.reject(error);
  }
);
```

### Protected Routes

**React Router Protected Route**:
```typescript
function ProtectedRoute({ children, allowedRoles }: ProtectedRouteProps) {
  const { isAuthenticated, user } = useAuth();
  
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  
  if (allowedRoles && !allowedRoles.includes(user.role)) {
    return <Navigate to="/unauthorized" replace />;
  }
  
  return <>{children}</>;
}
```

**Route Configuration**:
```typescript
<Route path="/senior" element={
  <ProtectedRoute allowedRoles={['SENIOR']}>
    <SeniorCitizenHome />
  </ProtectedRoute>
} />
```

### Login Screen

**New Component**: `src/pages/Login.tsx`

**Features**:
- Email or mobile input field
- Password input (masked)
- "Forgot Password" link (placeholder)
- "Register" link
- Error message display
- Loading state during authentication
- Multi-language support (en, hi, mr)

### Registration Screen

**New Component**: `src/pages/Register.tsx`

**Features**:
- Full name input
- Email input with validation
- Mobile number input with validation
- Password input with strength indicator
- Confirm password input
- Role selection (SENIOR or CAREGIVER)
- Terms and conditions checkbox
- Error message display
- Success message with redirect to login

### Token Storage Strategy

**Recommended Approach**:
- **Access Token**: Memory (React state) or sessionStorage
- **Refresh Token**: httpOnly, secure cookie set by backend

**Alternative Approach** (If backend doesn't set cookies):
- **Access Token**: sessionStorage
- **Refresh Token**: localStorage (less secure but functional)

**Token Refresh Timing**:
- Proactive refresh: 1 minute before access token expiry
- Reactive refresh: On 401 response (current implementation)

---

## Testing Strategy

### Unit Tests

**PasswordService Tests**:
```java
@Test
void testHashPassword_ValidPassword_ReturnsHash() {
    String password = "SecurePass123!";
    String hash = passwordService.hashPassword(password);
    assertNotNull(hash);
    assertTrue(BCrypt.checkpw(password, hash));
}

@Test
void testVerifyPassword_CorrectPassword_ReturnsTrue() {
    String password = "SecurePass123!";
    String hash = passwordService.hashPassword(password);
    assertTrue(passwordService.verifyPassword(password, hash));
}
```

**JwtTokenProvider Tests**:
```java
@Test
void testGenerateAccessToken_ValidUser_ReturnsToken() {
    User user = createTestUser();
    String token = jwtTokenProvider.generateAccessToken(user);
    assertNotNull(token);
    assertTrue(jwtTokenProvider.validateAccessToken(token));
}

@Test
void testGetUserIdFromToken_ValidToken_ReturnsUserId() {
    User user = createTestUser();
    String token = jwtTokenProvider.generateAccessToken(user);
    String userId = jwtTokenProvider.getUserIdFromToken(token);
    assertEquals(user.getId(), userId);
}
```

### Integration Tests

**AuthController Registration Tests**:
```java
@Test
void testRegister_ValidRequest_ReturnsCreated() throws Exception {
    RegisterRequest request = RegisterRequest.builder()
        .fullName("Test User")
        .email("test@example.com")
        .mobile("+91-9876543210")
        .password("SecurePass123!")
        .role("SENIOR")
        .build();

    mockMvc.perform(post("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.email").value("test@example.com"))
        .andExpect(jsonPath("$.role").value("SENIOR"));
}

@Test
void testRegister_DuplicateEmail_ReturnsConflict() throws Exception {
    // First registration
    registerUser("test@example.com");
    
    // Duplicate registration
    RegisterRequest request = createRegisterRequest("test@example.com");
    
    mockMvc.perform(post("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict());
}
```

**AuthController Login Tests**:
```java
@Test
void testLogin_ValidCredentials_ReturnsTokens() throws Exception {
    registerUser("test@example.com", "SecurePass123!");
    
    LoginRequest request = LoginRequest.builder()
        .identifier("test@example.com")
        .password("SecurePass123!")
        .build();

    mockMvc.perform(post("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").exists())
        .andExpect(jsonPath("$.refreshToken").exists())
        .andExpect(jsonPath("$.user.email").value("test@example.com"));
}

@Test
void testLogin_InvalidPassword_ReturnsUnauthorized() throws Exception {
    registerUser("test@example.com", "SecurePass123!");
    
    LoginRequest request = LoginRequest.builder()
        .identifier("test@example.com")
        .password("WrongPassword!")
        .build();

    mockMvc.perform(post("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());
}
```

**Protected Endpoint Tests**:
```java
@Test
void testGetCurrentUser_WithValidToken_ReturnsUser() throws Exception {
    String token = loginAndGetToken("test@example.com", "SecurePass123!");
    
    mockMvc.perform(get("/api/auth/me")
        .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("test@example.com"));
}

@Test
void testGetCurrentUser_WithoutToken_ReturnsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/auth/me"))
        .andExpect(status().isUnauthorized());
}
```

### Security Tests

**Role-Based Access Tests**:
```java
@Test
void testCreateSession_AsCaregiver_Forbidden() throws Exception {
    String caregiverToken = loginAndGetToken("caregiver@example.com", "Pass123!");
    
    CreateEmergencySessionRequest request = createSessionRequest();
    
    mockMvc.perform(post("/api/emergency-sessions")
        .header("Authorization", "Bearer " + caregiverToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
}
```

**Token Expiry Tests**:
```java
@Test
void testAccessToken_AfterExpiry_ReturnsUnauthorized() throws Exception {
    // Generate token with 1-second expiry
    String token = generateExpiredToken();
    Thread.sleep(1500);
    
    mockMvc.perform(get("/api/auth/me")
        .header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
}
```

### Test Data Cleanup

**Existing Pattern** (from EmergencySessionControllerTest):
```java
@AfterEach
void cleanUp() {
    userRepository.deleteAll();
    refreshTokenRepository.deleteAll();
}
```

### Test Coverage Goals

- **Unit Tests**: 80%+ coverage for service layer
- **Integration Tests**: 90%+ coverage for controller endpoints
- **Security Tests**: 100% coverage for authorization rules

---

## Implementation Tasks

### Phase 1: Core Authentication Infrastructure

#### Task 1.2.1: User Document and Repository
**Estimated Effort**: 1-2 hours

**Files to Create**:
- `backend/src/main/java/com/aayush/backend/document/User.java`
- `backend/src/main/java/com/aayush/backend/document/UserRole.java`
- `backend/src/main/java/com/aayush/backend/document/AccountStatus.java`
- `backend/src/main/java/com/aayush/backend/repository/UserRepository.java`

**Acceptance Criteria**:
- [ ] User document matches schema defined in this plan
- [ ] UserRepository extends MongoRepository
- [ ] Custom query methods: `findByEmail`, `findByMobile`, `existsByEmail`, `existsByMobile`
- [ ] MongoDB indexes created (unique on email and mobile)
- [ ] Compiles without errors

---

#### Task 1.2.2: Refresh Token Document and Repository
**Estimated Effort**: 1 hour

**Files to Create**:
- `backend/src/main/java/com/aayush/backend/document/RefreshToken.java`
- `backend/src/main/java/com/aayush/backend/repository/RefreshTokenRepository.java`

**Acceptance Criteria**:
- [ ] RefreshToken document matches schema
- [ ] TTL index configured on expiresAt field
- [ ] Custom query methods: `findByToken`, `findByUserId`, `deleteByUserId`
- [ ] Compiles without errors

---

#### Task 1.2.3: Authentication DTOs
**Estimated Effort**: 1-2 hours

**Files to Create**:
- `backend/src/main/java/com/aayush/backend/dto/RegisterRequest.java`
- `backend/src/main/java/com/aayush/backend/dto/LoginRequest.java`
- `backend/src/main/java/com/aayush/backend/dto/LoginResponse.java`
- `backend/src/main/java/com/aayush/backend/dto/RefreshTokenRequest.java`
- `backend/src/main/java/com/aayush/backend/dto/RefreshTokenResponse.java`
- `backend/src/main/java/com/aayush/backend/dto/UserResponse.java`

**Acceptance Criteria**:
- [ ] All DTOs follow existing project patterns (Lombok @Data, @Builder)
- [ ] Validation annotations match requirements in this plan
- [ ] Password regex validation on RegisterRequest
- [ ] Role validation ensures only SENIOR/CAREGIVER allowed
- [ ] Compiles without errors

---

#### Task 1.2.4: Custom Exceptions
**Estimated Effort**: 30 minutes

**Files to Create**:
- `backend/src/main/java/com/aayush/backend/exception/DuplicateUserException.java`
- `backend/src/main/java/com/aayush/backend/exception/InvalidCredentialsException.java`
- `backend/src/main/java/com/aayush/backend/exception/InvalidTokenException.java`
- `backend/src/main/java/com/aayush/backend/exception/ForbiddenException.java`

**Files to Modify**:
- `backend/src/main/java/com/aayush/backend/exception/GlobalExceptionHandler.java`

**Acceptance Criteria**:
- [ ] Custom exceptions follow existing pattern (extend RuntimeException)
- [ ] GlobalExceptionHandler updated with handlers:
  - `DuplicateUserException` → 409 Conflict
  - `InvalidCredentialsException` → 401 Unauthorized
  - `InvalidTokenException` → 401 Unauthorized
  - `ForbiddenException` → 403 Forbidden
- [ ] Compiles without errors

---

### Phase 2: Security Services

#### Task 1.2.5: Password Service
**Estimated Effort**: 1 hour

**Files to Create**:
- `backend/src/main/java/com/aayush/backend/service/PasswordService.java`

**Acceptance Criteria**:
- [ ] `hashPassword(String plainPassword)` method using BCrypt strength 12
- [ ] `verifyPassword(String plainPassword, String hashedPassword)` method
- [ ] Unit tests with 100% coverage
- [ ] Compiles without errors

---

#### Task 1.2.6: JWT Token Provider
**Estimated Effort**: 2-3 hours

**Files to Create**:
- `backend/src/main/java/com/aayush/backend/security/JwtTokenProvider.java`

**Acceptance Criteria**:
- [ ] `generateAccessToken(User user)` - 15-minute expiry, HS256
- [ ] `validateAccessToken(String token)` - signature, expiry, type
- [ ] `getUserIdFromToken(String token)` - extract subject claim
- [ ] `getRoleFromToken(String token)` - extract role claim
- [ ] Uses `JWT_SECRET_KEY` from environment/application.yml
- [ ] Unit tests with 90%+ coverage
- [ ] Compiles without errors

---

#### Task 1.2.7: Refresh Token Service
**Estimated Effort**: 2 hours

**Files to Create**:
- `backend/src/main/java/com/aayush/backend/service/RefreshTokenService.java`

**Acceptance Criteria**:
- [ ] `generateRefreshToken(String userId)` - 7-day expiry, stored hashed
- [ ] `validateRefreshToken(String token)` - verify hash, expiry, revoked
- [ ] `rotateRefreshToken(String oldToken)` - revoke old, issue new
- [ ] `revokeRefreshToken(String token)` - mark as revoked
- [ ] `revokeAllUserTokens(String userId)` - revoke all user's tokens
- [ ] Unit tests with 80%+ coverage
- [ ] Compiles without errors

---

### Phase 3: Authentication Logic

#### Task 1.2.8: User Service
**Estimated Effort**: 3-4 hours

**Files to Create**:
- `backend/src/main/java/com/aayush/backend/service/UserService.java`

**Methods to Implement**:
- `registerUser(RegisterRequest request)` - validate, normalize, hash password, save
- `getUserByEmail(String email)` - normalize and query
- `getUserByMobile(String mobile)` - normalize and query
- `getUserById(String id)` - simple lookup
- `updateLastLogin(String userId)` - update timestamp
- `normalizeEmail(String email)` - lowercase, trim
- `normalizeMobile(String mobile)` - digits only, add country code

**Acceptance Criteria**:
- [ ] Email and mobile normalization working correctly
- [ ] Duplicate detection via existsByEmail/existsByMobile
- [ ] Password hashing via PasswordService
- [ ] Throws DuplicateUserException if email/mobile exists
- [ ] Role must be SENIOR or CAREGIVER (reject ADMIN)
- [ ] Unit tests with 80%+ coverage
- [ ] Compiles without errors

---

#### Task 1.2.9: Authentication Service
**Estimated Effort**: 3-4 hours

**Files to Create**:
- `backend/src/main/java/com/aayush/backend/service/AuthenticationService.java`

**Methods to Implement**:
- `login(LoginRequest request)` - verify credentials, generate tokens
- `refreshAccessToken(String refreshToken)` - validate, rotate, issue new access token
- `logout(String userId, String refreshToken)` - revoke refresh token
- `getCurrentUser(String userId)` - get authenticated user details

**Acceptance Criteria**:
- [ ] Login accepts email or mobile as identifier
- [ ] Password verification via PasswordService
- [ ] Invalid credentials throw InvalidCredentialsException
- [ ] Suspended accounts throw ForbiddenException
- [ ] Tokens generated via JwtTokenProvider and RefreshTokenService
- [ ] Last login timestamp updated on successful login
- [ ] Unit tests with 80%+ coverage
- [ ] Compiles without errors

---

### Phase 4: Spring Security Configuration

#### Task 1.2.10: UserDetailsService Implementation
**Estimated Effort**: 1-2 hours

**Files to Create**:
- `backend/src/main/java/com/aayush/backend/security/CustomUserDetailsService.java`

**Acceptance Criteria**:
- [ ] Implements `UserDetailsService`
- [ ] `loadUserByUsername(String userId)` method loads User from MongoDB
- [ ] Returns Spring Security `UserDetails` with authorities from role
- [ ] Throws `UsernameNotFoundException` if user not found
- [ ] Unit tests with 90%+ coverage
- [ ] Compiles without errors

---

#### Task 1.2.11: JWT Authentication Filter
**Estimated Effort**: 2-3 hours

**Files to Create**:
- `backend/src/main/java/com/aayush/backend/security/JwtAuthenticationFilter.java`

**Acceptance Criteria**:
- [ ] Extends `OncePerRequestFilter`
- [ ] Extracts JWT from `Authorization: Bearer <token>` header
- [ ] Validates token via JwtTokenProvider
- [ ] Loads UserDetails and sets Spring Security authentication
- [ ] Skips authentication if no token present (allows public endpoints)
- [ ] Handles exceptions gracefully (log and continue filter chain)
- [ ] Unit tests with 70%+ coverage
- [ ] Compiles without errors

---

#### Task 1.2.12: Authentication Entry Point
**Estimated Effort**: 30 minutes

**Files to Create**:
- `backend/src/main/java/com/aayush/backend/security/JwtAuthenticationEntryPoint.java`

**Acceptance Criteria**:
- [ ] Implements `AuthenticationEntryPoint`
- [ ] Returns 401 Unauthorized with JSON error response
- [ ] Matches GlobalExceptionHandler error format
- [ ] Compiles without errors

---

#### Task 1.2.13: Security Configuration Update
**Estimated Effort**: 2 hours

**Files to Modify**:
- `backend/src/main/java/com/aayush/backend/config/SecurityConfig.java`

**Changes Required**:
- [ ] Remove `.anyRequest().permitAll()`
- [ ] Configure public endpoints: `/api/auth/register`, `/api/auth/login`, `/api/auth/refresh`, `/api/health`
- [ ] Configure authenticated endpoints: `/api/auth/me`, `/api/auth/logout`, `/api/emergency-sessions/**`
- [ ] Add JwtAuthenticationFilter before UsernamePasswordAuthenticationFilter
- [ ] Set JwtAuthenticationEntryPoint as exception handler
- [ ] Configure AuthenticationManager bean
- [ ] Keep existing CORS configuration
- [ ] Keep stateless session management
- [ ] Integration tests to verify public vs protected endpoints

**Acceptance Criteria**:
- [ ] Public endpoints accessible without token
- [ ] Protected endpoints return 401 without valid token
- [ ] Valid token grants access to protected endpoints
- [ ] Existing CORS and session config preserved
- [ ] Compiles without errors
- [ ] All existing 14 tests still pass

---

### Phase 5: Authentication Controller

#### Task 1.2.14: Authentication Controller
**Estimated Effort**: 3-4 hours

**Files to Create**:
- `backend/src/main/java/com/aayush/backend/controller/AuthController.java`

**Endpoints to Implement**:
- `POST /api/auth/register` - user registration
- `POST /api/auth/login` - user login
- `POST /api/auth/refresh` - refresh access token
- `POST /api/auth/logout` - logout (revoke refresh token)
- `GET /api/auth/me` - get current user

**Acceptance Criteria**:
- [ ] All endpoints follow API contracts in this plan
- [ ] Request validation with `@Valid`
- [ ] Proper HTTP status codes (201, 200, 401, 403, 409, etc.)
- [ ] Error responses match GlobalExceptionHandler format
- [ ] `/api/auth/me` and `/api/auth/logout` require authentication
- [ ] Integration tests for all endpoints (happy path + error cases)
- [ ] Test coverage 90%+
- [ ] Compiles without errors

---

### Phase 6: Emergency Session Authorization

#### Task 1.2.15: Emergency Session Ownership
**Estimated Effort**: 2-3 hours

**Files to Modify**:
- `backend/src/main/java/com/aayush/backend/document/EmergencySession.java`
- `backend/src/main/java/com/aayush/backend/service/EmergencySessionService.java`
- `backend/src/main/java/com/aayush/backend/controller/EmergencySessionController.java`

**Changes Required**:
- [ ] Add `createdByUserId` field to EmergencySession document
- [ ] Add MongoDB index on `createdByUserId`
- [ ] Update `createEmergencySession` to capture authenticated user ID
- [ ] Add ownership verification in `getSessionById`
- [ ] Add authorization check in controller methods
- [ ] Update repository: `findByCreatedByUserId(String userId)`

**Acceptance Criteria**:
- [ ] Emergency sessions linked to creating user
- [ ] Users can only view/update own sessions
- [ ] Attempting to access another user's session returns 403
- [ ] Integration tests verify ownership checks
- [ ] All existing tests updated and passing
- [ ] Compiles without errors

---

#### Task 1.2.16: Role-Based Authorization on Emergency Endpoints
**Estimated Effort**: 1-2 hours

**Files to Modify**:
- `backend/src/main/java/com/aayush/backend/controller/EmergencySessionController.java`

**Changes Required**:
- [ ] Add `@PreAuthorize("hasRole('SENIOR')")` to create session endpoint
- [ ] Add ownership check in get session endpoint
- [ ] Future: Add roles for Emergency Response, Transport, Hospital

**Acceptance Criteria**:
- [ ] Only SENIOR role can create emergency sessions
- [ ] CAREGIVER attempting to create session receives 403
- [ ] Integration tests verify role-based access control
- [ ] All existing tests passing
- [ ] Compiles without errors

---

### Phase 7: Frontend Authentication

#### Task 1.2.17: Authentication Service (Frontend)
**Estimated Effort**: 2-3 hours

**Files to Create**:
- `src/services/authService.ts`
- `src/contexts/AuthContext.tsx`

**Acceptance Criteria**:
- [ ] API calls to backend `/api/auth/*` endpoints
- [ ] AuthContext provides authentication state and methods
- [ ] Token storage (access token in memory, refresh token in localStorage)
- [ ] Axios interceptor for adding Bearer token
- [ ] Axios interceptor for automatic token refresh on 401
- [ ] TypeScript types for requests/responses

---

#### Task 1.2.18: Login and Registration Screens
**Estimated Effort**: 4-5 hours

**Files to Create**:
- `src/pages/Login.tsx`
- `src/pages/Register.tsx`

**Files to Modify**:
- `src/App.tsx` (add routes)

**Acceptance Criteria**:
- [ ] Login screen with email/mobile and password fields
- [ ] Registration screen with all required fields
- [ ] Client-side validation (email format, password strength)
- [ ] Error message display from backend responses
- [ ] Loading states during API calls
- [ ] Multi-language support (en, hi, mr)
- [ ] Password strength indicator on registration
- [ ] Role selection (SENIOR or CAREGIVER)
- [ ] Redirect to dashboard after successful login

---

#### Task 1.2.19: Protected Routes
**Estimated Effort**: 1-2 hours

**Files to Create**:
- `src/components/ProtectedRoute.tsx`

**Files to Modify**:
- `src/App.tsx` (wrap existing routes)

**Acceptance Criteria**:
- [ ] Unauthenticated users redirected to login
- [ ] Role-based route protection (e.g., `/senior` only for SENIOR role)
- [ ] Unauthorized access redirected to error page
- [ ] Persists authentication across page refresh

---

### Phase 8: Testing and Verification

#### Task 1.2.20: Integration Test Suite
**Estimated Effort**: 3-4 hours

**Files to Create**:
- `backend/src/test/java/com/aayush/backend/controller/AuthControllerTest.java`
- `backend/src/test/java/com/aayush/backend/security/JwtAuthenticationFilterTest.java`

**Acceptance Criteria**:
- [ ] AuthController: 15+ test cases covering all endpoints
- [ ] Security: 10+ test cases covering authentication and authorization
- [ ] Test data cleanup with @AfterEach
- [ ] All tests use test database (aayush_test)
- [ ] All tests passing (new + existing 14 tests)
- [ ] Test coverage report: 85%+ overall

---

#### Task 1.2.21: End-to-End Manual Testing
**Estimated Effort**: 2-3 hours

**Test Scenarios**:
1. Register new SENIOR user → success
2. Register duplicate email → 409 error
3. Login with valid credentials → tokens received
4. Login with invalid credentials → 401 error
5. Access `/api/auth/me` with valid token → user data
6. Access `/api/auth/me` without token → 401 error
7. Create emergency session with SENIOR token → success
8. Create emergency session with CAREGIVER token → 403 error
9. Get own emergency session → success
10. Get another user's session → 403 error
11. Refresh access token → new tokens received
12. Logout → refresh token revoked
13. Use revoked refresh token → 401 error
14. Frontend: Register → Login → Create Session → Logout flow

**Acceptance Criteria**:
- [ ] All scenarios pass manually
- [ ] Frontend and backend integrated successfully
- [ ] No console errors
- [ ] Clear error messages displayed to users

---

#### Task 1.2.22: Documentation and Git Commit
**Estimated Effort**: 1 hour

**Files to Create**:
- `docs/AUTHENTICATION_IMPLEMENTATION_COMPLETE.md`

**Acceptance Criteria**:
- [ ] Document summarizes implementation
- [ ] Lists all files created/modified
- [ ] Reports final test results
- [ ] Git commit: `feat: implement authentication and authorization`
- [ ] Git push to origin/main
- [ ] No credentials, .env, or build artifacts committed

---

## Environment Configuration

### Backend Configuration

**application.yml** (Update Required):
```yaml
jwt:
  secret-key: ${JWT_SECRET_KEY:demo-secret-key-change-in-production}
  access-token-expiry-ms: 900000  # 15 minutes
  refresh-token-expiry-ms: 604800000  # 7 days

security:
  allowed-origins:
    - http://localhost:5173
    - http://localhost:5174
    - http://localhost:3000
```

**Environment Variables** (Required):
- `JWT_SECRET_KEY` - **CRITICAL**: Must be 256+ bit cryptographically secure random string
- `MONGODB_URI` - MongoDB connection string (default: `mongodb://localhost:27017/aayush_db`)

**Environment Variables** (Optional):
- `JWT_ACCESS_TOKEN_EXPIRY_MS` - Override default 15-minute expiry
- `JWT_REFRESH_TOKEN_EXPIRY_MS` - Override default 7-day expiry
- `RATE_LIMIT_ENABLED` - Enable/disable rate limiting (default: true)

### Generating JWT Secret

**Command** (Linux/Mac):
```bash
openssl rand -hex 64
```

**Command** (Windows PowerShell):
```powershell
[Convert]::ToBase64String((1..64 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 }))
```

**Development** (.env.example):
```
JWT_SECRET_KEY=demo-secret-do-not-use-in-production
MONGODB_URI=mongodb://localhost:27017/aayush_db
```

**Production**:
- Never commit JWT_SECRET_KEY to Git
- Use environment-specific secrets management (AWS Secrets Manager, Azure Key Vault, etc.)
- Rotate JWT_SECRET_KEY periodically (invalidates all tokens)

### Frontend Configuration

**Environment Variables** (`.env`):
```
VITE_API_BASE_URL=http://localhost:8080
VITE_ENABLE_LOGGING=true
```

**API Base URL**:
- Development: `http://localhost:8080`
- Production: `https://api.aayush.example.com`

---

## Unresolved Decisions

### 1. Email and Mobile Verification

**Status**: Deferred to later phase

**Options**:
- **Option A**: Email via SMTP (requires email provider configuration)
- **Option B**: Mobile via SMS gateway (Twilio, AWS SNS)
- **Option C**: Development adapter (mock verification for demo)

**Recommendation**: Implement Option C (mock adapter) for Phase 1, plan real verification for production.

**Implementation**:
- Add `/api/auth/verify-email` endpoint (mock sends console log)
- Add `/api/auth/verify-mobile` endpoint (mock sends console log)
- Frontend shows "Verification required" but allows demo usage

---

### 2. Forgot Password / Password Reset

**Status**: Deferred to later phase

**Requirements**:
- Password reset flow requires email/SMS verification
- Must prevent account enumeration attacks
- Token-based reset with expiry

**Recommendation**: Implement in Phase 2 after email/SMS verification.

---

### 3. Account Lockout After Failed Login Attempts

**Status**: Planned but not implemented in Phase 1

**Requirements**:
- Track failed login attempts per user account
- Lock account after N failed attempts (e.g., 5)
- Automatic unlock after time period (e.g., 30 minutes)
- Manual unlock by admin

**Recommendation**: Implement in Phase 2 with admin user management.

---

### 4. Refresh Token Cookie vs. localStorage

**Status**: Open decision

**Option A**: httpOnly Cookie (More Secure)
- Backend sets cookie with refresh token
- Frontend cannot access token directly
- Requires SameSite=Strict for CSRF protection
- Requires CORS allowCredentials=true

**Option B**: localStorage (Simpler, Less Secure)
- Frontend manages token storage
- Vulnerable to XSS attacks
- Simpler implementation
- Works with current CORS setup

**Recommendation**: Start with Option B for Phase 1, migrate to Option A for production.

---

### 5. Multi-Factor Authentication (MFA)

**Status**: Deferred to future phase

**Options**:
- TOTP (Time-based One-Time Password) via authenticator apps
- SMS-based OTP
- Email-based OTP

**Recommendation**: Plan for Phase 3 or later, optional for users.

---

### 6. Social Login (Google, Facebook)

**Status**: Out of scope for MVP

**Recommendation**: Consider for Phase 3+ based on user feedback.

---

### 7. Session Management and Concurrent Logins

**Status**: Open decision

**Current Behavior**: Multiple concurrent logins allowed (multiple refresh tokens)

**Options**:
- **Option A**: Allow unlimited concurrent logins
- **Option B**: Limit to N devices (revoke oldest tokens)
- **Option C**: Single session only (revoke all previous tokens on login)

**Recommendation**: Start with Option A, add device management UI later.

---

### 8. User Profile Management

**Status**: Minimal implementation planned

**Current Scope**:
- `/api/auth/me` returns user profile (read-only)
- No update profile endpoint

**Future Scope**:
- `PATCH /api/users/{userId}` - Update name, mobile, email
- Email/mobile change requires re-verification
- Password change requires current password

**Recommendation**: Implement in Phase 2.

---

### 9. Admin User Provisioning

**Status**: Manual database insertion for Phase 1

**Current Approach**:
- Manually create ADMIN user via MongoDB shell
- ADMIN role cannot be selected during registration

**Future Approach**:
- Admin user management UI
- Create/suspend/delete users
- Assign roles
- View audit logs

**Recommendation**: Manual provisioning for Phase 1, admin UI in Phase 3.

---

### 10. Audit Logging

**Status**: Basic logging planned

**Current Scope**:
- Log authentication events to console
- No persistent audit log

**Future Scope**:
- Persistent audit log in MongoDB
- Track all user actions (create, read, update, delete)
- Admin UI to view audit logs
- Export audit logs for compliance

**Recommendation**: Console logging for Phase 1, persistent logs in Phase 3.

---

## Summary

This authentication implementation plan provides a complete roadmap for adding secure authentication and authorization to the AAYUSH platform. The plan:

- **Preserves existing functionality**: All 14 existing tests remain passing
- **Follows existing patterns**: DTOs, repositories, services match current codebase style
- **Implements security best practices**: BCrypt, JWT, token rotation, rate limiting
- **Provides clear task breakdown**: 22 implementation tasks with acceptance criteria
- **Defines API contracts**: Complete request/response specifications
- **Plans for future**: Identifies deferred features and extension points

**Next Steps**:
1. Review this plan for accuracy and completeness
2. Clarify any unresolved decisions
3. Begin implementation with Task 1.2.1
4. Execute tasks sequentially, verifying tests pass after each task
5. Commit and push after each major phase

---

**Document Status**: ✅ Ready for Review  
**Estimated Total Effort**: 40-55 hours  
**Target Completion**: Phase 1 implementation complete within 1-2 weeks  
**Risk Level**: Low (leverages existing infrastructure, proven technologies)

