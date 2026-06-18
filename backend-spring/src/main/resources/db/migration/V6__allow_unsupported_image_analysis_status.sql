ALTER TABLE uploaded_images
    DROP CONSTRAINT IF EXISTS uploaded_images_analysis_status_check;

ALTER TABLE uploaded_images
    ADD CONSTRAINT uploaded_images_analysis_status_check
    CHECK (analysis_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'UNSUPPORTED', 'FAILED'));
