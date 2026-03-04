package com.example.facesapp.repository;

import com.example.facesapp.config.JpaConfig;
import com.example.facesapp.model.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for User entity using Hibernate with clean repository pattern.
 * Provides type-safe queries with automatic session management.
 */
@ApplicationScoped
public class UserDataRepository {

    @Inject
    private JpaConfig jpaConfig;

    private EntityManager em() {
        return jpaConfig.getEntityManagerFactory().createEntityManager();
    }

    /**
     * Find a user by their email address.
     */
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

    /**
     * Find a user by ID.
     */
    public Optional<User> findById(Long id) {
        EntityManager em = em();
        try {
            User user = em.find(User.class, id);
            return Optional.ofNullable(user);
        } finally {
            em.close();
        }
    }

    /**
     * Save or update a user.
     */
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

    /**
     * Delete a user.
     */
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

    /**
     * Find users whose email has never been verified and who were created before the cutoff date.
     */
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
}
