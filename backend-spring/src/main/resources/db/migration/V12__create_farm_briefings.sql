CREATE TABLE IF NOT EXISTS farm_briefing (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL REFERENCES member (id) ON DELETE CASCADE,
    briefing_date DATE NOT NULL,
    weather_hash VARCHAR(64) NOT NULL,
    profile_hash VARCHAR(64) NOT NULL,
    weather_payload TEXT NOT NULL,
    response_payload TEXT NOT NULL,
    ai_generated BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_farm_briefing_member_date
    ON farm_briefing (member_id, briefing_date DESC);

CREATE INDEX IF NOT EXISTS idx_farm_briefing_cache
    ON farm_briefing (member_id, briefing_date, weather_hash, profile_hash);
