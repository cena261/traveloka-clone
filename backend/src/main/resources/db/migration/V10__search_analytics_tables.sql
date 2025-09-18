-- ============================
-- V10: SEARCH ANALYTICS TABLES
-- ============================
-- Create search analytics tables for user behavior tracking,
-- popular destinations, and search performance metrics

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================
-- SEARCH HISTORY TABLE
-- ============================
-- Captures individual search interactions for analytics and personalization
CREATE TABLE IF NOT EXISTS search.search_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id UUID NOT NULL,
    user_id UUID NULL, -- References iam.user, nullable for anonymous users
    search_query TEXT,
    search_type VARCHAR(50) NOT NULL CHECK (search_type IN ('FULL_TEXT', 'LOCATION_BASED', 'SUGGESTION', 'FILTER_ONLY')),

    -- Search filters applied (JSONB for flexibility)
    filters JSONB DEFAULT '{}',

    -- Location context for the search
    search_location GEOGRAPHY(POINT, 4326), -- PostGIS geometry for user location
    search_radius DECIMAL(10,2), -- Search radius in kilometers
    detected_location BOOLEAN DEFAULT FALSE, -- Whether location was auto-detected

    -- Search results metadata
    total_results INTEGER DEFAULT 0,
    response_time_ms INTEGER, -- Query response time in milliseconds
    clicked_property_ids UUID[], -- Array of property IDs user clicked
    booking_completed BOOLEAN DEFAULT FALSE,
    conversion_value DECIMAL(12,2), -- Booking value if conversion occurred

    -- Context information
    search_timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_agent TEXT,
    language VARCHAR(10) DEFAULT 'vi',
    device_type VARCHAR(20), -- MOBILE, DESKTOP, TABLET

    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for search_history
CREATE INDEX IF NOT EXISTS search_history_user_idx ON search.search_history(user_id);
CREATE INDEX IF NOT EXISTS search_history_session_idx ON search.search_history(session_id);
CREATE INDEX IF NOT EXISTS search_history_timestamp_idx ON search.search_history(search_timestamp);
CREATE INDEX IF NOT EXISTS search_history_location_idx ON search.search_history USING GIST(search_location);
CREATE INDEX IF NOT EXISTS search_history_type_idx ON search.search_history(search_type);
CREATE INDEX IF NOT EXISTS search_history_conversion_idx ON search.search_history(booking_completed, conversion_value) WHERE booking_completed = TRUE;
CREATE INDEX IF NOT EXISTS search_history_filters_idx ON search.search_history USING GIN(filters);

-- ============================
-- SEARCH SESSIONS TABLE
-- ============================
-- Groups related search interactions within a user session
CREATE TABLE IF NOT EXISTS search.search_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NULL, -- References iam.user, nullable for anonymous users
    session_start TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    session_end TIMESTAMP WITH TIME ZONE,

    -- Session metrics
    search_count INTEGER DEFAULT 0,
    filter_changes INTEGER DEFAULT 0,
    unique_properties_viewed INTEGER DEFAULT 0,

    -- Conversion data
    booking_completed BOOLEAN DEFAULT FALSE,
    booking_value DECIMAL(12,2),
    property_booked UUID, -- References inventory.property

    -- Device and context
    device_type VARCHAR(20), -- MOBILE, DESKTOP, TABLET
    user_agent TEXT,
    initial_location GEOGRAPHY(POINT, 4326),

    -- Session duration in seconds
    session_duration INTEGER,

    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for search_sessions
CREATE INDEX IF NOT EXISTS search_sessions_user_idx ON search.search_sessions(user_id);
CREATE INDEX IF NOT EXISTS search_sessions_start_idx ON search.search_sessions(session_start);
CREATE INDEX IF NOT EXISTS search_sessions_conversion_idx ON search.search_sessions(booking_completed, booking_value);
CREATE INDEX IF NOT EXISTS search_sessions_device_idx ON search.search_sessions(device_type);

-- ============================
-- POPULAR DESTINATIONS TABLE
-- ============================
-- Tracks trending destinations and search patterns for business intelligence
CREATE TABLE IF NOT EXISTS search.popular_destinations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    destination_type VARCHAR(20) NOT NULL CHECK (destination_type IN ('CITY', 'LANDMARK', 'REGION', 'AIRPORT', 'DISTRICT')),
    destination_name VARCHAR(200) NOT NULL,
    country_code CHAR(2) NOT NULL,
    coordinates GEOGRAPHY(POINT, 4326), -- PostGIS geometry for destination location

    -- Analytics period
    period_start TIMESTAMP WITH TIME ZONE NOT NULL,
    period_end TIMESTAMP WITH TIME ZONE NOT NULL,

    -- Search metrics (JSONB for flexibility and performance)
    search_metrics JSONB NOT NULL DEFAULT '{}',
    -- Expected structure:
    -- {
    --   "searchVolume": 1250,
    --   "uniqueUsers": 890,
    --   "conversionRate": 0.15,
    --   "averageStayDuration": 3.2,
    --   "seasonalTrends": {"01": 120, "02": 98, ...},
    --   "topSearchTerms": ["luxury hotel", "beach resort", ...]
    -- }

    -- Ranking data
    popularity_rank INTEGER,
    trending_score DECIMAL(10,4), -- Momentum-based trending calculation
    business_value DECIMAL(15,2), -- Revenue impact score

    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Ensure unique destination per period
    CONSTRAINT unique_destination_period UNIQUE (destination_name, country_code, period_start)
);

-- Create indexes for popular_destinations
CREATE INDEX IF NOT EXISTS popular_destinations_rank_idx ON search.popular_destinations(popularity_rank);
CREATE INDEX IF NOT EXISTS popular_destinations_location_idx ON search.popular_destinations USING GIST(coordinates);
CREATE INDEX IF NOT EXISTS popular_destinations_period_idx ON search.popular_destinations(period_start, period_end);
CREATE INDEX IF NOT EXISTS popular_destinations_country_idx ON search.popular_destinations(country_code);
CREATE INDEX IF NOT EXISTS popular_destinations_type_idx ON search.popular_destinations(destination_type);
CREATE INDEX IF NOT EXISTS popular_destinations_trending_idx ON search.popular_destinations(trending_score DESC);
CREATE INDEX IF NOT EXISTS popular_destinations_metrics_idx ON search.popular_destinations USING GIN(search_metrics);

-- ============================
-- SEARCH PERFORMANCE METRICS TABLE
-- ============================
-- Tracks search engine performance and optimization metrics
CREATE TABLE IF NOT EXISTS search.search_performance_metrics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    metric_date DATE NOT NULL,
    metric_hour INTEGER CHECK (metric_hour >= 0 AND metric_hour <= 23),

    -- Performance metrics
    total_searches INTEGER DEFAULT 0,
    average_response_time DECIMAL(10,2), -- In milliseconds
    p95_response_time DECIMAL(10,2), -- 95th percentile response time
    cache_hit_rate DECIMAL(5,4), -- Percentage as decimal (0.95 = 95%)

    -- Search quality metrics
    zero_results_rate DECIMAL(5,4), -- Percentage of searches with no results
    click_through_rate DECIMAL(5,4), -- Percentage of searches that led to clicks
    conversion_rate DECIMAL(5,4), -- Percentage of searches that led to bookings

    -- Resource utilization
    elasticsearch_cpu_usage DECIMAL(5,2),
    elasticsearch_memory_usage DECIMAL(5,2),
    redis_memory_usage DECIMAL(5,2),
    database_cpu_usage DECIMAL(5,2),

    -- Error metrics
    elasticsearch_errors INTEGER DEFAULT 0,
    timeout_errors INTEGER DEFAULT 0,
    cache_errors INTEGER DEFAULT 0,

    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Ensure unique metrics per hour
    CONSTRAINT unique_metric_hour UNIQUE (metric_date, metric_hour)
);

-- Create indexes for search_performance_metrics
CREATE INDEX IF NOT EXISTS search_performance_date_idx ON search.search_performance_metrics(metric_date DESC);
CREATE INDEX IF NOT EXISTS search_performance_response_time_idx ON search.search_performance_metrics(average_response_time);
CREATE INDEX IF NOT EXISTS search_performance_cache_idx ON search.search_performance_metrics(cache_hit_rate);

-- ============================
-- SEARCH KEYWORDS TABLE
-- ============================
-- Tracks popular search keywords and phrases for optimization
CREATE TABLE IF NOT EXISTS search.search_keywords (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    keyword TEXT NOT NULL,
    normalized_keyword TEXT NOT NULL, -- Lowercase, diacritic-removed version
    language VARCHAR(10) DEFAULT 'vi',

    -- Usage statistics
    search_count INTEGER DEFAULT 1,
    result_count_avg DECIMAL(10,2), -- Average number of results returned
    click_through_rate DECIMAL(5,4),
    conversion_rate DECIMAL(5,4),

    -- Temporal data
    first_seen TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    peak_usage_date DATE,

    -- Classification
    keyword_type VARCHAR(50), -- PROPERTY_NAME, LOCATION, AMENITY, BRAND, etc.
    is_trending BOOLEAN DEFAULT FALSE,

    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Ensure unique keywords per language
    CONSTRAINT unique_keyword_language UNIQUE (normalized_keyword, language)
);

-- Create indexes for search_keywords
CREATE INDEX IF NOT EXISTS search_keywords_normalized_idx ON search.search_keywords(normalized_keyword);
CREATE INDEX IF NOT EXISTS search_keywords_count_idx ON search.search_keywords(search_count DESC);
CREATE INDEX IF NOT EXISTS search_keywords_trending_idx ON search.search_keywords(is_trending, search_count DESC) WHERE is_trending = TRUE;
CREATE INDEX IF NOT EXISTS search_keywords_language_idx ON search.search_keywords(language);
CREATE INDEX IF NOT EXISTS search_keywords_type_idx ON search.search_keywords(keyword_type);
CREATE INDEX IF NOT EXISTS search_keywords_ctr_idx ON search.search_keywords(click_through_rate DESC);

-- ============================
-- TRIGGERS FOR UPDATED_AT
-- ============================
-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION search.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for updated_at on all tables
CREATE TRIGGER update_search_history_updated_at
    BEFORE UPDATE ON search.search_history
    FOR EACH ROW EXECUTE FUNCTION search.update_updated_at_column();

CREATE TRIGGER update_search_sessions_updated_at
    BEFORE UPDATE ON search.search_sessions
    FOR EACH ROW EXECUTE FUNCTION search.update_updated_at_column();

CREATE TRIGGER update_popular_destinations_updated_at
    BEFORE UPDATE ON search.popular_destinations
    FOR EACH ROW EXECUTE FUNCTION search.update_updated_at_column();

CREATE TRIGGER update_search_performance_metrics_updated_at
    BEFORE UPDATE ON search.search_performance_metrics
    FOR EACH ROW EXECUTE FUNCTION search.update_updated_at_column();

CREATE TRIGGER update_search_keywords_updated_at
    BEFORE UPDATE ON search.search_keywords
    FOR EACH ROW EXECUTE FUNCTION search.update_updated_at_column();

-- ============================
-- SAMPLE DATA FOR DEVELOPMENT
-- ============================
-- Insert some sample popular destinations for testing
INSERT INTO search.popular_destinations (
    destination_type, destination_name, country_code, coordinates,
    period_start, period_end, search_metrics, popularity_rank, trending_score, business_value
) VALUES
    ('CITY', 'Hà Nội', 'VN', ST_GeogFromText('POINT(105.8542 21.0285)'),
     CURRENT_DATE - INTERVAL '7 days', CURRENT_DATE,
     '{"searchVolume": 1250, "uniqueUsers": 890, "conversionRate": 0.15, "averageStayDuration": 3.2}',
     1, 95.5, 125000.00),
    ('CITY', 'Hồ Chí Minh', 'VN', ST_GeogFromText('POINT(106.6297 10.8231)'),
     CURRENT_DATE - INTERVAL '7 days', CURRENT_DATE,
     '{"searchVolume": 1180, "uniqueUsers": 820, "conversionRate": 0.18, "averageStayDuration": 2.8}',
     2, 88.2, 142000.00),
    ('CITY', 'Đà Nẵng', 'VN', ST_GeogFromText('POINT(108.2022 16.0544)'),
     CURRENT_DATE - INTERVAL '7 days', CURRENT_DATE,
     '{"searchVolume": 750, "uniqueUsers": 580, "conversionRate": 0.22, "averageStayDuration": 4.1}',
     3, 76.8, 98000.00),
    ('LANDMARK', 'Vịnh Hạ Long', 'VN', ST_GeogFromText('POINT(107.0431 20.9101)'),
     CURRENT_DATE - INTERVAL '7 days', CURRENT_DATE,
     '{"searchVolume": 420, "uniqueUsers": 380, "conversionRate": 0.25, "averageStayDuration": 2.5}',
     4, 82.1, 67000.00),
    ('CITY', 'Nha Trang', 'VN', ST_GeogFromText('POINT(109.1967 12.2388)'),
     CURRENT_DATE - INTERVAL '7 days', CURRENT_DATE,
     '{"searchVolume": 380, "uniqueUsers": 320, "conversionRate": 0.20, "averageStayDuration": 3.8}',
     5, 69.4, 58000.00)
ON CONFLICT (destination_name, country_code, period_start) DO NOTHING;

-- Insert sample search keywords
INSERT INTO search.search_keywords (
    keyword, normalized_keyword, language, search_count, result_count_avg,
    click_through_rate, conversion_rate, keyword_type, is_trending
) VALUES
    ('khách sạn Hà Nội', 'khach san ha noi', 'vi', 1250, 45.2, 0.12, 0.03, 'LOCATION', true),
    ('resort Đà Nẵng', 'resort da nang', 'vi', 890, 32.1, 0.15, 0.04, 'LOCATION', true),
    ('villa Hồ Chí Minh', 'villa ho chi minh', 'vi', 650, 28.5, 0.18, 0.05, 'LOCATION', false),
    ('luxury hotel Hanoi', 'luxury hotel hanoi', 'en', 420, 22.8, 0.20, 0.06, 'PROPERTY_TYPE', false),
    ('beach resort', 'beach resort', 'en', 380, 35.6, 0.14, 0.04, 'AMENITY', true)
ON CONFLICT (normalized_keyword, language) DO NOTHING;

-- ============================
-- COMMENTS FOR DOCUMENTATION
-- ============================
COMMENT ON TABLE search.search_history IS 'Captures individual search interactions for analytics and personalization';
COMMENT ON TABLE search.search_sessions IS 'Groups related search interactions within a user session for behavior analysis';
COMMENT ON TABLE search.popular_destinations IS 'Tracks trending destinations and search patterns for business intelligence';
COMMENT ON TABLE search.search_performance_metrics IS 'Tracks search engine performance and optimization metrics';
COMMENT ON TABLE search.search_keywords IS 'Tracks popular search keywords and phrases for search optimization';

COMMENT ON COLUMN search.search_history.filters IS 'JSONB containing applied search filters (price range, amenities, ratings, etc.)';
COMMENT ON COLUMN search.search_history.search_location IS 'PostGIS geography point for user location during search';
COMMENT ON COLUMN search.search_history.clicked_property_ids IS 'Array of property UUIDs that user clicked in search results';
COMMENT ON COLUMN search.popular_destinations.search_metrics IS 'JSONB containing search volume, conversion rates, and trend data';
COMMENT ON COLUMN search.search_keywords.normalized_keyword IS 'Lowercase, diacritic-removed version for consistent matching';