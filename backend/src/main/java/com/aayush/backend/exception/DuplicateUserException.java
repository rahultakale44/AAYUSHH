package com.aayush.backend.exception;

/**
 * Exception thrown when attempting to register a user with an email or mobile that already exists
 */
public class DuplicateUserException extends RuntimeException {
    
    public DuplicateUserException(String message) {
        super(message);
    }
    
    public DuplicateUserException(String message, Throwable cause) {
        super(message, cause);
    }
}
