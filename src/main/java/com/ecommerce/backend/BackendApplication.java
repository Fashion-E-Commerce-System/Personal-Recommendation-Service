package com.ecommerce.backend;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableJpaAuditing
@EnableBatchProcessing
@SpringBootApplication
public class BackendApplication {
    
	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

}
