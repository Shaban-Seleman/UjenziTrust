CREATE TABLE users.users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone VARCHAR(32),
    email VARCHAR(255),
    status VARCHAR(32) NOT NULL,
    password_hash VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_users_contact_present CHECK (phone IS NOT NULL OR email IS NOT NULL)
);

CREATE UNIQUE INDEX uq_users_phone ON users.users (phone) WHERE phone IS NOT NULL;
CREATE UNIQUE INDEX uq_users_email ON users.users (email) WHERE email IS NOT NULL;

CREATE TABLE users.user_roles (
    user_id UUID NOT NULL REFERENCES users.users(id),
    role VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, role)
);

CREATE TABLE users.sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users.users(id),
    token_id VARCHAR(128) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_at TIMESTAMPTZ,
    UNIQUE (token_id)
);

CREATE INDEX idx_sessions_user_id ON users.sessions (user_id);
CREATE INDEX idx_sessions_expires_at ON users.sessions (expires_at);
