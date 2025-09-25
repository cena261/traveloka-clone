-- V9: IAM Enhancements for Keycloak Integration and Session Management
-- ended
-- Extend existing users table with Keycloak integration and profile tracking
ALTER TABLE iam.users
ADD COLUMN keycloak_id TEXT UNIQUE,
ADD COLUMN first_name TEXT,
ADD COLUMN last_name TEXT,
ADD COLUMN phone_number TEXT,
ADD COLUMN status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED')),
ADD COLUMN profile_completeness INTEGER DEFAULT 0 CHECK (profile_completeness >= 0 AND profile_completeness <= 100),
ADD COLUMN last_sync_at TIMESTAMPTZ;

-- Create unique index on keycloak_id for fast lookups
CREATE UNIQUE INDEX IF NOT EXISTS users_keycloak_id_idx ON iam.users(keycloak_id) WHERE keycloak_id IS NOT NULL;

-- Create index for user status queries
CREATE INDEX IF NOT EXISTS users_status_idx ON iam.users(status) WHERE status = 'ACTIVE';

-- User Profiles: Extended user information beyond Keycloak core data
CREATE TABLE iam.user_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES iam.users(id) ON DELETE CASCADE,
    date_of_birth DATE,
    gender TEXT CHECK (gender IN ('MALE', 'FEMALE', 'OTHER', 'PREFER_NOT_TO_SAY')),
    nationality CHAR(2), -- ISO 3166-1 country code
    passport_number TEXT,
    emergency_contact_name TEXT,
    emergency_contact_phone TEXT,
    bio TEXT CHECK (LENGTH(bio) <= 500),
    avatar_url TEXT,
    verification_status TEXT NOT NULL DEFAULT 'UNVERIFIED' CHECK (verification_status IN ('UNVERIFIED', 'PENDING', 'VERIFIED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by TEXT,
    updated_by TEXT,
    UNIQUE(user_id)
);

-- User Preferences: Settings and personalization data stored as JSONB
CREATE TABLE iam.user_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES iam.users(id) ON DELETE CASCADE,
    language TEXT DEFAULT 'en-US' CHECK (language ~ '^[a-z]{2}-[A-Z]{2}$'),
    timezone TEXT DEFAULT 'UTC',
    currency CHAR(3) DEFAULT 'USD', -- ISO 4217 currency code
    notification_preferences JSONB NOT NULL DEFAULT '{}',
    booking_preferences JSONB NOT NULL DEFAULT '{}',
    privacy_settings JSONB NOT NULL DEFAULT '{}',
    accessibility_options JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by TEXT,
    updated_by TEXT,
    UNIQUE(user_id)
);

-- Create GIN indexes for efficient JSONB queries
CREATE INDEX IF NOT EXISTS user_preferences_notifications_gin ON iam.user_preferences USING GIN (notification_preferences);
CREATE INDEX IF NOT EXISTS user_preferences_booking_gin ON iam.user_preferences USING GIN (booking_preferences);
CREATE INDEX IF NOT EXISTS user_preferences_language_idx ON iam.user_preferences(language);
CREATE INDEX IF NOT EXISTS user_preferences_timezone_idx ON iam.user_preferences(timezone);

-- User Sessions: Multi-device session tracking with security metadata
CREATE TABLE iam.user_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES iam.users(id) ON DELETE CASCADE,
    session_id TEXT NOT NULL UNIQUE,
    device_type TEXT CHECK (device_type IN ('WEB', 'MOBILE_IOS', 'MOBILE_ANDROID')),
    device_name TEXT,
    ip_address INET,
    user_agent TEXT CHECK (LENGTH(user_agent) <= 500),
    location TEXT, -- City, Country derived from IP
    metadata JSONB NOT NULL DEFAULT '{}', -- Security level, risk score, features, etc.
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_activity_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL,
    status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'EXPIRED', 'TERMINATED'))
);

-- Partition user_sessions by month for better performance
-- Note: This creates the partition structure, but actual partitions should be created dynamically
CREATE INDEX IF NOT EXISTS user_sessions_user_id_idx ON iam.user_sessions(user_id);
CREATE INDEX IF NOT EXISTS user_sessions_session_id_idx ON iam.user_sessions(session_id);
CREATE INDEX IF NOT EXISTS user_sessions_status_idx ON iam.user_sessions(status) WHERE status = 'ACTIVE';
CREATE INDEX IF NOT EXISTS user_sessions_expires_at_idx ON iam.user_sessions(expires_at);
CREATE INDEX IF NOT EXISTS user_sessions_created_at_idx ON iam.user_sessions(created_at);

-- Enhance existing roles table with permissions and metadata
ALTER TABLE iam.roles
ADD COLUMN permissions JSONB NOT NULL DEFAULT '[]',
ADD COLUMN is_default BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

-- Create GIN index for permissions queries
CREATE INDEX IF NOT EXISTS roles_permissions_gin ON iam.roles USING GIN (permissions);

-- Enhance user_roles table with assignment metadata and expiration
ALTER TABLE iam.user_roles
ADD COLUMN assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
ADD COLUMN assigned_by TEXT,
ADD COLUMN expires_at TIMESTAMPTZ,
ADD COLUMN status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED', 'EXPIRED')),
ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

-- Create indexes for user_roles queries
CREATE INDEX IF NOT EXISTS user_roles_user_id_idx ON iam.user_roles(user_id);
CREATE INDEX IF NOT EXISTS user_roles_role_id_idx ON iam.user_roles(role_id);
CREATE INDEX IF NOT EXISTS user_roles_status_idx ON iam.user_roles(status) WHERE status = 'ACTIVE';
CREATE INDEX IF NOT EXISTS user_roles_expires_at_idx ON iam.user_roles(expires_at) WHERE expires_at IS NOT NULL;

-- Sync Events: Audit trail for Keycloak synchronization activities
CREATE TABLE iam.sync_events (
    id BIGSERIAL PRIMARY KEY,
    event_type TEXT NOT NULL CHECK (event_type IN ('USER_CREATED', 'USER_UPDATED', 'USER_DELETED', 'ROLE_ASSIGNED', 'ROLE_REMOVED')),
    keycloak_user_id TEXT,
    local_user_id UUID REFERENCES iam.users(id) ON DELETE SET NULL,
    sync_direction TEXT NOT NULL CHECK (sync_direction IN ('KEYCLOAK_TO_LOCAL', 'LOCAL_TO_KEYCLOAK', 'BIDIRECTIONAL')),
    status TEXT NOT NULL CHECK (status IN ('SUCCESS', 'FAILED', 'PARTIAL')),
    payload JSONB NOT NULL DEFAULT '{}',
    error_message TEXT CHECK (LENGTH(error_message) <= 1000),
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    retry_count INTEGER NOT NULL DEFAULT 0
);

-- Create indexes for sync events queries
CREATE INDEX IF NOT EXISTS sync_events_keycloak_user_id_idx ON iam.sync_events(keycloak_user_id);
CREATE INDEX IF NOT EXISTS sync_events_local_user_id_idx ON iam.sync_events(local_user_id);
CREATE INDEX IF NOT EXISTS sync_events_event_type_idx ON iam.sync_events(event_type);
CREATE INDEX IF NOT EXISTS sync_events_status_idx ON iam.sync_events(status);
CREATE INDEX IF NOT EXISTS sync_events_processed_at_idx ON iam.sync_events(processed_at);

-- Insert default roles for the IAM system
INSERT INTO iam.roles (name, description, permissions, is_default) VALUES
('CUSTOMER', 'Standard customer role for booking hotels',
 '["user.profile.read", "user.profile.update", "booking.create", "booking.read", "booking.update"]',
 true),
('PARTNER', 'Hotel partner role for property management',
 '["user.profile.read", "user.profile.update", "property.create", "property.read", "property.update", "booking.read"]',
 false),
('ADMIN', 'System administrator with elevated permissions',
 '["user.all", "property.all", "booking.all", "admin.system"]',
 false),
('SUPER_ADMIN', 'Super administrator with full system access',
 '["*"]',
 false);

-- Create trigger function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply updated_at triggers to relevant tables
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON iam.users FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_user_profiles_updated_at BEFORE UPDATE ON iam.user_profiles FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_user_preferences_updated_at BEFORE UPDATE ON iam.user_preferences FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_roles_updated_at BEFORE UPDATE ON iam.roles FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_user_roles_updated_at BEFORE UPDATE ON iam.user_roles FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create function to calculate profile completeness
CREATE OR REPLACE FUNCTION calculate_profile_completeness(user_uuid UUID)
RETURNS INTEGER AS $$
DECLARE
    completeness INTEGER := 0;
    user_rec RECORD;
    profile_rec RECORD;
BEGIN
    -- Get user data
    SELECT * INTO user_rec FROM iam.users WHERE id = user_uuid;
    IF NOT FOUND THEN RETURN 0; END IF;

    -- Get profile data
    SELECT * INTO profile_rec FROM iam.user_profiles WHERE user_id = user_uuid;

    -- Calculate completeness based on filled fields
    -- Basic info (40 points total)
    IF user_rec.first_name IS NOT NULL AND LENGTH(TRIM(user_rec.first_name)) > 0 THEN completeness := completeness + 10; END IF;
    IF user_rec.last_name IS NOT NULL AND LENGTH(TRIM(user_rec.last_name)) > 0 THEN completeness := completeness + 10; END IF;
    IF user_rec.email IS NOT NULL THEN completeness := completeness + 10; END IF;
    IF user_rec.phone_number IS NOT NULL AND LENGTH(TRIM(user_rec.phone_number)) > 0 THEN completeness := completeness + 10; END IF;

    -- Profile info (60 points total)
    IF profile_rec IS NOT NULL THEN
        IF profile_rec.date_of_birth IS NOT NULL THEN completeness := completeness + 15; END IF;
        IF profile_rec.gender IS NOT NULL THEN completeness := completeness + 10; END IF;
        IF profile_rec.nationality IS NOT NULL THEN completeness := completeness + 15; END IF;
        IF profile_rec.emergency_contact_name IS NOT NULL AND LENGTH(TRIM(profile_rec.emergency_contact_name)) > 0 THEN completeness := completeness + 10; END IF;
        IF profile_rec.emergency_contact_phone IS NOT NULL AND LENGTH(TRIM(profile_rec.emergency_contact_phone)) > 0 THEN completeness := completeness + 10; END IF;
    END IF;

    RETURN LEAST(completeness, 100);
END;
$$ LANGUAGE plpgsql;

-- Create trigger to automatically update profile completeness
CREATE OR REPLACE FUNCTION update_profile_completeness()
RETURNS TRIGGER AS $$
BEGIN
    -- Update profile completeness for the affected user
    UPDATE iam.users
    SET profile_completeness = calculate_profile_completeness(NEW.user_id)
    WHERE id = NEW.user_id;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply profile completeness triggers
CREATE TRIGGER trigger_update_profile_completeness_users
    AFTER UPDATE ON iam.users
    FOR EACH ROW
    WHEN (OLD.first_name IS DISTINCT FROM NEW.first_name OR
          OLD.last_name IS DISTINCT FROM NEW.last_name OR
          OLD.phone_number IS DISTINCT FROM NEW.phone_number)
    EXECUTE FUNCTION update_profile_completeness();

CREATE TRIGGER trigger_update_profile_completeness_profiles
    AFTER INSERT OR UPDATE ON iam.user_profiles
    FOR EACH ROW
    EXECUTE FUNCTION update_profile_completeness();

-- Session cleanup function for expired sessions
CREATE OR REPLACE FUNCTION cleanup_expired_sessions()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    UPDATE iam.user_sessions
    SET status = 'EXPIRED', last_activity_at = NOW()
    WHERE status = 'ACTIVE' AND expires_at < NOW();

    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Create comments for documentation
COMMENT ON TABLE iam.user_profiles IS 'Extended user profile information beyond Keycloak core data';
COMMENT ON TABLE iam.user_preferences IS 'User preferences and settings stored as JSONB for flexibility';
COMMENT ON TABLE iam.user_sessions IS 'Multi-device session tracking with security metadata';
COMMENT ON TABLE iam.sync_events IS 'Audit trail for Keycloak synchronization activities';
COMMENT ON FUNCTION calculate_profile_completeness(UUID) IS 'Calculate profile completeness percentage (0-100)';
COMMENT ON FUNCTION cleanup_expired_sessions() IS 'Mark expired sessions and return count of affected records';