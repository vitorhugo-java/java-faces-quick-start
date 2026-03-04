package com.example.facesapp.service;

import com.example.facesapp.model.User;

/**
 * Contract for all outbound e-mail operations.
 */
public interface EmailService {

    /** Sends a "verify your e-mail" message with the given token. */
    void sendVerificationEmail(User user, String token);

    /** Sends a password-reset link containing the given token. */
    void sendPasswordResetEmail(User user, String token);
}
