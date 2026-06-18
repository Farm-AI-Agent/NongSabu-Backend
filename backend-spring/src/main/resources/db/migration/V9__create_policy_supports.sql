CREATE TABLE IF NOT EXISTS policy_supports (
    id BIGSERIAL PRIMARY KEY,
    source VARCHAR(40) NOT NULL,
    external_id VARCHAR(120) NOT NULL,
    title VARCHAR(255) NOT NULL,
    summary TEXT,
    target_group VARCHAR(1000),
    region VARCHAR(100),
    category VARCHAR(120),
    application_period VARCHAR(120),
    apply_method VARCHAR(1000),
    department VARCHAR(200),
    contact VARCHAR(100),
    detail_url VARCHAR(1000),
    content_text TEXT NOT NULL,
    content_hash CHAR(64) NOT NULL,
    embedding_synced_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_policy_supports_source_external UNIQUE (source, external_id)
);

CREATE INDEX IF NOT EXISTS idx_policy_supports_source ON policy_supports (source);
CREATE INDEX IF NOT EXISTS idx_policy_supports_region ON policy_supports (region);
CREATE INDEX IF NOT EXISTS idx_policy_supports_category ON policy_supports (category);
