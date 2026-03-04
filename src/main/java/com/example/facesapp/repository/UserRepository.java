package com.example.facesapp.repository;

import com.example.facesapp.config.EnvConfig;
import com.example.facesapp.model.PasswordResetToken;
import com.example.facesapp.model.User;
import com.example.facesapp.model.VerificationToken;
import org.flywaydb.core.Flyway;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Data-access layer for all user-related entities.
 * Uses a {@link RESOURCE_LOCAL} JPA persistence unit whose JDBC connection
 * details are supplied at runtime via the {@link EnvConfig} (.env) bean.
 */
@ApplicationScoped
public class UserRepository {

    @Inject
    private EnvConfig env;

    private EntityManagerFactory emf;

    @PostConstruct
    public void init() {
        Flyway.configure()
            .dataSource(env.getDbUrl(), env.getDbUsername(), env.getDbPassword())
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .baselineVersion("1")
            .load()
            .migrate();

        java.util.Map<String, Object> props = new java.util.HashMap<>();
        props.put("jakarta.persistence.jdbc.url",      env.getDbUrl());
        props.put("jakarta.persistence.jdbc.user",     env.getDbUsername());
        props.put("jakarta.persistence.jdbc.password", env.getDbPassword());
        emf = Persistence.createEntityManagerFactory("FacesAppPU", props);
    }

    @PreDestroy
    public void destroy() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    // ── helper ───────────────────────────────────────────────────────────────

    private EntityManager em() {
        return emf.createEntityManager();
    }

    // ── User CRUD ─────────────────────────────────────────────────────────────

    public Optional<User> findByEmail(String email) {
        EntityManager em = em();
        try {
            List<User> results = em.createNamedQuery(User.FIND_BY_EMAIL, User.class)
                    .setParameter("email", email)
                    .getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } finally {
            em.close();
        }
    }

    public User findById(Long id) {
        EntityManager em = em();
        try {
            return em.find(User.class, id);
        } finally {
            em.close();
        }
    }

    public User save(User user) {
        EntityManager em = em();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            User saved;
            if (user.getId() == null) {
                em.persist(user);
                saved = user;
            } else {
                saved = em.merge(user);
            }
            tx.commit();
            return saved;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public void delete(User user) {
        EntityManager em = em();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            User managed = em.contains(user) ? user : em.merge(user);
            em.remove(managed);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    /** Returns users whose email has never been verified and who were created before {@code cutoff}. */
    public List<User> findUnverifiedBefore(LocalDateTime cutoff) {
        EntityManager em = em();
        try {
            return em.createNamedQuery(User.FIND_UNVERIFIED_BEFORE, User.class)
                    .setParameter("cutoff", cutoff)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // ── VerificationToken ─────────────────────────────────────────────────────

    public VerificationToken saveVerificationToken(VerificationToken token) {
        EntityManager em = em();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(token);
            tx.commit();
            return token;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public Optional<VerificationToken> findVerificationToken(String token) {
        EntityManager em = em();
        try {
            List<VerificationToken> results =
                    em.createNamedQuery(VerificationToken.FIND_BY_TOKEN, VerificationToken.class)
                      .setParameter("token", token)
                      .getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } finally {
            em.close();
        }
    }

    public void updateVerificationToken(VerificationToken token) {
        EntityManager em = em();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.merge(token);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    // ── PasswordResetToken ────────────────────────────────────────────────────

    public PasswordResetToken savePasswordResetToken(PasswordResetToken token) {
        EntityManager em = em();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(token);
            tx.commit();
            return token;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public Optional<PasswordResetToken> findPasswordResetToken(String token) {
        EntityManager em = em();
        try {
            List<PasswordResetToken> results =
                    em.createNamedQuery(PasswordResetToken.FIND_BY_TOKEN, PasswordResetToken.class)
                      .setParameter("token", token)
                      .getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } finally {
            em.close();
        }
    }

    public void updatePasswordResetToken(PasswordResetToken token) {
        EntityManager em = em();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.merge(token);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }
}
