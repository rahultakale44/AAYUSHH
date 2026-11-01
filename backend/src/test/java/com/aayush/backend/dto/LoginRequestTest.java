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
 * Unit tests for LoginRequest validation
 */
class LoginRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidLoginWithEmail() {
        // Given
        LoginRequest request = LoginRequest.builder()
                .identifier("test@example.com")
                .password("SecurePass123!")
                .build();

        // When
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    void testValidLoginWithMobile() {
        // Given
        LoginRequest request = LoginRequest.builder()
                .identifier("+91-9876543210")
                .password("SecurePass123!")
                .build();

        // When
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    void testMissingIdentifier() {
        // Given
        LoginRequest request = LoginRequest.builder()
                .password("SecurePass123!")
                .build();

        // When
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("identifier") &&
                              v.getMessage().contains("required")));
    }

    @Test
    void testBlankIdentifier() {
        // Given
        LoginRequest request = LoginRequest.builder()
                .identifier("   ")
                .password("SecurePass123!")
                .build();

        // When
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("identifier")));
    }

    @Test
    void testMissingPassword() {
        // Given
        LoginRequest request = LoginRequest.builder()
                .identifier("test@example.com")
                .build();

        // When
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("password") &&
                              v.getMessage().contains("required")));
    }

    @Test
    void testBlankPassword() {
        // Given
        LoginRequest request = LoginRequest.builder()
                .identifier("test@example.com")
                .password("   ")
                .build();

        // When
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    @Test
    void testIdentifierTooLong() {
        // Given
        String longIdentifier = "a".repeat(256) + "@example.com";
        LoginRequest request = LoginRequest.builder()
                .identifier(longIdentifier)
                .password("SecurePass123!")
                .build();

        // When
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("identifier") &&
                              v.getMessage().contains("255")));
    }

    @Test
    void testPasswordTooLong() {
        // Given
        String longPassword = "A".repeat(101);
        LoginRequest request = LoginRequest.builder()
                .identifier("test@example.com")
                .password(longPassword)
                .build();

        // When
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("password") &&
                              v.getMessage().contains("100")));
    }

    @Test
    void testValidLoginWithVariousIdentifierFormats() {
        // Email identifier
        LoginRequest request1 = LoginRequest.builder()
                .identifier("user@example.com")
                .password("password")
                .build();
        assertTrue(validator.validate(request1).isEmpty());

        // Mobile identifier with +
        LoginRequest request2 = LoginRequest.builder()
                .identifier("+919876543210")
                .password("password")
                .build();
        assertTrue(validator.validate(request2).isEmpty());

        // Mobile identifier with spaces
        LoginRequest request3 = LoginRequest.builder()
                .identifier("98765 43210")
                .password("password")
                .build();
        assertTrue(validator.validate(request3).isEmpty());

        // Username-like identifier
        LoginRequest request4 = LoginRequest.builder()
                .identifier("username123")
                .password("password")
                .build();
        assertTrue(validator.validate(request4).isEmpty());
    }
}
