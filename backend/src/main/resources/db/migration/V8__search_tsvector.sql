-- ============================
-- V8: FULL-TEXT SEARCH SUPPORT
-- ============================
-- Tạo cột tsvector + trigger cho inventory.property
-- (và bật pg_trgm để có thể thêm index fuzzy sau này)

CREATE EXTENSION IF NOT EXISTS pg_trgm;

ALTER TABLE inventory.property
    ADD COLUMN IF NOT EXISTS search_tsv tsvector;

-- Index full-text
CREATE INDEX IF NOT EXISTS property_tsv_idx
    ON inventory.property USING GIN (search_tsv);

-- (tuỳ chọn) Index trigram cho search xấp xỉ tên/địa chỉ
CREATE INDEX IF NOT EXISTS property_name_trgm_idx
    ON inventory.property USING GIN (name gin_trgm_ops);

CREATE INDEX IF NOT EXISTS property_addr_trgm_idx
    ON inventory.property USING GIN (address_line gin_trgm_ops);

-- Trigger duy trì tsv từ name/city/address/description
CREATE OR REPLACE FUNCTION inventory.property_tsv_trigger() RETURNS trigger AS $$
BEGIN
  NEW.search_tsv :=
    setweight(to_tsvector('simple', coalesce(NEW.name,'')), 'A') ||
    setweight(to_tsvector('simple', coalesce(NEW.city,'')), 'B') ||
    setweight(to_tsvector('simple', coalesce(NEW.address_line,'')), 'C') ||
    setweight(to_tsvector('simple', coalesce(NEW.description,'')), 'D');
RETURN NEW;
END
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS property_tsv_update ON inventory.property;

CREATE TRIGGER property_tsv_update
    BEFORE INSERT OR UPDATE OF name, city, address_line, description ON inventory.property
    FOR EACH ROW EXECUTE FUNCTION inventory.property_tsv_trigger();

-- Backfill cho dữ liệu sẵn có
UPDATE inventory.property p
SET search_tsv =
        setweight(to_tsvector('simple', coalesce(p.name,'')), 'A') ||
        setweight(to_tsvector('simple', coalesce(p.city,'')), 'B') ||
        setweight(to_tsvector('simple', coalesce(p.address_line,'')), 'C') ||
        setweight(to_tsvector('simple', coalesce(p.description,'')), 'D');
