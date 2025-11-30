package com.ecommerce.backend.config;

import com.ecommerce.backend.batch.GenerateRecommendationsTasklet;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties; // 새 import 구문
import org.springframework.boot.jdbc.init.DataSourceScriptDatabaseInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.boot.sql.init.DatabaseInitializationSettings;

import javax.sql.DataSource;
import java.util.Collections;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(BatchProperties.class) // BatchProperties 활성화
public class BatchConfig {

    private final GenerateRecommendationsTasklet generateRecommendationsTasklet;

    @Bean
    public Job generateRecommendationsJob(JobRepository jobRepository, Step generateRecommendationsStep) {
        return new JobBuilder("generateRecommendationsJob", jobRepository)
                .start(generateRecommendationsStep)
                .build();
    }

    @Bean
    public Step generateRecommendationsStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("generateRecommendationsStep", jobRepository)
                .tasklet(generateRecommendationsTasklet, transactionManager)
                .build();
    }

    @Bean
    public DataSourceScriptDatabaseInitializer batchDatabaseInitializer(
            DataSource dataSource,
            BatchProperties properties) {

        DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
        settings.setSchemaLocations(Collections.singletonList(properties.getJdbc().getSchema()));
        settings.setMode(properties.getJdbc().getInitializeSchema());

        return new DataSourceScriptDatabaseInitializer(dataSource, settings);
    }
}