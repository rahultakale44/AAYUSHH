package com.aayush.backend.repository;

import com.aayush.backend.document.User;
import com.aayush.backend.document.UserRole;
import com.aayush.backend.document.AccountStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User document
 * Provides data access operations for user accounts
 */
@Repository
public interface UserRepository extends MongoRepository<User, String> {

    /**
     * Find user by normalized email address
     * 
     * @param email Normalized email (lowercase, trimmed)
     * @return Optional containing user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by normalized mobile number
     * 
     * @param mobile Normalized mobile (digits only with country code)
     * @return Optional containing user if found
     */
    Optional<User> findByMobile(String mobile);

    /**
     * Check if email address already exists
     * 
     * @param email Normalized email
     * @return true if email exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Check if mobile number already exists
     * 
     * @param mobile Normalized mobile number
     * @return true if mobile exists, false otherwise
     */
    boolean existsByMobile(String mobile);

    /**
     * Find all users with a specific role
     * 
     * @param role User role
     * @return List of users with the specified role
     */
    List<User> findByRole(UserRole role);

    /**
     * Find all users with a specific account status
     * 
     * @param status Account status
     * @return List of users with the specified status
     */
    List<User> findByStatus(AccountStatus status);

    /**
     * Find users by role and status
     * 
     * @param role User role
     * @param status Account status
     * @return List of users matching both criteria
     */
    List<User> findByRoleAndStatus(UserRole role, AccountStatus status);

    /**
     * Count users by role
     * 
     * @param role User role
     * @return Number of users with the specified role
     */
    long countByRole(UserRole role);

    /**
     * Count users by status
     * 
     * @param status Account status
     * @return Number of users with the specified status
     */
    long countByStatus(AccountStatus status);
}
