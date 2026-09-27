CREATE TABLE coupons (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code text NOT NULL,
    name text NOT NULL,
    description text,
    min_spend numeric(10,2) NOT NULL DEFAULT 0 CHECK (min_spend >= 0),
    discount_upto numeric(10,2) CHECK (discount_upto >= 0),
    type text NOT NULL CHECK (type IN ('flat', 'percentage')),
    discount_value numeric(10,2) NOT NULL CHECK (discount_value > 0),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by uuid REFERENCES users(id),
    updated_by uuid REFERENCES users(id),
    CONSTRAINT coupons_percentage_check CHECK (type <> 'percentage' OR discount_value <= 100)
);
CREATE UNIQUE INDEX coupons_code_unique ON coupons (upper(code));

CREATE TRIGGER coupons_updated_at
    BEFORE UPDATE ON coupons
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
