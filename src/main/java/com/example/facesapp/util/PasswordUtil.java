package com.example.facesapp.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Thin wrapper around Spring Security's BCrypt encoder.
 * All password-hashing logic lives here so it is easy to swap algorithms.
 */
public final class PasswordUtil {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder(12);

    private PasswordUtil() {
    }

    public static String hash(String rawPassword) {
        return ENCODER.encode(rawPassword);
    }

    public static boolean matches(String rawPassword, String storedHash) {
        return ENCODER.matches(rawPassword, storedHash);
    }
}
