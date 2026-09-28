# Database migrations

The schema follows `tables.md` and the food-delivery assignment. PostgreSQL with
PostGIS is required. Flyway runs automatically at application startup using
`DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`; Hibernate validates rather than
creating tables. The database must already exist, and the migration user must
be allowed to install `postgis` and `pg_trgm` (or an administrator must install
them first).

| Version | Domain               | Tables / changes                                              |
| ------- | -------------------- | ------------------------------------------------------------- |
| V1      | Database setup       | PostGIS, trigram search, shared `updated_at` trigger function |
| V2      | Users                | All four roles in one users table                             |
| V3      | Cities               | Cities and currency                                           |
| V4      | Restaurant discovery | Restaurants, weekly timings, cuisines, restaurant cuisines    |
| V5      | Menus                | Categories and items                                          |
| V6      | Coupons              | Codes and discount configuration                              |
| V7      | Ordering             | Orders and order items                                        |
| V8      | Payments             | Payment attempts                                              |
| V9      | Ratings              | Optional order review and delivery-partner rating on orders   |

Related tables share a migration when they form one flow. Each migration has its
own commit and depends only on preceding versions. Once a migration has been
applied to a shared database, change the schema with a new version; do not edit
the applied SQL or repair checksums to hide changes.

## Schema decisions

- UUID primary keys use `gen_random_uuid()`. Audit timestamps use `timestamptz`;
  triggers refresh `updated_at` on updates. Audit user references are nullable
  for bootstrapping or system changes.
- Email uniqueness is case-insensitive per role. Phone numbers use E.164 syntax
  and are unique per role. The same person can have separate rows for different
  roles. This is basic contact validation, not verification of email or phone ownership.
- Cities are unique by case-insensitive name, state, and country, since city
  names repeat. Cuisines are unique by name; menu category names are unique
  within a restaurant. Restaurant names and menu item names need not be unique.
- Each restaurant has at most one opening interval per weekday. Closed days
  have no hours; an end time earlier than the start means overnight opening.
  Equal start/end times are rejected; multiple daily intervals and 24-hour
  openings are outside this initial model.
- A composite foreign key prevents a menu item from referencing another
  restaurant's category. Stock, counters, prices, and monetary amounts cannot
  be negative; ordered quantities must be positive.
- Coupons add `code` and `discount_value`, which are needed to apply the planned
  discounts. Monetary fields use decimals. Percentage discounts cannot exceed
  100; `discount_upto` is an optional cap. Expiry and eligibility rules remain
  out of scope.
- Order tax amounts use `numeric(10,2)` like other amounts; tax percentages use
  `numeric(5,2)` with a 0–100 range. Order totals and line subtotals must match
  their stored components. Applied coupon codes, delivery addresses, and item
  prices are snapshots. There is at most one line per menu item per order.
- Multiple payment attempts are allowed. Provider references cannot be reused
  for the same provider, and only one payment can succeed per order. Provider
  references may be absent for mock or cash payments.
- Ratings stay on orders as planned, with optional 1–5 values. No separate
  review table is needed for one review per order.
- Foreign keys restrict deletion by default, preserving referenced order and
  payment history. Hard deletion of referenced restaurants, menu items, or
  users requires an explicit future retention policy.

## Indexes and application responsibilities

Indexes cover city and owner restaurant listings, nearby search (GiST),
restaurant/menu name search (trigram GIN), cuisine filtering, sorted menus,
customer order history, restaurant and partner status queues, unassigned
orders, and payment attempts. Unique constraints already create indexes;
there are no extra standalone indexes on low-selectivity booleans or statuses.

The service must validate roles on user references, ensure every order item
belongs to the order's restaurant, and check review ownership and delivery
status. It must also maintain rating/category counters and keep payment status
consistent with the successful payment attempt. These cross-row rules are not
implemented by the migrations.

To prevent overselling, reserve stock with a conditional update such as
`UPDATE menu_items SET available_quantity = available_quantity - :quantity
WHERE id = :id AND available_quantity >= :quantity` and check the affected row
count. Keep all item reservations, order creation, and mock payment changes in
one transaction. Assign a partner with a conditional update on an unassigned
order, checking status and affected row count. The nonnegative-stock constraint
is a backstop; it does not replace concurrency control. Publish asynchronous
status notifications only after the transaction commits.

## Verification

Use a dedicated PostgreSQL test database with PostGIS available. The test suite
applies Flyway migrations automatically and rolls back schema-test fixtures:

```sh
DB_URL=jdbc:postgresql://localhost:5432/fooddelivery_test \
DB_USERNAME=your_user DB_PASSWORD=your_password ./mvnw test
```

`SchemaMigrationTests` exercises valid inserts and rejected writes for contact
uniqueness, category ownership, stock, monetary totals, payment retries,
percentage discounts, opening hours, audit updates, and both rating bounds.
