package com.aayush.backend.service;

import com.aayush.backend.document.AccountStatus;
import com.aayush.backend.document.User;
import com.aayush.backend.document.UserRole;
import com.aayush.backend.dto.RegisterRequest;
import com.aayush.backend.dto.UserResponse;
import com.aayush.backend.exception.DuplicateUserException;
import com.aayush.backend.exception.ResourceNotFoundException;
import com.aayush.backend.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthenticationService
 * 
 * Uses Mockito to test service logic without MongoDB
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationService Unit Tests")
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordService passwordService;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    @DisplayName("Should successfully register user with email only")
    void testRegister_EmailOnly_Success() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("John Doe")
                .email("  JOHN@EXAMPLE.COM  ")  // Uppercase with spaces
                .password("SecureP@ss123")
                .role("SENIOR")
                .preferredLanguage("en")
                .build();

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordService.encodePassword(anyString())).thenReturn("$2a$12$hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("user123");
            return user;
        });

        // When
        UserResponse response = authenticationService.register(request);

        // Then
        assertNotNull(response);
        assertEquals("user123", response.getId());
        assertEquals("John Doe", response.getFullName());
        assertEquals("john@example.com", response.getEmail());  // Normalized
        assertNull(response.getMobile());
        assertEquals(UserRole.SENIOR, response.getRole());
        assertEquals(AccountStatus.ACTIVE, response.getStatus());
        assertFalse(response.isEmailVerified());
        assertFalse(response.isMobileVerified());

        // Verify password was hashed
        verify(passwordService).encodePassword("SecureP@ss123");

        // Verify user was saved with normalized email
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("john@example.com", savedUser.getEmail());
        assertEquals("$2a$12$hashedPassword", savedUser.getPasswordHash());
    }

    @Test
    @DisplayName("Should successfully register user with mobile only")
    void testRegister_MobileOnly_Success() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Jane Smith")
                .mobile("9876543210")  // 10-digit Indian number
                .password("SecureP@ss456")
                .role("CAREGIVER")
                .build();

        when(userRepository.existsByMobile("919876543210")).thenReturn(false);
        when(passwordService.encodePassword(anyString())).thenReturn("$2a$12$hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("user456");
            return user;
        });

        // When
        UserResponse response = authenticationService.register(request);

        // Then
        assertNotNull(response);
        assertEquals("Jane Smith", response.getFullName());
        assertNull(response.getEmail());
        assertEquals("919876543210", response.getMobile());  // Normalized with country code
        assertEquals(UserRole.CAREGIVER, response.getRole());
        assertEquals("en", response.getPreferredLanguage());  // Default
    }

    @Test
    @DisplayName("Should successfully register user with both email and mobile")
    void testRegister_EmailAndMobile_Success() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Bob Johnson")
                .email("bob@example.com")
                .mobile("+91-987-654-3210")  // With country code and formatting
                .password("SecureP@ss789")
                .role("SENIOR")
                .preferredLanguage("hi")
                .build();

        when(userRepository.existsByEmail("bob@example.com")).thenReturn(false);
        when(userRepository.existsByMobile("919876543210")).thenReturn(false);
        when(passwordService.encodePassword(anyString())).thenReturn("$2a$12$hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("user789");
            return user;
        });

        // When
        UserResponse response = authenticationService.register(request);

        // Then
        assertNotNull(response);
        assertEquals("bob@example.com", response.getEmail());
        assertEquals("919876543210", response.getMobile());
        assertEquals("hi", response.getPreferredLanguage());
    }

    @Test
    @DisplayName("Should reject null registration request")
    void testRegister_NullRequest_ThrowsException() {
        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> authenticationService.register(null)
        );

        assertEquals("Registration request cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should reject registration without contact method")
    void testRegister_NoContactMethod_ThrowsException() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("No Contact")
                .password("SecureP@ss123")
                .role("SENIOR")
                .build();

        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> authenticationService.register(request)
        );

        assertEquals("At least one contact method (email or mobile) must be provided", 
            exception.getMessage());
    }

    @Test
    @DisplayName("Should reject duplicate email")
    void testRegister_DuplicateEmail_ThrowsException() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Duplicate User")
                .email("existing@example.com")
                .password("SecureP@ss123")
                .role("SENIOR")
                .build();

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // When/Then
        DuplicateUserException exception = assertThrows(
            DuplicateUserException.class,
            () -> authenticationService.register(request)
        );

        assertEquals("Email address is already registered", exception.getMessage());
        
        // Verify password was never hashed
        verify(passwordService, never()).encodePassword(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should reject duplicate mobile")
    void testRegister_DuplicateMobile_ThrowsException() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Duplicate User")
                .mobile("9876543210")
                .password("SecureP@ss123")
                .role("SENIOR")
                .build();

        when(userRepository.existsByMobile("919876543210")).thenReturn(true);

        // When/Then
        DuplicateUserException exception = assertThrows(
            DuplicateUserException.class,
            () -> authenticationService.register(request)
        );

        assertEquals("Mobile number is already registered", exception.getMessage());
    }

    @Test
    @DisplayName("Should handle race condition duplicate key exception for email")
    void testRegister_RaceConditionEmail_ThrowsException() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Race User")
                .email("race@example.com")
                .password("SecureP@ss123")
                .role("SENIOR")
                .build();

        // First check passes (no duplicate)
        when(userRepository.existsByEmail("race@example.com")).thenReturn(false, true);
        when(passwordService.encodePassword(anyString())).thenReturn("$2a$12$hashedPassword");
        // Save fails due to race condition
        when(userRepository.save(any(User.class))).thenThrow(new DuplicateKeyException("Duplicate key"));

        // When/Then
        DuplicateUserException exception = assertThrows(
            DuplicateUserException.class,
            () -> authenticationService.register(request)
        );

        assertEquals("Email address is already registered", exception.getMessage());
    }

    @Test
    @DisplayName("Should handle race condition duplicate key exception for mobile")
    void testRegister_RaceConditionMobile_ThrowsException() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Race User")
                .mobile("9876543210")
                .password("SecureP@ss123")
                .role("SENIOR")
                .build();

        // First check passes (no duplicate), second check after save fails returns true
        when(userRepository.existsByMobile("919876543210")).thenReturn(false, true);
        when(passwordService.encodePassword(anyString())).thenReturn("$2a$12$hashedPassword");
        // Save fails due to race condition
        when(userRepository.save(any(User.class))).thenThrow(new DuplicateKeyException("Duplicate key"));

        // When/Then
        DuplicateUserException exception = assertThrows(
            DuplicateUserException.class,
            () -> authenticationService.register(request)
        );

        assertEquals("Mobile number is already registered", exception.getMessage());
    }

    @Test
    @DisplayName("Should reject ADMIN role in registration")
    void testRegister_AdminRole_ThrowsException() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Admin User")
                .email("admin@example.com")
                .password("SecureP@ss123")
                .role("ADMIN")
                .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> authenticationService.register(request)
        );

        assertEquals("ADMIN role cannot be assigned through self-registration", 
            exception.getMessage());
    }

    @Test
    @DisplayName("Should reject invalid role")
    void testRegister_InvalidRole_ThrowsException() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Invalid Role User")
                .email("invalid@example.com")
                .password("SecureP@ss123")
                .role("SUPERUSER")
                .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> authenticationService.register(request)
        );

        assertEquals("Invalid role. Allowed values: SENIOR, CAREGIVER", exception.getMessage());
    }

    @Test
    @DisplayName("Should normalize email (trim and lowercase)")
    void testRegister_EmailNormalization() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Email Test")
                .email("  TEST@EXAMPLE.COM  ")
                .password("SecureP@ss123")
                .role("SENIOR")
                .build();

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordService.encodePassword(anyString())).thenReturn("$2a$12$hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("user123");
            return user;
        });

        // When
        UserResponse response = authenticationService.register(request);

        // Then
        assertEquals("test@example.com", response.getEmail());
        verify(userRepository).existsByEmail("test@example.com");
    }

    @Test
    @DisplayName("Should normalize mobile (10 digits to country code format)")
    void testRegister_MobileNormalization_10Digits() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Mobile Test")
                .mobile("9876543210")
                .password("SecureP@ss123")
                .role("SENIOR")
                .build();

        when(userRepository.existsByMobile("919876543210")).thenReturn(false);
        when(passwordService.encodePassword(anyString())).thenReturn("$2a$12$hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("user123");
            return user;
        });

        // When
        UserResponse response = authenticationService.register(request);

        // Then
        assertEquals("919876543210", response.getMobile());
    }

    @Test
    @DisplayName("Should normalize mobile (remove formatting)")
    void testRegister_MobileNormalization_WithFormatting() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Mobile Test")
                .mobile("+91 (987) 654-3210")
                .password("SecureP@ss123")
                .role("SENIOR")
                .build();

        when(userRepository.existsByMobile("919876543210")).thenReturn(false);
        when(passwordService.encodePassword(anyString())).thenReturn("$2a$12$hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("user123");
            return user;
        });

        // When
        UserResponse response = authenticationService.register(request);

        // Then
        assertEquals("919876543210", response.getMobile());
    }

    @Test
    @DisplayName("Should not expose passwordHash in response")
    void testRegister_PasswordHashNotExposed() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Security Test")
                .email("security@example.com")
                .password("SecureP@ss123")
                .role("SENIOR")
                .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordService.encodePassword(anyString())).thenReturn("$2a$12$hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("user123");
            return user;
        });

        // When
        UserResponse response = authenticationService.register(request);

        // Then
        assertNotNull(response);
        // UserResponse should not have passwordHash field at all
        assertDoesNotThrow(() -> {
            // If passwordHash was exposed, this would fail
            String json = response.toString();
            assertFalse(json.contains("passwordHash"));
            assertFalse(json.contains("$2a$12$"));
        });
    }

    @Test
    @DisplayName("Should set verification flags to false by default")
    void testRegister_VerificationFlagsDefault() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Verification Test")
                .email("verify@example.com")
                .mobile("9876543210")
                .password("SecureP@ss123")
                .role("SENIOR")
                .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByMobile(anyString())).thenReturn(false);
        when(passwordService.encodePassword(anyString())).thenReturn("$2a$12$hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("user123");
            return user;
        });

        // When
        UserResponse response = authenticationService.register(request);

        // Then
        assertFalse(response.isEmailVerified());
        assertFalse(response.isMobileVerified());

        // Verify saved user also has false flags
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertFalse(savedUser.isEmailVerified());
        assertFalse(savedUser.isMobileVerified());
    }

    @Test
    @DisplayName("Should set default ACTIVE status")
    void testRegister_DefaultActiveStatus() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Status Test")
                .email("status@example.com")
                .password("SecureP@ss123")
                .role("SENIOR")
                .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordService.encodePassword(anyString())).thenReturn("$2a$12$hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("user123");
            return user;
        });

        // When
        UserResponse response = authenticationService.register(request);

        // Then
        assertEquals(AccountStatus.ACTIVE, response.getStatus());
    }

    @Test
    @DisplayName("Should use default language if not provided")
    void testRegister_DefaultLanguage() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Language Test")
                .email("lang@example.com")
                .password("SecureP@ss123")
                .role("SENIOR")
                .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordService.encodePassword(anyString())).thenReturn("$2a$12$hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("user123");
            return user;
        });

        // When
        UserResponse response = authenticationService.register(request);

        // Then
        assertEquals("en", response.getPreferredLanguage());
    }

    @Test
    @DisplayName("Should get user by ID successfully")
    void testGetUserById_Success() {
        // Given
        String userId = "user123";
        User user = User.builder()
                .id(userId)
                .fullName("Test User")
                .email("test@example.com")
                .role(UserRole.SENIOR)
                .status(AccountStatus.ACTIVE)
                .emailVerified(false)
                .mobileVerified(false)
                .preferredLanguage("en")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        UserResponse response = authenticationService.getUserById(userId);

        // Then
        assertNotNull(response);
        assertEquals(userId, response.getId());
        assertEquals("Test User", response.getFullName());
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void testGetUserById_NotFound() {
        // Given
        String userId = "nonexistent";
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When/Then
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> authenticationService.getUserById(userId)
        );

        assertEquals("User not found with ID: nonexistent", exception.getMessage());
    }

    @Test
    @DisplayName("Should reject null user ID")
    void testGetUserById_NullId() {
        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> authenticationService.getUserById(null)
        );

        assertEquals("User ID cannot be null or blank", exception.getMessage());
    }

    @Test
    @DisplayName("Should reject blank user ID")
    void testGetUserById_BlankId() {
        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> authenticationService.getUserById("   ")
        );

        assertEquals("User ID cannot be null or blank", exception.getMessage());
    }

    @Test
    @DisplayName("Should accept SENIOR role")
    void testRegister_SeniorRole_Success() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Senior User")
                .email("senior@example.com")
                .password("SecureP@ss123")
                .role("SENIOR")
                .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordService.encodePassword(anyString())).thenReturn("$2a$12$hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("user123");
            return user;
        });

        // When
        UserResponse response = authenticationService.register(request);

        // Then
        assertEquals(UserRole.SENIOR, response.getRole());
    }

    @Test
    @DisplayName("Should accept CAREGIVER role")
    void testRegister_CaregiverRole_Success() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Caregiver User")
                .email("caregiver@example.com")
                .password("SecureP@ss123")
                .role("CAREGIVER")
                .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordService.encodePassword(anyString())).thenReturn("$2a$12$hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("user123");
            return user;
        });

        // When
        UserResponse response = authenticationService.register(request);

        // Then
        assertEquals(UserRole.CAREGIVER, response.getRole());
    }

    @Test
    @DisplayName("Should detect duplicate mobile with different formatting (10-digit vs +91 format)")
    void testRegister_DuplicateMobile_DifferentFormat() {
        // Given: User tries to register with "+91 9876543210"
        // when "9876543210" is already registered (both normalize to "919876543210")
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Duplicate Mobile User")
                .mobile("+91 9876543210")  // With country code
                .password("SecureP@ss123")
                .role("SENIOR")
                .build();

        // This mobile is already in database as "919876543210"
        when(userRepository.existsByMobile("919876543210")).thenReturn(true);

        // When/Then
        DuplicateUserException exception = assertThrows(
            DuplicateUserException.class,
            () -> authenticationService.register(request)
        );

        assertEquals("Mobile number is already registered", exception.getMessage());
    }

    @Test
    @DisplayName("Should detect duplicate mobile with whitespace and hyphens")
    void testRegister_DuplicateMobile_WithWhitespaceAndHyphens() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Format Test User")
                .mobile(" 98765-43210 ")  // With whitespace and hyphen
                .password("SecureP@ss123")
                .role("SENIOR")
                .build();

        when(userRepository.existsByMobile("919876543210")).thenReturn(true);

        // When/Then
        DuplicateUserException exception = assertThrows(
            DuplicateUserException.class,
            () -> authenticationService.register(request)
        );

        assertEquals("Mobile number is already registered", exception.getMessage());
    }

    @Test
    @DisplayName("Should normalize mobile with 12 digits starting with 91")
    void testRegister_MobileNormalization_12DigitsWithCountryCode() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Mobile Test")
                .mobile("919876543210")  // Already has country code
                .password("SecureP@ss123")
                .role("SENIOR")
                .build();

        when(userRepository.existsByMobile("919876543210")).thenReturn(false);
        when(passwordService.encodePassword(anyString())).thenReturn("$2a$12$hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("user123");
            return user;
        });

        // When
        UserResponse response = authenticationService.register(request);

        // Then
        assertEquals("919876543210", response.getMobile());
        verify(userRepository).existsByMobile("919876543210");
    }

    @Test
    @DisplayName("Should normalize international mobile (non-India)")
    void testRegister_MobileNormalization_InternationalNumber() {
        // Given: US number with country code
        RegisterRequest request = RegisterRequest.builder()
                .fullName("International User")
                .mobile("+1 (555) 123-4567")  // US format
                .password("SecureP@ss123")
                .role("SENIOR")
                .build();

        // Normalized to "15551234567" (13 digits)
        when(userRepository.existsByMobile("15551234567")).thenReturn(false);
        when(passwordService.encodePassword(anyString())).thenReturn("$2a$12$hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("user123");
            return user;
        });

        // When
        UserResponse response = authenticationService.register(request);

        // Then
        assertEquals("15551234567", response.getMobile());
    }

    @Test
    @DisplayName("Should handle mobile with only whitespace as null")
    void testRegister_MobileNormalization_OnlyWhitespace() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Whitespace Test")
                .email("whitespace@example.com")
                .mobile("   ")  // Only whitespace
                .password("SecureP@ss123")
                .role("SENIOR")
                .build();

        when(userRepository.existsByEmail("whitespace@example.com")).thenReturn(false);
        when(passwordService.encodePassword(anyString())).thenReturn("$2a$12$hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("user123");
            return user;
        });

        // When
        UserResponse response = authenticationService.register(request);

        // Then
        assertNull(response.getMobile());  // Should be null, not empty string
        verify(userRepository, never()).existsByMobile(anyString());
    }

    @Test
    @DisplayName("Should reject ADMIN role case-insensitively")
    void testRegister_AdminRole_CaseInsensitive_ThrowsException() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Admin Attempt")
                .email("admin@example.com")
                .password("SecureP@ss123")
                .role("admin")  // Lowercase
                .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> authenticationService.register(request)
        );

        assertEquals("ADMIN role cannot be assigned through self-registration", 
            exception.getMessage());
    }

    @Test
    @DisplayName("Should accept role with different casing (SENIOR, senior, Senior)")
    void testRegister_RoleCaseInsensitive_Success() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Case Test User")
                .email("case@example.com")
                .password("SecureP@ss123")
                .role("senior")  // Lowercase
                .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordService.encodePassword(anyString())).thenReturn("$2a$12$hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("user123");
            return user;
        });

        // When
        UserResponse response = authenticationService.register(request);

        // Then
        assertEquals(UserRole.SENIOR, response.getRole());
    }

    @Test
    @DisplayName("Should reject null role")
    void testRegister_NullRole_ThrowsException() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Null Role User")
                .email("nullrole@example.com")
                .password("SecureP@ss123")
                .role(null)
                .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> authenticationService.register(request)
        );

        assertEquals("Role is required", exception.getMessage());
    }

    @Test
    @DisplayName("Should reject blank role")
    void testRegister_BlankRole_ThrowsException() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Blank Role User")
                .email("blankrole@example.com")
                .password("SecureP@ss123")
                .role("   ")
                .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> authenticationService.register(request)
        );

        assertEquals("Role is required", exception.getMessage());
    }
}
