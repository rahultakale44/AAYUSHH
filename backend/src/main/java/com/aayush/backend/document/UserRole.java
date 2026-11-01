package com.aayush.backend.document;

/**
 * User Role Enumeration
 * Defines the different types of users in the AAYUSH system
 */
public enum UserRole {
    /**
     * Senior citizen who can request emergency assistance
     */
    SENIOR,
    
    /**
     * Family member or caregiver who can assist senior citizens
     */
    CAREGIVER,
    
    /**
     * System administrator with full access
     * Note: Cannot be assigned through self-registration
     */
    ADMIN
}
