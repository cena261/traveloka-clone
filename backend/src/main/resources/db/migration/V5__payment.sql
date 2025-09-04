-- ================
-- V5: PAYMENT CORE
-- ================

-- Giao dịch thanh toán gắn với booking
CREATE TABLE payment.payment (
                                 id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                 booking_id        UUID NOT NULL REFERENCES booking.booking(id) ON DELETE CASCADE,

                                 provider          TEXT NOT NULL CHECK (provider IN ('vnpay','momo','stripe','test')),
                                 provider_txn_id   TEXT,                      -- mã giao dịch phía cổng (có thể NULL khi chưa có)
                                 external_order_id TEXT,                      -- mã đơn phía gateway (nếu có), để so khớp

                                 amount_cents      BIGINT NOT NULL CHECK (amount_cents >= 0),
                                 currency          CHAR(3) NOT NULL,

                                 status            TEXT NOT NULL CHECK (status IN ('initiated','pending','succeeded','failed','refunded','chargeback')),
                                 method            TEXT,                      -- card/qr/wallet/bank_transfer...
                                 description       TEXT,

                                 meta              JSONB,                     -- payload/tối thiểu trường cần đối soát

    -- idempotency để tạo giao dịch an toàn (theo booking + client)
                                 idempotency_key   TEXT,
                                 UNIQUE (idempotency_key),

                                 created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
                                 updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Chỉ mục phổ biến
CREATE INDEX payment_booking_idx ON payment.payment(booking_id);
CREATE INDEX payment_status_idx  ON payment.payment(status);
CREATE INDEX payment_provider_idx ON payment.payment(provider);

-- Đảm bảo (provider, provider_txn_id) là unique khi provider_txn_id không NULL
-- (partial unique index cho Postgres)
CREATE UNIQUE INDEX payment_provider_txn_uidx
    ON payment.payment(provider, provider_txn_id)
    WHERE provider_txn_id IS NOT NULL;

-- Sự kiện webhook inbound để lưu vết & xử lý bất đồng bộ
CREATE TABLE payment.webhook_event (
                                       id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                       provider          TEXT NOT NULL,            -- vnpay/momo/stripe/test
                                       event_type        TEXT NOT NULL,            -- payment.succeeded/payment.failed/...
                                       raw_payload       JSONB NOT NULL,           -- lưu nguyên body đã chuẩn hoá (JSON)
                                       signature_ok      BOOLEAN,                  -- đã verify HMAC/chữ ký chưa

    -- liên kết mềm (nếu parse được)
                                       booking_id        UUID REFERENCES booking.booking(id) ON DELETE SET NULL,
                                       payment_id        UUID REFERENCES payment.payment(id) ON DELETE SET NULL,
                                       provider_txn_id   TEXT,

                                       received_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
                                       handled           BOOLEAN NOT NULL DEFAULT FALSE,
                                       handled_at        TIMESTAMPTZ
);

CREATE INDEX webhook_provider_idx ON payment.webhook_event(provider, event_type);
CREATE INDEX webhook_handled_idx  ON payment.webhook_event(handled, received_at);
CREATE INDEX webhook_txn_idx      ON payment.webhook_event(provider_txn_id);

