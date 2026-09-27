ALTER TABLE users ADD COLUMN cart_version bigint NOT NULL DEFAULT 0 CHECK (cart_version >= 0);
