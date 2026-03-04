package com.example.facesapp.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Spring Batch configuration for the unverified-user cleanup job.
 * <p>
 * The job runs a single step that deletes users whose e-mail has never been
 * verified and whose account was created more than 24 hours ago.
 */
@Configuration
public class UnverifiedUserCleanupJobConfig {

    /**
     * The single-step {@link Job} that removes stale, unverified accounts.
     */
    @Bean
    public Job unverifiedUserCleanupJob(JobRepository jobRepository,
                                        Step deleteUnverifiedStep) {
        return new JobBuilder("unverifiedUserCleanupJob", jobRepository)
                .start(deleteUnverifiedStep)
                .build();
    }

    @Bean
    public Step deleteUnverifiedStep(JobRepository jobRepository,
                                     PlatformTransactionManager batchTransactionManager,
                                     DataSource batchDataSource) {
        return new StepBuilder("deleteUnverifiedStep", jobRepository)
                .tasklet(new UnverifiedUserDeleteTasklet(new JdbcTemplate(batchDataSource)),
                         batchTransactionManager)
                .build();
    }
}
