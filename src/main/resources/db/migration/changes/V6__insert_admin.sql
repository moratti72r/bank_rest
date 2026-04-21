INSERT INTO users (id, first_name, last_name, phone_number, email, password)
VALUES (
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'Admin',
    'User',
    '+79990000000',
    'admin@example.com',
    '$2a$10$I/9XI61bvxfV/a.2iPXfX.4G1zq9uMHLdqf1QsJFu9ITINMnNABsq'
);
--ON CONFLICT (email) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
VALUES ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 2);
--ON CONFLICT DO NOTHING;