
-- V8 (FIX): Performance indexes, constraints, hygiene + safe analytics dependency
-- This replaces the original V8 that failed when analytics.daily_revenue didn't exist.

-- ===== Availability partial index =====
CREATE INDEX IF NOT EXISTS invcal_available_idx
    ON availability.inventory_calendar(room_type_id, date)
    WHERE (total_available - holds - sold) > 0;

-- ===== Booking safety constraint (guarded) =====
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.table_constraints
    WHERE table_schema = 'booking'
      AND table_name = 'booking'
      AND constraint_name = 'booking_amounts_non_negative'
  ) THEN
ALTER TABLE booking.booking
    ADD CONSTRAINT booking_amounts_non_negative
        CHECK (price_subtotal_cents >= 0 AND price_total_cents >= 0);
END IF;
END $$;

-- ===== Payment hygiene indexes =====
CREATE INDEX IF NOT EXISTS payment_created_idx          ON payment.payment(created_at);
CREATE INDEX IF NOT EXISTS payment_status_created_idx   ON payment.payment(status, created_at);

-- ===== Analytics safety: ensure base table exists before MV =====
CREATE SCHEMA IF NOT EXISTS analytics;

CREATE TABLE IF NOT EXISTS analytics.daily_revenue (
                                                       id BIGSERIAL PRIMARY KEY,
                                                       date DATE NOT NULL,
                                                       property_id UUID REFERENCES catalog_inventory.properties(id),
    currency common.currency_code NOT NULL,
    gross_amount NUMERIC(18,2) NOT NULL,
    net_amount NUMERIC(18,2),
    bookings_count BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(date, property_id, currency)
    );

-- ===== Materialized view (no data; refresh by job) =====
DROP MATERIALIZED VIEW IF EXISTS analytics.monthly_revenue_mv;

CREATE MATERIALIZED VIEW analytics.monthly_revenue_mv AS
SELECT date_trunc('month', date) AS month,
       property_id,
       currency,
       SUM(gross_amount) AS gross_amount,
       SUM(net_amount)   AS net_amount,
       SUM(bookings_count) AS bookings_count
FROM analytics.daily_revenue
GROUP BY 1,2,3
WITH NO DATA;
