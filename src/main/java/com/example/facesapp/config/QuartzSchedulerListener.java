package com.example.facesapp.config;

import com.example.facesapp.batch.UnverifiedUserCleanupJob;
import io.github.cdimascio.dotenv.Dotenv;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Jakarta EE {@link ServletContextListener} that initialises and tears down
 * the Quartz {@link Scheduler}.
 *
 * <p>On startup it:
 * <ol>
 *   <li>Reads DB credentials and the {@code CLEANUP_CRON} expression from the
 *       {@code .env} file (via dotenv-java).</li>
 *   <li>Stores the credentials in the {@link SchedulerContext} so the
 *       {@link UnverifiedUserCleanupJob} can open a JDBC connection without
 *       depending on Spring or CDI.</li>
 *   <li>Schedules {@link UnverifiedUserCleanupJob} with the supplied cron
 *       expression (default: {@code 0 0 3 * * ?} – every day at 03:00).</li>
 *   <li>Starts the scheduler.</li>
 * </ol>
 *
 * <p><strong>Quartz cron format</strong>: 6 or 7 fields –
 * {@code second minute hour day-of-month month day-of-week [year]}.
 * Use {@code ?} for whichever of day-of-month / day-of-week is not specified.
 * Example: {@code 0 0 3 * * ?}
 */
@WebListener
public class QuartzSchedulerListener implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(QuartzSchedulerListener.class.getName());

    /** Default cron: every day at 03:00:00 in Quartz format. */
    private static final String DEFAULT_CRON = "0 0 3 * * ?";

    private Scheduler scheduler;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        String dbUrl    = dotenv.get("DB_URL",
                "jdbc:mysql://localhost:3306/facesapp?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
        String dbUser   = dotenv.get("DB_USERNAME", "facesapp_user");
        String dbPass   = dotenv.get("DB_PASSWORD", "");
        String cronExpr = dotenv.get("CLEANUP_CRON", DEFAULT_CRON);

        try {
            scheduler = new StdSchedulerFactory().getScheduler();

            // Expose DB credentials to jobs via SchedulerContext (no Spring needed)
            scheduler.getContext().put("DB_URL",      dbUrl);
            scheduler.getContext().put("DB_USERNAME",  dbUser);
            scheduler.getContext().put("DB_PASSWORD",  dbPass);

            JobDetail job = JobBuilder.newJob(UnverifiedUserCleanupJob.class)
                    .withIdentity("unverifiedUserCleanup", "maintenance")
                    .build();

            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("cleanupTrigger", "maintenance")
                    .withSchedule(CronScheduleBuilder.cronSchedule(cronExpr))
                    .build();

            scheduler.scheduleJob(job, trigger);
            scheduler.start();

            LOG.info("Quartz scheduler started. Cleanup cron: " + cronExpr);
        } catch (SchedulerException e) {
            LOG.log(Level.SEVERE, "Failed to start Quartz scheduler", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (scheduler != null) {
            try {
                scheduler.shutdown(true);
                LOG.info("Quartz scheduler stopped.");
            } catch (SchedulerException e) {
                LOG.log(Level.WARNING, "Error shutting down Quartz scheduler", e);
            }
        }
    }
}
