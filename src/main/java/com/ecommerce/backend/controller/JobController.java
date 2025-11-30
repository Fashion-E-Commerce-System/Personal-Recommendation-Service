package com.ecommerce.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobLauncher jobLauncher;

    @Qualifier("generateRecommendationsJob")
    private final Job generateRecommendationsJob;

    @GetMapping("/recommendations")
    public ResponseEntity<String> launchRecommendationJob() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(generateRecommendationsJob, jobParameters);
            return ResponseEntity.ok("Recommendation generation job started.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error starting job: " + e.getMessage());
        }
    }
}
