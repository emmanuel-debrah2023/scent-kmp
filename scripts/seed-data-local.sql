-- Local test data seed for Scent
-- For development only — never run against Supabase or production
-- Run after migrations: psql -U fragrance_user -d fragrances_dev -f scripts/seed-data-local.sql

\echo '=== Seeding Scent Local Database ==='

-- Seed users
INSERT INTO users (username, email, password_hash, display_name, created_at, is_seller)
VALUES
    ('alice_fragrance', 'alice@local.test', 'hash_alice', 'Alice Perfumer', NOW(), true),
    ('bob_collector', 'bob@local.test', 'hash_bob', 'Bob Collector', NOW(), false),
    ('charlie_dealer', 'charlie@local.test', 'hash_charlie', 'Charlie Dealer', NOW(), true),
    ('diana_tester', 'diana@local.test', 'hash_diana', 'Diana Tester', NOW(), false)
ON CONFLICT DO NOTHING;

-- Get IDs for use in other inserts
\set alice_id '(SELECT id FROM users WHERE username = ''alice_fragrance'' LIMIT 1)'
\set bob_id '(SELECT id FROM users WHERE username = ''bob_collector'' LIMIT 1)'
\set charlie_id '(SELECT id FROM users WHERE username = ''charlie_dealer'' LIMIT 1)'

-- Seed fragrances (for sale)
INSERT INTO fragrances (seller_id, name, brand, description, price, volume_ml, concentration, condition, stock_quantity, is_active, created_at)
VALUES
    (:alice_id, 'Aventus', 'Creed', 'Legendary fragrance with pineapple and ambroxan', 285.00, 100, 'EAU_DE_PARFUM', 'NEW', 5, true, NOW()),
    (:alice_id, 'Santal 33', 'Le Labo', 'Warm sandalwood with smooth ambroxan', 180.00, 50, 'EAU_DE_PARFUM', 'NEW', 3, true, NOW()),
    (:charlie_id, 'Bleu de Chanel', 'Chanel', 'Crisp and woody', 110.00, 100, 'EAU_DE_PARFUM', 'USED', 2, true, NOW()),
    (:charlie_id, 'Sauvage', 'Dior', 'Fresh and spicy', 95.00, 100, 'EAU_DE_TOILETTE', 'NEW', 10, true, NOW())
ON CONFLICT DO NOTHING;

-- Seed listings (marketplace)
INSERT INTO listings (seller_id, fragrance_id, price, condition, is_negotiable, stock_quantity, is_active, kind, created_at)
SELECT
    u.id as seller_id,
    f.id as fragrance_id,
    (f.price * 0.9) as price,
    f.condition,
    (f.id % 2 = 0) as is_negotiable,
    f.stock_quantity,
    true,
    'SEALED',
    NOW()
FROM fragrances f
JOIN users u ON f.seller_id = u.id
WHERE f.is_active
ON CONFLICT DO NOTHING;

\echo 'Seed data applied successfully.'
\echo 'Created 4 users, 4 fragrances, and 4 listings.'
