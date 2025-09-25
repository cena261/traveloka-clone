
-- V1: Bootstrap - extensions, schemas, common helpers, enums
-- Target: PostgreSQL 14+
-- Notes:
-- - Standardize on gen_random_uuid() (pgcrypto) for UUID generation.
-- - Separate domain schemas similar to DB2, but keep DB1's clear module boundaries.

CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Core domain schemas
CREATE SCHEMA IF NOT EXISTS common;
CREATE SCHEMA IF NOT EXISTS iam;
CREATE SCHEMA IF NOT EXISTS catalog_location;
CREATE SCHEMA IF NOT EXISTS catalog_inventory;
CREATE SCHEMA IF NOT EXISTS catalog_media;
CREATE SCHEMA IF NOT EXISTS availability;
CREATE SCHEMA IF NOT EXISTS pricing;
CREATE SCHEMA IF NOT EXISTS booking;
CREATE SCHEMA IF NOT EXISTS payment;
CREATE SCHEMA IF NOT EXISTS review;
CREATE SCHEMA IF NOT EXISTS communication;
CREATE SCHEMA IF NOT EXISTS search;
CREATE SCHEMA IF NOT EXISTS analytics;

-- ===== Common helpers =====

-- Currency code domain
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'currency_code') THEN
        CREATE DOMAIN common.currency_code AS CHAR(3)
        CHECK (VALUE ~ '^[A-Z]{3}$');
    END IF;
END $$;

-- Email (simple) domain
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'email_text') THEN
        CREATE DOMAIN common.email_text AS TEXT CHECK (POSITION('@' IN VALUE) > 1);
    END IF;
END $$;

-- Generic "updated_at" trigger
CREATE OR REPLACE FUNCTION common.set_updated_at() RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Soft-delete support
CREATE OR REPLACE FUNCTION common.soft_delete() RETURNS TRIGGER AS $$
BEGIN
  NEW.deleted_at := NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ===== Enums (native) =====
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'booking_status') THEN
        CREATE TYPE booking_status AS ENUM ('pending','held','confirmed','cancelled','expired','completed');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'payment_status') THEN
        CREATE TYPE payment_status AS ENUM ('initiated','pending','succeeded','failed','refunded','chargeback');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'allocation_status') THEN
        CREATE TYPE allocation_status AS ENUM ('reserved','held','released','confirmed','cancelled');
    END IF;
END $$;
