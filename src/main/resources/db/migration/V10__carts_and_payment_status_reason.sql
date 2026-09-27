ALTER TABLE payments ADD COLUMN status_reason text;

-- Composite keys ensure every cart item belongs to its cart's restaurant.
ALTER TABLE menu_items
    ADD CONSTRAINT menu_items_id_restaurant_unique UNIQUE (id, restaurant_id);

CREATE TABLE carts (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id uuid NOT NULL REFERENCES users(id),
    restaurant_id uuid NOT NULL REFERENCES restaurants(id),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT carts_customer_unique UNIQUE (customer_id),
    CONSTRAINT carts_id_restaurant_unique UNIQUE (id, restaurant_id)
);
CREATE INDEX carts_restaurant_idx ON carts (restaurant_id);

CREATE TABLE cart_items (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    cart_id uuid NOT NULL,
    restaurant_id uuid NOT NULL,
    menu_item_id uuid NOT NULL,
    quantity integer NOT NULL CHECK (quantity > 0),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT cart_items_cart_menu_unique UNIQUE (cart_id, menu_item_id),
    CONSTRAINT cart_items_cart_restaurant_fk FOREIGN KEY (cart_id, restaurant_id)
        REFERENCES carts (id, restaurant_id) ON DELETE CASCADE,
    CONSTRAINT cart_items_menu_restaurant_fk FOREIGN KEY (menu_item_id, restaurant_id)
        REFERENCES menu_items (id, restaurant_id)
);
CREATE INDEX cart_items_menu_item_idx ON cart_items (menu_item_id);

CREATE TRIGGER cart_items_updated_at
    BEFORE UPDATE ON cart_items
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
