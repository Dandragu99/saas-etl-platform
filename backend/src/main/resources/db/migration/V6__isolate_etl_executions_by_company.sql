ALTER TABLE etl_executions
    ADD COLUMN company_id UUID;

ALTER TABLE etl_executions
    ADD COLUMN source_file_name VARCHAR(255);

ALTER TABLE etl_executions
    ADD COLUMN result_file_name VARCHAR(255);

ALTER TABLE etl_executions
    ADD CONSTRAINT fk_etl_executions_company
        FOREIGN KEY (company_id) REFERENCES companies (id) ON DELETE RESTRICT;

CREATE INDEX idx_etl_executions_company_started_at
    ON etl_executions (company_id, started_at DESC, id DESC);

CREATE INDEX idx_etl_executions_company_status_started_at
    ON etl_executions (company_id, status, started_at DESC, id DESC);
