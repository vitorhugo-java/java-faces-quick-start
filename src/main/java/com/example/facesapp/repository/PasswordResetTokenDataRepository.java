package com.example.facesapp.repository;

import com.example.facesapp.config.JpaConfig;
import com.example.facesapp.model.PasswordResetToken;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.util.List;
import java.util.Optional;

/**
 * Repository for PasswordResetToken entity using Hibernate.
 */
@ApplicationScoped
public class PasswordResetTokenDataRepository {

    @Inject
    private JpaConfig jpaConfig;

    private EntityManager em() {
        return jpaConfig.getEntityManagerFactory().createEntityManager();
    }

    /**
     * Find a password reset token by its token string.
     */
    public Optional<PasswordResetToken> findByToken(String token) {
        EntityManager em = em();
        try {
            List<PasswordResetToken> results = em.createNamedQuery(PasswordResetToken.FIND_BY_TOKEN, PasswordResetToken.class)
                    .setParameter("token", token)
                    .getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } finally {
            em.close();
        }
    }

    /**
     * Save or update a password reset token.
     */
    public PasswordResetToken save(PasswordResetToken token) {
        EntityManager em = em();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            PasswordResetToken saved;
            if (token.getId() == null) {
                em.persist(token);
                saved = token;
            } else {
                saved = em.merge(token);
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
}
