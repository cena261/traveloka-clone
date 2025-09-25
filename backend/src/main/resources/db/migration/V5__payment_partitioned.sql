
-- V5 (re-FIX v2): Payment HASH(id) + global uniqueness (idempotency_key, provider+txn) via registry tables

-- Clean old if retrying
DROP TABLE IF EXISTS payment.webhook_event CASCADE;
DROP TABLE IF EXISTS payment.payment CASCADE;
DROP TABLE IF EXISTS payment.idempotency_registry CASCADE;
DROP TABLE IF EXISTS payment.provider_txn_registry CASCADE;

CREATE TABLE payment.payment (
                                 id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                 booking_id        UUID NOT NULL REFERENCES booking.booking(id) ON DELETE CASCADE,
                                 provider          TEXT NOT NULL CHECK (provider IN ('vnpay','momo','stripe','test')),
                                 provider_txn_id   TEXT,
                                 external_order_id TEXT,
                                 amount_cents      BIGINT NOT NULL CHECK (amount_cents >= 0),
                                 currency          common.currency_code NOT NULL,
                                 status            payment_status NOT NULL DEFAULT 'initiated',
                                 method            TEXT,
                                 description       TEXT,
                                 meta              JSONB,
                                 idempotency_key   TEXT,
                                 created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                 updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
) PARTITION BY HASH (id);

-- partitions
DO $$
DECLARE i int;
BEGIN
FOR i IN 0..7 LOOP
    EXECUTE format('
      CREATE TABLE IF NOT EXISTS payment.payment_p%s PARTITION OF payment.payment
      FOR VALUES WITH (MODULUS 8, REMAINDER %s);
    ', i, i);
END LOOP;
END $$;

-- Registries for global uniqueness
CREATE TABLE payment.idempotency_registry (
                                              idempotency_key TEXT PRIMARY KEY,
                                              payment_id      UUID NOT NULL UNIQUE REFERENCES payment.payment(id) ON DELETE CASCADE,
                                              created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE payment.provider_txn_registry (
                                               provider        TEXT NOT NULL,
                                               provider_txn_id TEXT NOT NULL,
                                               payment_id      UUID NOT NULL UNIQUE REFERENCES payment.payment(id) ON DELETE CASCADE,
                                               created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                               PRIMARY KEY (provider, provider_txn_id)
);

-- Trigger function to enforce uniqueness
CREATE OR REPLACE FUNCTION payment.enforce_payment_uniques() RETURNS TRIGGER AS $$
DECLARE
v_pid UUID;
  v_pid2 UUID;
BEGIN
  IF NEW.id IS NULL THEN
    NEW.id := gen_random_uuid();
END IF;

  -- idempotency_key
  IF TG_OP = 'UPDATE' AND NEW.idempotency_key IS DISTINCT FROM OLD.idempotency_key THEN
    IF OLD.idempotency_key IS NOT NULL THEN
DELETE FROM payment.idempotency_registry
WHERE idempotency_key = OLD.idempotency_key AND payment_id = OLD.id;
END IF;
END IF;
  IF NEW.idempotency_key IS NOT NULL THEN
    INSERT INTO payment.idempotency_registry(idempotency_key, payment_id)
    VALUES (NEW.idempotency_key, NEW.id)
    ON CONFLICT (idempotency_key) DO UPDATE SET payment_id = payment.idempotency_registry.payment_id
                                     WHERE payment.idempotency_registry.payment_id = EXCLUDED.payment_id
                                         RETURNING payment_id INTO v_pid;
IF v_pid IS NULL THEN
      RAISE EXCEPTION 'duplicate payment idempotency_key % for payment_id %', NEW.idempotency_key, NEW.id
        USING ERRCODE = '23505';
END IF;
END IF;

  -- provider + provider_txn_id
  IF TG_OP = 'UPDATE' AND (NEW.provider IS DISTINCT FROM OLD.provider OR NEW.provider_txn_id IS DISTINCT FROM OLD.provider_txn_id) THEN
    IF OLD.provider IS NOT NULL AND OLD.provider_txn_id IS NOT NULL THEN
DELETE FROM payment.provider_txn_registry
WHERE provider = OLD.provider AND provider_txn_id = OLD.provider_txn_id AND payment_id = OLD.id;
END IF;
END IF;
  IF NEW.provider IS NOT NULL AND NEW.provider_txn_id IS NOT NULL THEN
    INSERT INTO payment.provider_txn_registry(provider, provider_txn_id, payment_id)
    VALUES (NEW.provider, NEW.provider_txn_id, NEW.id)
    ON CONFLICT (provider, provider_txn_id) DO UPDATE SET payment_id = payment.provider_txn_registry.payment_id
                                               WHERE payment.provider_txn_registry.payment_id = EXCLUDED.payment_id
                                                   RETURNING payment_id INTO v_pid2;
IF v_pid2 IS NULL THEN
      RAISE EXCEPTION 'duplicate provider_txn for % / %', NEW.provider, NEW.provider_txn_id
        USING ERRCODE = '23505';
END IF;
END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_payment_enforce_uniques
    BEFORE INSERT OR UPDATE OF idempotency_key, provider, provider_txn_id ON payment.payment
    FOR EACH ROW EXECUTE FUNCTION payment.enforce_payment_uniques();

-- Secondary indexes
CREATE INDEX IF NOT EXISTS payment_booking_idx    ON payment.payment(booking_id);
CREATE INDEX IF NOT EXISTS payment_status_idx     ON payment.payment(status);
CREATE INDEX IF NOT EXISTS payment_provider_idx   ON payment.payment(provider);
CREATE INDEX IF NOT EXISTS payment_created_idx    ON payment.payment(created_at);

-- Webhook event (unchanged)
CREATE TABLE payment.webhook_event (
                                       id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                       provider          TEXT NOT NULL,
                                       event_type        TEXT NOT NULL,
                                       raw_payload       JSONB NOT NULL,
                                       signature_ok      BOOLEAN,
                                       booking_id        UUID REFERENCES booking.booking(id) ON DELETE SET NULL,
                                       payment_id        UUID,
                                       provider_txn_id   TEXT,
                                       received_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                       handled           BOOLEAN NOT NULL DEFAULT FALSE,
                                       handled_at        TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS webhook_provider_idx ON payment.webhook_event(provider, event_type);
CREATE INDEX IF NOT EXISTS webhook_handled_idx  ON payment.webhook_event(handled, received_at);
CREATE INDEX IF NOT EXISTS webhook_txn_idx      ON payment.webhook_event(provider_txn_id);

CREATE TRIGGER trg_payment_updated_at BEFORE UPDATE ON payment.payment
    FOR EACH ROW EXECUTE FUNCTION common.set_updated_at();
