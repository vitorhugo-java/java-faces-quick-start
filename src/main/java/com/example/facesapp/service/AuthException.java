package com.example.facesapp.service;

/**
 * Thrown when authentication fails (bad credentials, unverified account, etc.).
 */
public class AuthException extends RuntimeException {

    public AuthException(String message) {
        super(message);
    }

    public AuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
