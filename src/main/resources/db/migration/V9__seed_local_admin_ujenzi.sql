INSERT INTO users.users (id, email, status, password_hash)
VALUES (
    '00000000-0000-0000-0000-000000000002',
    'admin@ujenzi.co.tz',
    'ACTIVE',
    '$2y$10$PoKbdBgNdQL/IX0TLxUOZuHv826kC.OKFsJOnwVH360T/aGkMqahu'
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO users.user_roles (user_id, role)
VALUES ('00000000-0000-0000-0000-000000000002', 'ADMIN')
ON CONFLICT (user_id, role) DO NOTHING;
