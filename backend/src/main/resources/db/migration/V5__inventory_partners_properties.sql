-- =====================================================
-- V5: Inventory - Partners and Properties
-- Description: Core business entities - Partners, Properties, Hotels, Rooms, Flights, Airlines
-- Author: Traveloka Clone Team
-- Date: 2025-01-01
-- =====================================================

-- =====================================================
-- PARTNER TYPES ENUM
-- =====================================================

CREATE TYPE inventory.partner_type AS ENUM (
    'hotel',
    'airline',
    'car_rental',
    'tour_operator',
    'restaurant',
    'activity_provider',
    'transport',
    'insurance'
);

CREATE TYPE inventory.property_type AS ENUM (
    'hotel',
    'resort',
    'apartment',
    'villa',
    'hostel',
    'guesthouse',
    'homestay',
    'motel',
    'boutique_hotel',
    'bed_breakfast'
);

CREATE TYPE inventory.room_type AS ENUM (
    'single',
    'double',
    'twin',
    'triple',
    'quad',
    'suite',
    'junior_suite',
    'deluxe',
    'executive',
    'presidential',
    'studio',
    'family',
    'dormitory'
);

CREATE TYPE inventory.bed_type AS ENUM (
    'single',
    'double',
    'queen',
    'king',
    'twin',
    'bunk',
    'sofa_bed',
    'futon',
    'murphy_bed'
);

CREATE TYPE inventory.board_type AS ENUM (
    'room_only',
    'bed_breakfast',
    'half_board',
    'full_board',
    'all_inclusive'
);

CREATE TYPE inventory.flight_class AS ENUM (
    'economy',
    'premium_economy',
    'business',
    'first'
);

CREATE TYPE inventory.aircraft_type AS ENUM (
    'narrow_body',
    'wide_body',
    'regional_jet',
    'turboprop'
);

-- =====================================================
-- PARTNERS TABLE
-- =====================================================

CREATE TABLE inventory.partners (
                                    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                    code VARCHAR(50) UNIQUE NOT NULL,
                                    name VARCHAR(200) NOT NULL,
                                    legal_name VARCHAR(300),
                                    partner_type inventory.partner_type NOT NULL,

    -- Company information
                                    registration_number VARCHAR(100),
                                    tax_number VARCHAR(100),
                                    founded_date DATE,
                                    website common.url,
                                    description TEXT,

    -- Contact information
                                    primary_email common.email NOT NULL,
                                    primary_phone common.phone NOT NULL,
                                    support_email common.email,
                                    support_phone common.phone,
                                    emergency_phone common.phone,

    -- Address
                                    headquarters_address TEXT,
                                    headquarters_city VARCHAR(100),
                                    headquarters_country common.country_code,
                                    billing_address TEXT,

    -- Business details
                                    commission_rate common.percentage,
                                    payment_terms_days INTEGER DEFAULT 30,
                                    contract_start_date DATE,
                                    contract_end_date DATE,
                                    credit_limit common.price,

    -- Banking
                                    bank_name VARCHAR(200),
                                    bank_account_number VARCHAR(100),
                                    bank_account_name VARCHAR(200),
                                    bank_swift_code VARCHAR(20),
                                    bank_iban VARCHAR(50),

    -- API Integration
                                    api_endpoint common.url,
                                    api_key_encrypted TEXT,
                                    api_version VARCHAR(20),
                                    webhook_url common.url,

    -- Status
                                    status common.status DEFAULT 'pending',
                                    verification_status VARCHAR(50) DEFAULT 'pending',
                                    verified_at TIMESTAMP WITH TIME ZONE,
                                    verified_by VARCHAR(100),

    -- Ratings
                                    overall_rating common.rating,
                                    service_rating common.rating,
                                    reliability_rating common.rating,
                                    total_reviews INTEGER DEFAULT 0,

    -- Statistics
                                    total_bookings INTEGER DEFAULT 0,
                                    total_revenue common.price DEFAULT 0,
                                    cancellation_rate common.percentage DEFAULT 0,
                                    dispute_rate common.percentage DEFAULT 0,

    -- Metadata
                                    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                    created_by VARCHAR(100),
                                    updated_by VARCHAR(100),
                                    is_deleted BOOLEAN DEFAULT FALSE,
                                    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_partners_code ON inventory.partners(code);
CREATE INDEX idx_partners_type ON inventory.partners(partner_type);
CREATE INDEX idx_partners_status ON inventory.partners(status) WHERE is_deleted = FALSE;
CREATE INDEX idx_partners_country ON inventory.partners(headquarters_country);

-- =====================================================
-- PARTNER CONTACTS TABLE
-- =====================================================

CREATE TABLE inventory.partner_contacts (
                                            id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                            partner_id UUID NOT NULL REFERENCES inventory.partners(id) ON DELETE CASCADE,

    -- Contact person
                                            first_name VARCHAR(100) NOT NULL,
                                            last_name VARCHAR(100) NOT NULL,
                                            title VARCHAR(100),
                                            department VARCHAR(100),

    -- Contact details
                                            email common.email NOT NULL,
                                            phone common.phone,
                                            mobile common.phone,

    -- Role
                                            is_primary BOOLEAN DEFAULT FALSE,
                                            is_technical BOOLEAN DEFAULT FALSE,
                                            is_billing BOOLEAN DEFAULT FALSE,
                                            is_emergency BOOLEAN DEFAULT FALSE,

    -- Status
                                            is_active BOOLEAN DEFAULT TRUE,

    -- Metadata
                                            created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                            updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_partner_contacts_partner ON inventory.partner_contacts(partner_id);
CREATE INDEX idx_partner_contacts_email ON inventory.partner_contacts(email);

-- =====================================================
-- PROPERTIES TABLE (Hotels, Resorts, etc.)
-- =====================================================

CREATE TABLE inventory.properties (
                                      id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                      partner_id UUID NOT NULL REFERENCES inventory.partners(id) ON DELETE CASCADE,
                                      code VARCHAR(50) UNIQUE NOT NULL,
                                      name VARCHAR(200) NOT NULL,
                                      property_type inventory.property_type NOT NULL,

    -- Classification
                                      star_rating INTEGER CHECK (star_rating >= 1 AND star_rating <= 5),
                                      chain_name VARCHAR(200),
                                      brand VARCHAR(200),

    -- Location
                                      address TEXT NOT NULL,
                                      city_id UUID NOT NULL REFERENCES geo.cities(id),
                                      country_id UUID NOT NULL REFERENCES geo.countries(id),
                                      district_id UUID REFERENCES geo.districts(id),
                                      postal_code VARCHAR(20),

    -- Geographic data
                                      latitude DECIMAL(10,8) NOT NULL,
                                      longitude DECIMAL(11,8) NOT NULL,
                                      location GEOMETRY(Point, 4326) NOT NULL,

    -- Contact
                                      phone common.phone,
                                      email common.email,
                                      website common.url,

    -- Property details
                                      total_rooms INTEGER NOT NULL,
                                      total_floors INTEGER,
                                      built_year INTEGER,
                                      renovated_year INTEGER,
                                      check_in_time TIME DEFAULT '14:00',
                                      check_out_time TIME DEFAULT '12:00',

    -- Descriptions
                                      description TEXT,
                                      short_description VARCHAR(500),
                                      cancellation_policy TEXT,
                                      house_rules TEXT,

    -- Amenities (stored as array for quick filtering)
                                      amenities TEXT[],

    -- Images
                                      primary_image_url common.url,
                                      logo_url common.url,

    -- Payment
                                      accepted_payment_methods TEXT[],
                                      deposit_required BOOLEAN DEFAULT FALSE,
                                      deposit_amount common.price,
                                      deposit_type VARCHAR(50), -- fixed, percentage

    -- Policies
                                      pets_allowed BOOLEAN DEFAULT FALSE,
                                      smoking_allowed BOOLEAN DEFAULT FALSE,
                                      children_allowed BOOLEAN DEFAULT TRUE,
                                      min_age_requirement INTEGER DEFAULT 18,

    -- Ratings
                                      overall_rating common.rating,
                                      cleanliness_rating common.rating,
                                      comfort_rating common.rating,
                                      location_rating common.rating,
                                      facilities_rating common.rating,
                                      staff_rating common.rating,
                                      value_rating common.rating,
                                      total_reviews INTEGER DEFAULT 0,

    -- Status
                                      status common.status DEFAULT 'pending',
                                      is_active BOOLEAN DEFAULT TRUE,
                                      is_featured BOOLEAN DEFAULT FALSE,
                                      featured_until TIMESTAMP WITH TIME ZONE,

    -- SEO
                                      slug VARCHAR(255) UNIQUE,
                                      meta_title VARCHAR(255),
                                      meta_description TEXT,
                                      meta_keywords TEXT[],

    -- Metadata
                                      created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                      updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                      created_by VARCHAR(100),
                                      updated_by VARCHAR(100),
                                      is_deleted BOOLEAN DEFAULT FALSE,
                                      deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_properties_partner ON inventory.properties(partner_id);
CREATE INDEX idx_properties_code ON inventory.properties(code);
CREATE INDEX idx_properties_city ON inventory.properties(city_id);
CREATE INDEX idx_properties_location ON inventory.properties USING GIST(location);
CREATE INDEX idx_properties_type ON inventory.properties(property_type);
CREATE INDEX idx_properties_status ON inventory.properties(status) WHERE is_deleted = FALSE;
CREATE INDEX idx_properties_rating ON inventory.properties(overall_rating) WHERE overall_rating IS NOT NULL;
CREATE INDEX idx_properties_amenities ON inventory.properties USING GIN(amenities);
CREATE INDEX idx_properties_slug ON inventory.properties(slug);

-- =====================================================
-- PROPERTY AMENITIES TABLE
-- =====================================================

CREATE TABLE inventory.property_amenities (
                                              id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                              property_id UUID NOT NULL REFERENCES inventory.properties(id) ON DELETE CASCADE,
                                              category VARCHAR(100) NOT NULL, -- general, room, bathroom, kitchen, entertainment, etc
                                              name VARCHAR(200) NOT NULL,
                                              description TEXT,
                                              icon VARCHAR(100),
                                              is_free BOOLEAN DEFAULT TRUE,
                                              additional_charge common.price,
                                              is_available BOOLEAN DEFAULT TRUE,

    -- Metadata
                                              created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                              updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_property_amenities_property ON inventory.property_amenities(property_id);
CREATE INDEX idx_property_amenities_category ON inventory.property_amenities(category);

-- =====================================================
-- ROOM TYPES TABLE
-- =====================================================

CREATE TABLE inventory.room_types (
                                      id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                      property_id UUID NOT NULL REFERENCES inventory.properties(id) ON DELETE CASCADE,
                                      code VARCHAR(50) NOT NULL,
                                      name VARCHAR(200) NOT NULL,
                                      room_type inventory.room_type NOT NULL,

    -- Room details
                                      size_sqm DECIMAL(6,2),
                                      max_occupancy INTEGER NOT NULL,
                                      adults_capacity INTEGER NOT NULL,
                                      children_capacity INTEGER DEFAULT 0,
                                      infants_capacity INTEGER DEFAULT 0,

    -- Bed configuration
                                      bed_types JSONB, -- Array of {type, count}
                                      total_beds INTEGER,

    -- Features
                                      description TEXT,
                                      amenities TEXT[],
                                      view_type VARCHAR(100), -- sea_view, city_view, garden_view, etc
                                      floor_numbers INTEGER[],

    -- Pricing
                                      base_price common.price NOT NULL,
                                      extra_adult_charge common.price DEFAULT 0,
                                      extra_child_charge common.price DEFAULT 0,

    -- Inventory
                                      total_rooms INTEGER NOT NULL,

    -- Images
                                      primary_image_url common.url,

    -- Status
                                      is_active BOOLEAN DEFAULT TRUE,
                                      is_bookable BOOLEAN DEFAULT TRUE,

    -- Metadata
                                      created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                      updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                                      UNIQUE(property_id, code)
);

CREATE INDEX idx_room_types_property ON inventory.room_types(property_id);
CREATE INDEX idx_room_types_type ON inventory.room_types(room_type);
CREATE INDEX idx_room_types_occupancy ON inventory.room_types(max_occupancy);
CREATE INDEX idx_room_types_price ON inventory.room_types(base_price);

-- =====================================================
-- PROPERTY IMAGES TABLE
-- =====================================================

CREATE TABLE inventory.property_images (
                                           id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                           property_id UUID NOT NULL REFERENCES inventory.properties(id) ON DELETE CASCADE,
                                           room_type_id UUID REFERENCES inventory.room_types(id) ON DELETE CASCADE,

    -- Image details
                                           url common.url NOT NULL,
                                           thumbnail_url common.url,
                                           title VARCHAR(200),
                                           description TEXT,
                                           category VARCHAR(100), -- exterior, lobby, room, bathroom, amenity, dining, etc

    -- Order
                                           display_order INTEGER DEFAULT 0,
                                           is_primary BOOLEAN DEFAULT FALSE,

    -- Status
                                           is_active BOOLEAN DEFAULT TRUE,

    -- Metadata
                                           uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                           uploaded_by VARCHAR(100)
);

CREATE INDEX idx_property_images_property ON inventory.property_images(property_id);
CREATE INDEX idx_property_images_room_type ON inventory.property_images(room_type_id);
CREATE INDEX idx_property_images_category ON inventory.property_images(category);

-- =====================================================
-- AIRLINES TABLE
-- =====================================================

CREATE TABLE inventory.airlines (
                                    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                    partner_id UUID REFERENCES inventory.partners(id) ON DELETE CASCADE,
                                    iata_code CHAR(2) UNIQUE NOT NULL,
                                    icao_code CHAR(3) UNIQUE,
                                    name VARCHAR(200) NOT NULL,

    -- Airline details
                                    country_id UUID NOT NULL REFERENCES geo.countries(id),
                                    hub_airports TEXT[], -- Array of IATA codes
                                    alliance VARCHAR(50), -- star_alliance, oneworld, skyteam

    -- Fleet
                                    fleet_size INTEGER,
                                    average_fleet_age DECIMAL(4,1),

    -- Services
                                    has_online_checkin BOOLEAN DEFAULT TRUE,
                                    has_mobile_app BOOLEAN DEFAULT TRUE,
                                    has_meal_service BOOLEAN DEFAULT TRUE,
                                    has_seat_selection BOOLEAN DEFAULT TRUE,
                                    has_wifi BOOLEAN DEFAULT FALSE,
                                    has_entertainment BOOLEAN DEFAULT TRUE,

    -- Baggage policy
                                    carry_on_allowance_kg DECIMAL(4,1),
                                    checked_baggage_allowance_kg DECIMAL(4,1),
                                    excess_baggage_fee_per_kg common.price,

    -- Ratings
                                    overall_rating common.rating,
                                    punctuality_rating common.rating,
                                    service_rating common.rating,
                                    comfort_rating common.rating,
                                    value_rating common.rating,
                                    total_reviews INTEGER DEFAULT 0,

    -- Status
                                    status common.status DEFAULT 'active',
                                    is_active BOOLEAN DEFAULT TRUE,
                                    is_low_cost BOOLEAN DEFAULT FALSE,

    -- Metadata
                                    logo_url common.url,
                                    website common.url,
                                    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_airlines_partner ON inventory.airlines(partner_id);
CREATE INDEX idx_airlines_iata ON inventory.airlines(iata_code);
CREATE INDEX idx_airlines_country ON inventory.airlines(country_id);
CREATE INDEX idx_airlines_alliance ON inventory.airlines(alliance);

-- =====================================================
-- AIRCRAFT TABLE
-- =====================================================

CREATE TABLE inventory.aircraft (
                                    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                    airline_id UUID NOT NULL REFERENCES inventory.airlines(id) ON DELETE CASCADE,
                                    registration_number VARCHAR(20) UNIQUE NOT NULL,

    -- Aircraft details
                                    manufacturer VARCHAR(100) NOT NULL,
                                    model VARCHAR(100) NOT NULL,
                                    aircraft_type inventory.aircraft_type NOT NULL,

    -- Capacity
                                    economy_seats INTEGER DEFAULT 0,
                                    premium_economy_seats INTEGER DEFAULT 0,
                                    business_seats INTEGER DEFAULT 0,
                                    first_seats INTEGER DEFAULT 0,
                                    total_seats INTEGER NOT NULL,

    -- Configuration
                                    seat_configuration JSONB,
                                    has_wifi BOOLEAN DEFAULT FALSE,
                                    has_entertainment BOOLEAN DEFAULT TRUE,
                                    has_power_outlets BOOLEAN DEFAULT FALSE,
                                    has_usb_ports BOOLEAN DEFAULT TRUE,

    -- Status
                                    is_active BOOLEAN DEFAULT TRUE,
                                    manufactured_year INTEGER,

    -- Metadata
                                    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_aircraft_airline ON inventory.aircraft(airline_id);
CREATE INDEX idx_aircraft_model ON inventory.aircraft(model);
CREATE INDEX idx_aircraft_type ON inventory.aircraft(aircraft_type);

-- =====================================================
-- FLIGHT ROUTES TABLE
-- =====================================================

CREATE TABLE inventory.flight_routes (
                                         id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                         airline_id UUID NOT NULL REFERENCES inventory.airlines(id) ON DELETE CASCADE,

    -- Route
                                         origin_airport_id UUID NOT NULL REFERENCES geo.airports(id),
                                         destination_airport_id UUID NOT NULL REFERENCES geo.airports(id),

    -- Route details
                                         route_code VARCHAR(20),
                                         distance_km DECIMAL(8,2),
                                         typical_duration_minutes INTEGER,

    -- Frequency
                                         flights_per_week INTEGER,
                                         seasonal BOOLEAN DEFAULT FALSE,
                                         season_start_month INTEGER,
                                         season_end_month INTEGER,

    -- Status
                                         is_active BOOLEAN DEFAULT TRUE,
                                         is_international BOOLEAN,

    -- Metadata
                                         created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                         updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                                         UNIQUE(airline_id, origin_airport_id, destination_airport_id)
);

CREATE INDEX idx_flight_routes_airline ON inventory.flight_routes(airline_id);
CREATE INDEX idx_flight_routes_origin ON inventory.flight_routes(origin_airport_id);
CREATE INDEX idx_flight_routes_destination ON inventory.flight_routes(destination_airport_id);

-- =====================================================
-- FLIGHT SCHEDULES TABLE
-- =====================================================

CREATE TABLE inventory.flight_schedules (
                                            id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                            route_id UUID NOT NULL REFERENCES inventory.flight_routes(id) ON DELETE CASCADE,
                                            flight_number VARCHAR(10) NOT NULL,

    -- Schedule
                                            departure_time TIME NOT NULL,
                                            arrival_time TIME NOT NULL,
                                            duration_minutes INTEGER NOT NULL,

    -- Days of operation (bit mask: Mon=1, Tue=2, Wed=4, Thu=8, Fri=16, Sat=32, Sun=64)
                                            operating_days INTEGER NOT NULL,

    -- Aircraft
                                            typical_aircraft_id UUID REFERENCES inventory.aircraft(id),

    -- Valid period
                                            valid_from DATE NOT NULL,
                                            valid_until DATE,

    -- Status
                                            is_active BOOLEAN DEFAULT TRUE,

    -- Metadata
                                            created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                            updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                                            UNIQUE(route_id, flight_number, departure_time)
);

CREATE INDEX idx_flight_schedules_route ON inventory.flight_schedules(route_id);
CREATE INDEX idx_flight_schedules_number ON inventory.flight_schedules(flight_number);
CREATE INDEX idx_flight_schedules_valid ON inventory.flight_schedules(valid_from, valid_until);

-- =====================================================
-- TRIGGERS
-- =====================================================

-- Auto-update updated_at timestamps
CREATE TRIGGER update_partners_updated_at
    BEFORE UPDATE ON inventory.partners
    FOR EACH ROW EXECUTE FUNCTION common.update_updated_at_column();

CREATE TRIGGER update_properties_updated_at
    BEFORE UPDATE ON inventory.properties
    FOR EACH ROW EXECUTE FUNCTION common.update_updated_at_column();

CREATE TRIGGER update_room_types_updated_at
    BEFORE UPDATE ON inventory.room_types
    FOR EACH ROW EXECUTE FUNCTION common.update_updated_at_column();

CREATE TRIGGER update_airlines_updated_at
    BEFORE UPDATE ON inventory.airlines
    FOR EACH ROW EXECUTE FUNCTION common.update_updated_at_column();

-- Auto-generate slugs for properties
CREATE OR REPLACE FUNCTION inventory.generate_property_slug()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.slug IS NULL THEN
        NEW.slug := common.slugify(NEW.name || '-' || NEW.code);
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER generate_property_slug_trigger
    BEFORE INSERT OR UPDATE ON inventory.properties
                         FOR EACH ROW EXECUTE FUNCTION inventory.generate_property_slug();

-- =====================================================
-- FUNCTIONS
-- =====================================================

-- Function to search properties by location
CREATE OR REPLACE FUNCTION inventory.search_properties_nearby(
    p_latitude DECIMAL,
    p_longitude DECIMAL,
    p_radius_km DECIMAL,
    p_property_type inventory.property_type DEFAULT NULL
)
RETURNS TABLE (
    property_id UUID,
    property_name VARCHAR,
    property_type inventory.property_type,
    distance_km DECIMAL,
    rating common.rating
) AS $$
BEGIN
RETURN QUERY
SELECT
    p.id AS property_id,
    p.name AS property_name,
    p.property_type,
    ROUND(ST_Distance(
                  p.location::geography,
                  ST_SetSRID(ST_MakePoint(p_longitude, p_latitude), 4326)::geography
          ) / 1000, 2) AS distance_km,
    p.overall_rating AS rating
FROM inventory.properties p
WHERE ST_DWithin(
        p.location::geography,
        ST_SetSRID(ST_MakePoint(p_longitude, p_latitude), 4326)::geography,
        p_radius_km * 1000
      )
  AND p.is_active = TRUE
  AND p.is_deleted = FALSE
  AND (p_property_type IS NULL OR p.property_type = p_property_type)
ORDER BY distance_km;
END;
$$ LANGUAGE plpgsql;

-- Function to get property availability summary
CREATE OR REPLACE FUNCTION inventory.get_property_availability_summary(
    p_property_id UUID,
    p_check_in DATE,
    p_check_out DATE
)
RETURNS TABLE (
    room_type_id UUID,
    room_type_name VARCHAR,
    total_rooms INTEGER,
    available_rooms INTEGER,
    base_price DECIMAL
) AS $$
BEGIN
    -- This is a placeholder that will be implemented when availability tables are created
RETURN QUERY
SELECT
    rt.id AS room_type_id,
    rt.name AS room_type_name,
    rt.total_rooms,
    rt.total_rooms AS available_rooms, -- Placeholder
    rt.base_price
FROM inventory.room_types rt
WHERE rt.property_id = p_property_id
  AND rt.is_active = TRUE
  AND rt.is_bookable = TRUE;
END;
$$ LANGUAGE plpgsql;

-- Function to calculate airline route statistics
CREATE OR REPLACE FUNCTION inventory.calculate_route_statistics(
    p_origin_airport_id UUID,
    p_destination_airport_id UUID
)
RETURNS TABLE (
    airline_id UUID,
    airline_name VARCHAR,
    flights_per_week INTEGER,
    average_duration_minutes INTEGER,
    has_direct_flights BOOLEAN
) AS $$
BEGIN
RETURN QUERY
SELECT
    a.id AS airline_id,
    a.name AS airline_name,
    fr.flights_per_week,
    fr.typical_duration_minutes AS average_duration_minutes,
    TRUE AS has_direct_flights
FROM inventory.flight_routes fr
         JOIN inventory.airlines a ON fr.airline_id = a.id
WHERE fr.origin_airport_id = p_origin_airport_id
  AND fr.destination_airport_id = p_destination_airport_id
  AND fr.is_active = TRUE
  AND a.is_active = TRUE
ORDER BY fr.flights_per_week DESC;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- SAMPLE DATA
-- =====================================================

-- Insert sample hotel partner
INSERT INTO inventory.partners (
    code, name, partner_type, primary_email, primary_phone,
    headquarters_country, status, commission_rate
)
VALUES (
           'HTL001',
           'Luxury Hotels Group',
           'hotel',
           'partners@luxuryhotels.com',
           '+84901234567',
           'VN',
           'active',
           15.00
       );

-- Insert sample airline partner
INSERT INTO inventory.partners (
    code, name, partner_type, primary_email, primary_phone,
    headquarters_country, status, commission_rate
)
VALUES (
           'AIR001',
           'Vietnam Airlines Partnership',
           'airline',
           'partners@vietnamairlines.com',
           '+84901234568',
           'VN',
           'active',
           8.00
       );

-- Insert sample property
INSERT INTO inventory.properties (
    partner_id, code, name, property_type, star_rating,
    address, city_id, country_id,
    latitude, longitude, location,
    total_rooms, email, phone,
    description, amenities, overall_rating, status
)
VALUES (
           (SELECT id FROM inventory.partners WHERE code = 'HTL001'),
           'SGN-HTL-001',
           'Saigon Luxury Hotel',
           'hotel',
           5,
           '123 Nguyen Hue Street, District 1',
           (SELECT id FROM geo.cities WHERE name = 'Ho Chi Minh City'),
           (SELECT id FROM geo.countries WHERE code = 'VN'),
           10.7756, 106.7019,
           ST_SetSRID(ST_MakePoint(106.7019, 10.7756), 4326),
           200,
           'info@saigonluxury.com',
           '+84283456789',
           'A luxury hotel in the heart of Saigon',
           ARRAY['wifi', 'pool', 'gym', 'spa', 'restaurant', 'bar', 'parking'],
           4.5,
           'active'
       );

-- Insert sample room types
INSERT INTO inventory.room_types (
    property_id, code, name, room_type,
    size_sqm, max_occupancy, adults_capacity, children_capacity,
    bed_types, total_beds, base_price, total_rooms,
    amenities, is_active
)
VALUES
    (
        (SELECT id FROM inventory.properties WHERE code = 'SGN-HTL-001'),
        'DELUXE',
        'Deluxe Room',
        'deluxe',
        35, 2, 2, 1,
        '[{"type": "king", "count": 1}]'::jsonb,
        1, 1500000, 50,
        ARRAY['wifi', 'minibar', 'safe', 'air_conditioning'],
        TRUE
    ),
    (
        (SELECT id FROM inventory.properties WHERE code = 'SGN-HTL-001'),
        'SUITE',
        'Executive Suite',
        'suite',
        65, 4, 2, 2,
        '[{"type": "king", "count": 1}, {"type": "single", "count": 2}]'::jsonb,
        3, 3500000, 20,
        ARRAY['wifi', 'minibar', 'safe', 'air_conditioning', 'living_room', 'kitchenette'],
        TRUE
    );

-- Insert sample airline
INSERT INTO inventory.airlines (
    partner_id, iata_code, icao_code, name,
    country_id, hub_airports,
    carry_on_allowance_kg, checked_baggage_allowance_kg,
    overall_rating, status, is_active
)
VALUES (
           (SELECT id FROM inventory.partners WHERE code = 'AIR001'),
           'VN', 'HVN', 'Vietnam Airlines',
           (SELECT id FROM geo.countries WHERE code = 'VN'),
           ARRAY['SGN', 'HAN'],
           7, 23,
           4.2, 'active', TRUE
       );

-- Insert sample flight route
INSERT INTO inventory.flight_routes (
    airline_id,
    origin_airport_id,
    destination_airport_id,
    route_code,
    distance_km,
    typical_duration_minutes,
    flights_per_week,
    is_active,
    is_international
)
VALUES (
           (SELECT id FROM inventory.airlines WHERE iata_code = 'VN'),
           (SELECT id FROM geo.airports WHERE iata_code = 'SGN'),
           (SELECT id FROM geo.airports WHERE iata_code = 'HAN'),
           'VN-SGN-HAN',
           1177,
           125,
           70,
           TRUE,
           FALSE
       );

-- =====================================================
-- VIEWS
-- =====================================================

-- View for property search results
CREATE OR REPLACE VIEW inventory.v_property_search AS
SELECT
    p.id,
    p.code,
    p.name,
    p.property_type,
    p.star_rating,
    p.slug,
    c.name AS city_name,
    co.name AS country_name,
    co.code AS country_code,
    p.latitude,
    p.longitude,
    p.address,
    p.overall_rating,
    p.total_reviews,
    p.amenities,
    p.primary_image_url,
    p.status,
    p.is_featured,
    MIN(rt.base_price) AS min_price,
    MAX(rt.base_price) AS max_price,
    COUNT(DISTINCT rt.id) AS room_type_count
FROM inventory.properties p
         JOIN geo.cities c ON p.city_id = c.id
         JOIN geo.countries co ON p.country_id = co.id
         LEFT JOIN inventory.room_types rt ON p.id = rt.property_id AND rt.is_active = TRUE
WHERE p.is_deleted = FALSE
  AND p.is_active = TRUE
GROUP BY
    p.id, p.code, p.name, p.property_type, p.star_rating,
    p.slug, c.name, co.name, co.code, p.latitude, p.longitude,
    p.address, p.overall_rating, p.total_reviews, p.amenities,
    p.primary_image_url, p.status, p.is_featured;

-- View for flight search results
CREATE OR REPLACE VIEW inventory.v_flight_search AS
SELECT
    fr.id AS route_id,
    a.id AS airline_id,
    a.iata_code AS airline_code,
    a.name AS airline_name,
    a.logo_url AS airline_logo,
    orig.iata_code AS origin_code,
    orig.name AS origin_airport,
    orig_city.name AS origin_city,
    dest.iata_code AS destination_code,
    dest.name AS destination_airport,
    dest_city.name AS destination_city,
    fr.distance_km,
    fr.typical_duration_minutes,
    fr.flights_per_week,
    a.overall_rating AS airline_rating,
    a.is_low_cost,
    fr.is_international
FROM inventory.flight_routes fr
         JOIN inventory.airlines a ON fr.airline_id = a.id
         JOIN geo.airports orig ON fr.origin_airport_id = orig.id
         JOIN geo.airports dest ON fr.destination_airport_id = dest.id
         JOIN geo.cities orig_city ON orig.city_id = orig_city.id
         JOIN geo.cities dest_city ON dest.city_id = dest_city.id
WHERE fr.is_active = TRUE
  AND a.is_active = TRUE;

-- =====================================================
-- INDEXES FOR PERFORMANCE
-- =====================================================

-- Additional composite indexes for common queries
CREATE INDEX idx_properties_search ON inventory.properties(city_id, property_type, status)
    WHERE is_deleted = FALSE AND is_active = TRUE;

CREATE INDEX idx_room_types_search ON inventory.room_types(property_id, base_price, max_occupancy)
    WHERE is_active = TRUE AND is_bookable = TRUE;

CREATE INDEX idx_flight_routes_search ON inventory.flight_routes(origin_airport_id, destination_airport_id, airline_id)
    WHERE is_active = TRUE;

-- =====================================================
-- MIGRATION METADATA
-- =====================================================

INSERT INTO public.migration_metadata (version, description)
VALUES ('V5', 'Inventory - Partners, Properties, Hotels, Rooms, Airlines, and Flight Routes');