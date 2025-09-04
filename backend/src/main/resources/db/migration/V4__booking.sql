-- ===================
-- V4: BOOKING MODULE
-- ===================
-- Yêu cầu: V1 (schemas), V2 (iam/inventory), V3 (availability/pricing)

-- Đơn đặt chỗ (booking header)
CREATE TABLE booking.booking (
                                 id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- tham chiếu
                                 user_id                 UUID NOT NULL REFERENCES iam.app_user(id),
                                 property_id             UUID NOT NULL REFERENCES inventory.property(id),
                                 room_type_id            UUID NOT NULL REFERENCES inventory.room_type(id),

    -- thời gian lưu trú
                                 checkin_date            DATE NOT NULL,
                                 checkout_date           DATE NOT NULL,
                                 nights                  INT  GENERATED ALWAYS AS (GREATEST(0, (checkout_date - checkin_date))) STORED,

    -- khách
                                 guests_adult            INT  NOT NULL DEFAULT 1,
                                 guests_child            INT  NOT NULL DEFAULT 0,

    -- trạng thái
                                 status                  TEXT NOT NULL CHECK (status IN
                                                                              ('pending','held','confirmed','cancelled','expired','completed')),
                                 hold_expires_at         TIMESTAMPTZ,                 -- dùng khi giữ chỗ trước thanh toán

    -- giá tiền (đơn vị: cents)
                                 price_subtotal_cents    BIGINT NOT NULL,
                                 price_discount_cents    BIGINT NOT NULL DEFAULT 0,
                                 price_tax_cents         BIGINT NOT NULL DEFAULT 0,
                                 price_total_cents       BIGINT NOT NULL,
                                 currency                CHAR(3) NOT NULL DEFAULT 'VND',

                                 refundable              BOOLEAN NOT NULL DEFAULT TRUE,
                                 cancellation_policy     TEXT,

    -- thông tin liên hệ (snapshot tại thời điểm đặt)
                                 contact_name            TEXT,
                                 contact_email           CITEXT,
                                 contact_phone           TEXT,

    -- idempotency để chống double-submit khi tạo booking
                                 idempotency_key         TEXT,
                                 UNIQUE (idempotency_key),

    -- mã hiển thị cho người dùng (dễ đọc hơn UUID)
                                 reference_code          TEXT UNIQUE,                 -- ví dụ: BK-2025-08-000123

                                 created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
                                 updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX booking_user_idx      ON booking.booking(user_id);
CREATE INDEX booking_property_idx  ON booking.booking(property_id, room_type_id);
CREATE INDEX booking_status_idx    ON booking.booking(status);
CREATE INDEX booking_dates_idx     ON booking.booking(checkin_date, checkout_date);

-- Chi tiết từng đêm (line items)
CREATE TABLE booking.booking_item (
                                      id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                      booking_id            UUID NOT NULL REFERENCES booking.booking(id) ON DELETE CASCADE,
                                      stay_date             DATE NOT NULL,                 -- đêm lưu trú
                                      unit_price_cents      BIGINT NOT NULL,
                                      currency              CHAR(3) NOT NULL,
                                      tax_cents             BIGINT NOT NULL DEFAULT 0,
                                      discount_cents        BIGINT NOT NULL DEFAULT 0,
                                      created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
                                      UNIQUE (booking_id, stay_date)
);

CREATE INDEX booking_item_idx ON booking.booking_item(booking_id, stay_date);
