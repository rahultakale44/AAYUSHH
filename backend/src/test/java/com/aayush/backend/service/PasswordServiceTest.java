package com.aayush.backend.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PasswordService
 * 
 * Tests password encoding and verification functionality
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("PasswordService Tests")
class PasswordServiceTest {

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Should encode password to non-plaintext BCrypt hash")
    void testEncodePassword_ValidPassword_ReturnsEncodedHash() {
        // Given
        String rawPassword = "SecureP@ssw0rd123";

        // When
        String encodedPassword = passwordService.encodePassword(rawPassword);

        // Then
        assertNotNull(encodedPassword, "Encoded password should not be null");
        assertNotEquals(rawPassword, encodedPassword, "Encoded password should not match plaintext");
        assertTrue(encodedPassword.length() > 50, "BCrypt hash should be at least 50 characters");
    }

    @Test
    @DisplayName("Should produce BCrypt hash starting with valid identifier")
    void testEncodePassword_ValidPassword_StartsWithBCryptIdentifier() {
        // Given
        String rawPassword = "AnotherSecure@Pass123";

        // When
        String encodedPassword = passwordService.encodePassword(rawPassword);

        // Then
        assertTrue(
            encodedPassword.startsWith("$2a$") || 
            encodedPassword.startsWith("$2b$") || 
            encodedPassword.startsWith("$2y$"),
            "Encoded password should start with BCrypt identifier ($2a$, $2b$, or $2y$)"
        );
    }

    @Test
    @DisplayName("Should produce different hashes for same password due to random salt")
    void testEncodePassword_SamePassword_ProducesDifferentHashes() {
        // Given
        String rawPassword = "TestP@ssword456";

        // When
        String encodedPassword1 = passwordService.encodePassword(rawPassword);
        String encodedPassword2 = passwordService.encodePassword(rawPassword);

        // Then
        assertNotEquals(encodedPassword1, encodedPassword2, 
            "Same password should produce different hashes due to random salt");
    }

    @Test
    @DisplayName("Should verify correct password matches encoded hash")
    void testMatches_CorrectPassword_ReturnsTrue() {
        // Given
        String rawPassword = "MySecure@Pass789";
        String encodedPassword = passwordService.encodePassword(rawPassword);

        // When
        boolean matches = passwordService.matches(rawPassword, encodedPassword);

        // Then
        assertTrue(matches, "Correct password should match its encoded hash");
    }

    @Test
    @DisplayName("Should verify incorrect password does not match")
    void testMatches_IncorrectPassword_ReturnsFalse() {
        // Given
        String rawPassword = "CorrectP@ssword123";
        String wrongPassword = "WrongP@ssword456";
        String encodedPassword = passwordService.encodePassword(rawPassword);

        // When
        boolean matches = passwordService.matches(wrongPassword, encodedPassword);

        // Then
        assertFalse(matches, "Incorrect password should not match");
    }

    @Test
    @DisplayName("Should reject null raw password during encoding")
    void testEncodePassword_NullPassword_ThrowsException() {
        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> passwordService.encodePassword(null),
            "Encoding null password should throw IllegalArgumentException"
        );

        assertEquals("Password cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should reject blank raw password during encoding")
    void testEncodePassword_BlankPassword_ThrowsException() {
        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> passwordService.encodePassword("   "),
            "Encoding blank password should throw IllegalArgumentException"
        );

        assertEquals("Password cannot be blank", exception.getMessage());
    }

    @Test
    @DisplayName("Should reject empty string password during encoding")
    void testEncodePassword_EmptyPassword_ThrowsException() {
        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> passwordService.encodePassword(""),
            "Encoding empty password should throw IllegalArgumentException"
        );

        assertEquals("Password cannot be blank", exception.getMessage());
    }

    @Test
    @DisplayName("Should reject null raw password during verification")
    void testMatches_NullRawPassword_ThrowsException() {
        // Given
        String encodedPassword = passwordService.encodePassword("ValidP@ss123");

        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> passwordService.matches(null, encodedPassword),
            "Verifying with null raw password should throw IllegalArgumentException"
        );

        assertEquals("Raw password cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should reject null encoded password during verification")
    void testMatches_NullEncodedPassword_ThrowsException() {
        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> passwordService.matches("ValidP@ss123", null),
            "Verifying with null encoded password should throw IllegalArgumentException"
        );

        assertEquals("Encoded password cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should return false for malformed encoded hash")
    void testMatches_MalformedEncodedHash_ReturnsFalse() {
        // Given
        String rawPassword = "TestP@ssword123";
        String malformedHash = "not-a-valid-bcrypt-hash";

        // When
        boolean matches = passwordService.matches(rawPassword, malformedHash);

        // Then
        assertFalse(matches, "Malformed encoded hash should not authenticate");
    }

    @Test
    @DisplayName("Should return false for invalid BCrypt format")
    void testMatches_InvalidBCryptFormat_ReturnsFalse() {
        // Given
        String rawPassword = "TestP@ssword123";
        String invalidHash = "$2a$invalid$format";

        // When
        boolean matches = passwordService.matches(rawPassword, invalidHash);

        // Then
        assertFalse(matches, "Invalid BCrypt format should not authenticate");
    }

    @Test
    @DisplayName("Should verify two independently encoded passwords correctly")
    void testMatches_TwoIndependentEncodings_BothVerifyCorrectly() {
        // Given
        String password1 = "FirstP@ssword123";
        String password2 = "SecondP@ssword456";
        String encoded1 = passwordService.encodePassword(password1);
        String encoded2 = passwordService.encodePassword(password2);

        // When/Then
        assertTrue(passwordService.matches(password1, encoded1), 
            "First password should match its encoded hash");
        assertTrue(passwordService.matches(password2, encoded2), 
            "Second password should match its encoded hash");
        assertFalse(passwordService.matches(password1, encoded2), 
            "First password should not match second encoded hash");
        assertFalse(passwordService.matches(password2, encoded1), 
            "Second password should not match first encoded hash");
    }

    @Test
    @DisplayName("Should verify BCrypt strength is configured to 12")
    void testPasswordEncoder_HasCorrectStrength() {
        // Given
        String rawPassword = "TestP@ssword123";
        String encodedPassword = passwordService.encodePassword(rawPassword);

        // When - Extract strength from BCrypt hash format: $2a$12$...
        String[] parts = encodedPassword.split("\\$");
        
        // Then
        assertTrue(parts.length >= 4, "BCrypt hash should have at least 4 parts");
        assertEquals("12", parts[2], "BCrypt strength should be 12");
    }

    @Test
    @DisplayName("Should handle special characters in password")
    void testEncodePassword_SpecialCharacters_EncodesCorrectly() {
        // Given
        String rawPassword = "P@$$w0rd!#%&*()_+-=[]{}|;:',.<>?/";

        // When
        String encodedPassword = passwordService.encodePassword(rawPassword);

        // Then
        assertNotNull(encodedPassword);
        assertTrue(passwordService.matches(rawPassword, encodedPassword), 
            "Password with special characters should match after encoding");
    }

    @Test
    @DisplayName("Should handle long password")
    void testEncodePassword_LongPassword_EncodesCorrectly() {
        // Given
        String rawPassword = "ThisIsAVeryLongP@sswordWithMoreThan72CharactersToTestBCryptLimitations123456789!";

        // When
        String encodedPassword = passwordService.encodePassword(rawPassword);

        // Then
        assertNotNull(encodedPassword);
        assertTrue(passwordService.matches(rawPassword, encodedPassword), 
            "Long password should match after encoding");
    }

    @Test
    @DisplayName("Should handle password with unicode characters")
    void testEncodePassword_UnicodeCharacters_EncodesCorrectly() {
        // Given
        String rawPassword = "P@ssw0rd_आयुष_मराठी_123";

        // When
        String encodedPassword = passwordService.encodePassword(rawPassword);

        // Then
        assertNotNull(encodedPassword);
        assertTrue(passwordService.matches(rawPassword, encodedPassword), 
            "Password with unicode characters should match after encoding");
    }

    @Test
    @DisplayName("Should not match when case differs")
    void testMatches_CaseDiffers_ReturnsFalse() {
        // Given
        String rawPassword = "P@ssword123";
        String encodedPassword = passwordService.encodePassword(rawPassword);

        // When
        boolean matchesUpperCase = passwordService.matches("P@SSWORD123", encodedPassword);
        boolean matchesLowerCase = passwordService.matches("p@ssword123", encodedPassword);

        // Then
        assertFalse(matchesUpperCase, "Password matching should be case-sensitive");
        assertFalse(matchesLowerCase, "Password matching should be case-sensitive");
    }

    @Test
    @DisplayName("Should not match when whitespace differs")
    void testMatches_WhitespaceDiffers_ReturnsFalse() {
        // Given
        String rawPassword = "P@ssword123";
        String encodedPassword = passwordService.encodePassword(rawPassword);

        // When
        boolean matchesWithLeadingSpace = passwordService.matches(" P@ssword123", encodedPassword);
        boolean matchesWithTrailingSpace = passwordService.matches("P@ssword123 ", encodedPassword);

        // Then
        assertFalse(matchesWithLeadingSpace, "Leading whitespace should not match");
        assertFalse(matchesWithTrailingSpace, "Trailing whitespace should not match");
    }

    @Test
    @DisplayName("Should handle minimum length password")
    void testEncodePassword_MinimumLengthPassword_EncodesCorrectly() {
        // Given - Minimum from RegisterRequest is 8 characters
        String rawPassword = "P@ssw0rd";

        // When
        String encodedPassword = passwordService.encodePassword(rawPassword);

        // Then
        assertNotNull(encodedPassword);
        assertTrue(passwordService.matches(rawPassword, encodedPassword), 
            "Minimum length password should match after encoding");
    }
}
