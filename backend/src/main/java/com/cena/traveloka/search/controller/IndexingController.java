package com.cena.traveloka.search.controller;

import com.cena.traveloka.search.service.IndexingService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/search/admin/indexing")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class IndexingController {

    IndexingService indexingService;

    @PostMapping("/property/{propertyId}")
    public ResponseEntity<Void> indexProperty(@PathVariable UUID propertyId) {
        log.info("Request to index property: {}", propertyId);

        try {
            // In a real implementation, would fetch Property from PropertyService
            // For now, this would trigger indexing if the property exists
            log.warn("Property indexing endpoint called but Property entity fetch not implemented");
            return ResponseEntity.accepted().build();

        } catch (Exception e) {
            log.error("Failed to index property: {}", propertyId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/property/{propertyId}/reindex")
    public ResponseEntity<Void> reindexProperty(@PathVariable UUID propertyId) {
        log.info("Request to reindex property: {}", propertyId);

        try {
            // In a real implementation, would fetch Property from PropertyService
            // For now, this would trigger reindexing if the property exists
            log.warn("Property reindexing endpoint called but Property entity fetch not implemented");
            return ResponseEntity.accepted().build();

        } catch (Exception e) {
            log.error("Failed to reindex property: {}", propertyId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/property/{propertyId}")
    public ResponseEntity<Void> removeFromIndex(@PathVariable UUID propertyId) {
        log.info("Request to remove property from index: {}", propertyId);

        try {
            indexingService.removeFromIndex(propertyId);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Failed to remove property from index: {}", propertyId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/refresh-all")
    public ResponseEntity<Void> refreshAllIndices() {
        log.info("Request to refresh all search indices");

        try {
            indexingService.refreshAllIndices();
            return ResponseEntity.accepted().build();

        } catch (Exception e) {
            log.error("Failed to refresh all indices", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getIndexingStatus() {
        try {
            long indexedCount = indexingService.getIndexedPropertyCount();

            Map<String, Object> status = Map.of(
                "indexedPropertiesCount", indexedCount,
                "indexingService", "active",
                "lastUpdated", System.currentTimeMillis()
            );

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            log.error("Failed to get indexing status", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/property/{propertyId}/status")
    public ResponseEntity<Map<String, Object>> getPropertyIndexStatus(@PathVariable UUID propertyId) {
        try {
            boolean isIndexed = indexingService.isIndexed(propertyId);

            Map<String, Object> status = Map.of(
                "propertyId", propertyId,
                "isIndexed", isIndexed,
                "checkedAt", System.currentTimeMillis()
            );

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            log.error("Failed to check property index status for: {}", propertyId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/bulk/properties")
    public ResponseEntity<Void> bulkIndexProperties(
            @RequestBody BulkIndexingRequest request) {

        log.info("Request to bulk index {} properties",
                request.getPropertyIds() != null ? request.getPropertyIds().size() : 0);

        try {
            if (request.getPropertyIds() == null || request.getPropertyIds().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            if (request.getPropertyIds().size() > 1000) {
                return ResponseEntity.badRequest().build(); // Limit batch size
            }

            // In a real implementation, would fetch Properties from PropertyService
            // For now, this would trigger bulk indexing if the properties exist
            log.warn("Bulk property indexing endpoint called but Property entity fetch not implemented");
            return ResponseEntity.accepted().build();

        } catch (Exception e) {
            log.error("Failed to bulk index properties", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // DTO for bulk indexing request
    public static class BulkIndexingRequest {
        private java.util.List<UUID> propertyIds;

        public java.util.List<UUID> getPropertyIds() {
            return propertyIds;
        }

        public void setPropertyIds(java.util.List<UUID> propertyIds) {
            this.propertyIds = propertyIds;
        }
    }
}