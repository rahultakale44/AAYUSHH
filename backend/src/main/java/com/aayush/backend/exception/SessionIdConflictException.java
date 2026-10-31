package com.aayush.backend.exception;

/**
 * Exception thrown when unable to generate unique session ID
 */
public class SessionIdConflictException extends RuntimeException {
    
    public SessionIdConflictException(String message) {
        super(message);
    }
    
    public SessionIdConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
