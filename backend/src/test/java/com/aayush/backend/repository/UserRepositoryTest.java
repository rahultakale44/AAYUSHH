package com.aayush.backend.repository;

import com.aayush.backend.document.AccountStatus;
import com.aayush.backend.document.User;
import com.aayush.backend.document.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for UserRepository
 * Uses Docker MongoDB container at localhost:27017 with dedicated test database
 */
@SpringBootTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void cleanUp() {
        // Clean up test data after each test to ensure isolation
        userRepository.deleteAll();
    }

    @Test
    void testSaveUser_Success() {
        // Given
        User user = User.builder()
                .fullName("Test User")
                .email("test@example.com")
                .mobile("919876543210")
                .passwordHash("$2a$12$hashedpassword")
                .role(UserRole.SENIOR)
                .status(AccountStatus.ACTIVE)
                .emailVerified(false)
                .mobileVerified(false)
                .preferredLanguage("en")
                .build();

        // When
        User savedUser = userRepository.save(user);

        // Then
        assertNotNull(savedUser.getId());
        assertEquals("Test User", savedUser.getFullName());
        assertEquals("test@example.com", savedUser.getEmail());
        assertEquals("919876543210", savedUser.getMobile());
        assertEquals(UserRole.SENIOR, savedUser.getRole());
        assertEquals(AccountStatus.ACTIVE, savedUser.getStatus());
        assertFalse(savedUser.isEmailVerified());
        assertFalse(savedUser.isMobileVerified());
        assertNotNull(savedUser.getCreatedAt());
        assertNotNull(savedUser.getUpdatedAt());
    }

    @Test
    void testSaveUser_DuplicateEmail_ThrowsException() {
        // Given - First user
        User user1 = User.builder()
                .fullName("User One")
                .email("duplicate@example.com")
                .mobile("919876543210")
                .passwordHash("$2a$12$hash1")
                .role(UserRole.SENIOR)
                .status(AccountStatus.ACTIVE)
                .build();
        userRepository.save(user1);

        // When - Second user with same email
        User user2 = User.builder()
                .fullName("User Two")
                .email("duplicate@example.com")
                .mobile("919876543211")
                .passwordHash("$2a$12$hash2")
                .role(UserRole.CAREGIVER)
                .status(AccountStatus.ACTIVE)
                .build();

        // Then - Should throw DuplicateKeyException
        assertThrows(DuplicateKeyException.class, () -> {
            userRepository.save(user2);
        });
    }

    @Test
    void testSaveUser_DuplicateMobile_ThrowsException() {
        // Given - First user
        User user1 = User.builder()
                .fullName("User One")
                .email("user1@example.com")
                .mobile("919876543210")
                .passwordHash("$2a$12$hash1")
                .role(UserRole.SENIOR)
                .status(AccountStatus.ACTIVE)
                .build();
        userRepository.save(user1);

        // When - Second user with same mobile
        User user2 = User.builder()
                .fullName("User Two")
                .email("user2@example.com")
                .mobile("919876543210")
                .passwordHash("$2a$12$hash2")
                .role(UserRole.CAREGIVER)
                .status(AccountStatus.ACTIVE)
                .build();

        // Then - Should throw DuplicateKeyException
        assertThrows(DuplicateKeyException.class, () -> {
            userRepository.save(user2);
        });
    }

    @Test
    void testFindByEmail_Found() {
        // Given
        User user = createAndSaveUser("found@example.com", "919876543210", UserRole.SENIOR);

        // When
        Optional<User> found = userRepository.findByEmail("found@example.com");

        // Then
        assertTrue(found.isPresent());
        assertEquals("found@example.com", found.get().getEmail());
        assertEquals(user.getId(), found.get().getId());
    }

    @Test
    void testFindByEmail_NotFound() {
        // When
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        // Then
        assertFalse(found.isPresent());
    }

    @Test
    void testFindByMobile_Found() {
        // Given
        User user = createAndSaveUser("user@example.com", "919876543210", UserRole.SENIOR);

        // When
        Optional<User> found = userRepository.findByMobile("919876543210");

        // Then
        assertTrue(found.isPresent());
        assertEquals("919876543210", found.get().getMobile());
        assertEquals(user.getId(), found.get().getId());
    }

    @Test
    void testFindByMobile_NotFound() {
        // When
        Optional<User> found = userRepository.findByMobile("910000000000");

        // Then
        assertFalse(found.isPresent());
    }

    @Test
    void testExistsByEmail() {
        // Given
        createAndSaveUser("exists@example.com", "919876543210", UserRole.SENIOR);

        // When & Then
        assertTrue(userRepository.existsByEmail("exists@example.com"));
        assertFalse(userRepository.existsByEmail("notexists@example.com"));
    }

    @Test
    void testExistsByMobile() {
        // Given
        createAndSaveUser("user@example.com", "919876543210", UserRole.SENIOR);

        // When & Then
        assertTrue(userRepository.existsByMobile("919876543210"));
        assertFalse(userRepository.existsByMobile("910000000000"));
    }

    @Test
    void testFindByRole() {
        // Given
        createAndSaveUser("senior1@example.com", "919876543210", UserRole.SENIOR);
        createAndSaveUser("senior2@example.com", "919876543211", UserRole.SENIOR);
        createAndSaveUser("caregiver@example.com", "919876543212", UserRole.CAREGIVER);

        // When
        List<User> seniors = userRepository.findByRole(UserRole.SENIOR);
        List<User> caregivers = userRepository.findByRole(UserRole.CAREGIVER);
        List<User> admins = userRepository.findByRole(UserRole.ADMIN);

        // Then
        assertEquals(2, seniors.size());
        assertEquals(1, caregivers.size());
        assertEquals(0, admins.size());
    }

    @Test
    void testFindByStatus() {
        // Given
        createAndSaveUserWithStatus("active1@example.com", "919876543210", AccountStatus.ACTIVE);
        createAndSaveUserWithStatus("active2@example.com", "919876543211", AccountStatus.ACTIVE);
        createAndSaveUserWithStatus("suspended@example.com", "919876543212", AccountStatus.SUSPENDED);

        // When
        List<User> activeUsers = userRepository.findByStatus(AccountStatus.ACTIVE);
        List<User> suspendedUsers = userRepository.findByStatus(AccountStatus.SUSPENDED);
        List<User> deletedUsers = userRepository.findByStatus(AccountStatus.DELETED);

        // Then
        assertEquals(2, activeUsers.size());
        assertEquals(1, suspendedUsers.size());
        assertEquals(0, deletedUsers.size());
    }

    @Test
    void testFindByRoleAndStatus() {
        // Given
        createAndSaveUserWithRoleAndStatus("senior1@example.com", "919876543210", 
                UserRole.SENIOR, AccountStatus.ACTIVE);
        createAndSaveUserWithRoleAndStatus("senior2@example.com", "919876543211", 
                UserRole.SENIOR, AccountStatus.SUSPENDED);
        createAndSaveUserWithRoleAndStatus("caregiver@example.com", "919876543212", 
                UserRole.CAREGIVER, AccountStatus.ACTIVE);

        // When
        List<User> activeSeniors = userRepository.findByRoleAndStatus(
                UserRole.SENIOR, AccountStatus.ACTIVE);
        List<User> suspendedSeniors = userRepository.findByRoleAndStatus(
                UserRole.SENIOR, AccountStatus.SUSPENDED);
        List<User> activeCaregivers = userRepository.findByRoleAndStatus(
                UserRole.CAREGIVER, AccountStatus.ACTIVE);

        // Then
        assertEquals(1, activeSeniors.size());
        assertEquals(1, suspendedSeniors.size());
        assertEquals(1, activeCaregivers.size());
    }

    @Test
    void testCountByRole() {
        // Given
        createAndSaveUser("senior1@example.com", "919876543210", UserRole.SENIOR);
        createAndSaveUser("senior2@example.com", "919876543211", UserRole.SENIOR);
        createAndSaveUser("caregiver@example.com", "919876543212", UserRole.CAREGIVER);

        // When
        long seniorCount = userRepository.countByRole(UserRole.SENIOR);
        long caregiverCount = userRepository.countByRole(UserRole.CAREGIVER);
        long adminCount = userRepository.countByRole(UserRole.ADMIN);

        // Then
        assertEquals(2, seniorCount);
        assertEquals(1, caregiverCount);
        assertEquals(0, adminCount);
    }

    @Test
    void testCountByStatus() {
        // Given
        createAndSaveUserWithStatus("active1@example.com", "919876543210", AccountStatus.ACTIVE);
        createAndSaveUserWithStatus("active2@example.com", "919876543211", AccountStatus.ACTIVE);
        createAndSaveUserWithStatus("suspended@example.com", "919876543212", AccountStatus.SUSPENDED);

        // When
        long activeCount = userRepository.countByStatus(AccountStatus.ACTIVE);
        long suspendedCount = userRepository.countByStatus(AccountStatus.SUSPENDED);
        long deletedCount = userRepository.countByStatus(AccountStatus.DELETED);

        // Then
        assertEquals(2, activeCount);
        assertEquals(1, suspendedCount);
        assertEquals(0, deletedCount);
    }

    @Test
    void testUserWithoutEmailOrMobile_Success() {
        // Given - User with only email
        User userWithEmail = User.builder()
                .fullName("Email Only User")
                .email("emailonly@example.com")
                .passwordHash("$2a$12$hash")
                .role(UserRole.SENIOR)
                .status(AccountStatus.ACTIVE)
                .build();

        // When
        User savedEmailUser = userRepository.save(userWithEmail);

        // Given - User with only mobile
        User userWithMobile = User.builder()
                .fullName("Mobile Only User")
                .mobile("919876543210")
                .passwordHash("$2a$12$hash")
                .role(UserRole.SENIOR)
                .status(AccountStatus.ACTIVE)
                .build();

        // When
        User savedMobileUser = userRepository.save(userWithMobile);

        // Then
        assertNotNull(savedEmailUser.getId());
        assertNull(savedEmailUser.getMobile());
        assertNotNull(savedMobileUser.getId());
        assertNull(savedMobileUser.getEmail());
    }

    @Test
    void testPasswordHashExcludedFromToString() {
        // Given
        User user = User.builder()
                .fullName("Test User")
                .email("test@example.com")
                .mobile("919876543210")
                .passwordHash("$2a$12$secrethash")
                .role(UserRole.SENIOR)
                .status(AccountStatus.ACTIVE)
                .build();

        // When
        String userString = user.toString();

        // Then
        assertFalse(userString.contains("$2a$12$secrethash"));
        assertFalse(userString.contains("passwordHash"));
        assertTrue(userString.contains("test@example.com"));
    }

    @Test
    void testAuditFieldsAutoPopulated() throws InterruptedException {
        // Given
        User user = User.builder()
                .fullName("Audit Test User")
                .email("audit@example.com")
                .mobile("919876543210")
                .passwordHash("$2a$12$hash")
                .role(UserRole.SENIOR)
                .status(AccountStatus.ACTIVE)
                .build();

        // When
        User savedUser = userRepository.save(user);
        LocalDateTime createdAt = savedUser.getCreatedAt();
        assertNotNull(createdAt);

        // Wait a bit to ensure updatedAt changes
        Thread.sleep(100);

        // Update user
        savedUser.setFullName("Updated Name");
        User updatedUser = userRepository.save(savedUser);

        // Then
        assertNotNull(updatedUser.getCreatedAt());
        assertNotNull(updatedUser.getUpdatedAt());
        assertEquals(createdAt, updatedUser.getCreatedAt());
        assertTrue(updatedUser.getUpdatedAt().isAfter(updatedUser.getCreatedAt()) ||
                   updatedUser.getUpdatedAt().isEqual(updatedUser.getCreatedAt()));
    }

    // Helper methods

    private User createAndSaveUser(String email, String mobile, UserRole role) {
        User user = User.builder()
                .fullName("Test User")
                .email(email)
                .mobile(mobile)
                .passwordHash("$2a$12$hashedpassword")
                .role(role)
                .status(AccountStatus.ACTIVE)
                .emailVerified(false)
                .mobileVerified(false)
                .build();
        return userRepository.save(user);
    }

    private User createAndSaveUserWithStatus(String email, String mobile, AccountStatus status) {
        User user = User.builder()
                .fullName("Test User")
                .email(email)
                .mobile(mobile)
                .passwordHash("$2a$12$hashedpassword")
                .role(UserRole.SENIOR)
                .status(status)
                .emailVerified(false)
                .mobileVerified(false)
                .build();
        return userRepository.save(user);
    }

    private User createAndSaveUserWithRoleAndStatus(String email, String mobile, 
                                                     UserRole role, AccountStatus status) {
        User user = User.builder()
                .fullName("Test User")
                .email(email)
                .mobile(mobile)
                .passwordHash("$2a$12$hashedpassword")
                .role(role)
                .status(status)
                .emailVerified(false)
                .mobileVerified(false)
                .build();
        return userRepository.save(user);
    }
}
