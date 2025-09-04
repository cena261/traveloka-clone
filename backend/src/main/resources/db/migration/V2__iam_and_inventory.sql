-- =========================
-- V2: IAM + INVENTORY CORE
-- =========================
-- Yêu cầu: các SCHEMA đã có từ V1 (iam, inventory, ...)
-- Yêu cầu: các extension citext, postgis... đã được tạo ở bước docker init

-- ---------- IAM ----------
CREATE TABLE iam.app_user (
                              id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                              keycloak_user_id  UUID NOT NULL UNIQUE,             -- map với sub trong token
                              keycloak_realm    TEXT NOT NULL DEFAULT 'master',
                              email             CITEXT UNIQUE,
                              full_name         TEXT,
                              phone             TEXT,
                              picture_url       TEXT,
                              is_active         BOOLEAN NOT NULL DEFAULT TRUE,
                              created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
                              updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX app_user_realm_idx ON iam.app_user(keycloak_realm);

-- ------- INVENTORY -------
-- Đối tác (chủ khách sạn/nhà hàng)
CREATE TABLE inventory.partner (
                                   id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                   owner_user_id   UUID NOT NULL REFERENCES iam.app_user(id),
                                   name            TEXT NOT NULL,
                                   legal_name      TEXT,
                                   tax_number      TEXT,
                                   status          TEXT NOT NULL CHECK (status IN ('active','suspended','pending')),
                                   created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                                   updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX partner_owner_idx ON inventory.partner(owner_user_id);

-- Tài sản (khách sạn/homestay/nhà hàng/phòng họp)
CREATE TABLE inventory.property (
                                    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                    partner_id      UUID NOT NULL REFERENCES inventory.partner(id) ON DELETE CASCADE,
                                    kind            TEXT NOT NULL CHECK (kind IN ('hotel','homestay','villa','restaurant','meeting_room')),
                                    name            TEXT NOT NULL,
                                    description     TEXT,
                                    country_code    CHAR(2) NOT NULL,
                                    city            TEXT NOT NULL,
                                    address_line    TEXT NOT NULL,
                                    postal_code     TEXT,
                                    lat             DOUBLE PRECISION,
                                    lng             DOUBLE PRECISION,
                                    geog            GEOGRAPHY(Point, 4326),
                                    rating_avg      NUMERIC(3,2) DEFAULT 0,
                                    rating_count    INT NOT NULL DEFAULT 0,
                                    status          TEXT NOT NULL CHECK (status IN ('draft','active','inactive')),
                                    timezone        TEXT DEFAULT 'Asia/Ho_Chi_Minh',
                                    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                                    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX property_partner_idx ON inventory.property(partner_id);
CREATE INDEX property_city_idx    ON inventory.property(country_code, city);
CREATE INDEX property_geo_idx     ON inventory.property USING GIST (geog);

-- Ảnh tài sản
CREATE TABLE inventory.property_image (
                                          id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                          property_id     UUID NOT NULL REFERENCES inventory.property(id) ON DELETE CASCADE,
                                          url             TEXT NOT NULL,
                                          sort_order      INT  NOT NULL DEFAULT 0,
                                          created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX property_image_prop_idx ON inventory.property_image(property_id);

-- Tiện nghi (master) & mapping
CREATE TABLE inventory.amenity (
                                   id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                   code            TEXT UNIQUE NOT NULL,     -- WIFI, POOL, PARKING...
                                   name            TEXT NOT NULL
);

CREATE TABLE inventory.amenity_map (
                                       property_id     UUID NOT NULL REFERENCES inventory.property(id) ON DELETE CASCADE,
                                       amenity_id      UUID NOT NULL REFERENCES inventory.amenity(id)  ON DELETE CASCADE,
                                       PRIMARY KEY (property_id, amenity_id)
);

-- Loại phòng/bàn
CREATE TABLE inventory.room_type (
                                     id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                     property_id        UUID NOT NULL REFERENCES inventory.property(id) ON DELETE CASCADE,
                                     name               TEXT NOT NULL,            -- Deluxe Double / Table for 4 ...
                                     capacity_adult     INT  NOT NULL DEFAULT 2,
                                     capacity_child     INT  NOT NULL DEFAULT 0,
                                     base_price_cents   BIGINT NOT NULL,
                                     currency           CHAR(3) NOT NULL DEFAULT 'VND',
                                     refundable         BOOLEAN NOT NULL DEFAULT TRUE,
                                     description        TEXT,
                                     total_units        INT  NOT NULL DEFAULT 0,
                                     created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
                                     updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX room_type_prop_idx ON inventory.room_type(property_id);

-- Đơn vị phòng/bàn vật lý
CREATE TABLE inventory.room_unit (
                                     id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                     room_type_id    UUID NOT NULL REFERENCES inventory.room_type(id) ON DELETE CASCADE,
                                     code            TEXT NOT NULL,
                                     UNIQUE (room_type_id, code)
);
CREATE INDEX room_unit_rt_idx ON inventory.room_unit(room_type_id);

-- Phân quyền nhân sự theo property
CREATE TABLE inventory.property_staff (
                                          property_id     UUID NOT NULL REFERENCES inventory.property(id) ON DELETE CASCADE,
                                          user_id         UUID NOT NULL REFERENCES iam.app_user(id) ON DELETE CASCADE,
                                          role            TEXT NOT NULL CHECK (role IN ('manager','editor','viewer')),
                                          PRIMARY KEY (property_id, user_id)
);
