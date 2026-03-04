package com.example.facesapp.service;

import com.example.facesapp.model.User;

/**
 * Defines the contract for all user-management operations.
 */
public interface UserService {

    /**
     * Registers a new user and, when email-verification is enabled, sends
     * a verification e-mail.
     *
     * @return the persisted {@link User}
     * @throws IllegalArgumentException if the e-mail is already taken
     */
    User register(String name, String email, String rawPassword);

    /**
     * Attempts to authenticate a user.
     *
     * @return the authenticated {@link User}
     * @throws jakarta.security.enterprise.AuthException equivalent – use
     *         {@link com.example.facesapp.service.AuthException} for callers.
     */
    User login(String email, String rawPassword);

    /**
     * Verifies the user's e-mail address using the supplied token.
     */
    void verifyEmail(String token);

    /**
     * Initiates a password-reset flow by sending a reset link via e-mail.
     */
    void requestPasswordReset(String email);

    /**
     * Completes a password reset using the supplied token and new password.
     */
    void resetPassword(String token, String newPassword);
}
