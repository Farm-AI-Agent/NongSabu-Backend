ALTER TABLE farm_profile
    ALTER COLUMN experience_level DROP NOT NULL;

ALTER TABLE farm_profile
    ADD COLUMN IF NOT EXISTS age INTEGER,
    ADD COLUMN IF NOT EXISTS young_farmer_eligible BOOLEAN,
    ADD COLUMN IF NOT EXISTS farming_start_year INTEGER,
    ADD COLUMN IF NOT EXISTS farming_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS residence_region VARCHAR(120),
    ADD COLUMN IF NOT EXISTS farmland_region VARCHAR(120),
    ADD COLUMN IF NOT EXISTS primary_crop_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS secondary_crop_names VARCHAR(500),
    ADD COLUMN IF NOT EXISTS cultivation_area VARCHAR(100),
    ADD COLUMN IF NOT EXISTS cultivation_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS applicant_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS registered_farm_business BOOLEAN,
    ADD COLUMN IF NOT EXISTS annual_sales_range VARCHAR(100),
    ADD COLUMN IF NOT EXISTS desired_support_types VARCHAR(500),
    ADD COLUMN IF NOT EXISTS self_contribution_available BOOLEAN,
    ADD COLUMN IF NOT EXISTS received_policy_names VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS application_period_preference VARCHAR(100);
