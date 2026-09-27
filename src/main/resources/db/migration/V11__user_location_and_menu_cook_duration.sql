-- NULL represents a location or cooking duration that is not yet known.
ALTER TABLE users ADD COLUMN location geography(Point, 4326);
CREATE INDEX users_location_idx ON users USING gist (location);

ALTER TABLE menu_items
    ADD COLUMN cook_duration_seconds integer
        CONSTRAINT menu_items_cook_duration_nonnegative CHECK (cook_duration_seconds >= 0);
