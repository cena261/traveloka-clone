package com.cena.traveloka.search.repository;

import com.cena.traveloka.search.entity.PopularDestination;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PopularDestinationRepository extends JpaRepository<PopularDestination, UUID> {

    /**
     * Find destinations by type and country
     */
    List<PopularDestination> findByDestinationTypeAndCountryCodeOrderByPopularityRankAsc(
            PopularDestination.DestinationType destinationType, String countryCode);

    /**
     * Find current period destinations (most recent period)
     */
    @Query("SELECT pd FROM PopularDestination pd WHERE pd.periodEnd >= :currentDate ORDER BY pd.popularityRank ASC")
    List<PopularDestination> findCurrentPeriodDestinations(@Param("currentDate") OffsetDateTime currentDate);

    /**
     * Find top destinations by popularity rank
     */
    @Query("SELECT pd FROM PopularDestination pd WHERE pd.popularityRank <= :maxRank ORDER BY pd.popularityRank ASC")
    List<PopularDestination> findTopDestinations(@Param("maxRank") Integer maxRank);

    /**
     * Find trending destinations by trending score
     */
    @Query("SELECT pd FROM PopularDestination pd WHERE pd.trendingScore > :minScore ORDER BY pd.trendingScore DESC")
    Page<PopularDestination> findTrendingDestinations(@Param("minScore") BigDecimal minScore, Pageable pageable);

    /**
     * Find destinations near location using PostGIS
     */
    @Query(value = """
        SELECT * FROM search.popular_destinations
        WHERE coordinates IS NOT NULL
        AND ST_DWithin(coordinates, ST_GeogFromText('POINT(:longitude :latitude)'), :radiusMeters)
        ORDER BY ST_Distance(coordinates, ST_GeogFromText('POINT(:longitude :latitude)'))
        """, nativeQuery = true)
    List<PopularDestination> findDestinationsNearLocation(@Param("latitude") Double latitude,
                                                         @Param("longitude") Double longitude,
                                                         @Param("radiusMeters") Double radiusMeters);

    /**
     * Find destinations by name search (Vietnamese text support)
     */
    @Query(value = """
        SELECT * FROM search.popular_destinations
        WHERE LOWER(unaccent(destination_name)) LIKE LOWER(unaccent('%' || :searchTerm || '%'))
        OR destination_name ILIKE '%' || :searchTerm || '%'
        ORDER BY popularity_rank ASC
        """, nativeQuery = true)
    List<PopularDestination> findByDestinationNameContaining(@Param("searchTerm") String searchTerm);

    /**
     * Find destinations with high business value
     */
    @Query("SELECT pd FROM PopularDestination pd WHERE pd.businessValue >= :minValue ORDER BY pd.businessValue DESC")
    List<PopularDestination> findHighValueDestinations(@Param("minValue") BigDecimal minValue);

    /**
     * Get destinations by search volume range
     */
    @Query(value = """
        SELECT * FROM search.popular_destinations
        WHERE (search_metrics->>'search_volume')::integer BETWEEN :minVolume AND :maxVolume
        ORDER BY (search_metrics->>'search_volume')::integer DESC
        """, nativeQuery = true)
    List<PopularDestination> findBySearchVolumeRange(@Param("minVolume") Integer minVolume,
                                                    @Param("maxVolume") Integer maxVolume);

    /**
     * Find destinations with high conversion rate
     */
    @Query(value = """
        SELECT * FROM search.popular_destinations
        WHERE (search_metrics->>'conversion_rate')::decimal >= :minConversionRate
        ORDER BY (search_metrics->>'conversion_rate')::decimal DESC
        """, nativeQuery = true)
    List<PopularDestination> findHighConversionDestinations(@Param("minConversionRate") BigDecimal minConversionRate);

    /**
     * Get seasonal trending destinations for specific months
     */
    @Query(value = """
        SELECT * FROM search.popular_destinations
        WHERE search_metrics->'seasonal_trends' ? :month
        AND (search_metrics->'seasonal_trends'->:month)::integer >= :minSeasonalVolume
        ORDER BY (search_metrics->'seasonal_trends'->:month)::integer DESC
        """, nativeQuery = true)
    List<PopularDestination> findSeasonalTrendingDestinations(@Param("month") String month,
                                                             @Param("minSeasonalVolume") Integer minSeasonalVolume);

    /**
     * Find destinations by period range
     */
    @Query("SELECT pd FROM PopularDestination pd WHERE pd.periodStart >= :startDate AND pd.periodEnd <= :endDate ORDER BY pd.popularityRank ASC")
    List<PopularDestination> findByPeriodRange(@Param("startDate") OffsetDateTime startDate,
                                             @Param("endDate") OffsetDateTime endDate);

    /**
     * Get analytics summary for destinations
     */
    @Query(value = """
        SELECT
            destination_type,
            country_code,
            COUNT(*) as destination_count,
            AVG((search_metrics->>'search_volume')::integer) as avg_search_volume,
            AVG((search_metrics->>'conversion_rate')::decimal) as avg_conversion_rate,
            AVG(trending_score) as avg_trending_score,
            SUM(business_value) as total_business_value
        FROM search.popular_destinations
        WHERE period_start >= :since
        GROUP BY destination_type, country_code
        ORDER BY avg_search_volume DESC
        """, nativeQuery = true)
    List<Object[]> getDestinationAnalyticsSummary(@Param("since") OffsetDateTime since);

    /**
     * Find competing destinations (same city, different types)
     */
    @Query(value = """
        SELECT DISTINCT pd2.* FROM search.popular_destinations pd1
        JOIN search.popular_destinations pd2 ON pd1.destination_name = pd2.destination_name
        WHERE pd1.id = :destinationId
        AND pd2.id != pd1.id
        AND pd2.destination_type != pd1.destination_type
        ORDER BY pd2.popularity_rank ASC
        """, nativeQuery = true)
    List<PopularDestination> findCompetingDestinations(@Param("destinationId") UUID destinationId);

    /**
     * Get destination growth analysis
     */
    @Query(value = """
        WITH current_period AS (
            SELECT * FROM search.popular_destinations
            WHERE destination_name = :destinationName
            AND country_code = :countryCode
            ORDER BY period_end DESC
            LIMIT 1
        ),
        previous_period AS (
            SELECT * FROM search.popular_destinations
            WHERE destination_name = :destinationName
            AND country_code = :countryCode
            ORDER BY period_end DESC
            OFFSET 1 LIMIT 1
        )
        SELECT
            c.destination_name,
            c.country_code,
            (c.search_metrics->>'search_volume')::integer as current_volume,
            COALESCE((p.search_metrics->>'search_volume')::integer, 0) as previous_volume,
            CASE
                WHEN COALESCE((p.search_metrics->>'search_volume')::integer, 0) = 0 THEN 100.0
                ELSE ((c.search_metrics->>'search_volume')::integer::decimal /
                      (p.search_metrics->>'search_volume')::integer::decimal - 1) * 100
            END as volume_growth_percentage,
            c.trending_score,
            c.business_value
        FROM current_period c
        LEFT JOIN previous_period p ON c.destination_name = p.destination_name
        """, nativeQuery = true)
    List<Object[]> getDestinationGrowthAnalysis(@Param("destinationName") String destinationName,
                                              @Param("countryCode") String countryCode);

    /**
     * Find destinations with specific search terms
     */
    @Query(value = """
        SELECT * FROM search.popular_destinations
        WHERE search_metrics->'top_search_terms' @> CAST(? AS jsonb)
        ORDER BY popularity_rank ASC
        """, nativeQuery = true)
    List<PopularDestination> findByTopSearchTermsContaining(String searchTermJson);

    /**
     * Get market share analysis for destinations
     */
    @Query(value = """
        SELECT
            destination_name,
            country_code,
            destination_type,
            (search_metrics->>'search_volume')::integer as search_volume,
            (search_metrics->>'search_volume')::decimal / SUM((search_metrics->>'search_volume')::integer) OVER (PARTITION BY country_code) * 100 as market_share_percentage,
            popularity_rank,
            trending_score
        FROM search.popular_destinations
        WHERE country_code = :countryCode
        AND period_end >= :currentDate
        ORDER BY market_share_percentage DESC
        """, nativeQuery = true)
    List<Object[]> getMarketShareAnalysis(@Param("countryCode") String countryCode,
                                        @Param("currentDate") OffsetDateTime currentDate);

    /**
     * Find similar destinations by coordinates proximity
     */
    @Query(value = """
        SELECT * FROM search.popular_destinations
        WHERE id != :destinationId
        AND coordinates IS NOT NULL
        AND ST_DWithin(
            coordinates,
            (SELECT coordinates FROM search.popular_destinations WHERE id = :destinationId),
            :radiusMeters
        )
        ORDER BY ST_Distance(
            coordinates,
            (SELECT coordinates FROM search.popular_destinations WHERE id = :destinationId)
        )
        LIMIT :limit
        """, nativeQuery = true)
    List<PopularDestination> findSimilarDestinationsByProximity(@Param("destinationId") UUID destinationId,
                                                               @Param("radiusMeters") Double radiusMeters,
                                                               @Param("limit") Integer limit);

    /**
     * Update trending scores based on recent search activity
     */
    @Query(value = """
        UPDATE search.popular_destinations
        SET trending_score = CASE
            WHEN (search_metrics->>'search_volume')::integer > 0 THEN
                (search_metrics->>'search_volume')::decimal * (search_metrics->>'conversion_rate')::decimal * 100
            ELSE 0
        END,
        updated_at = CURRENT_TIMESTAMP
        WHERE period_end >= :currentDate
        """, nativeQuery = true)
    void updateTrendingScores(@Param("currentDate") OffsetDateTime currentDate);

    /**
     * Delete outdated destination records
     */
    @Query("DELETE FROM PopularDestination pd WHERE pd.periodEnd < :cutoffDate")
    void deleteOutdatedDestinations(@Param("cutoffDate") OffsetDateTime cutoffDate);
}
