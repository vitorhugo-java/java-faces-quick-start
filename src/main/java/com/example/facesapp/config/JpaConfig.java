package com.example.facesapp.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.flywaydb.core.Flyway;

import java.util.HashMap;
import java.util.Map;

/**
 * Configures JPA EntityManagerFactory for the application.
 * Runs Flyway migrations on startup and provides EntityManager for Jakarta Data Repositories.
 */
@ApplicationScoped
public class JpaConfig {

    @Inject
    private EnvConfig env;

    private EntityManagerFactory emf;

    @PostConstruct
    public void init() {
        // Run Flyway migrations first
        Flyway.configure()
            .dataSource(env.getDbUrl(), env.getDbUsername(), env.getDbPassword())
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .baselineVersion("1")
            .load()
            .migrate();

        // Create EntityManagerFactory with runtime configuration
        Map<String, Object> props = new HashMap<>();
        props.put("jakarta.persistence.jdbc.url", env.getDbUrl());
        props.put("jakarta.persistence.jdbc.user", env.getDbUsername());
        props.put("jakarta.persistence.jdbc.password", env.getDbPassword());
        emf = Persistence.createEntityManagerFactory("FacesAppPU", props);
    }

    @PreDestroy
    public void destroy() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    /**
     * Produces EntityManagerFactory for Jakarta Data Repositories.
     */
    @Produces
    @ApplicationScoped
    public EntityManagerFactory getEntityManagerFactory() {
        return emf;
    }

    /**
     * Produces EntityManager instances for injection.
     * Note: For Jakarta Data Repositories, the EntityManagerFactory is injected instead.
     */
    @Produces
    public EntityManager createEntityManager() {
        return emf.createEntityManager();
    }
}
