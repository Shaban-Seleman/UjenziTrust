INSERT INTO users.users (id, email, status, password_hash)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin@uzenji.local',
    'ACTIVE',
    '$2a$10$7EqJtq98hPqEX7fNZaFWoOeR9x3Vf5D6uY7m9Qj0tJYV8xG4uQ9pC'
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO users.user_roles (user_id, role)
VALUES ('00000000-0000-0000-0000-000000000001', 'ADMIN')
ON CONFLICT (user_id, role) DO NOTHING;
