package com.aayush.backend.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RegisterRequest validation
 */
class RegisterRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidRegistrationWithEmail() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Test User")
                .email("test@example.com")
                .password("SecurePass123!")
                .role("SENIOR")
                .preferredLanguage("en")
                .build();

        // When
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        // Then
        assertTrue(violations.isEmpty());
        assertTrue(request.hasAtLeastOneContactMethod());
    }

    @Test
    void testValidRegistrationWithMobile() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Test User")
                .mobile("+91-9876543210")
                .password("SecurePass123!")
                .role("SENIOR")
                .preferredLanguage("hi")
                .build();

        // When
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        // Then
        assertTrue(violations.isEmpty());
        assertTrue(request.hasAtLeastOneContactMethod());
    }

    @Test
    void testValidRegistrationWithBothContacts() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Test User")
                .email("test@example.com")
                .mobile("+91-9876543210")
                .password("SecurePass123!")
                .role("CAREGIVER")
                .preferredLanguage("mr")
                .build();

        // When
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        // Then
        assertTrue(violations.isEmpty());
        assertTrue(request.hasAtLeastOneContactMethod());
    }

    @Test
    void testMissingFullName() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .email("test@example.com")
                .password("SecurePass123!")
                .role("SENIOR")
                .build();

        // When
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("fullName")));
    }

    @Test
    void testFullNameTooShort() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("A")
                .email("test@example.com")
                .password("SecurePass123!")
                .role("SENIOR")
                .build();

        // When
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("fullName") &&
                              v.getMessage().contains("between 2 and 100")));
    }

    @Test
    void testFullNameTooLong() {
        // Given
        String longName = "A".repeat(101);
        RegisterRequest request = RegisterRequest.builder()
                .fullName(longName)
                .email("test@example.com")
                .password("SecurePass123!")
                .role("SENIOR")
                .build();

        // When
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("fullName")));
    }

    @Test
    void testInvalidEmailFormat() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Test User")
                .email("invalid-email")
                .password("SecurePass123!")
                .role("SENIOR")
                .build();

        // When
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("email") &&
                              v.getMessage().contains("Invalid email")));
    }

    @Test
    void testInvalidMobileFormat() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Test User")
                .mobile("abc123")
                .password("SecurePass123!")
                .role("SENIOR")
                .build();

        // When
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("mobile") &&
                              v.getMessage().contains("Invalid mobile")));
    }

    @Test
    void testMissingBothContactMethods() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Test User")
                .password("SecurePass123!")
                .role("SENIOR")
                .build();

        // When
        boolean hasContact = request.hasAtLeastOneContactMethod();

        // Then
        assertFalse(hasContact);
    }

    @Test
    void testMissingPassword() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Test User")
                .email("test@example.com")
                .role("SENIOR")
                .build();

        // When
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    @Test
    void testPasswordTooShort() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Test User")
                .email("test@example.com")
                .password("Short1!")
                .role("SENIOR")
                .build();

        // When
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    @Test
    void testPasswordMissingUppercase() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Test User")
                .email("test@example.com")
                .password("securepass123!")
                .role("SENIOR")
                .build();

        // When
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("password") &&
                              v.getMessage().contains("uppercase")));
    }

    @Test
    void testPasswordMissingLowercase() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Test User")
                .email("test@example.com")
                .password("SECUREPASS123!")
                .role("SENIOR")
                .build();

        // When
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("password") &&
                              v.getMessage().contains("lowercase")));
    }

    @Test
    void testPasswordMissingDigit() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Test User")
                .email("test@example.com")
                .password("SecurePass!")
                .role("SENIOR")
                .build();

        // When
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("password") &&
                              v.getMessage().contains("digit")));
    }

    @Test
    void testPasswordMissingSpecialCharacter() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Test User")
                .email("test@example.com")
                .password("SecurePass123")
                .role("SENIOR")
                .build();

        // When
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("password") &&
                              v.getMessage().contains("special character")));
    }

    @Test
    void testMissingRole() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Test User")
                .email("test@example.com")
                .password("SecurePass123!")
                .build();

        // When
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("role")));
    }

    @Test
    void testInvalidRole_Admin() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Test User")
                .email("test@example.com")
                .password("SecurePass123!")
                .role("ADMIN")
                .build();

        // When
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("role") &&
                              v.getMessage().contains("SENIOR or CAREGIVER")));
    }

    @Test
    void testInvalidRole_Invalid() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Test User")
                .email("test@example.com")
                .password("SecurePass123!")
                .role("INVALID_ROLE")
                .build();

        // When
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("role")));
    }

    @Test
    void testInvalidPreferredLanguage() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Test User")
                .email("test@example.com")
                .password("SecurePass123!")
                .role("SENIOR")
                .preferredLanguage("fr")
                .build();

        // When
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("preferredLanguage") &&
                              v.getMessage().contains("en, hi, or mr")));
    }

    @Test
    void testValidPreferredLanguages() {
        // Test en
        RegisterRequest requestEn = RegisterRequest.builder()
                .fullName("Test User")
                .email("test@example.com")
                .password("SecurePass123!")
                .role("SENIOR")
                .preferredLanguage("en")
                .build();
        assertTrue(validator.validate(requestEn).isEmpty());

        // Test hi
        RegisterRequest requestHi = RegisterRequest.builder()
                .fullName("Test User")
                .email("test2@example.com")
                .password("SecurePass123!")
                .role("SENIOR")
                .preferredLanguage("hi")
                .build();
        assertTrue(validator.validate(requestHi).isEmpty());

        // Test mr
        RegisterRequest requestMr = RegisterRequest.builder()
                .fullName("Test User")
                .email("test3@example.com")
                .password("SecurePass123!")
                .role("SENIOR")
                .preferredLanguage("mr")
                .build();
        assertTrue(validator.validate(requestMr).isEmpty());
    }

    @Test
    void testNullPreferredLanguageIsValid() {
        // Given - preferred language is optional
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Test User")
                .email("test@example.com")
                .password("SecurePass123!")
                .role("SENIOR")
                .build();

        // When
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        // Then - no violation for missing preferredLanguage
        assertTrue(violations.stream()
                .noneMatch(v -> v.getPropertyPath().toString().equals("preferredLanguage")));
    }

    @Test
    void testValidMobileFormats() {
        // Test with +
        RegisterRequest request1 = RegisterRequest.builder()
                .fullName("Test User")
                .mobile("+919876543210")
                .password("SecurePass123!")
                .role("SENIOR")
                .build();
        assertTrue(validator.validate(request1).stream()
                .noneMatch(v -> v.getPropertyPath().toString().equals("mobile")));

        // Test with spaces
        RegisterRequest request2 = RegisterRequest.builder()
                .fullName("Test User")
                .mobile("+91 9876543210")
                .password("SecurePass123!")
                .role("SENIOR")
                .build();
        assertTrue(validator.validate(request2).stream()
                .noneMatch(v -> v.getPropertyPath().toString().equals("mobile")));

        // Test with dashes
        RegisterRequest request3 = RegisterRequest.builder()
                .fullName("Test User")
                .mobile("+91-9876543210")
                .password("SecurePass123!")
                .role("SENIOR")
                .build();
        assertTrue(validator.validate(request3).stream()
                .noneMatch(v -> v.getPropertyPath().toString().equals("mobile")));

        // Test with parentheses
        RegisterRequest request4 = RegisterRequest.builder()
                .fullName("Test User")
                .mobile("(987) 654-3210")
                .password("SecurePass123!")
                .role("SENIOR")
                .build();
        assertTrue(validator.validate(request4).stream()
                .noneMatch(v -> v.getPropertyPath().toString().equals("mobile")));
    }
}
