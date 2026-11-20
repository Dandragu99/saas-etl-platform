CREATE TABLE customers (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies(id),
    name VARCHAR(160) NOT NULL,
    tax_id VARCHAR(30),
    email VARCHAR(320),
    phone VARCHAR(40),
    address VARCHAR(500),
    notes VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_customers_name CHECK (CHAR_LENGTH(TRIM(name)) > 0)
);
CREATE INDEX idx_customers_company_name ON customers(company_id, name);

CREATE TABLE works (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies(id),
    customer_id UUID NOT NULL REFERENCES customers(id),
    title VARCHAR(180) NOT NULL,
    description VARCHAR(2000),
    status VARCHAR(20) NOT NULL,
    start_date DATE,
    end_date DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_works_status CHECK (status IN ('PLANNED','IN_PROGRESS','COMPLETED'))
);
CREATE INDEX idx_works_company_status ON works(company_id, status);

CREATE TABLE quotes (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies(id),
    customer_id UUID NOT NULL REFERENCES customers(id),
    quote_number VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    issue_date DATE NOT NULL,
    valid_until DATE NOT NULL,
    notes VARCHAR(2000),
    subtotal NUMERIC(15,2) NOT NULL,
    tax_total NUMERIC(15,2) NOT NULL,
    total NUMERIC(15,2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_quotes_company_number UNIQUE(company_id, quote_number),
    CONSTRAINT chk_quotes_status CHECK (status IN ('DRAFT','ACCEPTED','INVOICED')),
    CONSTRAINT chk_quotes_totals CHECK (subtotal >= 0 AND tax_total >= 0 AND total >= 0)
);
CREATE INDEX idx_quotes_company_date ON quotes(company_id, issue_date DESC);

CREATE TABLE quote_lines (
    id UUID PRIMARY KEY,
    quote_id UUID NOT NULL REFERENCES quotes(id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    description VARCHAR(500) NOT NULL,
    quantity NUMERIC(12,3) NOT NULL,
    unit_price NUMERIC(15,2) NOT NULL,
    tax_rate NUMERIC(5,2) NOT NULL,
    line_subtotal NUMERIC(15,2) NOT NULL,
    line_tax NUMERIC(15,2) NOT NULL,
    line_total NUMERIC(15,2) NOT NULL,
    CONSTRAINT uq_quote_lines_position UNIQUE(quote_id, position),
    CONSTRAINT chk_quote_lines_values CHECK (
        quantity > 0 AND unit_price >= 0 AND tax_rate >= 0 AND tax_rate <= 100
    )
);

CREATE TABLE invoices (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies(id),
    customer_id UUID NOT NULL REFERENCES customers(id),
    quote_id UUID REFERENCES quotes(id),
    invoice_number VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    notes VARCHAR(2000),
    subtotal NUMERIC(15,2) NOT NULL,
    tax_total NUMERIC(15,2) NOT NULL,
    total NUMERIC(15,2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_invoices_company_number UNIQUE(company_id, invoice_number),
    CONSTRAINT uq_invoices_quote UNIQUE(quote_id),
    CONSTRAINT chk_invoices_status CHECK (status IN ('ISSUED','PAID')),
    CONSTRAINT chk_invoices_totals CHECK (subtotal >= 0 AND tax_total >= 0 AND total >= 0)
);
CREATE INDEX idx_invoices_company_date ON invoices(company_id, issue_date DESC);

CREATE TABLE invoice_lines (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    description VARCHAR(500) NOT NULL,
    quantity NUMERIC(12,3) NOT NULL,
    unit_price NUMERIC(15,2) NOT NULL,
    tax_rate NUMERIC(5,2) NOT NULL,
    line_subtotal NUMERIC(15,2) NOT NULL,
    line_tax NUMERIC(15,2) NOT NULL,
    line_total NUMERIC(15,2) NOT NULL,
    CONSTRAINT uq_invoice_lines_position UNIQUE(invoice_id, position)
);
