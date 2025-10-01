-- =====================================================
-- V4: Location and Geographic Data
-- Description: Countries, Cities, Airports, POIs with PostGIS support
-- =====================================================

-- =====================================================
-- COUNTRIES TABLE
-- =====================================================

CREATE TABLE geo.countries (
                               id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                               code common.country_code UNIQUE NOT NULL,
                               code3 CHAR(3) UNIQUE NOT NULL, -- ISO 3166-1 alpha-3
                               numeric_code CHAR(3) UNIQUE, -- ISO 3166-1 numeric
                               name VARCHAR(100) NOT NULL,
                               official_name VARCHAR(200),
                               native_name VARCHAR(200),

    -- Geographic data
                               capital_city VARCHAR(100),
                               region VARCHAR(100),
                               subregion VARCHAR(100),
                               continent VARCHAR(50),
                               latitude DECIMAL(10,8),
                               longitude DECIMAL(11,8),
                               location GEOMETRY(Point, 4326),
                               boundaries GEOMETRY(MultiPolygon, 4326),
                               area_sq_km DECIMAL(12,2),

    -- Political/Economic
                               independence_date DATE,
                               un_member BOOLEAN DEFAULT FALSE,
                               currency_code common.currency_code,
                               currency_name VARCHAR(100),
                               currency_symbol VARCHAR(10),

    -- Contact info
                               phone_code VARCHAR(10),
                               internet_tld VARCHAR(10),

    -- Language and culture
                               languages TEXT[],
                               official_languages TEXT[],
                               timezones TEXT[],

    -- Travel info
                               visa_required_countries TEXT[], -- Countries that need visa
                               visa_on_arrival_countries TEXT[],
                               visa_free_countries TEXT[],
                               drive_side VARCHAR(10), -- left or right

    -- Population
                               population BIGINT,
                               population_density DECIMAL(10,2),

    -- Status
                               is_active BOOLEAN DEFAULT TRUE,
                               is_supported BOOLEAN DEFAULT TRUE, -- If we operate in this country

    -- Metadata
                               created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_countries_code ON geo.countries(code);
CREATE INDEX idx_countries_name ON geo.countries(name);
CREATE INDEX idx_countries_location ON geo.countries USING GIST(location);
CREATE INDEX idx_countries_boundaries ON geo.countries USING GIST(boundaries);

-- =====================================================
-- STATES/PROVINCES TABLE
-- =====================================================

CREATE TABLE geo.states_provinces (
                                      id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                      country_id UUID NOT NULL REFERENCES geo.countries(id) ON DELETE CASCADE,
                                      code VARCHAR(10),
                                      name VARCHAR(100) NOT NULL,
                                      type VARCHAR(50), -- state, province, territory, region
                                      capital_city VARCHAR(100),

    -- Geographic data
                                      latitude DECIMAL(10,8),
                                      longitude DECIMAL(11,8),
                                      location GEOMETRY(Point, 4326),
                                      boundaries GEOMETRY(MultiPolygon, 4326),
                                      area_sq_km DECIMAL(12,2),

    -- Demographics
                                      population BIGINT,
                                      timezone VARCHAR(50),

    -- Status
                                      is_active BOOLEAN DEFAULT TRUE,

    -- Metadata
                                      created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                      updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                                      UNIQUE(country_id, code)
);

CREATE INDEX idx_states_provinces_country ON geo.states_provinces(country_id);
CREATE INDEX idx_states_provinces_name ON geo.states_provinces(name);
CREATE INDEX idx_states_provinces_location ON geo.states_provinces USING GIST(location);

-- =====================================================
-- CITIES TABLE
-- =====================================================

CREATE TABLE geo.cities (
                            id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                            country_id UUID NOT NULL REFERENCES geo.countries(id) ON DELETE CASCADE,
                            state_province_id UUID REFERENCES geo.states_provinces(id) ON DELETE SET NULL,
                            name VARCHAR(100) NOT NULL,
                            ascii_name VARCHAR(100),
                            alternate_names TEXT[],

    -- Geographic data
                            latitude DECIMAL(10,8) NOT NULL,
                            longitude DECIMAL(11,8) NOT NULL,
                            location GEOMETRY(Point, 4326) NOT NULL,
                            boundaries GEOMETRY(MultiPolygon, 4326),
                            elevation_meters INTEGER,

    -- City classification
                            city_type VARCHAR(50), -- capital, major_city, city, town, village
                            is_capital BOOLEAN DEFAULT FALSE,
                            is_state_capital BOOLEAN DEFAULT FALSE,

    -- Demographics
                            population BIGINT,
                            population_metro BIGINT,
                            population_density DECIMAL(10,2),

    -- Travel info
                            timezone VARCHAR(50) NOT NULL,
                            iata_code CHAR(3), -- Airport code if applicable
                            is_tourist_destination BOOLEAN DEFAULT FALSE,
                            tourist_rating common.rating,

    -- Weather
                            climate_zone VARCHAR(50),
                            avg_temp_summer DECIMAL(3,1),
                            avg_temp_winter DECIMAL(3,1),

    -- Status
                            is_active BOOLEAN DEFAULT TRUE,
                            is_supported BOOLEAN DEFAULT FALSE, -- If we have services here

    -- Metadata
                            geoname_id INTEGER, -- Reference to GeoNames database
                            created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_cities_country ON geo.cities(country_id);
CREATE INDEX idx_cities_state ON geo.cities(state_province_id);
CREATE INDEX idx_cities_name ON geo.cities(name);
CREATE INDEX idx_cities_location ON geo.cities USING GIST(location);
CREATE INDEX idx_cities_population ON geo.cities(population DESC);
CREATE INDEX idx_cities_iata ON geo.cities(iata_code) WHERE iata_code IS NOT NULL;

-- =====================================================
-- DISTRICTS TABLE
-- =====================================================

CREATE TABLE geo.districts (
                               id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                               city_id UUID NOT NULL REFERENCES geo.cities(id) ON DELETE CASCADE,
                               name VARCHAR(100) NOT NULL,
                               code VARCHAR(20),
                               type VARCHAR(50), -- district, ward, neighborhood, suburb

    -- Geographic data
                               latitude DECIMAL(10,8),
                               longitude DECIMAL(11,8),
                               location GEOMETRY(Point, 4326),
                               boundaries GEOMETRY(MultiPolygon, 4326),

    -- Demographics
                               population INTEGER,
                               postal_codes TEXT[],

    -- Status
                               is_active BOOLEAN DEFAULT TRUE,

    -- Metadata
                               created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_districts_city ON geo.districts(city_id);
CREATE INDEX idx_districts_name ON geo.districts(name);
CREATE INDEX idx_districts_location ON geo.districts USING GIST(location);

-- =====================================================
-- AIRPORTS TABLE
-- =====================================================

CREATE TABLE geo.airports (
                              id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                              iata_code CHAR(3) UNIQUE,
                              icao_code CHAR(4) UNIQUE,
                              name VARCHAR(200) NOT NULL,
                              full_name VARCHAR(300),

    -- Location
                              city_id UUID REFERENCES geo.cities(id) ON DELETE SET NULL,
                              country_id UUID NOT NULL REFERENCES geo.countries(id) ON DELETE CASCADE,
                              state_province_id UUID REFERENCES geo.states_provinces(id) ON DELETE SET NULL,

    -- Geographic data
                              latitude DECIMAL(10,8) NOT NULL,
                              longitude DECIMAL(11,8) NOT NULL,
                              location GEOMETRY(Point, 4326) NOT NULL,
                              elevation_meters INTEGER,
                              timezone VARCHAR(50) NOT NULL,

    -- Airport details
                              airport_type VARCHAR(50), -- international, domestic, regional, military
                              size_category VARCHAR(20), -- large, medium, small
                              passenger_capacity BIGINT,
                              terminals INTEGER,
                              runways INTEGER,

    -- Services
                              has_international BOOLEAN DEFAULT FALSE,
                              has_domestic BOOLEAN DEFAULT TRUE,
                              is_hub BOOLEAN DEFAULT FALSE,
                              hub_airlines TEXT[],

    -- Facilities
                              has_railway BOOLEAN DEFAULT FALSE,
                              has_subway BOOLEAN DEFAULT FALSE,
                              has_bus BOOLEAN DEFAULT TRUE,
                              has_taxi BOOLEAN DEFAULT TRUE,
                              has_car_rental BOOLEAN DEFAULT FALSE,
                              has_hotel_shuttle BOOLEAN DEFAULT FALSE,
                              has_lounge BOOLEAN DEFAULT FALSE,
                              has_wifi BOOLEAN DEFAULT TRUE,

    -- Distance from city center
                              distance_from_city_km DECIMAL(6,2),

    -- Status
                              is_active BOOLEAN DEFAULT TRUE,
                              is_supported BOOLEAN DEFAULT TRUE,

    -- Metadata
                              website common.url,
                              phone common.phone,
                              created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_airports_iata ON geo.airports(iata_code);
CREATE INDEX idx_airports_icao ON geo.airports(icao_code);
CREATE INDEX idx_airports_city ON geo.airports(city_id);
CREATE INDEX idx_airports_country ON geo.airports(country_id);
CREATE INDEX idx_airports_location ON geo.airports USING GIST(location);
CREATE INDEX idx_airports_type ON geo.airports(airport_type);

-- =====================================================
-- TRAIN STATIONS TABLE
-- =====================================================

CREATE TABLE geo.train_stations (
                                    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                    code VARCHAR(20) UNIQUE,
                                    name VARCHAR(200) NOT NULL,

    -- Location
                                    city_id UUID NOT NULL REFERENCES geo.cities(id) ON DELETE CASCADE,
                                    country_id UUID NOT NULL REFERENCES geo.countries(id) ON DELETE CASCADE,
                                    district_id UUID REFERENCES geo.districts(id) ON DELETE SET NULL,

    -- Geographic data
                                    latitude DECIMAL(10,8) NOT NULL,
                                    longitude DECIMAL(11,8) NOT NULL,
                                    location GEOMETRY(Point, 4326) NOT NULL,
                                    address TEXT,

    -- Station details
                                    station_type VARCHAR(50), -- central, regional, local, high_speed
                                    platforms INTEGER,
                                    tracks INTEGER,
                                    daily_passengers INTEGER,

    -- Services
                                    has_high_speed BOOLEAN DEFAULT FALSE,
                                    has_international BOOLEAN DEFAULT FALSE,
                                    has_parking BOOLEAN DEFAULT FALSE,
                                    has_taxi BOOLEAN DEFAULT TRUE,
                                    has_bus_connection BOOLEAN DEFAULT TRUE,

    -- Status
                                    is_active BOOLEAN DEFAULT TRUE,
                                    is_supported BOOLEAN DEFAULT FALSE,

    -- Metadata
                                    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_train_stations_code ON geo.train_stations(code);
CREATE INDEX idx_train_stations_city ON geo.train_stations(city_id);
CREATE INDEX idx_train_stations_location ON geo.train_stations USING GIST(location);

-- =====================================================
-- POINTS OF INTEREST TABLE
-- =====================================================

CREATE TABLE geo.points_of_interest (
                                        id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                        name VARCHAR(200) NOT NULL,
                                        description TEXT,

    -- Location
                                        city_id UUID NOT NULL REFERENCES geo.cities(id) ON DELETE CASCADE,
                                        country_id UUID NOT NULL REFERENCES geo.countries(id) ON DELETE CASCADE,
                                        district_id UUID REFERENCES geo.districts(id) ON DELETE SET NULL,

    -- Geographic data
                                        latitude DECIMAL(10,8) NOT NULL,
                                        longitude DECIMAL(11,8) NOT NULL,
                                        location GEOMETRY(Point, 4326) NOT NULL,
                                        address TEXT,
                                        postal_code VARCHAR(20),

    -- POI details
                                        category VARCHAR(100), -- tourist_attraction, restaurant, shopping, etc
                                        subcategory VARCHAR(100),
                                        tags TEXT[],

    -- Tourist info
                                        is_tourist_attraction BOOLEAN DEFAULT FALSE,
                                        tourist_rating common.rating,
                                        review_count INTEGER DEFAULT 0,
                                        popularity_score INTEGER DEFAULT 0,
                                        recommended_duration_minutes INTEGER,

    -- Operating hours
                                        operating_hours JSONB, -- Structured hours per day
                                        is_24_hours BOOLEAN DEFAULT FALSE,
                                        seasonal_schedule JSONB,

    -- Pricing
                                        admission_fee common.price,
                                        price_range VARCHAR(10), -- $, $$, $$$, $$$$

    -- Contact
                                        website common.url,
                                        phone common.phone,
                                        email common.email,

    -- Media
                                        primary_image_url common.url,
                                        images JSONB, -- Array of image objects
                                        virtual_tour_url common.url,

    -- Accessibility
                                        wheelchair_accessible BOOLEAN,
                                        parking_available BOOLEAN,
                                        public_transport_nearby BOOLEAN,

    -- Status
                                        is_active BOOLEAN DEFAULT TRUE,
                                        is_verified BOOLEAN DEFAULT FALSE,

    -- Metadata
                                        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                        updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                        created_by VARCHAR(100),
                                        updated_by VARCHAR(100)
);

CREATE INDEX idx_poi_name ON geo.points_of_interest(name);
CREATE INDEX idx_poi_city ON geo.points_of_interest(city_id);
CREATE INDEX idx_poi_category ON geo.points_of_interest(category);
CREATE INDEX idx_poi_location ON geo.points_of_interest USING GIST(location);
CREATE INDEX idx_poi_tags ON geo.points_of_interest USING GIN(tags);
CREATE INDEX idx_poi_rating ON geo.points_of_interest(tourist_rating) WHERE tourist_rating IS NOT NULL;

-- =====================================================
-- GEOGRAPHIC REGIONS TABLE (For search/grouping)
-- =====================================================

CREATE TABLE geo.regions (
                             id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                             name VARCHAR(200) NOT NULL,
                             slug VARCHAR(200) UNIQUE NOT NULL,
                             description TEXT,
                             region_type VARCHAR(50), -- continent, subcontinent, economic_zone, tourist_region

    -- Geographic data
                             center_latitude DECIMAL(10,8),
                             center_longitude DECIMAL(11,8),
                             center_point GEOMETRY(Point, 4326),
                             boundaries GEOMETRY(MultiPolygon, 4326),

    -- Countries in region
                             country_codes TEXT[],

    -- Tourist info
                             is_tourist_region BOOLEAN DEFAULT FALSE,
                             tourist_highlights TEXT[],
                             best_visit_months INTEGER[],

    -- Status
                             is_active BOOLEAN DEFAULT TRUE,
                             display_order INTEGER DEFAULT 0,

    -- Metadata
                             created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                             updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_regions_slug ON geo.regions(slug);
CREATE INDEX idx_regions_type ON geo.regions(region_type);
CREATE INDEX idx_regions_boundaries ON geo.regions USING GIST(boundaries);

-- =====================================================
-- DISTANCE MATRIX TABLE (Cache distances)
-- =====================================================

CREATE TABLE geo.distance_matrix (
                                     id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                     from_location_type VARCHAR(50), -- city, airport, poi
                                     from_location_id UUID NOT NULL,
                                     to_location_type VARCHAR(50),
                                     to_location_id UUID NOT NULL,

    -- Distance metrics
                                     distance_km DECIMAL(10,2),
                                     distance_miles DECIMAL(10,2),
                                     driving_time_minutes INTEGER,
                                     walking_time_minutes INTEGER,

    -- Route info
                                     route_polyline TEXT,
                                     toll_roads BOOLEAN DEFAULT FALSE,

    -- Metadata
                                     calculated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                     data_source VARCHAR(50), -- google, osm, manual

                                     UNIQUE(from_location_type, from_location_id, to_location_type, to_location_id)
);

CREATE INDEX idx_distance_from ON geo.distance_matrix(from_location_type, from_location_id);
CREATE INDEX idx_distance_to ON geo.distance_matrix(to_location_type, to_location_id);

-- =====================================================
-- TIMEZONE MAPPING TABLE
-- =====================================================

CREATE TABLE geo.timezones (
                               id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                               timezone_name VARCHAR(50),
                               utc_offset_hours DECIMAL(3,1),
                               dst_offset_hours DECIMAL(3,1),
                               uses_dst BOOLEAN DEFAULT FALSE,
                               dst_start VARCHAR(50),
                               dst_end VARCHAR(50),
                               country_codes TEXT[],
                               is_active BOOLEAN DEFAULT TRUE
);

CREATE INDEX idx_timezones_name ON geo.timezones(timezone_name);
CREATE INDEX idx_timezones_offset ON geo.timezones(utc_offset_hours);

-- =====================================================
-- FUNCTIONS
-- =====================================================

-- Function to find nearby locations
CREATE OR REPLACE FUNCTION geo.find_nearby_locations(
    p_latitude DECIMAL,
    p_longitude DECIMAL,
    p_radius_km DECIMAL,
    p_location_type VARCHAR DEFAULT NULL
)
RETURNS TABLE (
    location_id UUID,
    location_name VARCHAR,
    location_type VARCHAR,
    distance_km DECIMAL
) AS $$
BEGIN
RETURN QUERY
    WITH point AS (
        SELECT ST_SetSRID(ST_MakePoint(p_longitude, p_latitude), 4326)::geography AS geog
    )
SELECT * FROM (
                  -- Cities
                  SELECT
                      c.id AS location_id,
                      c.name AS location_name,
                      'city'::VARCHAR AS location_type,
                      ROUND(ST_Distance(c.location::geography, point.geog) / 1000, 2) AS distance_km
                  FROM geo.cities c, point
                  WHERE ST_DWithin(c.location::geography, point.geog, p_radius_km * 1000)
                    AND (p_location_type IS NULL OR p_location_type = 'city')

                  UNION ALL

                  -- Airports
                  SELECT
                      a.id AS location_id,
                      a.name AS location_name,
                      'airport'::VARCHAR AS location_type,
                      ROUND(ST_Distance(a.location::geography, point.geog) / 1000, 2) AS distance_km
                  FROM geo.airports a, point
                  WHERE ST_DWithin(a.location::geography, point.geog, p_radius_km * 1000)
                    AND (p_location_type IS NULL OR p_location_type = 'airport')

                  UNION ALL

                  -- Points of Interest
                  SELECT
                      p.id AS location_id,
                      p.name AS location_name,
                      'poi'::VARCHAR AS location_type,
                      ROUND(ST_Distance(p.location::geography, point.geog) / 1000, 2) AS distance_km
                  FROM geo.points_of_interest p, point
                  WHERE ST_DWithin(p.location::geography, point.geog, p_radius_km * 1000)
                    AND (p_location_type IS NULL OR p_location_type = 'poi')
              ) AS all_locations
ORDER BY distance_km;
END;
$$ LANGUAGE plpgsql;

-- Function to get country by coordinates
CREATE OR REPLACE FUNCTION geo.get_country_by_coords(
    p_latitude DECIMAL,
    p_longitude DECIMAL
)
RETURNS UUID AS $$
DECLARE
country_id UUID;
BEGIN
SELECT c.id INTO country_id
FROM geo.countries c
WHERE ST_Contains(c.boundaries, ST_SetSRID(ST_MakePoint(p_longitude, p_latitude), 4326))
    LIMIT 1;

RETURN country_id;
END;
$$ LANGUAGE plpgsql;

-- Function to calculate distance between two locations using PostGIS
CREATE OR REPLACE FUNCTION geo.calculate_distance_postgis(
    loc1_lat DECIMAL, loc1_lon DECIMAL,
    loc2_lat DECIMAL, loc2_lon DECIMAL
)
RETURNS DECIMAL AS $$
BEGIN
RETURN ROUND(
        ST_Distance(
                ST_SetSRID(ST_MakePoint(loc1_lon, loc1_lat), 4326)::geography,
                ST_SetSRID(ST_MakePoint(loc2_lon, loc2_lat), 4326)::geography
        ) / 1000, 2
       );
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- =====================================================
-- SAMPLE DATA - Major Southeast Asian Locations
-- =====================================================

-- Insert sample countries
INSERT INTO geo.countries (code, code3, name, capital_city, region, currency_code, phone_code, languages, is_supported, latitude, longitude, location)
VALUES
    ('VN', 'VNM', 'Vietnam', 'Hanoi', 'Southeast Asia', 'VND', '+84', ARRAY['Vietnamese'], TRUE, 14.0583, 108.2772, ST_SetSRID(ST_MakePoint(108.2772, 14.0583), 4326)),
    ('TH', 'THA', 'Thailand', 'Bangkok', 'Southeast Asia', 'THB', '+66', ARRAY['Thai'], TRUE, 15.8700, 100.9925, ST_SetSRID(ST_MakePoint(100.9925, 15.8700), 4326)),
    ('SG', 'SGP', 'Singapore', 'Singapore', 'Southeast Asia', 'SGD', '+65', ARRAY['English', 'Malay', 'Mandarin', 'Tamil'], TRUE, 1.3521, 103.8198, ST_SetSRID(ST_MakePoint(103.8198, 1.3521), 4326)),
    ('MY', 'MYS', 'Malaysia', 'Kuala Lumpur', 'Southeast Asia', 'MYR', '+60', ARRAY['Malay', 'English'], TRUE, 4.2105, 101.9758, ST_SetSRID(ST_MakePoint(101.9758, 4.2105), 4326)),
    ('ID', 'IDN', 'Indonesia', 'Jakarta', 'Southeast Asia', 'IDR', '+62', ARRAY['Indonesian'], TRUE, -0.7893, 113.9213, ST_SetSRID(ST_MakePoint(113.9213, -0.7893), 4326)),
    ('PH', 'PHL', 'Philippines', 'Manila', 'Southeast Asia', 'PHP', '+63', ARRAY['Filipino', 'English'], TRUE, 12.8797, 121.7740, ST_SetSRID(ST_MakePoint(121.7740, 12.8797), 4326));

-- Insert sample cities for Vietnam
INSERT INTO geo.cities (country_id, name, latitude, longitude, location, timezone, city_type, is_capital, population, is_supported, iata_code)
VALUES
    ((SELECT id FROM geo.countries WHERE code = 'VN'), 'Ho Chi Minh City', 10.8231, 106.6297, ST_SetSRID(ST_MakePoint(106.6297, 10.8231), 4326), 'Asia/Ho_Chi_Minh', 'major_city', FALSE, 8993000, TRUE, 'SGN'),
    ((SELECT id FROM geo.countries WHERE code = 'VN'), 'Hanoi', 21.0285, 105.8542, ST_SetSRID(ST_MakePoint(105.8542, 21.0285), 4326), 'Asia/Ho_Chi_Minh', 'capital', TRUE, 8054000, TRUE, 'HAN'),
    ((SELECT id FROM geo.countries WHERE code = 'VN'), 'Da Nang', 16.0544, 108.2022, ST_SetSRID(ST_MakePoint(108.2022, 16.0544), 4326), 'Asia/Ho_Chi_Minh', 'major_city', FALSE, 1134000, TRUE, 'DAD'),
    ((SELECT id FROM geo.countries WHERE code = 'VN'), 'Nha Trang', 12.2388, 109.1967, ST_SetSRID(ST_MakePoint(109.1967, 12.2388), 4326), 'Asia/Ho_Chi_Minh', 'city', FALSE, 535000, TRUE, 'CXR'),
    ((SELECT id FROM geo.countries WHERE code = 'VN'), 'Phu Quoc', 10.2271, 103.9578, ST_SetSRID(ST_MakePoint(103.9578, 10.2271), 4326), 'Asia/Ho_Chi_Minh', 'city', FALSE, 180000, TRUE, 'PQC');

-- Insert sample airports
INSERT INTO geo.airports (
    iata_code, icao_code, name, city_id, country_id,
    latitude, longitude, location, timezone,
    airport_type, has_international, has_domestic, is_supported
)
VALUES
    (
        'SGN', 'VVTS', 'Tan Son Nhat International Airport',
        (SELECT id FROM geo.cities WHERE name = 'Ho Chi Minh City'),
        (SELECT id FROM geo.countries WHERE code = 'VN'),
        10.8188, 106.6519, ST_SetSRID(ST_MakePoint(106.6519, 10.8188), 4326),
        'Asia/Ho_Chi_Minh', 'international', TRUE, TRUE, TRUE
    ),
    (
        'HAN', 'VVNB', 'Noi Bai International Airport',
        (SELECT id FROM geo.cities WHERE name = 'Hanoi'),
        (SELECT id FROM geo.countries WHERE code = 'VN'),
        21.2212, 105.8070, ST_SetSRID(ST_MakePoint(105.8070, 21.2212), 4326),
        'Asia/Ho_Chi_Minh', 'international', TRUE, TRUE, TRUE
    ),
    (
        'DAD', 'VVDN', 'Da Nang International Airport',
        (SELECT id FROM geo.cities WHERE name = 'Da Nang'),
        (SELECT id FROM geo.countries WHERE code = 'VN'),
        16.0439, 108.1999, ST_SetSRID(ST_MakePoint(108.1999, 16.0439), 4326),
        'Asia/Ho_Chi_Minh', 'international', TRUE, TRUE, TRUE
    );

-- =====================================================
-- TRIGGERS
-- =====================================================

-- Auto-update updated_at timestamps
CREATE TRIGGER update_countries_updated_at
    BEFORE UPDATE ON geo.countries
    FOR EACH ROW EXECUTE FUNCTION common.update_updated_at_column();

CREATE TRIGGER update_cities_updated_at
    BEFORE UPDATE ON geo.cities
    FOR EACH ROW EXECUTE FUNCTION common.update_updated_at_column();

CREATE TRIGGER update_airports_updated_at
    BEFORE UPDATE ON geo.airports
    FOR EACH ROW EXECUTE FUNCTION common.update_updated_at_column();

CREATE TRIGGER update_poi_updated_at
    BEFORE UPDATE ON geo.points_of_interest
    FOR EACH ROW EXECUTE FUNCTION common.update_updated_at_column();

-- =====================================================
-- MIGRATION METADATA
-- =====================================================

INSERT INTO public.migration_metadata (version, description)
VALUES ('V4', 'Location and Geographic Data with PostGIS - Countries, Cities, Airports, POIs');