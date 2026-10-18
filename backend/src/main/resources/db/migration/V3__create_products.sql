CREATE TABLE products (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL,
    sku VARCHAR(80) NOT NULL,
    normalized_sku VARCHAR(80) NOT NULL,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(2000),
    category VARCHAR(100) NOT NULL,
    price NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    unit_of_measure VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_products_company
        FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT uq_products_company_normalized_sku
        UNIQUE (company_id, normalized_sku),
    CONSTRAINT chk_products_sku_not_blank
        CHECK (CHAR_LENGTH(TRIM(sku)) > 0),
    CONSTRAINT chk_products_normalized_sku
        CHECK (
            CHAR_LENGTH(TRIM(normalized_sku)) > 0
            AND normalized_sku = UPPER(normalized_sku)
        ),
    CONSTRAINT chk_products_name_not_blank
        CHECK (CHAR_LENGTH(TRIM(name)) > 0),
    CONSTRAINT chk_products_category_not_blank
        CHECK (CHAR_LENGTH(TRIM(category)) > 0),
    CONSTRAINT chk_products_price
        CHECK (price >= 0),
    CONSTRAINT chk_products_currency_format
        CHECK (
            CHAR_LENGTH(currency) = 3
            AND currency = UPPER(currency)
        ),
    CONSTRAINT chk_products_unit_of_measure
        CHECK (unit_of_measure IN ('UNIT', 'KG', 'G', 'L', 'ML', 'M', 'M2', 'HOUR')),
    CONSTRAINT chk_products_version
        CHECK (version >= 0),
    CONSTRAINT chk_products_timestamp_order
        CHECK (updated_at >= created_at)
);

CREATE INDEX idx_products_company_active_name
    ON products (company_id, active, name);

CREATE INDEX idx_products_company_category
    ON products (company_id, category);
