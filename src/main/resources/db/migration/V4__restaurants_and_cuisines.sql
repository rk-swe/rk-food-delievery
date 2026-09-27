CREATE TABLE restaurants (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name text NOT NULL,
    description text,
    image_url text,
    owner_id uuid NOT NULL REFERENCES users(id),
    cost_for_two numeric(10,2) NOT NULL CHECK (cost_for_two >= 0),
    diet_type text NOT NULL CHECK (diet_type IN ('Veg', 'Non Veg')),
    average_rating numeric(3,2) NOT NULL DEFAULT 0 CHECK (average_rating BETWEEN 0 AND 5),
    rating_count integer NOT NULL DEFAULT 0 CHECK (rating_count >= 0),
    address_line_1 text NOT NULL,
    address_line_2 text,
    city_id uuid NOT NULL REFERENCES cities(id),
    location geography(Point, 4326) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by uuid REFERENCES users(id),
    updated_by uuid REFERENCES users(id)
);
CREATE INDEX restaurants_owner_idx ON restaurants (owner_id);
CREATE INDEX restaurants_city_idx ON restaurants (city_id);
CREATE INDEX restaurants_location_idx ON restaurants USING gist (location);
CREATE INDEX restaurants_name_search_idx ON restaurants USING gin (name gin_trgm_ops);

CREATE TABLE restaurant_timings (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id uuid NOT NULL REFERENCES restaurants(id),
    day text NOT NULL CHECK (day IN ('Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday')),
    start_time time,
    end_time time,
    is_open boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by uuid REFERENCES users(id),
    updated_by uuid REFERENCES users(id),
    CONSTRAINT restaurant_timings_day_unique UNIQUE (restaurant_id, day),
    -- An end before the start represents an overnight opening window.
    CONSTRAINT restaurant_timings_hours_check CHECK (
        (is_open AND start_time IS NOT NULL AND end_time IS NOT NULL AND start_time <> end_time)
        OR (NOT is_open AND start_time IS NULL AND end_time IS NULL)
    )
);

CREATE TABLE cuisines (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name text NOT NULL,
    image_url text,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by uuid REFERENCES users(id),
    updated_by uuid REFERENCES users(id)
);
CREATE UNIQUE INDEX cuisines_name_unique ON cuisines (lower(name));

CREATE TABLE restaurant_cuisines (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id uuid NOT NULL REFERENCES restaurants(id),
    cuisine_id uuid NOT NULL REFERENCES cuisines(id),
    sort_order integer NOT NULL DEFAULT 0 CHECK (sort_order >= 0),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by uuid REFERENCES users(id),
    updated_by uuid REFERENCES users(id),
    CONSTRAINT restaurant_cuisines_pair_unique UNIQUE (restaurant_id, cuisine_id)
);
CREATE INDEX restaurant_cuisines_cuisine_idx ON restaurant_cuisines (cuisine_id, restaurant_id);

CREATE TRIGGER restaurants_updated_at
    BEFORE UPDATE ON restaurants
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER restaurant_timings_updated_at
    BEFORE UPDATE ON restaurant_timings
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER cuisines_updated_at
    BEFORE UPDATE ON cuisines
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER restaurant_cuisines_updated_at
    BEFORE UPDATE ON restaurant_cuisines
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
