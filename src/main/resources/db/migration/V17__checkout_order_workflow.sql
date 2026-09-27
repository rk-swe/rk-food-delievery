ALTER TABLE orders DROP CONSTRAINT orders_order_status_check;
ALTER TABLE orders ADD CONSTRAINT orders_order_status_check CHECK (order_status IN ('Placed','Accepted','Rejected','Preparing','Ready for pickup','Out for delivery','Delivered','Cancelled'));
ALTER TABLE orders ADD COLUMN version integer NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN payment_deadline_at timestamptz;
ALTER TABLE orders ADD COLUMN restaurant_response_at timestamptz;
ALTER TABLE orders ADD COLUMN stock_released_at timestamptz;
ALTER TABLE orders ADD COLUMN assignment_status text NOT NULL DEFAULT 'Not started';
ALTER TABLE orders ADD COLUMN assignment_round integer NOT NULL DEFAULT 0;
ALTER TABLE orders ADD CONSTRAINT orders_assignment_status_check CHECK (assignment_status IN ('Not started','Searching','No partners available','Offers unanswered','Assigned','Completed'));
ALTER TABLE payments ADD COLUMN expires_at timestamptz;
ALTER TABLE payments ADD COLUMN currency varchar(3) NOT NULL DEFAULT 'INR';
ALTER TABLE payments ADD COLUMN refunded_at timestamptz;
CREATE TABLE idempotency_records (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id uuid NOT NULL REFERENCES users(id),
    operation varchar(120) NOT NULL,
    idempotency_key varchar(255) NOT NULL,
    request_hash varchar(128) NOT NULL,
    resource_id uuid,
    response_status integer NOT NULL,
    response_body jsonb NOT NULL,
    response_location text,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT idempotency_records_actor_operation_key_unique UNIQUE(actor_id, operation, idempotency_key)
);
CREATE INDEX idempotency_records_resource_idx ON idempotency_records(resource_id);
