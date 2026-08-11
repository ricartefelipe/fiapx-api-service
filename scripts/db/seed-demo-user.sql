INSERT INTO users (id, username, password_hash, email, created_at)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'fiapx',
    '$2b$10$qP8Eyf3sNatIGYSXxexPcuHx4JqL4lfqEND2FT752gmxG5PE2n0mi',
    'fiapx@fiapx.local',
    NOW()
)
ON CONFLICT (username) DO NOTHING;
