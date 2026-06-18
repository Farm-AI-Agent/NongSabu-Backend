-- RAG 검색 품질을 위해 원본 문서(document_assets)와 검색 단위 chunk(document_chunks)를 분리한다.
CREATE TABLE IF NOT EXISTS document_chunks (
    id BIGSERIAL PRIMARY KEY,
    document_asset_id BIGINT NOT NULL REFERENCES document_assets (id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    embedding VECTOR(${embeddingDimension}),
    page_number INTEGER,
    section_title VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_document_chunks_asset_index UNIQUE (document_asset_id, chunk_index)
);

CREATE INDEX IF NOT EXISTS idx_document_chunks_document_asset_id ON document_chunks (document_asset_id);

-- 회원 농업 프로필의 대표 작물은 문자열 대신 CROP 마스터와 연결한다.
ALTER TABLE farm_profile
    ADD COLUMN IF NOT EXISTS main_crop_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_farm_profile_main_crop_id'
    ) THEN
        ALTER TABLE farm_profile
            ADD CONSTRAINT fk_farm_profile_main_crop_id
            FOREIGN KEY (main_crop_id) REFERENCES crop (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_farm_profile_main_crop_id ON farm_profile (main_crop_id);

-- 기존 문자열 main_crop 값은 가능한 경우 crop FK로 옮겨 둔다.
UPDATE farm_profile fp
SET main_crop_id = c.id
FROM crop c
WHERE fp.main_crop_id IS NULL
  AND fp.main_crop IS NOT NULL
  AND c.name = fp.main_crop;

-- 농장별 실제 재배 작물은 farm_crops가 담당하므로 farms.crop_summary 중복 필드는 제거한다.
ALTER TABLE farms
    DROP COLUMN IF EXISTS crop_summary;

-- 병충해 분석 업로드에서는 선택 작물이 필수다.
-- 과거 레거시 업로드에 crop_id가 없으면 MVP 기본 지원 작물인 포도로 보정한 뒤 NOT NULL을 적용한다.
UPDATE uploaded_images ui
SET crop_id = c.id
FROM crop c
WHERE ui.crop_id IS NULL
  AND c.name = '포도';

ALTER TABLE uploaded_images
    ALTER COLUMN crop_id SET NOT NULL;

-- 외부 API 호출이 어떤 회원/리포트 생성 흐름에서 발생했는지 추적한다.
ALTER TABLE external_api_log
    ADD COLUMN IF NOT EXISTS member_id BIGINT,
    ADD COLUMN IF NOT EXISTS analysis_report_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_external_api_log_member_id'
    ) THEN
        ALTER TABLE external_api_log
            ADD CONSTRAINT fk_external_api_log_member_id
            FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_external_api_log_analysis_report_id'
    ) THEN
        ALTER TABLE external_api_log
            ADD CONSTRAINT fk_external_api_log_analysis_report_id
            FOREIGN KEY (analysis_report_id) REFERENCES analysis_reports (id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_external_api_log_member_id ON external_api_log (member_id);
CREATE INDEX IF NOT EXISTS idx_external_api_log_analysis_report_id ON external_api_log (analysis_report_id);

-- MCP와 LLM Tool Calling 호출 흐름을 추적하기 위한 선택 로그 테이블이다.
CREATE TABLE IF NOT EXISTS tool_call_log (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT REFERENCES member (id) ON DELETE SET NULL,
    tool_name VARCHAR(120) NOT NULL,
    request_payload JSONB,
    response_payload JSONB,
    success BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tool_call_log_member_id ON tool_call_log (member_id);
CREATE INDEX IF NOT EXISTS idx_tool_call_log_tool_name ON tool_call_log (tool_name);
