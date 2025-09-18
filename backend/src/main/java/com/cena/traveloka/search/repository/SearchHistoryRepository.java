package com.cena.traveloka.search.repository;

import com.cena.traveloka.search.entity.SearchHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, UUID> {

    /**
     * Find search history by user ID with pagination
     */
    Page<SearchHistory> findByUserIdOrderBySearchTimestampDesc(UUID userId, Pageable pageable);

    /**
     * Find search history by session ID
     */
    List<SearchHistory> findBySessionIdOrderBySearchTimestampAsc(UUID sessionId);

    /**
     * Find search history within date range
     */
    @Query("SELECT sh FROM SearchHistory sh WHERE sh.searchTimestamp BETWEEN :startDate AND :endDate ORDER BY sh.searchTimestamp DESC")
    Page<SearchHistory> findByDateRange(@Param("startDate") OffsetDateTime startDate,
                                      @Param("endDate") OffsetDateTime endDate,
                                      Pageable pageable);

    /**
     * Find search history by search type
     */
    Page<SearchHistory> findBySearchTypeOrderBySearchTimestampDesc(SearchHistory.SearchType searchType, Pageable pageable);

    /**
     * Find successful conversions (bookings completed)
     */
    @Query("SELECT sh FROM SearchHistory sh WHERE sh.bookingCompleted = true ORDER BY sh.searchTimestamp DESC")
    Page<SearchHistory> findConversions(Pageable pageable);

    /**
     * Find search history with location-based searches within radius using PostGIS
     */
    @Query(value = """
        SELECT * FROM search.search_history
        WHERE search_location IS NOT NULL
        AND ST_DWithin(search_location, ST_GeogFromText('POINT(:longitude :latitude)'), :radiusMeters)
        ORDER BY search_timestamp DESC
        """, nativeQuery = true)
    List<SearchHistory> findLocationBasedSearchesNearby(@Param("latitude") Double latitude,
                                                       @Param("longitude") Double longitude,
                                                       @Param("radiusMeters") Double radiusMeters);

    /**
     * Find popular search queries
     */
    @Query(value = """
        SELECT search_query, COUNT(*) as search_count
        FROM search.search_history
        WHERE search_query IS NOT NULL
        AND search_timestamp >= :since
        GROUP BY search_query
        HAVING COUNT(*) >= :minCount
        ORDER BY search_count DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findPopularSearchQueries(@Param("since") OffsetDateTime since,
                                          @Param("minCount") Integer minCount,
                                          @Param("limit") Integer limit);

    /**
     * Find searches with zero results for optimization
     */
    @Query("SELECT sh FROM SearchHistory sh WHERE sh.totalResults = 0 ORDER BY sh.searchTimestamp DESC")
    Page<SearchHistory> findZeroResultSearches(Pageable pageable);

    /**
     * Calculate conversion rate for a date range
     */
    @Query(value = """
        SELECT
            COUNT(CASE WHEN booking_completed = true THEN 1 END)::DECIMAL / COUNT(*)::DECIMAL as conversion_rate
        FROM search.search_history
        WHERE search_timestamp BETWEEN :startDate AND :endDate
        """, nativeQuery = true)
    BigDecimal calculateConversionRate(@Param("startDate") OffsetDateTime startDate,
                                     @Param("endDate") OffsetDateTime endDate);

    /**
     * Get search analytics by device type
     */
    @Query(value = """
        SELECT
            device_type,
            COUNT(*) as total_searches,
            AVG(response_time_ms) as avg_response_time,
            COUNT(CASE WHEN booking_completed = true THEN 1 END) as conversions,
            AVG(conversion_value) as avg_conversion_value
        FROM search.search_history
        WHERE search_timestamp >= :since
        GROUP BY device_type
        ORDER BY total_searches DESC
        """, nativeQuery = true)
    List<Object[]> getSearchAnalyticsByDeviceType(@Param("since") OffsetDateTime since);

    /**
     * Find searches by clicked property IDs
     */
    @Query(value = """
        SELECT * FROM search.search_history
        WHERE clicked_property_ids && ARRAY[:propertyIds]::uuid[]
        ORDER BY search_timestamp DESC
        """, nativeQuery = true)
    List<SearchHistory> findByClickedPropertyIds(@Param("propertyIds") UUID[] propertyIds);

    /**
     * Get average response time by search type
     */
    @Query(value = """
        SELECT
            search_type,
            AVG(response_time_ms) as avg_response_time,
            PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY response_time_ms) as p95_response_time,
            COUNT(*) as total_searches
        FROM search.search_history
        WHERE response_time_ms IS NOT NULL
        AND search_timestamp >= :since
        GROUP BY search_type
        ORDER BY avg_response_time
        """, nativeQuery = true)
    List<Object[]> getResponseTimeAnalytics(@Param("since") OffsetDateTime since);

    /**
     * Find searches with specific filters applied
     */
    @Query(value = """
        SELECT * FROM search.search_history
        WHERE filters @> CAST(:filterJson AS jsonb)
        ORDER BY search_timestamp DESC
        """, nativeQuery = true)
    List<SearchHistory> findByFiltersContaining(@Param("filterJson") String filterJson);

    /**
     * Get search frequency by hour of day
     */
    @Query(value = """
        SELECT
            EXTRACT(HOUR FROM search_timestamp) as hour_of_day,
            COUNT(*) as search_count
        FROM search.search_history
        WHERE search_timestamp >= :since
        GROUP BY EXTRACT(HOUR FROM search_timestamp)
        ORDER BY hour_of_day
        """, nativeQuery = true)
    List<Object[]> getSearchFrequencyByHour(@Param("since") OffsetDateTime since);

    /**
     * Find user search patterns (session analysis)
     */
    @Query(value = """
        SELECT
            session_id,
            COUNT(*) as searches_in_session,
            MIN(search_timestamp) as session_start,
            MAX(search_timestamp) as session_end,
            COUNT(DISTINCT search_query) as unique_queries,
            BOOL_OR(booking_completed) as session_converted
        FROM search.search_history
        WHERE session_id = :sessionId
        GROUP BY session_id
        """, nativeQuery = true)
    List<Object[]> getSessionAnalytics(@Param("sessionId") UUID sessionId);

    /**
     * Find trending search queries (increased search volume)
     */
    @Query(value = """
        WITH recent_searches AS (
            SELECT search_query, COUNT(*) as recent_count
            FROM search.search_history
            WHERE search_timestamp >= :recentPeriodStart
            AND search_query IS NOT NULL
            GROUP BY search_query
        ),
        historical_searches AS (
            SELECT search_query, COUNT(*) as historical_count
            FROM search.search_history
            WHERE search_timestamp BETWEEN :historicalPeriodStart AND :historicalPeriodEnd
            AND search_query IS NOT NULL
            GROUP BY search_query
        )
        SELECT
            r.search_query,
            r.recent_count,
            COALESCE(h.historical_count, 0) as historical_count,
            CASE
                WHEN COALESCE(h.historical_count, 0) = 0 THEN 100.0
                ELSE (r.recent_count::DECIMAL / h.historical_count::DECIMAL - 1) * 100
            END as growth_percentage
        FROM recent_searches r
        LEFT JOIN historical_searches h ON r.search_query = h.search_query
        WHERE r.recent_count >= :minRecentCount
        ORDER BY growth_percentage DESC, r.recent_count DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findTrendingSearchQueries(@Param("recentPeriodStart") OffsetDateTime recentPeriodStart,
                                           @Param("historicalPeriodStart") OffsetDateTime historicalPeriodStart,
                                           @Param("historicalPeriodEnd") OffsetDateTime historicalPeriodEnd,
                                           @Param("minRecentCount") Integer minRecentCount,
                                           @Param("limit") Integer limit);

    /**
     * Find searches with high response time for performance monitoring
     */
    @Query("SELECT sh FROM SearchHistory sh WHERE sh.responseTimeMs > :thresholdMs ORDER BY sh.responseTimeMs DESC")
    Page<SearchHistory> findSlowSearches(@Param("thresholdMs") Integer thresholdMs, Pageable pageable);

    /**
     * Count total searches by user in date range
     */
    @Query("SELECT COUNT(sh) FROM SearchHistory sh WHERE sh.userId = :userId AND sh.searchTimestamp BETWEEN :startDate AND :endDate")
    Long countSearchesByUserInDateRange(@Param("userId") UUID userId,
                                       @Param("startDate") OffsetDateTime startDate,
                                       @Param("endDate") OffsetDateTime endDate);

    /**
     * Delete old search history for cleanup
     */
    @Query("DELETE FROM SearchHistory sh WHERE sh.searchTimestamp < :cutoffDate")
    void deleteOldSearchHistory(@Param("cutoffDate") OffsetDateTime cutoffDate);
}
