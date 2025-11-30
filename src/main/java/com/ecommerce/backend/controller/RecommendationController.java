package com.ecommerce.backend.controller;

import com.ecommerce.backend.document.Recommendation;
import com.ecommerce.backend.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/{username}")
    public ResponseEntity<List<Recommendation>> getRecommendationsForUser(
            @PathVariable String username,
            @RequestParam(required = false) List<Integer> productIds) {
        List<Recommendation> recommendations = recommendationService.getRecommendationsForUser(username, productIds);
        return ResponseEntity.ok(recommendations);
    }
}
