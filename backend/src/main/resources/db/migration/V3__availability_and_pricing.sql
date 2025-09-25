
-- V3: Availability + Pricing

-- Availability
CREATE TABLE availability.inventory_calendar (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  room_type_id     UUID NOT NULL REFERENCES catalog_inventory.room_types(id) ON DELETE CASCADE,
  date             DATE NOT NULL,
  total_available  INT  NOT NULL,
  holds            INT  NOT NULL DEFAULT 0,
  sold             INT  NOT NULL DEFAULT 0,
  UNIQUE(room_type_id, date)
);
CREATE INDEX IF NOT EXISTS invcal_rt_date_idx ON availability.inventory_calendar(room_type_id, date);

-- Pricing rules
CREATE TABLE pricing.pricing_rule (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  property_id      UUID NOT NULL REFERENCES catalog_inventory.properties(id) ON DELETE CASCADE,
  room_type_id     UUID REFERENCES catalog_inventory.room_types(id) ON DELETE CASCADE,
  name             TEXT NOT NULL,
  kind             TEXT NOT NULL CHECK (kind IN ('percentage','absolute','override')),
  amount_cents     BIGINT,
  percentage       NUMERIC(5,2),
  date_start       DATE NOT NULL,
  date_end         DATE NOT NULL,
  days_of_week     INT[],
  min_stay_nights  INT DEFAULT 1,
  is_active        BOOLEAN NOT NULL DEFAULT TRUE,
  created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT amount_presence CHECK (
    (kind='percentage' AND percentage IS NOT NULL)
    OR (kind IN ('absolute','override') AND amount_cents IS NOT NULL)
  )
);
CREATE INDEX IF NOT EXISTS pricing_rule_range_idx ON pricing.pricing_rule(property_id, room_type_id, date_start, date_end);
CREATE INDEX IF NOT EXISTS pricing_rule_active_idx ON pricing.pricing_rule(is_active);
