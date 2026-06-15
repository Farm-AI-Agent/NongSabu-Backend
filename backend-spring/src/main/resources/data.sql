INSERT INTO crop (name, category, description, created_at, updated_at)
VALUES
    ('포도', '과수', '초보 농가도 품종과 시설 조건에 맞춰 관리할 수 있는 과수 작물', now(), now()),
    ('호박', '채소', '노지와 시설 재배 모두에서 시작하기 좋은 채소 작물', now(), now())
ON CONFLICT (name) DO NOTHING;
