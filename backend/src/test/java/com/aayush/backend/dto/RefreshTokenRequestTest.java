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
 * Unit tests for RefreshTokenRequest validation
 */
class RefreshTokenRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidRefreshTokenRequest() {
        // Given
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("a7f3c2e1b9d8f4a6c3e2b1d9f8a7c6e5b4d3a2c1")
                .build();

        // When
        Set<ConstraintViolation<RefreshTokenRequest>> violations = validator.validate(request);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    void testMissingRefreshToken() {
        // Given
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .build();

        // When
        Set<ConstraintViolation<RefreshTokenRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("refreshToken") &&
                              v.getMessage().contains("required")));
    }

    @Test
    void testBlankRefreshToken() {
        // Given
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("   ")
                .build();

        // When
        Set<ConstraintViolation<RefreshTokenRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("refreshToken")));
    }

    @Test
    void testEmptyRefreshToken() {
        // Given
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("")
                .build();

        // When
        Set<ConstraintViolation<RefreshTokenRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("refreshToken")));
    }
}
