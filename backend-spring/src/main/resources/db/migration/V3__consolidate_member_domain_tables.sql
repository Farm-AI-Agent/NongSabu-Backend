-- Canonical API tables. These were previously created implicitly by Hibernate.
CREATE TABLE IF NOT EXISTS farms (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL REFERENCES member (id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    location VARCHAR(120),
    cultivation_area VARCHAR(50),
    crop_summary VARCHAR(255),
    notes VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS farm_crops (
    id BIGSERIAL PRIMARY KEY,
    farm_id BIGINT NOT NULL REFERENCES farms (id) ON DELETE CASCADE,
    crop_id BIGINT NOT NULL REFERENCES crop (id),
    status VARCHAR(30),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_farm_crops_farm_crop UNIQUE (farm_id, crop_id)
);

CREATE TABLE IF NOT EXISTS uploaded_images (
    id BIGSERIAL PRIMARY KEY,
    farm_id BIGINT NOT NULL REFERENCES farms (id) ON DELETE CASCADE,
    member_id BIGINT NOT NULL REFERENCES member (id) ON DELETE CASCADE,
    original_filename VARCHAR(255) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    content_type VARCHAR(100),
    file_size BIGINT NOT NULL,
    analysis_status VARCHAR(20) NOT NULL
        CHECK (analysis_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS image_analysis_results (
    id BIGSERIAL PRIMARY KEY,
    uploaded_image_id BIGINT NOT NULL UNIQUE REFERENCES uploaded_images (id) ON DELETE CASCADE,
    disease_name VARCHAR(120),
    confidence DOUBLE PRECISION NOT NULL,
    severity VARCHAR(50),
    summary VARCHAR(1000),
    recommendation VARCHAR(2000),
    raw_response TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS document_assets (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL REFERENCES member (id) ON DELETE CASCADE,
    original_filename VARCHAR(255) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    content_type VARCHAR(100),
    source_type VARCHAR(50),
    parsing_status VARCHAR(20) NOT NULL
        CHECK (parsing_status IN ('UPLOADED', 'PARSED', 'FAILED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS analysis_reports (
    id BIGSERIAL PRIMARY KEY,
    uploaded_image_id BIGINT NOT NULL UNIQUE REFERENCES uploaded_images (id) ON DELETE CASCADE,
    report_text TEXT,
    rag_context TEXT,
    external_market_context TEXT,
    status VARCHAR(20) NOT NULL CHECK (status IN ('GENERATED', 'FAILED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Move legacy users into Member before removing legacy ownership columns.
DO $$
BEGIN
    IF to_regclass('public.users') IS NOT NULL THEN
        INSERT INTO member (email, password, name, role, created_at, updated_at)
        SELECT email, password_hash, full_name, role, created_at, updated_at
        FROM users
        ON CONFLICT (email) DO NOTHING;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'farms' AND column_name = 'owner_id'
    ) THEN
        UPDATE farms f
        SET member_id = m.id
        FROM users u
        JOIN member m ON m.email = u.email
        WHERE f.owner_id = u.id AND f.member_id IS NULL;
        ALTER TABLE farms DROP COLUMN owner_id CASCADE;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'uploaded_images' AND column_name = 'uploaded_by'
    ) THEN
        UPDATE uploaded_images i
        SET member_id = m.id
        FROM users u
        JOIN member m ON m.email = u.email
        WHERE i.uploaded_by = u.id AND i.member_id IS NULL;
        ALTER TABLE uploaded_images DROP COLUMN uploaded_by CASCADE;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'document_assets' AND column_name = 'uploaded_by'
    ) THEN
        UPDATE document_assets d
        SET member_id = m.id
        FROM users u
        JOIN member m ON m.email = u.email
        WHERE d.uploaded_by = u.id AND d.member_id IS NULL;
        ALTER TABLE document_assets DROP COLUMN uploaded_by CASCADE;
    END IF;
END $$;

-- Deprecated tables were never used by the active API. Refuse to discard unexpected data.
DO $$
DECLARE
    table_name TEXT;
    row_count BIGINT;
BEGIN
    FOREACH table_name IN ARRAY ARRAY[
        'analysis_report',
        'disease_analysis',
        'crop_image',
        'agriculture_document_chunk',
        'agriculture_document',
        'document_chunks'
    ]
    LOOP
        IF to_regclass('public.' || table_name) IS NOT NULL THEN
            EXECUTE format('SELECT count(*) FROM %I', table_name) INTO row_count;
            IF row_count > 0 THEN
                RAISE EXCEPTION
                    'Deprecated table % contains % rows. Migrate them manually before applying V3.',
                    table_name, row_count;
            END IF;
        END IF;
    END LOOP;
END $$;

DROP TABLE IF EXISTS analysis_report CASCADE;
DROP TABLE IF EXISTS disease_analysis CASCADE;
DROP TABLE IF EXISTS crop_image CASCADE;
DROP TABLE IF EXISTS agriculture_document_chunk CASCADE;
DROP TABLE IF EXISTS agriculture_document CASCADE;
DROP TABLE IF EXISTS document_chunks CASCADE;
DROP TABLE IF EXISTS users CASCADE;

CREATE INDEX IF NOT EXISTS idx_farms_member_id ON farms (member_id);
CREATE INDEX IF NOT EXISTS idx_farm_crops_farm_id ON farm_crops (farm_id);
CREATE INDEX IF NOT EXISTS idx_farm_crops_crop_id ON farm_crops (crop_id);
CREATE INDEX IF NOT EXISTS idx_uploaded_images_member_id ON uploaded_images (member_id);
CREATE INDEX IF NOT EXISTS idx_uploaded_images_farm_id ON uploaded_images (farm_id);
CREATE INDEX IF NOT EXISTS idx_document_assets_member_id ON document_assets (member_id);

INSERT INTO crop (name, category, description, created_at, updated_at)
VALUES
    ('포도', '과수', '초보 농가도 품종과 시설 조건에 맞춰 관리할 수 있는 과수 작물', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('호박', '채소', '노지와 시설 재배 모두에서 시작하기 좋은 채소 작물', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;
