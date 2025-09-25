
-- V4 (re-FIX v2): Booking HASH(id) + global uniqueness via registry tables

-- Clean old objects if retrying
DROP TABLE IF EXISTS booking.booking_item CASCADE;
DROP TABLE IF EXISTS booking.booking CASCADE;
DROP TABLE IF EXISTS booking.idempotency_registry CASCADE;
DROP TABLE IF EXISTS booking.reference_registry CASCADE;

CREATE TABLE booking.booking (
                                 id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                 user_id                 UUID NOT NULL REFERENCES iam.users(id),
                                 property_id             UUID NOT NULL REFERENCES catalog_inventory.properties(id),
                                 room_type_id            UUID NOT NULL REFERENCES catalog_inventory.room_types(id),
                                 checkin_date            DATE NOT NULL,
                                 checkout_date           DATE NOT NULL,
                                 nights                  INT  GENERATED ALWAYS AS (GREATEST(0, (checkout_date - checkin_date))) STORED,
                                 guests_adult            INT  NOT NULL DEFAULT 1,
                                 guests_child            INT  NOT NULL DEFAULT 0,
                                 status                  booking_status NOT NULL DEFAULT 'pending',
                                 hold_expires_at         TIMESTAMPTZ,
                                 price_subtotal_cents    BIGINT NOT NULL,
                                 price_discount_cents    BIGINT NOT NULL DEFAULT 0,
                                 price_tax_cents         BIGINT NOT NULL DEFAULT 0,
                                 price_total_cents       BIGINT NOT NULL,
                                 currency                common.currency_code NOT NULL DEFAULT 'VND',
                                 refundable              BOOLEAN NOT NULL DEFAULT TRUE,
                                 cancellation_policy     TEXT,
                                 contact_name            TEXT,
                                 contact_email           common.email_text,
                                 contact_phone           TEXT,
                                 idempotency_key         TEXT,
                                 reference_code          TEXT,
                                 created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                 updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
) PARTITION BY HASH (id);

-- partitions
DO $$
DECLARE i int;
BEGIN
FOR i IN 0..7 LOOP
    EXECUTE format('
      CREATE TABLE IF NOT EXISTS booking.booking_p%s PARTITION OF booking.booking
      FOR VALUES WITH (MODULUS 8, REMAINDER %s);
    ', i, i);
END LOOP;
END $$;

-- Global uniqueness via unpartitioned registry tables
CREATE TABLE booking.idempotency_registry (
                                              idempotency_key TEXT PRIMARY KEY,
                                              booking_id      UUID NOT NULL UNIQUE REFERENCES booking.booking(id) ON DELETE CASCADE,
                                              created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE booking.reference_registry (
                                            reference_code TEXT PRIMARY KEY,
                                            booking_id     UUID NOT NULL UNIQUE REFERENCES booking.booking(id) ON DELETE CASCADE,
                                            created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Trigger function to enforce registry uniqueness
CREATE OR REPLACE FUNCTION booking.enforce_booking_uniques() RETURNS TRIGGER AS $$
DECLARE
v_bid UUID;
  v_bid2 UUID;
BEGIN
  -- Ensure NEW.id exists for registry rows
  IF NEW.id IS NULL THEN
    NEW.id := gen_random_uuid();
END IF;

  -- Idempotency key
  IF TG_OP = 'UPDATE' AND NEW.idempotency_key IS DISTINCT FROM OLD.idempotency_key THEN
    IF OLD.idempotency_key IS NOT NULL THEN
DELETE FROM booking.idempotency_registry
WHERE idempotency_key = OLD.idempotency_key AND booking_id = OLD.id;
END IF;
END IF;
  IF NEW.idempotency_key IS NOT NULL THEN
    INSERT INTO booking.idempotency_registry(idempotency_key, booking_id)
    VALUES (NEW.idempotency_key, NEW.id)
    ON CONFLICT (idempotency_key) DO UPDATE SET booking_id = booking.idempotency_registry.booking_id
                                     WHERE booking.idempotency_registry.booking_id = EXCLUDED.booking_id
                                         RETURNING booking_id INTO v_bid;
IF v_bid IS NULL THEN
      RAISE EXCEPTION 'duplicate idempotency_key % for booking_id %', NEW.idempotency_key, NEW.id
        USING ERRCODE = '23505';
END IF;
END IF;

  -- Reference code
  IF TG_OP = 'UPDATE' AND NEW.reference_code IS DISTINCT FROM OLD.reference_code THEN
    IF OLD.reference_code IS NOT NULL THEN
DELETE FROM booking.reference_registry
WHERE reference_code = OLD.reference_code AND booking_id = OLD.id;
END IF;
END IF;
  IF NEW.reference_code IS NOT NULL THEN
    INSERT INTO booking.reference_registry(reference_code, booking_id)
    VALUES (NEW.reference_code, NEW.id)
    ON CONFLICT (reference_code) DO UPDATE SET booking_id = booking.reference_registry.booking_id
                                    WHERE booking.reference_registry.booking_id = EXCLUDED.booking_id
                                        RETURNING booking_id INTO v_bid2;
IF v_bid2 IS NULL THEN
      RAISE EXCEPTION 'duplicate reference_code % for booking_id %', NEW.reference_code, NEW.id
        USING ERRCODE = '23505';
END IF;
END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_booking_enforce_uniques
    BEFORE INSERT OR UPDATE OF idempotency_key, reference_code ON booking.booking
    FOR EACH ROW EXECUTE FUNCTION booking.enforce_booking_uniques();

-- Line items
CREATE TABLE booking.booking_item (
                                      id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                      booking_id            UUID NOT NULL REFERENCES booking.booking(id) ON DELETE CASCADE,
                                      stay_date             DATE NOT NULL,
                                      unit_price_cents      BIGINT NOT NULL,
                                      currency              common.currency_code NOT NULL,
                                      tax_cents             BIGINT NOT NULL DEFAULT 0,
                                      discount_cents        BIGINT NOT NULL DEFAULT 0,
                                      created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                      UNIQUE (booking_id, stay_date)
);
CREATE INDEX IF NOT EXISTS booking_item_idx ON booking.booking_item(booking_id, stay_date);

-- other indexes
CREATE INDEX IF NOT EXISTS booking_user_idx      ON booking.booking(user_id);
CREATE INDEX IF NOT EXISTS booking_property_idx  ON booking.booking(property_id, room_type_id);
CREATE INDEX IF NOT EXISTS booking_status_idx    ON booking.booking(status);
CREATE INDEX IF NOT EXISTS booking_dates_idx     ON booking.booking(checkin_date, checkout_date);

-- Updated at
CREATE TRIGGER trg_booking_updated_at BEFORE UPDATE ON booking.booking
    FOR EACH ROW EXECUTE FUNCTION common.set_updated_at();
