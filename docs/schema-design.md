# Database schema design

![Food Delivery database entity-relationship diagram](images/food-delivery-schema.png)

This diagram is a compact view of the PostgreSQL/PostGIS schema used by the
application. Arrows represent declared foreign-key relationships; a crow's-foot
arrowhead indicates the many side. It intentionally omits audit references
(`created_by` and `updated_by`) and non-relational event identifiers so the core
catalog, cart, ordering, payment, delivery and reliability paths remain legible.

The Flyway migrations in
[`src/main/resources/db/migration`](../src/main/resources/db/migration) are the
authoritative schema definition. The editable DBML source for the image is
[`diagrams/food-delivery-schema.dbml`](diagrams/food-delivery-schema.dbml);
regenerate the PNG after a schema change with:

```sh
npx --yes @softwaretechnik/dbml-renderer \
  --input docs/diagrams/food-delivery-schema.dbml \
  --format svg --output docs/images/food-delivery-schema.svg
sips -s format png docs/images/food-delivery-schema.svg \
  --out docs/images/food-delivery-schema.png
```

Key implementation details that are important but abbreviated in the visual:

- `menu_items` and `cart_items` use composite foreign keys with `restaurant_id`
  to keep a cart and its items within one restaurant.
- `orders.delivery_partner_id` is nullable; `delivery_offers` records the
  candidate/acceptance workflow separately.
- `outbox_events`, `inbox_entries`, and `notification_deliveries` model durable,
  at-least-once event delivery. Their IDs are application-level event references,
  rather than database foreign keys.
