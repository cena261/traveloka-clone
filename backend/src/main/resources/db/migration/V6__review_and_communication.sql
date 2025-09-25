
-- V6: Review + Communication

CREATE TABLE review.review (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  booking_id    UUID NOT NULL UNIQUE REFERENCES booking.booking(id) ON DELETE CASCADE,
  user_id       UUID NOT NULL REFERENCES iam.users(id) ON DELETE CASCADE,
  property_id   UUID NOT NULL REFERENCES catalog_inventory.properties(id) ON DELETE CASCADE,
  rating        INT  NOT NULL CHECK (rating BETWEEN 1 AND 5),
  title         TEXT,
  content       TEXT,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS review_prop_idx ON review.review(property_id);
CREATE INDEX IF NOT EXISTS review_user_idx ON review.review(user_id);

-- Recalc helper
CREATE OR REPLACE FUNCTION review.recalc_property_rating(p_property_id UUID)
RETURNS void AS $$
BEGIN
  UPDATE catalog_inventory.properties p
  SET rating_avg   = COALESCE(sub.avg_rating, 0),
      rating_count = COALESCE(sub.cnt, 0),
      updated_at   = NOW()
  FROM (
    SELECT property_id, AVG(rating)::NUMERIC(3,2) AS avg_rating, COUNT(*) AS cnt
    FROM review.review
    WHERE property_id = p_property_id
    GROUP BY property_id
  ) sub
  WHERE p.id = p_property_id;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION review.after_review_change() RETURNS trigger AS $$
BEGIN
  PERFORM review.recalc_property_rating(NEW.property_id);
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_review_after_ins_upd
AFTER INSERT OR UPDATE OF rating, content, title ON review.review
FOR EACH ROW EXECUTE FUNCTION review.after_review_change();

CREATE OR REPLACE FUNCTION review.after_review_delete() RETURNS trigger AS $$
BEGIN
  PERFORM review.recalc_property_rating(OLD.property_id);
  RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_review_after_del
AFTER DELETE ON review.review
FOR EACH ROW EXECUTE FUNCTION review.after_review_delete();

-- Communication
CREATE TABLE communication.notifications (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id         UUID NOT NULL REFERENCES iam.users(id) ON DELETE CASCADE,
  channel         TEXT NOT NULL CHECK (channel IN ('email','sms','push')),
  template_code   TEXT NOT NULL,
  payload         JSONB NOT NULL,
  status          TEXT NOT NULL CHECK (status IN ('queued','sent','failed')),
  scheduled_at    TIMESTAMPTZ,
  sent_at         TIMESTAMPTZ,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS notify_user_idx   ON communication.notifications(user_id, status);
CREATE INDEX IF NOT EXISTS notify_status_idx ON communication.notifications(status, scheduled_at);
