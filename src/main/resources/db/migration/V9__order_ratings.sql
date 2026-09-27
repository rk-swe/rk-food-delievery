-- One review per order, as in docs/tables.md. NULL means not rated yet.
-- The service checks customer ownership and Delivered status before accepting ratings.
ALTER TABLE orders
    ADD COLUMN order_rating smallint CHECK (order_rating BETWEEN 1 AND 5),
    ADD COLUMN order_rating_review text,
    ADD COLUMN delivery_partner_rating smallint CHECK (delivery_partner_rating BETWEEN 1 AND 5);
