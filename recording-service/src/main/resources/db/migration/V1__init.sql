-- Flyway: recordings table
CREATE TABLE IF NOT EXISTS recordings (
    id UUID PRIMARY KEY,
    meeting_id UUID NOT NULL,
    storage_url VARCHAR(1024),
    duration_seconds BIGINT,
    status VARCHAR(32) NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
