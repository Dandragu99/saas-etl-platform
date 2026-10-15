CREATE TABLE etl_executions (
    id UUID PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    finished_at TIMESTAMP WITH TIME ZONE,
    processed_record_count BIGINT,
    error_message VARCHAR(500),
    CONSTRAINT chk_etl_executions_processed_count
        CHECK (processed_record_count IS NULL OR processed_record_count >= 0)
);

CREATE INDEX idx_etl_executions_started_at
    ON etl_executions (started_at DESC);

CREATE INDEX idx_etl_executions_status_started_at
    ON etl_executions (status, started_at DESC);
