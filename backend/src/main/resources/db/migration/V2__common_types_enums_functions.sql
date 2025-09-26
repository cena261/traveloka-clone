-- =====================================================
-- V2: Common Types, Enums, and Utility Functions
-- Description: Create shared domains, enums, and utility functions
-- =====================================================

-- =====================================================
-- DOMAINS
-- =====================================================

-- Email domain with validation
CREATE DOMAIN common.email AS VARCHAR(255)
    CHECK (VALUE ~* '^[A-Za-z0-9._%-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}$');

-- Phone number domain (international format)
CREATE DOMAIN common.phone AS VARCHAR(20)
    CHECK (VALUE ~ '^\+?[1-9]\d{1,14}$');

-- URL domain
CREATE DOMAIN common.url AS TEXT
    CHECK (VALUE ~* '^https?://[-a-zA-Z0-9@:%._\+~#=]{1,256}\.[a-zA-Z0-9()]{1,6}\b([-a-zA-Z0-9()@:%_\+.~#?&//=]*)$');

-- Positive decimal for prices
CREATE DOMAIN common.price AS DECIMAL(12,2)
    CHECK (VALUE >= 0);

-- Percentage domain (0-100)
CREATE DOMAIN common.percentage AS DECIMAL(5,2)
    CHECK (VALUE >= 0 AND VALUE <= 100);

-- Rating domain (1-5 stars)
CREATE DOMAIN common.rating AS DECIMAL(2,1)
    CHECK (VALUE >= 1 AND VALUE <= 5);

-- Currency code (ISO 4217)
CREATE DOMAIN common.currency_code AS CHAR(3)
    CHECK (VALUE ~ '^[A-Z]{3}$');

-- Country code (ISO 3166-1 alpha-2)
CREATE DOMAIN common.country_code AS CHAR(2)
    CHECK (VALUE ~ '^[A-Z]{2}$');

-- Language code (ISO 639-1)
CREATE DOMAIN common.language_code AS CHAR(2)
    CHECK (VALUE ~ '^[a-z]{2}$');

-- =====================================================
-- ENUM TYPES
-- =====================================================

-- Generic status enum
CREATE TYPE common.status AS ENUM (
    'active',
    'inactive',
    'pending',
    'suspended',
    'deleted'
);

-- Record status for soft delete
CREATE TYPE common.record_status AS ENUM (
    'active',
    'archived',
    'deleted'
);

-- Gender enum
CREATE TYPE common.gender AS ENUM (
    'male',
    'female',
    'other',
    'prefer_not_to_say'
);

-- Document type enum
CREATE TYPE common.document_type AS ENUM (
    'passport',
    'national_id',
    'driver_license',
    'visa',
    'birth_certificate'
);

-- Contact type enum
CREATE TYPE common.contact_type AS ENUM (
    'email',
    'phone',
    'whatsapp',
    'telegram',
    'wechat'
);

-- Address type enum
CREATE TYPE common.address_type AS ENUM (
    'home',
    'work',
    'billing',
    'shipping',
    'other'
);

-- Day of week enum
CREATE TYPE common.day_of_week AS ENUM (
    'monday',
    'tuesday',
    'wednesday',
    'thursday',
    'friday',
    'saturday',
    'sunday'
);

-- Priority level enum
CREATE TYPE common.priority_level AS ENUM (
    'low',
    'medium',
    'high',
    'urgent',
    'critical'
);

-- Notification channel enum
CREATE TYPE common.notification_channel AS ENUM (
    'email',
    'sms',
    'push',
    'in_app',
    'whatsapp'
);

-- File type enum
CREATE TYPE common.file_type AS ENUM (
    'image',
    'document',
    'video',
    'audio',
    'other'
);

-- =====================================================
-- COMPOSITE TYPES
-- =====================================================

-- Address composite type
CREATE TYPE common.address AS (
    street_line1 VARCHAR(255),
    street_line2 VARCHAR(255),
    city VARCHAR(100),
    state_province VARCHAR(100),
    postal_code VARCHAR(20),
    country_code CHAR(2),
    latitude DECIMAL(10,8),
    longitude DECIMAL(11,8)
    );

-- Contact info composite type
CREATE TYPE common.contact_info AS (
    type common.contact_type,
    value VARCHAR(255),
    is_primary BOOLEAN,
    is_verified BOOLEAN
    );

-- Localized text composite type
CREATE TYPE common.localized_text AS (
    language_code CHAR(2),
    text TEXT
    );

-- Date range composite type
CREATE TYPE common.date_range AS (
    start_date DATE,
    end_date DATE
    );

-- Time range composite type
CREATE TYPE common.time_range AS (
    start_time TIME,
    end_time TIME
    );

-- =====================================================
-- UTILITY FUNCTIONS
-- =====================================================

-- Generate random string function
CREATE OR REPLACE FUNCTION common.generate_random_string(length INTEGER)
RETURNS TEXT AS $$
DECLARE
chars TEXT := 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    result TEXT := '';
    i INTEGER;
BEGIN
FOR i IN 1..length LOOP
        result := result || substr(chars, floor(random() * length(chars) + 1)::INTEGER, 1);
END LOOP;
RETURN result;
END;
$$ LANGUAGE plpgsql;

-- Generate booking reference number
CREATE OR REPLACE FUNCTION common.generate_booking_ref()
RETURNS VARCHAR(10) AS $$
BEGIN
RETURN upper(common.generate_random_string(3) || '-' ||
             lpad(floor(random() * 1000000)::TEXT, 6, '0'));
END;
$$ LANGUAGE plpgsql;

-- Calculate age from birthdate
CREATE OR REPLACE FUNCTION common.calculate_age(birthdate DATE)
RETURNS INTEGER AS $$
BEGIN
RETURN EXTRACT(YEAR FROM age(CURRENT_DATE, birthdate));
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Validate email format
CREATE OR REPLACE FUNCTION common.is_valid_email(email TEXT)
RETURNS BOOLEAN AS $$
BEGIN
RETURN email ~* '^[A-Za-z0-9._%-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}$';
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Validate phone number format
CREATE OR REPLACE FUNCTION common.is_valid_phone(phone TEXT)
RETURNS BOOLEAN AS $$
BEGIN
RETURN phone ~ '^\+?[1-9]\d{1,14}$';
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Slugify function for URLs
CREATE OR REPLACE FUNCTION common.slugify(input_text TEXT)
RETURNS TEXT AS $$
BEGIN
RETURN lower(
        regexp_replace(
                regexp_replace(
                        unaccent(trim(input_text)),
                        '[^a-zA-Z0-9\s-]', '', 'g'
                ),
                '\s+', '-', 'g'
        )
       );
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Calculate distance between two points (Haversine formula)
CREATE OR REPLACE FUNCTION common.calculate_distance(
    lat1 DECIMAL, lon1 DECIMAL,
    lat2 DECIMAL, lon2 DECIMAL
)
RETURNS DECIMAL AS $$
DECLARE
R DECIMAL := 6371; -- Earth radius in kilometers
    dlat DECIMAL;
    dlon DECIMAL;
    a DECIMAL;
    c DECIMAL;
BEGIN
    dlat := radians(lat2 - lat1);
    dlon := radians(lon2 - lon1);
    a := sin(dlat/2) * sin(dlat/2) + 
         cos(radians(lat1)) * cos(radians(lat2)) * 
         sin(dlon/2) * sin(dlon/2);
    c := 2 * atan2(sqrt(a), sqrt(1-a));
RETURN R * c;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Format currency
CREATE OR REPLACE FUNCTION common.format_currency(
    amount DECIMAL,
    currency_code VARCHAR(3)
)
RETURNS TEXT AS $$
BEGIN
RETURN currency_code || ' ' || to_char(amount, 'FM999,999,999.00');
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Generate UUID v4
CREATE OR REPLACE FUNCTION common.generate_uuid()
RETURNS UUID AS $$
BEGIN
RETURN uuid_generate_v4();
END;
$$ LANGUAGE plpgsql;

-- Trigger function for updated_at timestamp
CREATE OR REPLACE FUNCTION common.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger function for soft delete
CREATE OR REPLACE FUNCTION common.soft_delete()
RETURNS TRIGGER AS $$
BEGIN
    NEW.deleted_at = CURRENT_TIMESTAMP;
    NEW.deleted_by = CURRENT_USER;
    NEW.is_deleted = TRUE;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Function to check overlapping date ranges
CREATE OR REPLACE FUNCTION common.daterange_overlaps(
    range1_start DATE,
    range1_end DATE,
    range2_start DATE,
    range2_end DATE
)
RETURNS BOOLEAN AS $$
BEGIN
RETURN (range1_start, range1_end) OVERLAPS (range2_start, range2_end);
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Function to validate JSON schema
CREATE OR REPLACE FUNCTION common.is_valid_json(input_text TEXT)
RETURNS BOOLEAN AS $$
BEGIN
    PERFORM input_text::JSON;
RETURN TRUE;
EXCEPTION
    WHEN OTHERS THEN
        RETURN FALSE;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- =====================================================
-- AUDIT FUNCTIONS
-- =====================================================

-- Generic audit trigger function
CREATE OR REPLACE FUNCTION common.audit_trigger()
RETURNS TRIGGER AS $$
DECLARE
audit_row JSON;
    changed_fields JSON;
BEGIN
    IF TG_OP = 'INSERT' THEN
        audit_row = row_to_json(NEW);
INSERT INTO audit.audit_log (
    table_name, operation, new_data, user_name, client_ip
) VALUES (
             TG_TABLE_SCHEMA || '.' || TG_TABLE_NAME,
             TG_OP,
             audit_row,
             CURRENT_USER,
             inet_client_addr()
         );
ELSIF TG_OP = 'UPDATE' THEN
        audit_row = row_to_json(NEW);
        changed_fields = row_to_json(OLD);
INSERT INTO audit.audit_log (
    table_name, operation, old_data, new_data, user_name, client_ip
) VALUES (
             TG_TABLE_SCHEMA || '.' || TG_TABLE_NAME,
             TG_OP,
             changed_fields,
             audit_row,
             CURRENT_USER,
             inet_client_addr()
         );
ELSIF TG_OP = 'DELETE' THEN
        audit_row = row_to_json(OLD);
INSERT INTO audit.audit_log (
    table_name, operation, old_data, user_name, client_ip
) VALUES (
             TG_TABLE_SCHEMA || '.' || TG_TABLE_NAME,
             TG_OP,
             audit_row,
             CURRENT_USER,
             inet_client_addr()
         );
END IF;
RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- AUDIT LOG TABLE
-- =====================================================

CREATE TABLE IF NOT EXISTS audit.audit_log (
                                               id BIGSERIAL PRIMARY KEY,
                                               table_name VARCHAR(100) NOT NULL,
    operation VARCHAR(10) NOT NULL,
    old_data JSONB,
    new_data JSONB,
    user_name VARCHAR(100),
    client_ip INET,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                             );

CREATE INDEX idx_audit_log_table_name ON audit.audit_log(table_name);
CREATE INDEX idx_audit_log_operation ON audit.audit_log(operation);
CREATE INDEX idx_audit_log_created_at ON audit.audit_log(created_at);
CREATE INDEX idx_audit_log_user_name ON audit.audit_log(user_name);

-- =====================================================
-- MIGRATION METADATA
-- =====================================================

INSERT INTO public.migration_metadata (version, description)
VALUES ('V2', 'Common types, enums, and utility functions');