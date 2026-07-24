CREATE EXTENSION IF NOT EXISTS "pgcrypto";

INSERT INTO users (id, username, password_hash, email, created_at)
VALUES (
    gen_random_uuid(),
    'demo',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'demo@fiapx.local',
    NOW()
)
ON CONFLICT DO NOTHING;
