-- =====================================================
-- V1: Bootstrap Extensions and Schemas
-- Description: Initialize database with required extensions and schemas
-- =====================================================

-- =====================================================
-- EXTENSIONS
-- =====================================================

-- UUID Generation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- PostGIS for geographic data
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS postgis_topology;

-- Full text search
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;

-- Cryptography
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Time-based operations
CREATE EXTENSION IF NOT EXISTS btree_gin;
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- JSON operations enhancement
CREATE EXTENSION IF NOT EXISTS "hstore";

-- =====================================================
-- SCHEMAS
-- =====================================================

-- IAM (Identity & Access Management)
CREATE SCHEMA IF NOT EXISTS iam;
COMMENT ON SCHEMA iam IS 'Identity and Access Management - Users, Roles, Permissions, Sessions';

-- Inventory Management
CREATE SCHEMA IF NOT EXISTS inventory;
COMMENT ON SCHEMA inventory IS 'Inventory Management - Partners, Properties, Rooms, Flights, etc.';

-- Availability Management
CREATE SCHEMA IF NOT EXISTS availability;
COMMENT ON SCHEMA availability IS 'Availability Management - Room availability, Flight seats, etc.';

-- Pricing Management
CREATE SCHEMA IF NOT EXISTS pricing;
COMMENT ON SCHEMA pricing IS 'Pricing Management - Dynamic pricing, Promotions, Discounts';

-- Booking Management
CREATE SCHEMA IF NOT EXISTS booking;
COMMENT ON SCHEMA booking IS 'Booking Management - Reservations, Booking status, Cancellations';

-- Payment Processing
CREATE SCHEMA IF NOT EXISTS payment;
COMMENT ON SCHEMA payment IS 'Payment Processing - Transactions, Payment methods, Refunds';

-- Review & Rating
CREATE SCHEMA IF NOT EXISTS review;
COMMENT ON SCHEMA review IS 'Review and Rating System - Customer reviews, Ratings, Responses';

-- Notification System
CREATE SCHEMA IF NOT EXISTS notify;
COMMENT ON SCHEMA notify IS 'Notification System - Email, SMS, Push notifications';

-- Common/Shared Schema
CREATE SCHEMA IF NOT EXISTS common;
COMMENT ON SCHEMA common IS 'Common utilities, types, and shared functions';

-- Geographic Data Schema
CREATE SCHEMA IF NOT EXISTS geo;
COMMENT ON SCHEMA geo IS 'Geographic data - Countries, Cities, Locations, POIs';

-- =====================================================
-- SCHEMA PERMISSIONS
-- =====================================================

-- Grant usage on schemas to application role (assuming app_user role exists)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'app_user') THEN
        GRANT USAGE ON SCHEMA iam TO app_user;
        GRANT USAGE ON SCHEMA inventory TO app_user;
        GRANT USAGE ON SCHEMA availability TO app_user;
        GRANT USAGE ON SCHEMA pricing TO app_user;
        GRANT USAGE ON SCHEMA booking TO app_user;
        GRANT USAGE ON SCHEMA payment TO app_user;
        GRANT USAGE ON SCHEMA review TO app_user;
        GRANT USAGE ON SCHEMA notify TO app_user;
        GRANT USAGE ON SCHEMA common TO app_user;
        GRANT USAGE ON SCHEMA geo TO app_user;
END IF;
END $$;

-- =====================================================
-- AUDIT TRAIL SETUP
-- =====================================================

-- Create audit schema for tracking changes
CREATE SCHEMA IF NOT EXISTS audit;
COMMENT ON SCHEMA audit IS 'Audit trail for tracking data changes';

-- =====================================================
-- MIGRATION METADATA
-- =====================================================

-- Create migration metadata table
CREATE TABLE IF NOT EXISTS public.migration_metadata (
                                                         id SERIAL PRIMARY KEY,
                                                         version VARCHAR(50) NOT NULL,
    description TEXT,
    executed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                              executed_by VARCHAR(100) DEFAULT CURRENT_USER,
    execution_time_ms INTEGER,
    success BOOLEAN DEFAULT TRUE,
    error_message TEXT,
    UNIQUE(version)
    );

-- Record this migration
INSERT INTO public.migration_metadata (version, description)
VALUES ('V1', 'Bootstrap database with extensions and schemas');

-- =====================================================
-- VERIFY INSTALLATION
-- =====================================================

DO $$
DECLARE
ext_count INTEGER;
    schema_count INTEGER;
BEGIN
    -- Count installed extensions
SELECT COUNT(*) INTO ext_count
FROM pg_extension
WHERE extname IN ('uuid-ossp', 'postgis', 'pg_trgm', 'pgcrypto');

-- Count created schemas
SELECT COUNT(*) INTO schema_count
FROM information_schema.schemata
WHERE schema_name IN ('iam', 'inventory', 'availability', 'pricing',
                      'booking', 'payment', 'review', 'notify',
                      'common', 'geo', 'audit');

RAISE NOTICE 'Extensions installed: %', ext_count;
    RAISE NOTICE 'Schemas created: %', schema_count;
    
    IF ext_count < 4 THEN
        RAISE WARNING 'Not all required extensions were installed';
END IF;
    
    IF schema_count < 11 THEN
        RAISE WARNING 'Not all required schemas were created';
END IF;
END $$;