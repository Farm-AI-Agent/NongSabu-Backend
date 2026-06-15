INSERT INTO crop (name, category, description, created_at, updated_at)
VALUES
    ('strawberry', 'fruit', 'Popular crop for small greenhouse farms.', now(), now()),
    ('tomato', 'vegetable', 'Frequently managed for disease monitoring.', now(), now()),
    ('cucumber', 'vegetable', 'Common protected cultivation crop.', now(), now()),
    ('pepper', 'vegetable', 'Widely cultivated with recurring pest checks.', now(), now())
ON CONFLICT (name) DO NOTHING;
