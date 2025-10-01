-- =====================================================
-- V3: IAM Core Tables
-- Description: Users, Roles, Permissions, Sessions, and Authentication
-- =====================================================

-- =====================================================
-- USERS TABLE
-- =====================================================

CREATE TABLE iam.users (
                           id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                           keycloak_id UUID UNIQUE,
                           username VARCHAR(100) UNIQUE NOT NULL,
                           email common.email UNIQUE NOT NULL,
                           phone common.phone,
                           first_name VARCHAR(100),
                           last_name VARCHAR(100),
                           display_name VARCHAR(200),
                           avatar_url common.url,
                           date_of_birth DATE,
                           gender common.gender,
                           nationality common.country_code,
                           preferred_language common.language_code DEFAULT 'en',
                           preferred_currency common.currency_code DEFAULT 'USD',
                           timezone VARCHAR(50) DEFAULT 'UTC',

    -- Account status
                           status common.status DEFAULT 'pending',
                           email_verified BOOLEAN DEFAULT FALSE,
                           phone_verified BOOLEAN DEFAULT FALSE,
                           two_factor_enabled BOOLEAN DEFAULT FALSE,
                           account_locked BOOLEAN DEFAULT FALSE,
                           lock_reason TEXT,
                           locked_until TIMESTAMP WITH TIME ZONE,

    -- Metadata
                           last_login_at TIMESTAMP WITH TIME ZONE,
                           last_login_ip INET,
                           login_count INTEGER DEFAULT 0,
                           failed_login_attempts INTEGER DEFAULT 0,
                           password_changed_at TIMESTAMP WITH TIME ZONE,
                           terms_accepted_at TIMESTAMP WITH TIME ZONE,
                           privacy_accepted_at TIMESTAMP WITH TIME ZONE,
                           marketing_consent BOOLEAN DEFAULT FALSE,

    -- Audit fields
                           created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                           created_by VARCHAR(100),
                           updated_by VARCHAR(100),
                           is_deleted BOOLEAN DEFAULT FALSE,
                           deleted_at TIMESTAMP WITH TIME ZONE,
                           deleted_by VARCHAR(100),

                           CONSTRAINT chk_user_age CHECK (
                               date_of_birth IS NULL OR
                               date_of_birth <= CURRENT_DATE - INTERVAL '13 years'
)
    );

CREATE INDEX idx_users_email ON iam.users(email) WHERE is_deleted = FALSE;
CREATE INDEX idx_users_phone ON iam.users(phone) WHERE is_deleted = FALSE;
CREATE INDEX idx_users_username ON iam.users(username) WHERE is_deleted = FALSE;
CREATE INDEX idx_users_keycloak_id ON iam.users(keycloak_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_users_status ON iam.users(status) WHERE is_deleted = FALSE;
CREATE INDEX idx_users_created_at ON iam.users(created_at);

-- =====================================================
-- USER PROFILES TABLE (Extended information)
-- =====================================================

CREATE TABLE iam.user_profiles (
                                   user_id UUID PRIMARY KEY REFERENCES iam.users(id) ON DELETE CASCADE,

    -- Additional personal info
                                   middle_name VARCHAR(100),
                                   nickname VARCHAR(50),
                                   bio TEXT,
                                   occupation VARCHAR(100),
                                   company VARCHAR(200),

    -- Preferences
                                   notification_preferences JSONB DEFAULT '{}',
                                   privacy_settings JSONB DEFAULT '{}',
                                   accessibility_settings JSONB DEFAULT '{}',

    -- Social links
                                   website common.url,
                                   facebook_url common.url,
                                   twitter_url common.url,
                                   linkedin_url common.url,
                                   instagram_url common.url,

    -- Travel preferences
                                   preferred_airlines TEXT[],
                                   preferred_hotels TEXT[],
                                   dietary_restrictions TEXT[],
                                   special_needs TEXT,
                                   frequent_flyer_numbers JSONB DEFAULT '{}',
                                   hotel_loyalty_programs JSONB DEFAULT '{}',

    -- Statistics
                                   total_bookings INTEGER DEFAULT 0,
                                   total_spent common.price DEFAULT 0,
                                   member_since DATE DEFAULT CURRENT_DATE,
                                   loyalty_points INTEGER DEFAULT 0,
                                   loyalty_tier VARCHAR(50) DEFAULT 'bronze',

    -- Metadata
                                   profile_completion_percentage INTEGER DEFAULT 0,
                                   last_profile_update TIMESTAMP WITH TIME ZONE,
                                   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- USER ADDRESSES TABLE
-- =====================================================

CREATE TABLE iam.user_addresses (
                                    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                    user_id UUID NOT NULL REFERENCES iam.users(id) ON DELETE CASCADE,
                                    type common.address_type NOT NULL,
                                    is_primary BOOLEAN DEFAULT FALSE,
                                    label VARCHAR(100),

    -- Address fields
                                    street_line1 VARCHAR(255) NOT NULL,
                                    street_line2 VARCHAR(255),
                                    city VARCHAR(100) NOT NULL,
                                    state_province VARCHAR(100),
                                    postal_code VARCHAR(20),
                                    country_code common.country_code NOT NULL,

    -- Geographic coordinates
                                    latitude DECIMAL(10,8),
                                    longitude DECIMAL(11,8),
                                    location GEOMETRY(Point, 4326),

    -- Metadata
                                    verified BOOLEAN DEFAULT FALSE,
                                    verified_at TIMESTAMP WITH TIME ZONE,
                                    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                                    UNIQUE(user_id, type, is_primary)
);

CREATE INDEX idx_user_addresses_user_id ON iam.user_addresses(user_id);
CREATE INDEX idx_user_addresses_type ON iam.user_addresses(type);
CREATE INDEX idx_user_addresses_location ON iam.user_addresses USING GIST(location);

-- =====================================================
-- USER DOCUMENTS TABLE
-- =====================================================

CREATE TABLE iam.user_documents (
                                    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                    user_id UUID NOT NULL REFERENCES iam.users(id) ON DELETE CASCADE,
                                    document_type common.document_type NOT NULL,
                                    document_number VARCHAR(100) NOT NULL,
                                    issuing_country common.country_code NOT NULL,
                                    issue_date DATE,
                                    expiry_date DATE,

    -- Document details
                                    first_name VARCHAR(100),
                                    last_name VARCHAR(100),
                                    middle_name VARCHAR(100),
                                    date_of_birth DATE,
                                    place_of_birth VARCHAR(200),
                                    nationality common.country_code,

    -- Files
                                    front_image_url common.url,
                                    back_image_url common.url,

    -- Verification
                                    is_verified BOOLEAN DEFAULT FALSE,
                                    verified_at TIMESTAMP WITH TIME ZONE,
                                    verified_by VARCHAR(100),
                                    verification_method VARCHAR(50),

    -- Status
                                    status common.status DEFAULT 'pending',
                                    is_primary BOOLEAN DEFAULT FALSE,

    -- Metadata
                                    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                                    UNIQUE(user_id, document_type, document_number)
);

CREATE INDEX idx_user_documents_user_id ON iam.user_documents(user_id);
CREATE INDEX idx_user_documents_type ON iam.user_documents(document_type);
CREATE INDEX idx_user_documents_expiry ON iam.user_documents(expiry_date);

-- =====================================================
-- ROLES TABLE
-- =====================================================

CREATE TABLE iam.roles (
                           id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                           keycloak_role_id UUID UNIQUE,
                           name VARCHAR(100) UNIQUE NOT NULL,
                           display_name VARCHAR(200),
                           description TEXT,
                           role_type VARCHAR(50) DEFAULT 'custom', -- system, custom
                           is_system BOOLEAN DEFAULT FALSE,
                           priority INTEGER DEFAULT 0,

    -- Metadata
                           status common.status DEFAULT 'active',
                           created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                           created_by VARCHAR(100),
                           updated_by VARCHAR(100)
);

CREATE INDEX idx_roles_name ON iam.roles(name);
CREATE INDEX idx_roles_status ON iam.roles(status);

-- Insert default system roles
INSERT INTO iam.roles (name, display_name, description, role_type, is_system) VALUES
                                                                                  ('super_admin', 'Super Administrator', 'Full system access', 'system', TRUE),
                                                                                  ('admin', 'Administrator', 'Administrative access', 'system', TRUE),
                                                                                  ('partner_admin', 'Partner Administrator', 'Partner management access', 'system', TRUE),
                                                                                  ('partner_staff', 'Partner Staff', 'Partner operational access', 'system', TRUE),
                                                                                  ('customer', 'Customer', 'Regular customer access', 'system', TRUE),
                                                                                  ('guest', 'Guest', 'Limited guest access', 'system', TRUE);

-- =====================================================
-- PERMISSIONS TABLE
-- =====================================================

CREATE TABLE iam.permissions (
                                 id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                 resource VARCHAR(100) NOT NULL,
                                 action VARCHAR(50) NOT NULL,
                                 name VARCHAR(200) NOT NULL,
                                 description TEXT,

    -- Metadata
                                 status common.status DEFAULT 'active',
                                 created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                 updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                                 UNIQUE(resource, action)
);

CREATE INDEX idx_permissions_resource ON iam.permissions(resource);
CREATE INDEX idx_permissions_action ON iam.permissions(action);

-- Insert default permissions
INSERT INTO iam.permissions (resource, action, name, description) VALUES
                                                                      -- User permissions
                                                                      ('users', 'create', 'Create Users', 'Permission to create new users'),
                                                                      ('users', 'read', 'Read Users', 'Permission to view user information'),
                                                                      ('users', 'update', 'Update Users', 'Permission to update user information'),
                                                                      ('users', 'delete', 'Delete Users', 'Permission to delete users'),

                                                                      -- Booking permissions
                                                                      ('bookings', 'create', 'Create Bookings', 'Permission to create new bookings'),
                                                                      ('bookings', 'read', 'Read Bookings', 'Permission to view bookings'),
                                                                      ('bookings', 'update', 'Update Bookings', 'Permission to update bookings'),
                                                                      ('bookings', 'cancel', 'Cancel Bookings', 'Permission to cancel bookings'),

                                                                      -- Property permissions
                                                                      ('properties', 'create', 'Create Properties', 'Permission to create new properties'),
                                                                      ('properties', 'read', 'Read Properties', 'Permission to view properties'),
                                                                      ('properties', 'update', 'Update Properties', 'Permission to update properties'),
                                                                      ('properties', 'delete', 'Delete Properties', 'Permission to delete properties'),

                                                                      -- Payment permissions
                                                                      ('payments', 'process', 'Process Payments', 'Permission to process payments'),
                                                                      ('payments', 'refund', 'Refund Payments', 'Permission to issue refunds'),
                                                                      ('payments', 'view', 'View Payments', 'Permission to view payment details');

-- =====================================================
-- ROLE PERMISSIONS TABLE
-- =====================================================

CREATE TABLE iam.role_permissions (
                                      role_id UUID NOT NULL REFERENCES iam.roles(id) ON DELETE CASCADE,
                                      permission_id UUID NOT NULL REFERENCES iam.permissions(id) ON DELETE CASCADE,
                                      granted_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                      granted_by VARCHAR(100),

                                      PRIMARY KEY (role_id, permission_id)
);

CREATE INDEX idx_role_permissions_role ON iam.role_permissions(role_id);
CREATE INDEX idx_role_permissions_permission ON iam.role_permissions(permission_id);

-- =====================================================
-- USER ROLES TABLE
-- =====================================================

CREATE TABLE iam.user_roles (
                                user_id UUID NOT NULL REFERENCES iam.users(id) ON DELETE CASCADE,
                                role_id UUID NOT NULL REFERENCES iam.roles(id) ON DELETE CASCADE,
                                assigned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                assigned_by VARCHAR(100),
                                expires_at TIMESTAMP WITH TIME ZONE,
                                is_active BOOLEAN DEFAULT TRUE,

                                PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_user_roles_user ON iam.user_roles(user_id);
CREATE INDEX idx_user_roles_role ON iam.user_roles(role_id);
CREATE INDEX idx_user_roles_active ON iam.user_roles(is_active) WHERE is_active = TRUE;

-- =====================================================
-- SESSIONS TABLE
-- =====================================================

CREATE TABLE iam.sessions (
                              id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                              user_id UUID NOT NULL REFERENCES iam.users(id) ON DELETE CASCADE,
                              session_token VARCHAR(500) UNIQUE NOT NULL,
                              refresh_token VARCHAR(500),

    -- Session details
                              ip_address INET,
                              user_agent TEXT,
                              device_type VARCHAR(50),
                              device_id VARCHAR(255),
                              browser VARCHAR(100),
                              os VARCHAR(100),
                              location_country common.country_code,
                              location_city VARCHAR(100),

    -- Session management
                              is_active BOOLEAN DEFAULT TRUE,
                              last_activity TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                              expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
                              refresh_expires_at TIMESTAMP WITH TIME ZONE,

    -- Security
                              is_suspicious BOOLEAN DEFAULT FALSE,
                              risk_score INTEGER DEFAULT 0,
                              requires_2fa BOOLEAN DEFAULT FALSE,
                              two_fa_completed BOOLEAN DEFAULT FALSE,

    -- Metadata
                              created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                              terminated_at TIMESTAMP WITH TIME ZONE,
                              termination_reason VARCHAR(255)
);

CREATE INDEX idx_sessions_user_id ON iam.sessions(user_id);
CREATE INDEX idx_sessions_token ON iam.sessions(session_token) WHERE is_active = TRUE;
CREATE INDEX idx_sessions_expires ON iam.sessions(expires_at) WHERE is_active = TRUE;
CREATE INDEX idx_sessions_last_activity ON iam.sessions(last_activity);

-- =====================================================
-- LOGIN HISTORY TABLE
-- =====================================================

CREATE TABLE iam.login_history (
                                   id BIGSERIAL PRIMARY KEY,
                                   user_id UUID REFERENCES iam.users(id) ON DELETE SET NULL,
                                   username VARCHAR(100),
                                   email common.email,

    -- Login details
                                   login_type VARCHAR(50), -- password, oauth, sso, biometric
                                   provider VARCHAR(50), -- local, google, facebook, apple
                                   success BOOLEAN NOT NULL,
                                   failure_reason VARCHAR(255),

    -- Device and location
                                   ip_address INET,
                                   user_agent TEXT,
                                   device_type VARCHAR(50),
                                   device_id VARCHAR(255),
                                   browser VARCHAR(100),
                                   os VARCHAR(100),
                                   location_country common.country_code,
                                   location_city VARCHAR(100),

    -- Security
                                   risk_score INTEGER DEFAULT 0,
                                   is_suspicious BOOLEAN DEFAULT FALSE,
                                   required_2fa BOOLEAN DEFAULT FALSE,
                                   completed_2fa BOOLEAN DEFAULT FALSE,

    -- Timestamp
                                   attempted_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_login_history_user_id ON iam.login_history(user_id);
CREATE INDEX idx_login_history_username ON iam.login_history(username);
CREATE INDEX idx_login_history_email ON iam.login_history(email);
CREATE INDEX idx_login_history_attempted_at ON iam.login_history(attempted_at);
CREATE INDEX idx_login_history_success ON iam.login_history(success);

-- =====================================================
-- PASSWORD RESET TOKENS TABLE
-- =====================================================

CREATE TABLE iam.password_reset_tokens (
                                           id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                           user_id UUID NOT NULL REFERENCES iam.users(id) ON DELETE CASCADE,
                                           token VARCHAR(500) UNIQUE NOT NULL,
                                           expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                           used BOOLEAN DEFAULT FALSE,
                                           used_at TIMESTAMP WITH TIME ZONE,
                                           ip_address INET,
                                           user_agent TEXT,
                                           created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_password_reset_tokens_user ON iam.password_reset_tokens(user_id);
CREATE INDEX idx_password_reset_tokens_token ON iam.password_reset_tokens(token) WHERE used = FALSE;
CREATE INDEX idx_password_reset_tokens_expires ON iam.password_reset_tokens(expires_at);

-- =====================================================
-- EMAIL VERIFICATION TOKENS TABLE
-- =====================================================

CREATE TABLE iam.email_verification_tokens (
                                               id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                               user_id UUID NOT NULL REFERENCES iam.users(id) ON DELETE CASCADE,
                                               email common.email NOT NULL,
                                               token VARCHAR(500) UNIQUE NOT NULL,
                                               expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                               verified BOOLEAN DEFAULT FALSE,
                                               verified_at TIMESTAMP WITH TIME ZONE,
                                               attempts INTEGER DEFAULT 0,
                                               created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_email_verification_user ON iam.email_verification_tokens(user_id);
CREATE INDEX idx_email_verification_token ON iam.email_verification_tokens(token) WHERE verified = FALSE;

-- =====================================================
-- TWO FACTOR AUTH TABLE
-- =====================================================

CREATE TABLE iam.two_factor_auth (
                                     id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                     user_id UUID NOT NULL REFERENCES iam.users(id) ON DELETE CASCADE,
                                     method VARCHAR(50) NOT NULL, -- totp, sms, email, backup_codes
                                     secret TEXT,
                                     backup_codes TEXT[],
                                     phone_number common.phone,
                                     email common.email,
                                     is_primary BOOLEAN DEFAULT FALSE,
                                     is_active BOOLEAN DEFAULT TRUE,
                                     verified BOOLEAN DEFAULT FALSE,
                                     verified_at TIMESTAMP WITH TIME ZONE,
                                     last_used_at TIMESTAMP WITH TIME ZONE,
                                     created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                     updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                                     UNIQUE(user_id, method)
);

CREATE INDEX idx_two_factor_auth_user ON iam.two_factor_auth(user_id);
CREATE INDEX idx_two_factor_auth_method ON iam.two_factor_auth(method);

-- =====================================================
-- OAUTH PROVIDERS TABLE
-- =====================================================

CREATE TABLE iam.oauth_providers (
                                     id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                     user_id UUID NOT NULL REFERENCES iam.users(id) ON DELETE CASCADE,
                                     provider VARCHAR(50) NOT NULL, -- google, facebook, apple, etc
                                     provider_user_id VARCHAR(255) NOT NULL,
                                     email common.email,
                                     name VARCHAR(255),
                                     avatar_url common.url,
                                     access_token TEXT,
                                     refresh_token TEXT,
                                     token_expires_at TIMESTAMP WITH TIME ZONE,
                                     raw_data JSONB,
                                     linked_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                     last_used_at TIMESTAMP WITH TIME ZONE,

                                     UNIQUE(provider, provider_user_id),
                                     UNIQUE(user_id, provider)
);

CREATE INDEX idx_oauth_providers_user ON iam.oauth_providers(user_id);
CREATE INDEX idx_oauth_providers_provider ON iam.oauth_providers(provider);

-- =====================================================
-- API KEYS TABLE
-- =====================================================

CREATE TABLE iam.api_keys (
                              id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                              user_id UUID NOT NULL REFERENCES iam.users(id) ON DELETE CASCADE,
                              name VARCHAR(100) NOT NULL,
                              key_hash VARCHAR(255) UNIQUE NOT NULL,
                              key_prefix VARCHAR(20) NOT NULL, -- First few chars for identification
                              description TEXT,

    -- Permissions and restrictions
                              permissions JSONB DEFAULT '[]',
                              allowed_ips INET[],
                              allowed_origins TEXT[],
                              rate_limit INTEGER DEFAULT 1000, -- requests per hour

    -- Usage tracking
                              last_used_at TIMESTAMP WITH TIME ZONE,
                              last_used_ip INET,
                              usage_count BIGINT DEFAULT 0,

    -- Status
                              is_active BOOLEAN DEFAULT TRUE,
                              expires_at TIMESTAMP WITH TIME ZONE,
                              revoked_at TIMESTAMP WITH TIME ZONE,
                              revoked_by VARCHAR(100),
                              revoke_reason TEXT,

    -- Metadata
                              created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_api_keys_user ON iam.api_keys(user_id);
CREATE INDEX idx_api_keys_hash ON iam.api_keys(key_hash) WHERE is_active = TRUE;
CREATE INDEX idx_api_keys_prefix ON iam.api_keys(key_prefix);

-- =====================================================
-- AUDIT LOG TRIGGERS
-- =====================================================

-- Users audit trigger
CREATE TRIGGER audit_users
    AFTER INSERT OR UPDATE OR DELETE ON iam.users
    FOR EACH ROW EXECUTE FUNCTION common.audit_trigger();

-- Sessions audit trigger
CREATE TRIGGER audit_sessions
    AFTER INSERT OR UPDATE OR DELETE ON iam.sessions
    FOR EACH ROW EXECUTE FUNCTION common.audit_trigger();

-- Login history doesn't need audit as it's already a log

-- =====================================================
-- UPDATE TRIGGERS
-- =====================================================

-- Auto-update updated_at timestamps
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON iam.users
    FOR EACH ROW EXECUTE FUNCTION common.update_updated_at_column();

CREATE TRIGGER update_user_profiles_updated_at
    BEFORE UPDATE ON iam.user_profiles
    FOR EACH ROW EXECUTE FUNCTION common.update_updated_at_column();

CREATE TRIGGER update_roles_updated_at
    BEFORE UPDATE ON iam.roles
    FOR EACH ROW EXECUTE FUNCTION common.update_updated_at_column();

CREATE TRIGGER update_sessions_updated_at
    BEFORE UPDATE ON iam.sessions
    FOR EACH ROW EXECUTE FUNCTION common.update_updated_at_column();

CREATE TRIGGER update_two_factor_auth_updated_at
    BEFORE UPDATE ON iam.two_factor_auth
    FOR EACH ROW EXECUTE FUNCTION common.update_updated_at_column();

CREATE TRIGGER update_api_keys_updated_at
    BEFORE UPDATE ON iam.api_keys
    FOR EACH ROW EXECUTE FUNCTION common.update_updated_at_column();

-- =====================================================
-- FUNCTIONS
-- =====================================================

-- Function to check if user has permission
CREATE OR REPLACE FUNCTION iam.user_has_permission(
    p_user_id UUID,
    p_resource VARCHAR,
    p_action VARCHAR
)
RETURNS BOOLEAN AS $$
DECLARE
has_permission BOOLEAN;
BEGIN
SELECT EXISTS (
    SELECT 1
    FROM iam.user_roles ur
             JOIN iam.role_permissions rp ON ur.role_id = rp.role_id
             JOIN iam.permissions p ON rp.permission_id = p.id
    WHERE ur.user_id = p_user_id
      AND ur.is_active = TRUE
      AND p.resource = p_resource
      AND p.action = p_action
      AND p.status = 'active'
) INTO has_permission;

RETURN has_permission;
END;
$$ LANGUAGE plpgsql;

-- Function to get user's active roles
CREATE OR REPLACE FUNCTION iam.get_user_roles(p_user_id UUID)
RETURNS TABLE(role_name VARCHAR, display_name VARCHAR) AS $$
BEGIN
RETURN QUERY
SELECT r.name, r.display_name
FROM iam.user_roles ur
         JOIN iam.roles r ON ur.role_id = r.id
WHERE ur.user_id = p_user_id
  AND ur.is_active = TRUE
  AND r.status = 'active'
  AND (ur.expires_at IS NULL OR ur.expires_at > CURRENT_TIMESTAMP);
END;
$$ LANGUAGE plpgsql;

-- Function to clean expired sessions
CREATE OR REPLACE FUNCTION iam.cleanup_expired_sessions()
RETURNS INTEGER AS $$
DECLARE
deleted_count INTEGER;
BEGIN
UPDATE iam.sessions
SET is_active = FALSE,
    terminated_at = CURRENT_TIMESTAMP,
    termination_reason = 'Session expired'
WHERE is_active = TRUE
  AND expires_at < CURRENT_TIMESTAMP;

GET DIAGNOSTICS deleted_count = ROW_COUNT;
RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- MIGRATION METADATA
-- =====================================================

INSERT INTO public.migration_metadata (version, description)
VALUES ('V3', 'IAM core tables - Users, Roles, Permissions, Sessions, and Authentication');