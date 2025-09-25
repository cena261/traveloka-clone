
-- V7 (fixed): Search history with HASH(id) partitioning (unchanged)

DROP TABLE IF EXISTS search.search_history CASCADE;

CREATE TABLE search.search_history (
                                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                       session_id UUID NOT NULL,
                                       user_id UUID,
                                       search_query TEXT,
                                       search_type VARCHAR(50) NOT NULL CHECK (search_type IN ('FULL_TEXT','LOCATION_BASED','SUGGESTION','FILTER_ONLY')),
                                       filters JSONB DEFAULT '{}'::jsonb,
                                       search_location GEOGRAPHY(POINT, 4326),
                                       search_radius NUMERIC(10,2),
                                       total_results INT DEFAULT 0,
                                       response_time_ms INT,
                                       clicked_property_ids UUID[],
                                       booking_completed BOOLEAN DEFAULT FALSE,
                                       conversion_value NUMERIC(12,2),
                                       search_timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                       user_agent TEXT,
                                       language VARCHAR(10) DEFAULT 'vi',
                                       device_type VARCHAR(20),
                                       created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                       updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
) PARTITION BY HASH (id);

DO $$
DECLARE i int;
BEGIN
FOR i IN 0..7 LOOP
    EXECUTE format('
      CREATE TABLE IF NOT EXISTS search.search_history_p%s PARTITION OF search.search_history
      FOR VALUES WITH (MODULUS 8, REMAINDER %s);
    ', i, i);
END LOOP;
END $$;

CREATE INDEX IF NOT EXISTS search_history_user_idx ON search.search_history(user_id);
CREATE INDEX IF NOT EXISTS search_history_session_idx ON search.search_history(session_id);
CREATE INDEX IF NOT EXISTS search_history_timestamp_idx ON search.search_history(search_timestamp);
CREATE INDEX IF NOT EXISTS search_history_location_idx ON search.search_history USING GIST(search_location);
CREATE INDEX IF NOT EXISTS search_history_type_idx ON search.search_history(search_type);
CREATE INDEX IF NOT EXISTS search_history_filters_idx ON search.search_history USING GIN (filters);
