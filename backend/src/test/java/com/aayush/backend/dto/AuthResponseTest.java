package com.aayush.backend.dto;

import com.aayush.backend.document.AccountStatus;
import com.aayush.backend.document.UserRole;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AuthResponse
 * 
 * Verify correct serialization and field mapping
 */
class AuthResponseTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testAuthResponse_AllFields() {
        // Given
        UserResponse user = UserResponse.builder()
                .id("user123")
                .fullName("Test User")
                .email("test@example.com")
                .role(UserRole.SENIOR)
                .status(AccountStatus.ACTIVE)
                .emailVerified(true)
                .mobileVerified(false)
                .preferredLanguage("en")
                .createdAt(LocalDateTime.now())
                .build();

        // When
        AuthResponse response = AuthResponse.builder()
                .accessToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.access")
                .refreshToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.refresh")
                .tokenType("Bearer")
                .expiresIn(900)
                .user(user)
                .build();

        // Then
        assertEquals("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.access", response.getAccessToken());
        assertEquals("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.refresh", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(900, response.getExpiresIn());
        assertNotNull(response.getUser());
        assertEquals("user123", response.getUser().getId());
    }

    @Test
    void testAuthResponse_Serialization() throws JsonProcessingException {
        // Given
        UserResponse user = UserResponse.builder()
                .id("user456")
                .fullName("Serialize User")
                .email("serialize@example.com")
                .role(UserRole.CAREGIVER)
                .status(AccountStatus.ACTIVE)
                .emailVerified(false)
                .mobileVerified(true)
                .preferredLanguage("hi")
                .createdAt(LocalDateTime.of(2024, 1, 1, 10, 0))
                .build();

        AuthResponse response = AuthResponse.builder()
                .accessToken("access.token.here")
                .refreshToken("refresh.token.here")
                .tokenType("Bearer")
                .expiresIn(3600)
                .user(user)
                .build();

        // When
        String json = objectMapper.writeValueAsString(response);

        // Then
        assertTrue(json.contains("accessToken"));
        assertTrue(json.contains("refreshToken"));
        assertTrue(json.contains("tokenType"));
        assertTrue(json.contains("Bearer"));
        assertTrue(json.contains("expiresIn"));
        assertTrue(json.contains("3600"));
        assertTrue(json.contains("user"));
        assertTrue(json.contains("user456"));
        assertTrue(json.contains("Serialize User"));
        assertTrue(json.contains("serialize@example.com"));
    }

    @Test
    void testAuthResponse_Deserialization() throws JsonProcessingException {
        // Given
        String json = """
                {
                    "accessToken": "access.jwt.token",
                    "refreshToken": "refresh.jwt.token",
                    "tokenType": "Bearer",
                    "expiresIn": 1800,
                    "user": {
                        "id": "user789",
                        "fullName": "Deserialize User",
                        "email": "deserialize@example.com",
                        "mobile": null,
                        "role": "SENIOR",
                        "status": "ACTIVE",
                        "emailVerified": true,
                        "mobileVerified": false,
                        "preferredLanguage": "mr",
                        "createdAt": "2024-02-15T14:30:00"
                    }
                }
                """;

        // When
        AuthResponse response = objectMapper.readValue(json, AuthResponse.class);

        // Then
        assertEquals("access.jwt.token", response.getAccessToken());
        assertEquals("refresh.jwt.token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(1800, response.getExpiresIn());
        assertNotNull(response.getUser());
        assertEquals("user789", response.getUser().getId());
        assertEquals("Deserialize User", response.getUser().getFullName());
        assertEquals("deserialize@example.com", response.getUser().getEmail());
        assertEquals(UserRole.SENIOR, response.getUser().getRole());
        assertTrue(response.getUser().isEmailVerified());
    }

    @Test
    void testAuthResponse_NoArgsConstructor() {
        // When
        AuthResponse response = new AuthResponse();
        response.setAccessToken("test.access");
        response.setRefreshToken("test.refresh");
        response.setTokenType("Bearer");
        response.setExpiresIn(600);

        // Then
        assertEquals("test.access", response.getAccessToken());
        assertEquals("test.refresh", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(600, response.getExpiresIn());
    }

    @Test
    void testAuthResponse_AllArgsConstructor() {
        // Given
        UserResponse user = UserResponse.builder()
                .id("user999")
                .fullName("Constructor User")
                .email("constructor@example.com")
                .role(UserRole.SENIOR)
                .status(AccountStatus.ACTIVE)
                .emailVerified(true)
                .mobileVerified(true)
                .createdAt(LocalDateTime.now())
                .build();

        // When
        AuthResponse response = new AuthResponse(
                "access.token",
                "refresh.token",
                "Bearer",
                1200,
                user
        );

        // Then
        assertEquals("access.token", response.getAccessToken());
        assertEquals("refresh.token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(1200, response.getExpiresIn());
        assertNotNull(response.getUser());
        assertEquals("user999", response.getUser().getId());
    }

    @Test
    void testAuthResponse_EqualsAndHashCode() {
        // Given
        UserResponse user = UserResponse.builder()
                .id("same123")
                .fullName("Same User")
                .email("same@example.com")
                .role(UserRole.SENIOR)
                .status(AccountStatus.ACTIVE)
                .emailVerified(true)
                .mobileVerified(false)
                .createdAt(LocalDateTime.of(2024, 1, 1, 10, 0))
                .build();

        AuthResponse response1 = AuthResponse.builder()
                .accessToken("token1")
                .refreshToken("refresh1")
                .tokenType("Bearer")
                .expiresIn(900)
                .user(user)
                .build();

        AuthResponse response2 = AuthResponse.builder()
                .accessToken("token1")
                .refreshToken("refresh1")
                .tokenType("Bearer")
                .expiresIn(900)
                .user(user)
                .build();

        // Then
        assertEquals(response1, response2);
        assertEquals(response1.hashCode(), response2.hashCode());
    }
}
