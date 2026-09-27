ALTER TABLE cities ADD COLUMN active boolean NOT NULL DEFAULT true;
ALTER TABLE restaurants ADD COLUMN active boolean NOT NULL DEFAULT true;
ALTER TABLE users ADD COLUMN online boolean NOT NULL DEFAULT false;
ALTER TABLE users ADD COLUMN location_updated_at timestamptz;
ALTER TABLE users ADD COLUMN created_by uuid REFERENCES users(id);
ALTER TABLE users ADD COLUMN updated_by uuid REFERENCES users(id);

CREATE INDEX cities_active_idx ON cities (active, id);
CREATE INDEX restaurants_active_idx ON restaurants (active, city_id, id);
CREATE INDEX users_partner_presence_idx ON users (active, online, location_updated_at)
    WHERE role = 'delivery_partner';
