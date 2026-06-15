CREATE TABLE IF NOT EXISTS member (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(120) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    role VARCHAR(30) NOT NULL CHECK (role IN ('USER', 'ADMIN')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS farm_profile (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL UNIQUE REFERENCES member (id) ON DELETE CASCADE,
    region VARCHAR(100),
    experience_level VARCHAR(30) NOT NULL CHECK (experience_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    farm_size VARCHAR(100),
    main_crop VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS crop (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(60) NOT NULL UNIQUE,
    category VARCHAR(60),
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_crop (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL REFERENCES member (id) ON DELETE CASCADE,
    crop_id BIGINT NOT NULL REFERENCES crop (id),
    cultivation_area VARCHAR(100),
    memo TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS crop_image (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL REFERENCES member (id) ON DELETE CASCADE,
    crop_id BIGINT REFERENCES crop (id),
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    content_type VARCHAR(100),
    file_size BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS disease_analysis (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL REFERENCES member (id) ON DELETE CASCADE,
    crop_image_id BIGINT NOT NULL REFERENCES crop_image (id) ON DELETE CASCADE,
    crop_name VARCHAR(100),
    predicted_disease VARCHAR(200),
    confidence DOUBLE PRECISION,
    risk_level VARCHAR(30) NOT NULL CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'UNKNOWN')),
    raw_response_json JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS agriculture_document (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    source VARCHAR(255),
    source_type VARCHAR(30) NOT NULL CHECK (source_type IN ('PDF', 'TEXT', 'API', 'MANUAL')),
    original_filename VARCHAR(255),
    storage_path VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS agriculture_document_chunk (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL REFERENCES agriculture_document (id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    embedding VECTOR(${embeddingDimension}),
    metadata_json JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS analysis_report (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL REFERENCES member (id) ON DELETE CASCADE,
    disease_analysis_id BIGINT REFERENCES disease_analysis (id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    summary TEXT,
    suspected_problem TEXT,
    recommended_actions TEXT,
    checklist TEXT,
    rag_references_json JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS external_api_log (
    id BIGSERIAL PRIMARY KEY,
    provider VARCHAR(100) NOT NULL,
    endpoint VARCHAR(255) NOT NULL,
    request_params JSONB,
    status_code INTEGER,
    success BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_farm_profile_member_id ON farm_profile (member_id);
CREATE INDEX IF NOT EXISTS idx_user_crop_member_id ON user_crop (member_id);
CREATE INDEX IF NOT EXISTS idx_user_crop_crop_id ON user_crop (crop_id);
CREATE INDEX IF NOT EXISTS idx_crop_image_member_id ON crop_image (member_id);
CREATE INDEX IF NOT EXISTS idx_crop_image_crop_id ON crop_image (crop_id);
CREATE INDEX IF NOT EXISTS idx_disease_analysis_member_id ON disease_analysis (member_id);
CREATE INDEX IF NOT EXISTS idx_disease_analysis_crop_image_id ON disease_analysis (crop_image_id);
CREATE INDEX IF NOT EXISTS idx_agriculture_document_chunk_document_id ON agriculture_document_chunk (document_id);
CREATE INDEX IF NOT EXISTS idx_analysis_report_member_id ON analysis_report (member_id);
CREATE INDEX IF NOT EXISTS idx_analysis_report_disease_analysis_id ON analysis_report (disease_analysis_id);
CREATE INDEX IF NOT EXISTS idx_external_api_log_provider ON external_api_log (provider);
