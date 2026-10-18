CREATE TABLE companies (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    default_currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_companies_name_not_blank
        CHECK (CHAR_LENGTH(TRIM(name)) > 0),
    CONSTRAINT chk_companies_currency_format
        CHECK (
            CHAR_LENGTH(default_currency) = 3
            AND default_currency = UPPER(default_currency)
        ),
    CONSTRAINT chk_companies_status
        CHECK (status IN ('ACTIVE', 'SUSPENDED')),
    CONSTRAINT chk_companies_timestamp_order
        CHECK (updated_at >= created_at)
);

CREATE INDEX idx_companies_status
    ON companies (status);
