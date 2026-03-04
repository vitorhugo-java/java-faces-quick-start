package com.example.facesapp.repository;

import com.example.facesapp.config.JpaConfig;
import com.example.facesapp.model.VerificationToken;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.util.List;
import java.util.Optional;

/**
 * Repository for VerificationToken entity using Hibernate.
 */
@ApplicationScoped
public class VerificationTokenDataRepository {

    @Inject
    private JpaConfig jpaConfig;

    private EntityManager em() {
        return jpaConfig.getEntityManagerFactory().createEntityManager();
    }

    /**
     * Find a verification token by its token string.
     */
    public Optional<VerificationToken> findByToken(String token) {
        EntityManager em = em();
        try {
            List<VerificationToken> results = em.createNamedQuery(VerificationToken.FIND_BY_TOKEN, VerificationToken.class)
                    .setParameter("token", token)
                    .getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } finally {
            em.close();
        }
    }

    /**
     * Save or update a verification token.
     */
    public VerificationToken save(VerificationToken token) {
        EntityManager em = em();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            VerificationToken saved;
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
