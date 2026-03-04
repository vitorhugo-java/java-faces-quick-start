package com.example.facesapp.batch;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.logging.Logger;

/**
 * Spring Batch {@link Tasklet} that removes unverified user accounts
 * created more than 24 hours ago.
 *
 * <p>Deletes in the correct foreign-key order:
 * verification_tokens → password_reset_tokens → users
 */
public class UnverifiedUserDeleteTasklet implements Tasklet {

    private static final Logger LOG = Logger.getLogger(UnverifiedUserDeleteTasklet.class.getName());

    private final JdbcTemplate jdbc;

    public UnverifiedUserDeleteTasklet(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);

        // Remove linked tokens first (FK constraints)
        int vtDeleted = jdbc.update("""
                DELETE vt FROM verification_tokens vt
                INNER JOIN users u ON vt.user_id = u.id
                WHERE u.email_verified = false AND u.created_at < ?
                """, cutoff);

        int prtDeleted = jdbc.update("""
                DELETE prt FROM password_reset_tokens prt
                INNER JOIN users u ON prt.user_id = u.id
                WHERE u.email_verified = false AND u.created_at < ?
                """, cutoff);

        int usersDeleted = jdbc.update("""
                DELETE FROM users
                WHERE email_verified = false AND created_at < ?
                """, cutoff);

        LOG.info(String.format(
                "Cleanup job: removed %d unverified users (+ %d verification tokens, %d reset tokens)",
                usersDeleted, vtDeleted, prtDeleted));

        return RepeatStatus.FINISHED;
    }
}
