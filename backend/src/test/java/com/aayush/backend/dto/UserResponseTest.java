package com.aayush.backend.dto;

import com.aayush.backend.document.AccountStatus;
import com.aayush.backend.document.User;
import com.aayush.backend.document.UserRole;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for UserResponse
 * 
 * Critical security test: verify passwordHash is never exposed
 */
class UserResponseTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testFromUser_MapsAllFields() {
        // Given
        User user = User.builder()
                .id("user123")
                .fullName("Test User")
                .email("test@example.com")
                .mobile("+91-9876543210")
                .passwordHash("$2a$10$hashedpassword")  // Should NOT be exposed
                .role(UserRole.SENIOR)
                .status(AccountStatus.ACTIVE)
                .emailVerified(true)
                .mobileVerified(false)
                .preferredLanguage("en")
                .createdAt(LocalDateTime.of(2024, 1, 15, 10, 30))
                .build();

        // When
        UserResponse response = UserResponse.fromUser(user);

        // Then
        assertEquals("user123", response.getId());
        assertEquals("Test User", response.getFullName());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("+91-9876543210", response.getMobile());
        assertEquals(UserRole.SENIOR, response.getRole());
        assertEquals(AccountStatus.ACTIVE, response.getStatus());
        assertTrue(response.isEmailVerified());
        assertFalse(response.isMobileVerified());
        assertEquals("en", response.getPreferredLanguage());
        assertEquals(LocalDateTime.of(2024, 1, 15, 10, 30), response.getCreatedAt());
    }

    @Test
    void testFromUser_WithNullOptionalFields() {
        // Given
        User user = User.builder()
                .id("user456")
                .fullName("Minimal User")
                .email("minimal@example.com")
                .passwordHash("$2a$10$hashedpassword")
                .role(UserRole.CAREGIVER)
                .status(AccountStatus.ACTIVE)
                .emailVerified(false)
                .mobileVerified(false)
                .createdAt(LocalDateTime.now())
                .build();

        // When
        UserResponse response = UserResponse.fromUser(user);

        // Then
        assertEquals("user456", response.getId());
        assertEquals("Minimal User", response.getFullName());
        assertEquals("minimal@example.com", response.getEmail());
        assertNull(response.getMobile());
        assertNull(response.getPreferredLanguage());
    }

    @Test
    void testPasswordHashNotExposed_InSerialization() throws JsonProcessingException {
        // Given
        User user = User.builder()
                .id("user789")
                .fullName("Security Test")
                .email("security@example.com")
                .passwordHash("$2a$10$SENSITIVE_PASSWORD_HASH")
                .role(UserRole.SENIOR)
                .status(AccountStatus.ACTIVE)
                .emailVerified(true)
                .mobileVerified(true)
                .preferredLanguage("hi")
                .createdAt(LocalDateTime.now())
                .build();

        UserResponse response = UserResponse.fromUser(user);

        // When
        String json = objectMapper.writeValueAsString(response);

        // Then - passwordHash must NOT appear in JSON
        assertFalse(json.contains("passwordHash"));
        assertFalse(json.contains("SENSITIVE_PASSWORD_HASH"));
        assertFalse(json.contains("$2a$10"));
        
        // Verify expected fields ARE present
        assertTrue(json.contains("user789"));
        assertTrue(json.contains("Security Test"));
        assertTrue(json.contains("security@example.com"));
    }

    @Test
    void testPasswordHashNotExposed_InToString() {
        // Given
        User user = User.builder()
                .id("user999")
                .fullName("ToString Test")
                .email("tostring@example.com")
                .passwordHash("$2a$10$ANOTHER_SENSITIVE_HASH")
                .role(UserRole.CAREGIVER)
                .status(AccountStatus.ACTIVE)
                .emailVerified(false)
                .mobileVerified(false)
                .createdAt(LocalDateTime.now())
                .build();

        UserResponse response = UserResponse.fromUser(user);

        // When
        String toString = response.toString();

        // Then - passwordHash must NOT appear in toString
        assertFalse(toString.contains("passwordHash"));
        assertFalse(toString.contains("ANOTHER_SENSITIVE_HASH"));
        assertFalse(toString.contains("$2a$10"));
    }

    @Test
    void testBuilder() {
        // Given/When
        UserResponse response = UserResponse.builder()
                .id("custom123")
                .fullName("Builder Test")
                .email("builder@example.com")
                .mobile("+91-1234567890")
                .role(UserRole.SENIOR)
                .status(AccountStatus.SUSPENDED)
                .emailVerified(true)
                .mobileVerified(true)
                .preferredLanguage("mr")
                .createdAt(LocalDateTime.of(2024, 6, 1, 12, 0))
                .build();

        // Then
        assertEquals("custom123", response.getId());
        assertEquals("Builder Test", response.getFullName());
        assertEquals("builder@example.com", response.getEmail());
        assertEquals("+91-1234567890", response.getMobile());
        assertEquals(UserRole.SENIOR, response.getRole());
        assertEquals(AccountStatus.SUSPENDED, response.getStatus());
        assertTrue(response.isEmailVerified());
        assertTrue(response.isMobileVerified());
        assertEquals("mr", response.getPreferredLanguage());
    }

    @Test
    void testEqualsAndHashCode() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        UserResponse response1 = UserResponse.builder()
                .id("same123")
                .fullName("Same User")
                .email("same@example.com")
                .role(UserRole.SENIOR)
                .status(AccountStatus.ACTIVE)
                .emailVerified(true)
                .mobileVerified(false)
                .createdAt(now)
                .build();

        UserResponse response2 = UserResponse.builder()
                .id("same123")
                .fullName("Same User")
                .email("same@example.com")
                .role(UserRole.SENIOR)
                .status(AccountStatus.ACTIVE)
                .emailVerified(true)
                .mobileVerified(false)
                .createdAt(now)
                .build();

        // Then
        assertEquals(response1, response2);
        assertEquals(response1.hashCode(), response2.hashCode());
    }
}
