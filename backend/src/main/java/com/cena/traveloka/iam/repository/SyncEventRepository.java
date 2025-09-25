package com.cena.traveloka.iam.repository;

import com.cena.traveloka.iam.entity.SyncEvent;
import com.cena.traveloka.iam.enums.SyncDirection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for SyncEvent entity operations
 *
 * Provides comprehensive synchronization event management including:
 * - Keycloak synchronization tracking
 * - Event retry and failure handling
 * - Synchronization analytics and monitoring
 * - Error diagnosis and resolution
 * - Performance optimization for high-volume events
 *
 * Key Features:
 * - Event lifecycle management
 * - Retry mechanism support
 * - Error tracking and analysis
 * - Performance monitoring
 * - Cleanup operations for old events
 */
@Repository
public interface SyncEventRepository extends JpaRepository<SyncEvent, String> {

    // === Event Type and Status Management ===

    /**
     * Find events by type for specific synchronization operations
     */
    @Query("SELECT se FROM SyncEvent se WHERE se.eventType = :eventType ORDER BY se.processedAt DESC")
    Page<SyncEvent> findByEventType(@Param("eventType") String eventType, Pageable pageable);

    /**
     * Find events by status for monitoring and processing
     */
    @Query("SELECT se FROM SyncEvent se WHERE se.status = :status ORDER BY se.processedAt ASC")
    List<SyncEvent> findByStatus(@Param("status") String status);

    /**
     * Find events by entity type and ID for tracking specific entity synchronization
     */
    @Query("SELECT se FROM SyncEvent se WHERE se.localUserId = :localUserId ORDER BY se.processedAt DESC")
    List<SyncEvent> findByLocalUserId(@Param("localUserId") UUID localUserId);

    /**
     * Find events by Keycloak user ID for user-specific synchronization tracking
     */
    @Query("SELECT se FROM SyncEvent se WHERE se.keycloakUserId = :keycloakUserId ORDER BY se.processedAt DESC")
    List<SyncEvent> findByKeycloakUserId(@Param("keycloakUserId") String keycloakUserId);

    // === Event Processing and Retry Management ===

    /**
     * Find pending events ready for processing
     */
    @Query("SELECT se FROM SyncEvent se WHERE se.status = 'PENDING' ORDER BY se.processedAt ASC")
    List<SyncEvent> findPendingEvents();

    /**
     * Find failed events eligible for retry
     */
    @Query("SELECT se FROM SyncEvent se WHERE se.status = 'FAILED' AND " +
           "se.retryCount < :maxRetries " +
           "ORDER BY se.processedAt ASC")
    List<SyncEvent> findRetryableEvents(@Param("maxRetries") Integer maxRetries);

    /**
     * Find events in processing state for monitoring stuck processes
     */
    @Query("SELECT se FROM SyncEvent se WHERE se.status = 'PROCESSING' AND se.processedAt < :threshold")
    List<SyncEvent> findStuckProcessingEvents(@Param("threshold") Instant threshold);

    /**
     * Find events by retry count for retry pattern analysis
     */
    @Query("SELECT se FROM SyncEvent se WHERE se.retryCount = :retryCount ORDER BY se.processedAt DESC")
    List<SyncEvent> findByRetryCount(@Param("retryCount") Integer retryCount);

    /**
     * Find events that have exceeded maximum retry attempts
     */
    @Query("SELECT se FROM SyncEvent se WHERE se.status = 'FAILED' AND se.retryCount >= :maxRetries")
    List<SyncEvent> findExceededRetryLimitEvents(@Param("maxRetries") Integer maxRetries);

    // === Error Tracking and Analysis ===

    /**
     * Find failed events by error pattern
     */
    @Query("SELECT se FROM SyncEvent se WHERE se.status = 'FAILED' AND " +
           "se.errorMessage LIKE %:errorPattern% ORDER BY se.processedAt DESC")
    List<SyncEvent> findFailedEventsByErrorPattern(@Param("errorPattern") String errorPattern);

    /**
     * Find events with specific error codes
     */
    @Query("SELECT se FROM SyncEvent se WHERE " +
           "JSON_EXTRACT(se.payload, '$.errorCode') = :errorCode ORDER BY se.processedAt DESC")
    List<SyncEvent> findByErrorCode(@Param("errorCode") String errorCode);

    /**
     * Find recent failed events for error monitoring
     */
    @Query("SELECT se FROM SyncEvent se WHERE se.status = 'FAILED' AND se.processedAt >= :since ORDER BY se.processedAt DESC")
    List<SyncEvent> findRecentFailedEvents(@Param("since") Instant since);

    /**
     * Count failed events by error type for monitoring dashboard
     */
    @Query("SELECT JSON_UNQUOTE(JSON_EXTRACT(se.payload, '$.errorCode')) as errorCode, COUNT(*) " +
           "FROM SyncEvent se WHERE se.status = 'FAILED' AND se.processedAt >= :since " +
           "GROUP BY errorCode ORDER BY COUNT(*) DESC")
    List<Object[]> countRecentFailuresByErrorCode(@Param("since") Instant since);

    // === Synchronization Direction and Flow ===

    /**
     * Find events by synchronization direction
     */
    @Query("SELECT se FROM SyncEvent se WHERE se.syncDirection = :direction ORDER BY se.processedAt DESC")
    Page<SyncEvent> findBySyncDirection(@Param("direction") SyncDirection direction, Pageable pageable);

    /**
     * Find events for Keycloak to local synchronization
     */
    @Query("SELECT se FROM SyncEvent se WHERE se.syncDirection = 'KEYCLOAK_TO_LOCAL' ORDER BY se.processedAt DESC")
    List<SyncEvent> findKeycloakToLocalEvents();

    /**
     * Find events for local to Keycloak synchronization
     */
    @Query("SELECT se FROM SyncEvent se WHERE se.syncDirection = 'LOCAL_TO_KEYCLOAK' ORDER BY se.processedAt DESC")
    List<SyncEvent> findLocalToKeycloakEvents();

    /**
     * Find bidirectional synchronization events
     */
    @Query("SELECT se FROM SyncEvent se WHERE se.syncDirection = 'BIDIRECTIONAL' ORDER BY se.processedAt DESC")
    List<SyncEvent> findBidirectionalEvents();

    // === Performance and Timing Analysis ===

    /**
     * Find events by processing duration for performance analysis
     */
    @Query("SELECT se FROM SyncEvent se WHERE se.status = 'SUCCESS' AND " +
           "se.processedAt IS NOT NULL AND " +
           "se.retryCount > 0 " +
           "ORDER BY se.processedAt DESC")
    List<SyncEvent> findSlowProcessingEvents(@Param("durationMs") Long durationMs);

    /**
     * Find events processed within time range
     */
    @Query("SELECT se FROM SyncEvent se WHERE se.processedAt BETWEEN :startTime AND :endTime ORDER BY se.processedAt DESC")
    Page<SyncEvent> findEventsProcessedBetween(
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            Pageable pageable);

    /**
     * Find events created within time range
     */
    @Query("SELECT se FROM SyncEvent se WHERE se.processedAt BETWEEN :startTime AND :endTime ORDER BY se.processedAt DESC")
    Page<SyncEvent> findEventsCreatedBetween(
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            Pageable pageable);

    // === Bulk Operations and Status Updates ===

    /**
     * Bulk update event status
     */
    @Modifying
    @Query("UPDATE SyncEvent se SET se.status = :newStatus, se.processedAt = CURRENT_TIMESTAMP " +
           "WHERE se.id IN :eventIds")
    int updateEventStatus(@Param("eventIds") List<String> eventIds, @Param("newStatus") String newStatus);

    /**
     * Mark events as processing
     */
    @Modifying
    @Query("UPDATE SyncEvent se SET se.status = 'PROCESSING', se.processedAt = CURRENT_TIMESTAMP " +
           "WHERE se.id IN :eventIds AND se.status = 'PENDING'")
    int markEventsAsProcessing(@Param("eventIds") List<String> eventIds);

    /**
     * Mark events as successful
     */
    @Modifying
    @Query("UPDATE SyncEvent se SET se.status = 'SUCCESS', se.processedAt = CURRENT_TIMESTAMP " +
           "WHERE se.id IN :eventIds")
    int markEventsAsSuccessful(@Param("eventIds") List<String> eventIds);

    /**
     * Mark events as failed with error information
     */
    @Modifying
    @Query("UPDATE SyncEvent se SET se.status = 'FAILED', se.errorMessage = :errorMessage, " +
           "se.retryCount = se.retryCount + 1, se.processedAt = CURRENT_TIMESTAMP " +
           "WHERE se.id IN :eventIds")
    int markEventsAsFailed(
            @Param("eventIds") List<String> eventIds,
            @Param("errorMessage") String errorMessage);

    /**
     * Reset stuck processing events to pending
     */
    @Modifying
    @Query("UPDATE SyncEvent se SET se.status = 'PENDING', se.processedAt = CURRENT_TIMESTAMP " +
           "WHERE se.status = 'PROCESSING' AND se.processedAt < :threshold")
    int resetStuckProcessingEvents(@Param("threshold") Instant threshold);

    // === Deduplication and Idempotency ===

    /**
     * Find duplicate events by idempotency key
     */
    @Query("SELECT se FROM SyncEvent se WHERE JSON_EXTRACT(se.payload, '$.idempotencyKey') = :idempotencyKey ORDER BY se.processedAt ASC")
    List<SyncEvent> findByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    /**
     * Find latest event by idempotency key
     */
    @Query("SELECT se FROM SyncEvent se WHERE JSON_EXTRACT(se.payload, '$.idempotencyKey') = :idempotencyKey ORDER BY se.processedAt DESC LIMIT 1")
    Optional<SyncEvent> findLatestByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    /**
     * Check if event with idempotency key already exists
     */
    @Query("SELECT COUNT(se) > 0 FROM SyncEvent se WHERE JSON_EXTRACT(se.payload, '$.idempotencyKey') = :idempotencyKey")
    boolean existsByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    /**
     * Find events with duplicate idempotency keys for cleanup
     */
    @Query("SELECT se FROM SyncEvent se WHERE JSON_EXTRACT(se.payload, '$.idempotencyKey') IN (" +
           "SELECT JSON_EXTRACT(se2.payload, '$.idempotencyKey') FROM SyncEvent se2 WHERE JSON_EXTRACT(se2.payload, '$.idempotencyKey') IS NOT NULL " +
           "GROUP BY JSON_EXTRACT(se2.payload, '$.idempotencyKey') HAVING COUNT(*) > 1) " +
           "ORDER BY JSON_EXTRACT(se.payload, '$.idempotencyKey'), se.processedAt")
    List<SyncEvent> findDuplicateEvents();

    // === Search and Filtering ===

    /**
     * Search events by multiple criteria
     */
    @Query("SELECT se FROM SyncEvent se WHERE " +
           "(:eventType IS NULL OR se.eventType = :eventType) AND " +
           "(:status IS NULL OR se.status = :status) AND " +
           "(:localUserId IS NULL OR se.localUserId = :localUserId) AND " +
           "(:syncDirection IS NULL OR se.syncDirection = :syncDirection) AND " +
           "(:keycloakUserId IS NULL OR se.keycloakUserId = :keycloakUserId)")
    Page<SyncEvent> searchEvents(
            @Param("eventType") String eventType,
            @Param("status") String status,
            @Param("localUserId") UUID localUserId,
            @Param("syncDirection") String syncDirection,
            @Param("keycloakUserId") String keycloakUserId,
            Pageable pageable);

    /**
     * Find events by JSON data criteria
     */
    @Query("SELECT se FROM SyncEvent se WHERE JSON_EXTRACT(se.payload, :jsonPath) = :value")
    List<SyncEvent> findByEventData(@Param("jsonPath") String jsonPath, @Param("value") Object value);

    // === Analytics and Reporting ===

    /**
     * Get synchronization statistics for dashboard
     */
    @Query("SELECT " +
           "COUNT(*) as totalEvents, " +
           "COUNT(CASE WHEN se.status = 'SUCCESS' THEN 1 END) as successfulEvents, " +
           "COUNT(CASE WHEN se.status = 'FAILED' THEN 1 END) as failedEvents, " +
           "COUNT(CASE WHEN se.status = 'PENDING' THEN 1 END) as pendingEvents, " +
           "COUNT(CASE WHEN se.status = 'PROCESSING' THEN 1 END) as processingEvents, " +
           "AVG(CASE WHEN se.status = 'SUCCESS' AND se.retryCount >= 0 " +
           "     THEN se.retryCount * 1000 END) as avgProcessingTimeMs " +
           "FROM SyncEvent se WHERE se.processedAt >= :since")
    SyncStatistics getSyncStatistics(@Param("since") Instant since);

    /**
     * Interface for synchronization statistics projection
     */
    interface SyncStatistics {
        long getTotalEvents();
        long getSuccessfulEvents();
        long getFailedEvents();
        long getPendingEvents();
        long getProcessingEvents();
        Double getAvgProcessingTimeMs();
    }

    /**
     * Get event type distribution for analytics
     */
    @Query("SELECT se.eventType, COUNT(*) FROM SyncEvent se WHERE se.processedAt >= :since GROUP BY se.eventType")
    List<Object[]> getEventTypeDistribution(@Param("since") Instant since);

    /**
     * Get synchronization direction statistics
     */
    @Query("SELECT se.syncDirection, COUNT(*) FROM SyncEvent se WHERE se.processedAt >= :since GROUP BY se.syncDirection")
    List<Object[]> getSyncDirectionDistribution(@Param("since") Instant since);

    /**
     * Get retry count distribution for analysis
     */
    @Query("SELECT se.retryCount, COUNT(*) FROM SyncEvent se WHERE se.status = 'FAILED' GROUP BY se.retryCount ORDER BY se.retryCount")
    List<Object[]> getRetryCountDistribution();

    // === Cleanup and Maintenance ===

    /**
     * Find old successful events for cleanup
     */
    @Query("SELECT se FROM SyncEvent se WHERE se.status = 'SUCCESS' AND se.processedAt < :threshold")
    List<SyncEvent> findOldSuccessfulEvents(@Param("threshold") Instant threshold);

    /**
     * Find old failed events that exceeded retry limit for cleanup
     */
    @Query("SELECT se FROM SyncEvent se WHERE se.status = 'FAILED' AND " +
           "se.retryCount >= :maxRetries AND se.processedAt < :threshold")
    List<SyncEvent> findOldFailedEvents(@Param("maxRetries") Integer maxRetries, @Param("threshold") Instant threshold);

    /**
     * Delete old successful events
     */
    @Modifying
    @Query("DELETE FROM SyncEvent se WHERE se.status = 'SUCCESS' AND se.processedAt < :threshold")
    int deleteOldSuccessfulEvents(@Param("threshold") Instant threshold);

    /**
     * Delete old failed events that exceeded retry limit
     */
    @Modifying
    @Query("DELETE FROM SyncEvent se WHERE se.status = 'FAILED' AND " +
           "se.retryCount >= :maxRetries AND se.processedAt < :threshold")
    int deleteOldFailedEvents(@Param("maxRetries") Integer maxRetries, @Param("threshold") Instant threshold);

    /**
     * Get cleanup statistics for maintenance dashboard
     */
    @Query("SELECT " +
           "COUNT(CASE WHEN se.status = 'SUCCESS' AND se.processedAt < :successThreshold THEN 1 END) as oldSuccessfulEvents, " +
           "COUNT(CASE WHEN se.status = 'FAILED' AND se.retryCount >= :maxRetries AND se.processedAt < :failedThreshold THEN 1 END) as oldFailedEvents, " +
           "COUNT(CASE WHEN se.status = 'PROCESSING' AND se.processedAt < :stuckThreshold THEN 1 END) as stuckEvents " +
           "FROM SyncEvent se")
    CleanupStatistics getCleanupStatistics(
            @Param("successThreshold") Instant successThreshold,
            @Param("maxRetries") Integer maxRetries,
            @Param("failedThreshold") Instant failedThreshold,
            @Param("stuckThreshold") Instant stuckThreshold);

    /**
     * Interface for cleanup statistics projection
     */
    interface CleanupStatistics {
        long getOldSuccessfulEvents();
        long getOldFailedEvents();
        long getStuckEvents();
    }

    // === Data Quality and Validation ===

    /**
     * Find events with missing required fields
     */
    @Query("SELECT se FROM SyncEvent se WHERE " +
           "se.eventType IS NULL OR se.localUserId IS NULL OR se.status IS NULL")
    List<SyncEvent> findEventsWithMissingFields();

    /**
     * Find events with invalid status values
     */
    @Query("SELECT se FROM SyncEvent se WHERE " +
           "se.status NOT IN ('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED')")
    List<SyncEvent> findEventsWithInvalidStatus();

    /**
     * Find events with empty event data (using native query for JSONB)
     */
    @Query(value = "SELECT * FROM iam.sync_events se WHERE se.event_data IS NOT NULL AND se.event_data = '{}'::jsonb",
           nativeQuery = true)
    List<SyncEvent> findEventsWithInvalidJson();
}