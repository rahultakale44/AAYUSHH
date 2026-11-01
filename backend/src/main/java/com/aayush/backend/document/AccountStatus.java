package com.aayush.backend.document;

/**
 * Account Status Enumeration
 * Defines the various states an account can be in
 */
public enum AccountStatus {
    /**
     * Account is active and user can authenticate
     */
    ACTIVE,
    
    /**
     * Account is temporarily suspended (admin action)
     */
    SUSPENDED,
    
    /**
     * Account has been soft-deleted
     */
    DELETED
}
