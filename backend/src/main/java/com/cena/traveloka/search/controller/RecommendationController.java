package com.cena.traveloka.search.controller;

import com.cena.traveloka.search.dto.*;
import com.cena.traveloka.search.service.RecommendationEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/search/recommendations")
@RequiredArgsConstructor
@Slf4j
public class RecommendationController {

    private final RecommendationEngineService recommendationEngineService;

    @PostMapping
    public ResponseEntity<RecommendationResponse> getRecommendations(
            @RequestBody @Valid RecommendationRequest request) {

        log.info("Recommendation request received for user: {}, session: {}",
                request.getUserId(), request.getSessionId());

        try {
            RecommendationResponse response = recommendationEngineService.generateRecommendations(request);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid recommendation request", e);
            return ResponseEntity.badRequest().build();

        } catch (Exception e) {
            log.error("Recommendation generation failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/similar/{propertyId}")
    public ResponseEntity<RecommendationResponse> getSimilarProperties(
            @PathVariable String propertyId,
            @RequestParam(required = false) String userId,
            @RequestParam(defaultValue = "5") int maxResults) {

        log.info("Similar properties request for property: {}, user: {}", propertyId, userId);

        try {
            RecommendationResponse response = recommendationEngineService.generateSimilarProperties(
                    propertyId, userId, maxResults);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Similar properties generation failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/trending")
    public ResponseEntity<RecommendationResponse> getTrendingRecommendations(
            @RequestParam(required = false) String region,
            @RequestParam(defaultValue = "10") int maxResults) {

        log.info("Trending recommendations request for region: {}", region);

        try {
            RecommendationResponse response = recommendationEngineService.generateTrendingRecommendations(
                    region, maxResults);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Trending recommendations generation failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/personalized/{userId}")
    public ResponseEntity<RecommendationResponse> getPersonalizedRecommendations(
            @PathVariable String userId,
            @RequestParam(defaultValue = "10") int maxResults) {

        log.info("Personalized recommendations request for user: {}", userId);

        try {
            RecommendationResponse response = recommendationEngineService.generatePersonalizedRecommendations(
                    userId, maxResults);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Personalized recommendations generation failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/async")
    public CompletableFuture<ResponseEntity<RecommendationResponse>> getRecommendationsAsync(
            @RequestBody @Valid RecommendationRequest request) {

        log.info("Async recommendation request received for user: {}", request.getUserId());

        return recommendationEngineService.generateRecommendationsAsync(request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(throwable -> {
                    log.error("Async recommendation generation failed", throwable);
                    return ResponseEntity.internalServerError().build();
                });
    }
}