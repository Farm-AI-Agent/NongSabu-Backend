INSERT INTO crops (name, category, description, created_at, updated_at)
VALUES
    ('딸기', '채소/과채류', '소규모 시설 재배에서 많이 선택하는 작물', now(), now()),
    ('토마토', '채소/과채류', '병해충 모니터링 수요가 높은 작물', now(), now()),
    ('오이', '채소/과채류', '하우스 재배 비중이 높은 작물', now(), now()),
    ('고추', '채소/과채류', '노지와 시설 모두 재배 비중이 높음', now(), now())
ON CONFLICT (name) DO NOTHING;

