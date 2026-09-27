-- Multiple attempts per order are allowed, but only one may succeed.
CREATE TABLE payments (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id uuid NOT NULL REFERENCES orders(id),
    status text NOT NULL DEFAULT 'Pending' CHECK (status IN ('Pending', 'Success', 'Failed')),
    provider text NOT NULL DEFAULT 'Mock' CHECK (provider IN ('Mock')),
    payment_method text NOT NULL CHECK (payment_method IN ('UPI', 'Card', 'Cash on delivery')),
    provider_payment_id text,
    amount numeric(10,2) NOT NULL CHECK (amount >= 0),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT payments_provider_reference_unique UNIQUE (provider, provider_payment_id)
);
CREATE INDEX payments_order_idx ON payments (order_id, created_at DESC);
CREATE UNIQUE INDEX payments_one_success_per_order_unique ON payments (order_id) WHERE status = 'Success';

CREATE TRIGGER payments_updated_at
    BEFORE UPDATE ON payments
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
