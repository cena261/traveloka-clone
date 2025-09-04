-- =========================
-- V3: AVAILABILITY + PRICING
-- =========================
-- Yêu cầu: V1 (schemas) + V2 (iam/inventory) đã migrate

-- --------- AVAILABILITY ----------
CREATE TABLE availability.inventory_calendar (
                                                 id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                                 room_type_id     UUID NOT NULL REFERENCES inventory.room_type(id) ON DELETE CASCADE,
                                                 date             DATE NOT NULL,
                                                 total_available  INT  NOT NULL,           -- số phòng/bàn có thể bán (<= total_units)
                                                 holds            INT  NOT NULL DEFAULT 0, -- giữ chỗ tạm (trong lúc thanh toán)
                                                 sold             INT  NOT NULL DEFAULT 0,
                                                 UNIQUE (room_type_id, date)
);

CREATE INDEX invcal_rt_date_idx ON availability.inventory_calendar(room_type_id, date);
CREATE INDEX invcal_date_idx     ON availability.inventory_calendar(date);

-- ---------- PRICING ----------
-- kind:
--  - percentage : áp dụng % (dương/âm) trên base_price
--  - absolute   : cộng/trừ amount_cents vào base_price
--  - override   : đặt giá cố định = amount_cents
CREATE TABLE pricing.pricing_rule (
                                      id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                      property_id      UUID NOT NULL REFERENCES inventory.property(id) ON DELETE CASCADE,
                                      room_type_id     UUID     REFERENCES inventory.room_type(id) ON DELETE CASCADE,
                                      name             TEXT NOT NULL,
                                      kind             TEXT NOT NULL CHECK (kind IN ('percentage','absolute','override')),
                                      amount_cents     BIGINT,                 -- dùng cho absolute/override
                                      percentage       NUMERIC(5,2),           -- dùng cho percentage (ví dụ: -10.00)
                                      date_start       DATE NOT NULL,
                                      date_end         DATE NOT NULL,
                                      days_of_week     INT[] DEFAULT NULL,     -- 1=Mon .. 7=Sun, NULL = áp dụng mọi ngày
                                      min_stay_nights  INT DEFAULT 1,
                                      is_active        BOOLEAN NOT NULL DEFAULT TRUE,
                                      created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
                                      CHECK (
                                          (kind='percentage' AND percentage IS NOT NULL)
                                              OR (kind IN ('absolute','override') AND amount_cents IS NOT NULL)
                                          )
);

CREATE INDEX pricing_rule_range_idx ON pricing.pricing_rule(property_id, room_type_id, date_start, date_end);
CREATE INDEX pricing_rule_active_idx ON pricing.pricing_rule(is_active);

-- DROP VIEW IF EXISTS pricing.v_active_rules;
CREATE OR REPLACE VIEW pricing.v_active_rules AS
SELECT pr.*
FROM pricing.pricing_rule pr
WHERE pr.is_active = TRUE;


