
-- V2: IAM + Catalog (location, inventory, media)

-- IAM
CREATE TABLE iam.users (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  username      TEXT NOT NULL UNIQUE,
  email         common.email_text UNIQUE,
  display_name  TEXT,
  locale        TEXT DEFAULT 'vi',
  is_active     BOOLEAN NOT NULL DEFAULT TRUE,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS users_active_idx ON iam.users(is_active);

CREATE TABLE iam.roles (
  id            BIGSERIAL PRIMARY KEY,
  name          TEXT NOT NULL UNIQUE,
  description   TEXT,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE iam.user_roles (
  id         BIGSERIAL PRIMARY KEY,
  user_id    UUID NOT NULL REFERENCES iam.users(id) ON DELETE CASCADE,
  role_id    BIGINT NOT NULL REFERENCES iam.roles(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE(user_id, role_id)
);

-- Catalog: location
CREATE TABLE catalog_location.cities (
  id            BIGSERIAL PRIMARY KEY,
  name          TEXT NOT NULL,
  country_code  CHAR(2) NOT NULL,
  admin_region  TEXT,
  centroid      GEOGRAPHY(POINT, 4326),
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS cities_centroid_gix ON catalog_location.cities USING GIST (centroid);

CREATE TABLE catalog_location.airports (
  id            BIGSERIAL PRIMARY KEY,
  iata_code     TEXT,
  icao_code     TEXT,
  name          TEXT NOT NULL,
  city_id       BIGINT REFERENCES catalog_location.cities(id),
  location      GEOGRAPHY(POINT, 4326),
  timezone      TEXT,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS airports_location_gix ON catalog_location.airports USING GIST (location);

CREATE TABLE catalog_location.pois (
  id            BIGSERIAL PRIMARY KEY,
  name          TEXT NOT NULL,
  type          TEXT,
  location      GEOGRAPHY(POINT, 4326),
  metadata      JSONB,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Catalog: inventory
CREATE TABLE catalog_inventory.partners (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_user_id UUID NOT NULL REFERENCES iam.users(id) ON DELETE CASCADE,
  name          TEXT NOT NULL,
  legal_name    TEXT,
  tax_number    TEXT,
  status        TEXT NOT NULL CHECK (status IN ('active','suspended','pending')),
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS partners_owner_idx ON catalog_inventory.partners(owner_user_id);

CREATE TABLE catalog_inventory.properties (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  partner_id    UUID NOT NULL REFERENCES catalog_inventory.partners(id) ON DELETE CASCADE,
  kind          TEXT NOT NULL CHECK (kind IN ('HOTEL','RESORT','APARTMENT','VILLA','HOSTEL','GUESTHOUSE','BOUTIQUE_HOTEL','SERVICED_APARTMENT')),
  name          TEXT NOT NULL,
  description   TEXT,
  country_code  VARCHAR(2) NOT NULL,
  city_id       BIGINT REFERENCES catalog_location.cities(id),
  address_line  TEXT,
  postal_code   TEXT,
  location      GEOGRAPHY(POINT, 4326),
  rating_avg    NUMERIC(3,2) DEFAULT 0,
  rating_count  INT NOT NULL DEFAULT 0,
  status        TEXT NOT NULL CHECK (status IN ('draft','active','inactive')),
  timezone      TEXT DEFAULT 'Asia/Ho_Chi_Minh',
  search_tsv    TSVECTOR,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  deleted_at    TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS properties_partner_idx ON catalog_inventory.properties(partner_id);
CREATE INDEX IF NOT EXISTS properties_location_gix ON catalog_inventory.properties USING GIST(location);
CREATE INDEX IF NOT EXISTS properties_name_trgm_idx ON catalog_inventory.properties USING GIN (name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS properties_tsv_idx ON catalog_inventory.properties USING GIN (search_tsv);

-- tsvector trigger
CREATE OR REPLACE FUNCTION catalog_inventory.properties_tsv_trigger() RETURNS TRIGGER AS $$
BEGIN
  NEW.search_tsv :=
    setweight(to_tsvector('simple', coalesce(NEW.name,'')), 'A') ||
    setweight(to_tsvector('simple', coalesce(NEW.description,'')), 'D');
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_properties_tsv
  BEFORE INSERT OR UPDATE OF name, description ON catalog_inventory.properties
  FOR EACH ROW EXECUTE FUNCTION catalog_inventory.properties_tsv_trigger();

CREATE TABLE catalog_inventory.amenities (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  code        TEXT NOT NULL UNIQUE,
  name        TEXT NOT NULL,
  category    TEXT NOT NULL DEFAULT 'GENERAL',
  icon        TEXT,
  is_popular  BOOLEAN NOT NULL DEFAULT FALSE,
  sort_order  INT NOT NULL DEFAULT 0,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS amenity_category_idx ON catalog_inventory.amenities(category);

CREATE TABLE catalog_inventory.property_amenity (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  property_id       UUID NOT NULL REFERENCES catalog_inventory.properties(id) ON DELETE CASCADE,
  amenity_id        UUID NOT NULL REFERENCES catalog_inventory.amenities(id) ON DELETE CASCADE,
  is_free           BOOLEAN NOT NULL DEFAULT TRUE,
  additional_cost   NUMERIC(10,2) NOT NULL DEFAULT 0.00,
  available_from    TIME,
  available_to      TIME,
  seasonal_window   TEXT,
  notes             TEXT,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT unique_property_amenity UNIQUE(property_id, amenity_id),
  CONSTRAINT non_negative_cost CHECK (additional_cost >= 0.0)
);
CREATE INDEX IF NOT EXISTS property_amenity_prop_idx ON catalog_inventory.property_amenity(property_id);
CREATE INDEX IF NOT EXISTS property_amenity_amenity_idx ON catalog_inventory.property_amenity(amenity_id);

CREATE TABLE catalog_inventory.room_types (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  property_id       UUID NOT NULL REFERENCES catalog_inventory.properties(id) ON DELETE CASCADE,
  code              TEXT NOT NULL,
  name              TEXT NOT NULL,
  description       TEXT,
  capacity_adult    INT NOT NULL DEFAULT 2,
  capacity_child    INT NOT NULL DEFAULT 0,
  base_price_cents  BIGINT NOT NULL,
  currency          common.currency_code NOT NULL DEFAULT 'VND',
  refundable        BOOLEAN NOT NULL DEFAULT TRUE,
  total_units       INT NOT NULL DEFAULT 0,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE(property_id, code)
);
CREATE INDEX IF NOT EXISTS room_types_property_idx ON catalog_inventory.room_types(property_id);

-- Catalog: media
CREATE TABLE catalog_media.media_objects (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_type   TEXT NOT NULL, -- 'property','room_type'
  owner_id     UUID NOT NULL,
  kind         TEXT NOT NULL, -- 'image','video'
  url          TEXT NOT NULL,
  metadata     JSONB,
  is_primary   BOOLEAN DEFAULT FALSE,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS media_owner_idx ON catalog_media.media_objects(owner_type, owner_id);

-- Updated_at triggers
CREATE TRIGGER trg_users_updated_at BEFORE UPDATE ON iam.users
  FOR EACH ROW EXECUTE FUNCTION common.set_updated_at();
CREATE TRIGGER trg_partners_updated_at BEFORE UPDATE ON catalog_inventory.partners
  FOR EACH ROW EXECUTE FUNCTION common.set_updated_at();
CREATE TRIGGER trg_properties_updated_at BEFORE UPDATE ON catalog_inventory.properties
  FOR EACH ROW EXECUTE FUNCTION common.set_updated_at();
CREATE TRIGGER trg_room_types_updated_at BEFORE UPDATE ON catalog_inventory.room_types
  FOR EACH ROW EXECUTE FUNCTION common.set_updated_at();
CREATE TRIGGER trg_amenities_updated_at BEFORE UPDATE ON catalog_inventory.amenities
  FOR EACH ROW EXECUTE FUNCTION common.set_updated_at();
CREATE TRIGGER trg_property_amenity_updated_at BEFORE UPDATE ON catalog_inventory.property_amenity
  FOR EACH ROW EXECUTE FUNCTION common.set_updated_at();
