package com.example.facesapp.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Spring-managed scheduler that fires the unverified-user cleanup batch job
 * every day at 03:00 (server local time).
 *
 * <p>The cron expression can be overridden via the {@code CLEANUP_CRON}
 * environment variable / .env entry.
 */
@Component
public class UnverifiedUserCleanupScheduler {

    private static final Logger LOG = Logger.getLogger(UnverifiedUserCleanupScheduler.class.getName());

    private final JobLauncher jobLauncher;
    private final Job         cleanupJob;

    @Autowired
    public UnverifiedUserCleanupScheduler(JobLauncher jobLauncher,
                                          @Qualifier("unverifiedUserCleanupJob") Job cleanupJob) {
        this.jobLauncher = jobLauncher;
        this.cleanupJob  = cleanupJob;
    }

    /**
     * Triggered at 03:00 every day.
     * The cron expression here is the default; production deployments should
     * set {@code CLEANUP_CRON} in the .env file.
     */
    @Scheduled(cron = "${CLEANUP_CRON:0 0 3 * * *}")
    public void runCleanupJob() {
        LOG.info("Starting unverified-user cleanup batch job…");
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("run.id", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(cleanupJob, params);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Unverified-user cleanup job failed", e);
        }
    }
}
