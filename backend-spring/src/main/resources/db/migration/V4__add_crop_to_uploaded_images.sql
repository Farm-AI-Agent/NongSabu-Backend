ALTER TABLE uploaded_images
    ADD COLUMN IF NOT EXISTS crop_id BIGINT;

ALTER TABLE uploaded_images
    ALTER COLUMN farm_id DROP NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_uploaded_images_crop_id'
    ) THEN
        ALTER TABLE uploaded_images
            ADD CONSTRAINT fk_uploaded_images_crop_id
            FOREIGN KEY (crop_id) REFERENCES crop (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_uploaded_images_crop_id ON uploaded_images (crop_id);

INSERT INTO crop (name, category, description, created_at, updated_at)
VALUES
    (U&'\D3EC\B3C4', U&'\ACFC\C218', 'Primary MVP image analysis crop', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (U&'\D1A0\B9C8\D1A0', U&'\CC44\C18C', 'Candidate crop for future image analysis support', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (U&'\B538\AE30', U&'\ACFC\CC44\B958', 'Candidate crop for future image analysis support', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (U&'\C624\C774', U&'\CC44\C18C', 'Candidate crop for future image analysis support', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (U&'\D30C\D504\B9AC\CE74', U&'\CC44\C18C', 'Candidate crop for future image analysis support', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO UPDATE
SET category = EXCLUDED.category,
    description = EXCLUDED.description,
    updated_at = CURRENT_TIMESTAMP;
