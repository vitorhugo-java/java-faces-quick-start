package com.example.facesapp.batch;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerContext;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Quartz {@link Job} that removes unverified user accounts created more than
 * 24 hours ago.
 *
 * <p>JDBC connection details are read from the Quartz {@link SchedulerContext}
 * (populated by {@link com.example.facesapp.config.QuartzSchedulerListener}
 * at application startup).
 *
 * <p>Deletes in FK order:
 * {@code verification_tokens} → {@code password_reset_tokens} → {@code users}
 */
public class UnverifiedUserCleanupJob implements Job {

    private static final Logger LOG = Logger.getLogger(UnverifiedUserCleanupJob.class.getName());

    private static final String SQL_DEL_VT =
            "DELETE vt FROM verification_tokens vt " +
            "INNER JOIN users u ON vt.user_id = u.id " +
            "WHERE u.email_verified = false AND u.created_at < ?";

    private static final String SQL_DEL_PRT =
            "DELETE prt FROM password_reset_tokens prt " +
            "INNER JOIN users u ON prt.user_id = u.id " +
            "WHERE u.email_verified = false AND u.created_at < ?";

    private static final String SQL_DEL_USERS =
            "DELETE FROM users WHERE email_verified = false AND created_at < ?";

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            SchedulerContext sc  = context.getScheduler().getContext();
            String url  = (String) sc.get("DB_URL");
            String user = (String) sc.get("DB_USERNAME");
            String pass = (String) sc.get("DB_PASSWORD");

            Timestamp cutoff = Timestamp.valueOf(LocalDateTime.now().minusHours(24));

            try (Connection conn = DriverManager.getConnection(url, user, pass)) {
                conn.setAutoCommit(false);
                try {
                    int vtDeleted   = executeUpdate(conn, SQL_DEL_VT,    cutoff);
                    int prtDeleted  = executeUpdate(conn, SQL_DEL_PRT,   cutoff);
                    int userDeleted = executeUpdate(conn, SQL_DEL_USERS, cutoff);
                    conn.commit();
                    LOG.info(String.format(
                            "Cleanup job: removed %d unverified users " +
                            "(+ %d verification tokens, %d reset tokens)",
                            userDeleted, vtDeleted, prtDeleted));
                } catch (Exception ex) {
                    conn.rollback();
                    throw ex;
                }
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Unverified-user cleanup job failed", e);
            throw new JobExecutionException("Cleanup job failed", e, false);
        }
    }

    private int executeUpdate(Connection conn, String sql, Timestamp param) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, param);
            return ps.executeUpdate();
        }
    }
}
