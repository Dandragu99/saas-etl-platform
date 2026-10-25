CREATE TABLE inventory_stocks (
    company_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity NUMERIC(19,4) NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_inventory_stocks PRIMARY KEY (company_id, product_id),
    CONSTRAINT fk_inventory_stocks_company
        FOREIGN KEY (company_id) REFERENCES companies (id) ON DELETE RESTRICT,
    CONSTRAINT fk_inventory_stocks_product
        FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE RESTRICT,
    CONSTRAINT chk_inventory_stocks_quantity CHECK (quantity >= 0),
    CONSTRAINT chk_inventory_stocks_version CHECK (version >= 0)
);

INSERT INTO inventory_stocks (company_id, product_id, quantity, version, updated_at)
SELECT company_id, id, 0, 0, updated_at
FROM products;

CREATE TABLE inventory_movements (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL,
    product_id UUID NOT NULL,
    movement_type VARCHAR(20) NOT NULL,
    quantity NUMERIC(19,4) NOT NULL,
    responsible_user_id UUID NOT NULL,
    responsible_user_email VARCHAR(320) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_inventory_movements_company
        FOREIGN KEY (company_id) REFERENCES companies (id) ON DELETE RESTRICT,
    CONSTRAINT fk_inventory_movements_product
        FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE RESTRICT,
    CONSTRAINT fk_inventory_movements_user
        FOREIGN KEY (responsible_user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_inventory_movements_type CHECK (movement_type IN ('ENTRY', 'EXIT')),
    CONSTRAINT chk_inventory_movements_quantity CHECK (quantity > 0),
    CONSTRAINT chk_inventory_movements_email
        CHECK (CHAR_LENGTH(TRIM(responsible_user_email)) > 0)
);

CREATE INDEX idx_inventory_stocks_company_product
    ON inventory_stocks (company_id, product_id);

CREATE INDEX idx_inventory_movements_company_occurred
    ON inventory_movements (company_id, occurred_at DESC, id DESC);

CREATE INDEX idx_inventory_movements_company_product_occurred
    ON inventory_movements (company_id, product_id, occurred_at DESC, id DESC);
