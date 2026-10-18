CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    normalized_email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_login_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_users_normalized_email UNIQUE (normalized_email),
    CONSTRAINT chk_users_email_not_blank CHECK (CHAR_LENGTH(TRIM(email)) > 0),
    CONSTRAINT chk_users_normalized_email CHECK (
        CHAR_LENGTH(TRIM(normalized_email)) > 0
        AND normalized_email = LOWER(normalized_email)
    ),
    CONSTRAINT chk_users_password_hash_not_blank CHECK (CHAR_LENGTH(TRIM(password_hash)) > 0),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'LOCKED', 'DISABLED')),
    CONSTRAINT chk_users_version CHECK (version >= 0),
    CONSTRAINT chk_users_timestamp_order CHECK (updated_at >= created_at)
);

CREATE INDEX idx_users_status ON users (status);

CREATE TABLE company_memberships (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    company_id UUID NOT NULL,
    role VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_company_memberships_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_company_memberships_company FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT uq_company_memberships_user_company UNIQUE (user_id, company_id),
    CONSTRAINT chk_company_memberships_role
        CHECK (role IN ('OWNER', 'ADMIN', 'INVENTORY_MANAGER', 'VIEWER')),
    CONSTRAINT chk_company_memberships_status CHECK (status IN ('ACTIVE', 'SUSPENDED')),
    CONSTRAINT chk_company_memberships_version CHECK (version >= 0),
    CONSTRAINT chk_company_memberships_timestamp_order CHECK (updated_at >= created_at)
);

CREATE INDEX idx_company_memberships_company_status
    ON company_memberships (company_id, status);

CREATE INDEX idx_company_memberships_user_status
    ON company_memberships (user_id, status);
