DELETE FROM crop c
WHERE c.name NOT IN (
    U&'\D3EC\B3C4',
    U&'\D1A0\B9C8\D1A0',
    U&'\B538\AE30',
    U&'\C624\C774',
    U&'\D30C\D504\B9AC\CE74'
)
AND NOT EXISTS (
    SELECT 1 FROM user_crop uc WHERE uc.crop_id = c.id
)
AND NOT EXISTS (
    SELECT 1 FROM farm_crops fc WHERE fc.crop_id = c.id
)
AND NOT EXISTS (
    SELECT 1 FROM uploaded_images ui WHERE ui.crop_id = c.id
);
