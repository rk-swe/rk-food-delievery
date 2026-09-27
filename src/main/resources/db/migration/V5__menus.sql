CREATE TABLE menu_categories (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id uuid NOT NULL REFERENCES restaurants(id),
    name text NOT NULL,
    sort_order integer NOT NULL DEFAULT 0 CHECK (sort_order >= 0),
    item_count integer NOT NULL DEFAULT 0 CHECK (item_count >= 0),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by uuid REFERENCES users(id),
    updated_by uuid REFERENCES users(id),
    CONSTRAINT menu_categories_id_restaurant_unique UNIQUE (id, restaurant_id)
);
CREATE UNIQUE INDEX menu_categories_restaurant_name_unique ON menu_categories (restaurant_id, lower(name));
CREATE INDEX menu_categories_listing_idx ON menu_categories (restaurant_id, sort_order);

CREATE TABLE menu_items (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id uuid NOT NULL REFERENCES restaurants(id),
    category_id uuid NOT NULL,
    name text NOT NULL,
    description text,
    image_url text,
    ingredients text,
    calories_kcal integer CHECK (calories_kcal >= 0),
    diet_type text NOT NULL CHECK (diet_type IN ('Veg', 'Non Veg')),
    price numeric(10,2) NOT NULL CHECK (price >= 0),
    sort_order integer NOT NULL DEFAULT 0 CHECK (sort_order >= 0),
    average_rating numeric(3,2) NOT NULL DEFAULT 0 CHECK (average_rating BETWEEN 0 AND 5),
    total_ratings integer NOT NULL DEFAULT 0 CHECK (total_ratings >= 0),
    is_available boolean NOT NULL DEFAULT true,
    available_quantity integer NOT NULL DEFAULT 0 CHECK (available_quantity >= 0),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by uuid REFERENCES users(id),
    updated_by uuid REFERENCES users(id),
    CONSTRAINT menu_items_category_restaurant_fk FOREIGN KEY (category_id, restaurant_id)
        REFERENCES menu_categories (id, restaurant_id)
);
CREATE INDEX menu_items_listing_idx ON menu_items (restaurant_id, category_id, sort_order);
CREATE INDEX menu_items_category_idx ON menu_items (category_id);
CREATE INDEX menu_items_name_search_idx ON menu_items USING gin (name gin_trgm_ops);

CREATE TRIGGER menu_categories_updated_at
    BEFORE UPDATE ON menu_categories
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER menu_items_updated_at
    BEFORE UPDATE ON menu_items
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
