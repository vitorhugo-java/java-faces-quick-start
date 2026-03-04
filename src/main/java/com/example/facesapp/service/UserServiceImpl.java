package com.example.facesapp.service;

import com.example.facesapp.config.EnvConfig;
import com.example.facesapp.model.PasswordResetToken;
import com.example.facesapp.model.User;
import com.example.facesapp.model.VerificationToken;
import com.example.facesapp.repository.UserRepository;
import com.example.facesapp.util.PasswordUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Default implementation of {@link UserService}.
 * <p>
 * Registration flow:
 * <ol>
 *   <li>Validate that the e-mail is not already taken.</li>
 *   <li>Hash the password with BCrypt.</li>
 *   <li>Persist the {@link User}.</li>
 *   <li>If {@code EMAIL_VERIFICATION_REQUIRED=true}, create a {@link VerificationToken}
 *       and send the verification e-mail.</li>
 *   <li>Otherwise mark the account as already verified.</li>
 * </ol>
 */
@ApplicationScoped
public class UserServiceImpl implements UserService {

    @Inject
    private UserRepository userRepo;

    @Inject
    private EmailService emailService;

    @Inject
    private EnvConfig env;

    // ── register ──────────────────────────────────────────────────────────────

    @Override
    public User register(String name, String email, String rawPassword) {
        if (userRepo.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("E-mail already registered: " + email);
        }

        User user = new User(name, email, PasswordUtil.hash(rawPassword));

        if (!env.isEmailVerificationRequired()) {
            // No verification needed – account is immediately active.
            user.setEmailVerified(true);
        }

        userRepo.save(user);

        if (env.isEmailVerificationRequired()) {
            VerificationToken token = new VerificationToken(user);
            userRepo.saveVerificationToken(token);
            emailService.sendVerificationEmail(user, token.getToken());
        }

        return user;
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Override
    public User login(String email, String rawPassword) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new AuthException("Invalid e-mail or password."));

        if (!PasswordUtil.matches(rawPassword, user.getPasswordHash())) {
            throw new AuthException("Invalid e-mail or password.");
        }

        if (env.isEmailVerificationRequired() && !user.isEmailVerified()) {
            throw new AuthException(
                    "Please verify your e-mail address before logging in. "
                    + "Check your inbox for the verification link.");
        }

        return user;
    }

    // ── verifyEmail ───────────────────────────────────────────────────────────

    @Override
    public void verifyEmail(String token) {
        VerificationToken vt = userRepo.findVerificationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification token."));

        if (vt.isUsed()) {
            throw new IllegalStateException("Token already used.");
        }
        if (vt.isExpired()) {
            throw new IllegalStateException("Verification token has expired.");
        }

        User user = vt.getUser();
        user.setEmailVerified(true);
        userRepo.save(user);

        vt.setUsed(true);
        userRepo.updateVerificationToken(vt);
    }

    // ── requestPasswordReset ──────────────────────────────────────────────────

    @Override
    public void requestPasswordReset(String email) {
        // Don't reveal whether the e-mail exists (prevents enumeration attacks).
        userRepo.findByEmail(email).ifPresent(user -> {
            PasswordResetToken token = new PasswordResetToken(user);
            userRepo.savePasswordResetToken(token);
            emailService.sendPasswordResetEmail(user, token.getToken());
        });
    }

    // ── resetPassword ─────────────────────────────────────────────────────────

    @Override
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken prt = userRepo.findPasswordResetToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token."));

        if (prt.isUsed()) {
            throw new IllegalStateException("This reset link has already been used.");
        }
        if (prt.isExpired()) {
            throw new IllegalStateException("The reset link has expired. Please request a new one.");
        }

        User user = prt.getUser();
        user.setPasswordHash(PasswordUtil.hash(newPassword));
        userRepo.save(user);

        prt.setUsed(true);
        userRepo.updatePasswordResetToken(prt);
    }
}
