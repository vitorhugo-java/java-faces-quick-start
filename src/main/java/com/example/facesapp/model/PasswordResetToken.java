package com.example.facesapp.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Token issued when a user requests a password reset.
 * Valid for 1 hour.
 */
@Entity
@Table(name = "password_reset_tokens", indexes = {
        @Index(name = "idx_prt_token",   columnList = "token"),
        @Index(name = "idx_prt_user_id", columnList = "user_id")
})
@NamedQuery(name = PasswordResetToken.FIND_BY_TOKEN,
            query = "SELECT t FROM PasswordResetToken t WHERE t.token = :token")
public class PasswordResetToken implements Serializable {

    public static final String FIND_BY_TOKEN = "PasswordResetToken.findByToken";
    public static final long   EXPIRY_HOURS   = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    private String token;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_prt_user"))
    private User user;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used", nullable = false)
    private boolean used = false;

    public PasswordResetToken() {
    }

    public PasswordResetToken(User user) {
        this.user      = user;
        this.token     = UUID.randomUUID().toString();
        this.expiresAt = LocalDateTime.now().plusHours(EXPIRY_HOURS);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public String getToken() { return token; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDateTime getExpiresAt() { return expiresAt; }

    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }
}
