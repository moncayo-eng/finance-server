-- ============================================================
-- V1: Users, Households, Household Members
-- ============================================================

-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ---------- users ----------
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL,
    name          VARCHAR(255) NOT NULL,
    google_id     VARCHAR(255),
    password_hash VARCHAR(255),
    avatar_url    VARCHAR(512),
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT uq_users_email     UNIQUE (email),
    CONSTRAINT uq_users_google_id UNIQUE (google_id)
);

-- ---------- households ----------
CREATE TABLE households (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    max_members INT NOT NULL DEFAULT 6,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP NOT NULL DEFAULT now()
);

-- ---------- household_members ----------
CREATE TABLE household_members (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    household_id UUID NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role         VARCHAR(20) NOT NULL,
    display_name VARCHAR(255),
    joined_at    TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT uq_household_members_household_user UNIQUE (household_id, user_id),
    CONSTRAINT chk_household_members_role CHECK (role IN ('OWNER', 'MEMBER'))
);

-- Indexes for common query patterns
CREATE INDEX idx_household_members_user_id ON household_members(user_id);
CREATE INDEX idx_household_members_household_id ON household_members(household_id);
CREATE INDEX idx_users_email ON users(email);
