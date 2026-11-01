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
 * Unit tests for LogoutRequest validation
 */
class LogoutRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidLogoutRequest() {
        // Given
        LogoutRequest request = LogoutRequest.builder()
                .refreshToken("valid.refresh.token")
                .build();

        // When
        Set<ConstraintViolation<LogoutRequest>> violations = validator.validate(request);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    void testMissingRefreshToken() {
        // Given
        LogoutRequest request = LogoutRequest.builder()
                .build();

        // When
        Set<ConstraintViolation<LogoutRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("refreshToken") &&
                              v.getMessage().contains("required")));
    }

    @Test
    void testBlankRefreshToken() {
        // Given
        LogoutRequest request = LogoutRequest.builder()
                .refreshToken("   ")
                .build();

        // When
        Set<ConstraintViolation<LogoutRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("refreshToken") &&
                              v.getMessage().contains("required")));
    }

    @Test
    void testEmptyStringRefreshToken() {
        // Given
        LogoutRequest request = LogoutRequest.builder()
                .refreshToken("")
                .build();

        // When
        Set<ConstraintViolation<LogoutRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("refreshToken")));
    }
}
