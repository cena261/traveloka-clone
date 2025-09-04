-- ===================
-- V7: NOTIFICATION CORE
-- ===================

CREATE TABLE notify.notification (
                                     id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                     user_id         UUID NOT NULL REFERENCES iam.app_user(id) ON DELETE CASCADE,
                                     channel         TEXT NOT NULL CHECK (channel IN ('email','sms','push')),
                                     template_code   TEXT NOT NULL,          -- BOOKING_CONFIRMED, PAYMENT_FAILED, ...
                                     payload         JSONB NOT NULL,         -- biến để merge vào template
                                     status          TEXT NOT NULL CHECK (status IN ('queued','sent','failed')),
                                     scheduled_at    TIMESTAMPTZ,
                                     sent_at         TIMESTAMPTZ,
                                     created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX notify_user_idx   ON notify.notification(user_id, status);
CREATE INDEX notify_status_idx ON notify.notification(status, scheduled_at);
