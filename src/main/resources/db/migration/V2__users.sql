CREATE TABLE users (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name text NOT NULL,
    email text NOT NULL,
    phone_number text NOT NULL CHECK (phone_number ~ '^\+[1-9][0-9]{1,14}$'),
    role text NOT NULL CHECK (role IN ('admin', 'restaurant_owner', 'customer', 'delivery_partner')),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT users_role_phone_unique UNIQUE (role, phone_number)
);

CREATE UNIQUE INDEX users_role_email_unique ON users (role, lower(email));

CREATE TRIGGER users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
