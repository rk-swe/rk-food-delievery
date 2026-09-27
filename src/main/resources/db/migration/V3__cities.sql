CREATE TABLE cities (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name text NOT NULL,
    state text NOT NULL,
    country text NOT NULL,
    currency varchar(3) NOT NULL CHECK (currency ~ '^[A-Z]{3}$'),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by uuid REFERENCES users(id),
    updated_by uuid REFERENCES users(id)
);

-- Names can repeat across states and countries.
CREATE UNIQUE INDEX cities_name_state_country_unique
    ON cities (lower(name), lower(state), lower(country));

CREATE TRIGGER cities_updated_at
    BEFORE UPDATE ON cities
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
