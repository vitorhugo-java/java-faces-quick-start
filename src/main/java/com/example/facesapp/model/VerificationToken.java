package com.example.facesapp.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One-time token used to confirm a user's e-mail address.
 * The token expires 24 hours after creation.
 */
@Entity
@Table(name = "verification_tokens", indexes = {
        @Index(name = "idx_vt_token", columnList = "token"),
        @Index(name = "idx_vt_user",  columnList = "user_id")
})
@NamedQuery(name = VerificationToken.FIND_BY_TOKEN,
            query = "SELECT t FROM VerificationToken t WHERE t.token = :token")
public class VerificationToken implements Serializable {

    public static final String FIND_BY_TOKEN = "VerificationToken.findByToken";
    public static final long   EXPIRY_HOURS   = 24L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    private String token;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_vt_user"))
    private User user;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used", nullable = false)
    private boolean used = false;

    public VerificationToken() {
    }

    public VerificationToken(User user) {
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
