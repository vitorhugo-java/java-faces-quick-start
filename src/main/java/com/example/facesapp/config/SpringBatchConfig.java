package com.example.facesapp.config;

import com.example.facesapp.batch.UnverifiedUserCleanupJobConfig;
import com.mysql.cj.jdbc.MysqlDataSource;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Spring {@link Configuration} class that sets up:
 * <ul>
 *   <li>A JDBC {@link DataSource} for Spring Batch metadata tables</li>
 *   <li>Spring Batch infrastructure ({@link JobRepository}, {@link JobLauncher})</li>
 *   <li>Spring Scheduling ({@link EnableScheduling})</li>
 * </ul>
 * This context is bootstrapped by {@link SpringContextListener} on application startup.
 */
@Configuration
@EnableBatchProcessing
@EnableScheduling
@Import(UnverifiedUserCleanupJobConfig.class)
@ComponentScan(basePackages = "com.example.facesapp.batch")
public class SpringBatchConfig {

    private final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

    @Bean
    public DataSource batchDataSource() {
        MysqlDataSource ds = new MysqlDataSource();
        ds.setURL(dotenv.get("DB_URL",
                "jdbc:mysql://localhost:3306/facesapp?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"));
        try {
            ds.setUser(dotenv.get("DB_USERNAME", "facesapp_user"));
            ds.setPassword(dotenv.get("DB_PASSWORD", ""));
        } catch (Exception e) {
            throw new RuntimeException("Cannot configure batch DataSource", e);
        }
        return ds;
    }

    @Bean
    public PlatformTransactionManager batchTransactionManager(DataSource batchDataSource) {
        return new DataSourceTransactionManager(batchDataSource);
    }

    @Bean
    public JobRepository jobRepository(DataSource batchDataSource,
                                       PlatformTransactionManager batchTransactionManager) throws Exception {
        JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
        factory.setDataSource(batchDataSource);
        factory.setTransactionManager(batchTransactionManager);
        factory.setDatabaseType("MYSQL");
        factory.afterPropertiesSet();
        return factory.getObject();
    }

    @Bean
    public JobLauncher jobLauncher(JobRepository jobRepository) throws Exception {
        TaskExecutorJobLauncher launcher = new TaskExecutorJobLauncher();
        launcher.setJobRepository(jobRepository);
        launcher.afterPropertiesSet();
        return launcher;
    }
}
