CREATE TABLE orders (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id uuid NOT NULL REFERENCES users(id),
    restaurant_id uuid NOT NULL REFERENCES restaurants(id),
    order_status text NOT NULL DEFAULT 'Placed'
        CHECK (order_status IN ('Placed', 'Accepted', 'Rejected', 'Preparing', 'Out for delivery', 'Delivered')),
    payment_status text NOT NULL DEFAULT 'Pending' CHECK (payment_status IN ('Pending', 'Success', 'Failed')),
    delivery_partner_id uuid REFERENCES users(id),
    sub_total_amount numeric(10,2) NOT NULL CHECK (sub_total_amount >= 0),
    delivery_fee numeric(10,2) NOT NULL DEFAULT 0 CHECK (delivery_fee >= 0),
    platform_fee numeric(10,2) NOT NULL DEFAULT 0 CHECK (platform_fee >= 0),
    tax_percent numeric(5,2) NOT NULL DEFAULT 0 CHECK (tax_percent BETWEEN 0 AND 100),
    tax_amount numeric(10,2) NOT NULL DEFAULT 0 CHECK (tax_amount >= 0),
    -- Snapshot: retain the applied code even if the coupon later changes.
    coupon_code text,
    discount_amount numeric(10,2) NOT NULL DEFAULT 0 CHECK (discount_amount >= 0),
    total_amount numeric(10,2) NOT NULL CHECK (total_amount >= 0),
    address_line_1 text NOT NULL,
    address_line_2 text,
    city text NOT NULL,
    state text NOT NULL,
    country text NOT NULL,
    location geography(Point, 4326) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT orders_total_check CHECK (
        total_amount = sub_total_amount + delivery_fee + platform_fee + tax_amount - discount_amount
    )
);
CREATE INDEX orders_customer_history_idx ON orders (customer_id, created_at DESC);
CREATE INDEX orders_restaurant_status_idx ON orders (restaurant_id, order_status, created_at DESC);
CREATE INDEX orders_partner_status_idx ON orders (delivery_partner_id, order_status, created_at DESC)
    WHERE delivery_partner_id IS NOT NULL;
CREATE INDEX orders_unassigned_idx ON orders (created_at)
    WHERE delivery_partner_id IS NULL AND order_status IN ('Accepted', 'Preparing');

CREATE TABLE order_items (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id uuid NOT NULL REFERENCES orders(id),
    menu_item_id uuid NOT NULL REFERENCES menu_items(id),
    quantity integer NOT NULL CHECK (quantity > 0),
    unit_price numeric(10,2) NOT NULL CHECK (unit_price >= 0),
    sub_total numeric(10,2) NOT NULL CHECK (sub_total = quantity * unit_price),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT order_items_order_menu_unique UNIQUE (order_id, menu_item_id)
);
CREATE INDEX order_items_menu_item_idx ON order_items (menu_item_id);

CREATE TRIGGER orders_updated_at
    BEFORE UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
