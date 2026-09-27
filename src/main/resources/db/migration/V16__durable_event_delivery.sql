CREATE TABLE outbox_events (
    id uuid PRIMARY KEY,
    event_type varchar(100) NOT NULL,
    aggregate_id uuid NOT NULL,
    aggregate_version integer NOT NULL CHECK (aggregate_version >= 0),
    payload jsonb NOT NULL,
    occurred_at timestamptz NOT NULL,
    published_at timestamptz,
    publish_attempts integer NOT NULL DEFAULT 0 CHECK (publish_attempts >= 0),
    last_error text
);
CREATE INDEX outbox_events_pending_idx ON outbox_events (occurred_at, id) WHERE published_at IS NULL;

CREATE TABLE inbox_entries (
    consumer varchar(100) NOT NULL,
    event_id uuid NOT NULL,
    received_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (consumer, event_id)
);

CREATE TABLE notification_deliveries (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient varchar(100) NOT NULL,
    event_id uuid NOT NULL,
    aggregate_id uuid NOT NULL,
    aggregate_version integer NOT NULL,
    event_type varchar(100) NOT NULL,
    status varchar(30) NOT NULL DEFAULT 'DELIVERED',
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT notification_deliveries_recipient_event_unique UNIQUE (recipient, event_id)
);
