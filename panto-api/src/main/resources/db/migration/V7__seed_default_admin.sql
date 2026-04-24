INSERT INTO users (
    username,
    password_hash,
    full_name,
    email,
    role,
    is_active,
    must_change_password,
    created_at,
    updated_at,
    created_by,
    updated_by
)
SELECT
    'admin',
    '$2a$12$e/PAc5dneCYr8mbd.P5yOett6vSnbOyJRvTSSutS7f.3VnE/DNGLK',
    'System Administrator',
    NULL,
    'ADMIN',
    TRUE,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    NULL,
    NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM users
    WHERE username = 'admin'
);
