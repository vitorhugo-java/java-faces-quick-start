package com.example.facesapp.config;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.CDI;

/**
 * Loads variables from the .env file (if present) and exposes them
 * as convenient getters.  Falls back to system/OS environment variables
 * when the .env file is absent (e.g. in a containerised CI environment).
 */
@ApplicationScoped
public class EnvConfig {

    private static final String DEFAULT_DB_URL = "jdbc:mysql://localhost:3306/facesapp?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String DEFAULT_DB_USERNAME = "root";
    private static final String DEFAULT_DB_PASSWORD = "root";

    private Dotenv dotenv;

    @PostConstruct
    public void init() {
        dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();
    }

    public String get(String key) {
        String value = dotenv != null ? dotenv.get(key) : null;
        if (value != null && !value.isBlank()) {
            return value;
        }

        value = System.getenv(key);
        if (value != null && !value.isBlank()) {
            return value;
        }

        value = System.getProperty(key);
        return (value != null && !value.isBlank()) ? value : null;
    }

    public String get(String key, String defaultValue) {
        String val = get(key);
        return (val != null && !val.isBlank()) ? val : defaultValue;
    }

    // ── convenience accessors ────────────────────────────────────────────────

    public String getDbUrl() {
        return get("DB_URL", DEFAULT_DB_URL);
    }

    public String getDbUsername() {
        return get("DB_USERNAME", DEFAULT_DB_USERNAME);
    }

    public String getDbPassword() {
        return get("DB_PASSWORD", DEFAULT_DB_PASSWORD);
    }

    public String getAppBaseUrl() {
        return get("APP_BASE_URL", "http://localhost:8080/facesapp");
    }

    public String getAppSecretKey() {
        return get("APP_SECRET_KEY", "default-secret-key-change-me");
    }

    public String getMailHost() {
        return get("MAIL_HOST", "smtp.gmail.com");
    }

    public int getMailPort() {
        return Integer.parseInt(get("MAIL_PORT", "587"));
    }

    public String getMailUsername() {
        return get("MAIL_USERNAME");
    }

    public String getMailPassword() {
        return get("MAIL_PASSWORD");
    }

    public String getMailFrom() {
        return get("MAIL_FROM", "no-reply@example.com");
    }

    public boolean isMailStartTlsEnabled() {
        return Boolean.parseBoolean(get("MAIL_STARTTLS_ENABLE", "true"));
    }

    public boolean isEmailVerificationRequired() {
        return Boolean.parseBoolean(get("EMAIL_VERIFICATION_REQUIRED", "true"));
    }

    public String getCleanupCron() {
        return get("CLEANUP_CRON", "0 0 3 * * *");
    }

    // ── static accessor for use outside CDI (e.g. Spring beans) ─────────────

    public static EnvConfig instance() {
        return CDI.current().select(EnvConfig.class).get();
    }
}
