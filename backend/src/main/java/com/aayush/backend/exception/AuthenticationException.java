package com.aayush.backend.exception;

/**
 * Exception thrown when authentication fails
 * 
 * This includes:
 * - Invalid credentials (wrong password)
 * - Unknown identifier (email/mobile not found)
 * - Account suspended or deleted
 * - Invalid or expired tokens
 * 
 * Security Note:
 * Error messages should be generic to prevent information disclosure
 * Do not reveal whether an email/mobile exists or whether password was wrong
 */
public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
