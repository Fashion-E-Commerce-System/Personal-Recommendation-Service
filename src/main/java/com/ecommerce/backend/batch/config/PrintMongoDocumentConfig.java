package com.ecommerce.backend.batch.config;

import com.ecommerce.backend.batch.MongoReaderTasklet;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class PrintMongoDocumentConfig {
    private final MongoReaderTasklet mongoReaderTasklet;

    @Bean
    public Job printMongoDocumentJob(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new JobBuilder("printMongoDocumentJob", jobRepository)
                .start(printMongoDocumentStep(jobRepository, transactionManager))
                .build();
    }

    @Bean
    public Step printMongoDocumentStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("printMongoDocumentStep", jobRepository)
                .tasklet(mongoReaderTasklet, transactionManager)
                .build();
    }
}
