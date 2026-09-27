# Food delivery assignment design

Status: proposed design for review; implementation has not started.

## Goal and assignment constraints

Build a Spring Boot REST backend that demonstrates correct stock reservation,
concurrent delivery assignment, and asynchronous status fan-out. Include the
user's catalog/search/cart/payment flow and the assignment's delivered-order
ratings/reviews. Deliver a GitHub repository, meaningful incremental commits,
README, development instructions/skills/raw artifacts, and a recording of at
most 10 minutes. The assignment allows 48 hours and excludes UI, deployment,
containerization/CI work, microservices, advanced authentication, and production
observability. RabbitMQ is infrastructure for a single application, not a reason
to introduce microservices.

Keep Java 25 and Spring Boot 4.1.1 from the existing pom unless the initial build
proves a compatibility issue. Use PostgreSQL/PostGIS, Flyway, Spring Security,
Bean Validation, Spring AMQP and RabbitMQ. Add new migrations after V11; never
rewrite applied migrations. Existing schema tests remain part of verification.

## Architecture and alternatives

Recommended: one Spring Boot JVM, one PostgreSQL/PostGIS database, and one local
RabbitMQ broker. HTTP controllers and background listeners are components of
the same application. Spring manages listener threads. A separate worker service
or a process for every queue is unnecessary.

Alternatives considered:

- Spring application events plus a bounded executor: simplest for best-effort
  logs, but process failure can lose notifications. `AFTER_COMMIT` fixes publishing
  before rollback, not the crash gap between commit and delivery.
- Database outbox plus a scheduled local dispatcher: reliable and avoids a broker;
  a valid assignment solution if setup time is tight, but requires implementing
  dispatch, retries, and fan-out yourself.
- RabbitMQ plus a small outbox: recommended for this user's requested queue flow,
  explicit independent subscribers, and easy pause/recovery demonstration. Kafka
  adds no required capability here. Do not build a distributed workflow engine.

Do correctness work before polishing CRUD breadth. No JWT or OAuth is necessary:
HTTP Basic with hashed credentials is sufficient for a local assignment demo.
Document that credentials need TLS outside local development. Because email is
currently unique per role, add a globally unique login username rather than
assuming email alone is an unambiguous principal.

## Domain boundaries and invariants

- Controllers validate request DTOs and authentication; services enforce ownership,
  transactions and transitions; repositories own persistence queries. Never expose
  entities as API payloads or accept caller-supplied role/customer identity.
- All money uses BigDecimal and a documented two-decimal rounding rule. Prices,
  fees, item names and delivery address/location are order snapshots. Recalculate
  totals on the server. Initial demo has INR, no coupons, fixed delivery fee 30.00,
  platform fee 0.00 and tax 0.00; these are explicit demo assumptions.
- Cart operations do not reserve stock. A customer has one restaurant per cart.
  Adding an item from another restaurant clears and replaces the cart atomically.
  Lock the customer row before all cart mutations and checkout so first-cart
  creation, replacement and checkout are serialized even when no cart exists.
  Add a persistent cart version; increment it on every mutation and reject a
  stale checkout version with 409. Repeated idempotency keys are checked first.
- An order references one restaurant. Every line belongs to that restaurant.
- Stock never goes below zero. Failed checkout changes nothing. Canceled or rejected
  reservations are released once, and only once.
- One order has at most one partner; one partner has at most one active order.
- State updates require the correct actor and expected current state.
- Every successful business transition and its event are committed together.
  Rolled-back transitions have no committed events.
- Delivery is at least once; database effects are idempotent. Exactly-once console
  logging is not promised because stdout and database commits cannot be atomic.

## Order/payment/assignment state model

Keep order progress, payment, restaurant response and delivery assignment separate. Partner assignment must not overwrite cooking
status. Add an order transition version for event identification and tracking.

Order statuses: `Awaiting payment`, `Placed`, `Accepted`, `Preparing`,
`Ready for pickup`, `Out for delivery`, `Delivered`, `Rejected`, `Cancelled`.

Payment attempt statuses: `Pending`, `Success`, `Failed`. Store refund status
separately (`None`, `Pending`, `Success`) so a successful charge remains an
accurate historical record after a refund. Order payment summary also retains
`Pending`, `Success`, `Failed`; return refund status alongside it.

Restaurant response statuses: `Not requested`, `Awaiting response`, `Delayed`,
`Accepted`, `Rejected`. Payment success sets Awaiting response; after a configurable
5 minutes without a decision, set Delayed and notify the customer once. Keep the
order Placed and allow the owner to accept/reject later. A late owner decision
and the delay job lock the same order so Delayed cannot overwrite a decision.

Assignment statuses: `Not requested`, `Searching`, `No partners available`,
`Offers unanswered`, `Assigned`, `Completed`. No partners available means no
eligible partner was found; Offers unanswered means offers were issued but all
expired without acceptance. Store offers separately with expiry and unique
(order, partner). Expose these statuses in the order tracking response.

### Complete interaction after checkout

1. Customer calls `POST /api/orders` with an `Idempotency-Key`, cart version and
   delivery address/location. In one short database transaction: lock customer
   and cart, revalidate catalog, reserve every item in sorted UUID order, create
   the order and immutable lines, create a Pending mock payment attempt, record
   `OrderCreated` in the outbox, and clear the cart. Return 201 with order and
   payment IDs. Start a configurable 10-minute payment deadline.
2. Customer uses the mock-payment endpoint to submit Success or Failed for the
   pending attempt. The mock provider calls the same signed webhook handler a
   real provider would call, outside the checkout transaction. A direct signed
   webhook request is also available in the demo collection.
3. The webhook validates HMAC, provider event ID, payment reference, currency,
   amount and allowed transition. Lock order then payment. Success changes
   payment to Success, order to Placed and restaurant response to Awaiting response,
   increments version and inserts
   `PaymentSucceeded` in one transaction. Customer and restaurant get async logs.
   Replaying the same valid event returns 200 without repeating effects.
4. Failed payment cancels the order and releases stock once in that transaction;
   customer receives the failure update. Keep retries simple: a failed/canceled
   order is terminal; the customer creates a fresh cart/order. Do not silently
   reuse a released reservation. An expiry job cancels unpaid orders similarly.
   Expiry and webhook use the same order lock. A late Success after cancellation
   records the charge and requests a mock refund; it never resurrects the order.
   Conflicting terminal provider events are rejected and logged, not applied.
5. Owner sees only their restaurant's paid Placed orders. Accept changes Placed
   to Accepted and writes `OrderAccepted`. Reject changes Placed to Rejected,
   restores reserved stock once and creates a mock-refund request atomically.
   Send rejection and eventual refund-result updates to the customer.
6. `OrderAccepted` asynchronously starts assignment discovery. Owner explicitly
   calls start-preparation to move Accepted to Preparing; customer gets that
   update. These are distinct transitions matching the assignment's lifecycle.
7. Discovery finds active, online, unoccupied partners whose locations are fresh
   (within 5 minutes) and within 5,000 meters of the restaurant. It persists
   60-second offers and `DeliveryOfferCreated` events. Each eligible partner gets
   a log containing order ID/offer ID/expiry and can list offers through an API.
8. A partner calls the offer-acceptance endpoint. In one transaction lock order,
   then partner; recheck live offer, expiry, location eligibility, payment and
   order state, and partner availability. Claim the unassigned order and partner
   slot together, invalidate competing offers, write `DeliveryPartnerAssigned`.
   First committed valid claim wins; losing claims get 409. Customer, owner and
   winning partner receive logs. Repeating the winning claim is idempotent.
9. Owner marks Preparing to Ready for pickup. The assigned partner can mark
   pickup only from Ready for pickup, producing Out for delivery. Assignment
   may happen before or after readiness. Pickup notifies the customer and owner.
10. Only the assigned partner marks Out for delivery to Delivered. Release the
    partner's active slot atomically, finish assignment and emit OrderDelivered.
11. Customer can submit one order rating/review and optional partner rating after
    delivery. Update the restaurant rating aggregate atomically; duplicate reviews
    get 409. Item rating submission is deferred; existing item rating fields can
    remain zero, and menu rating sorting still works with a stable tiebreaker.

If discovery finds nobody eligible, persist No partners available immediately.
If issued offers expire unclaimed after 60 seconds, persist Offers unanswered.
Notify customer and restaurant once per actual status change. Do not automatically
cancel/refund the order or restore stock because of these waiting conditions.
Cooking status remains Accepted/Preparing/Ready as appropriate.

Keep recovery explicit and small: owner/admin may call
`POST /orders/{id}/retry-assignment` with an Idempotency-Key once no live offers
remain. Under the order lock, move a waiting assignment to Searching, increment
its round, and append DeliveryAssignmentRequested for the assignment listener.
Re-evaluate current partner locations/availability and update the existing
(order, partner) offer row with a new round and expiry as needed. The acceptance
request includes the offer round so an old notification cannot accept a renewed
offer. Same-key retry creates no extra round/events. Accept and offer expiry lock
the same order; one wins and the other rechecks state. No unlimited timed retries.

The discovery listener rechecks that the order is still paid,
accepted/preparing/ready and unassigned before creating offers; delayed or
redelivered events cannot reopen completed/canceled work. Waiting states remain
visible until the owner responds or assignment is retried successfully. This
assignment scope intentionally has no automatic terminal resolution for prolonged
waiting; document that held inventory/payment and prepared food remain unresolved
until action is taken. Payment expiry before successful payment remains separate.

Delivery matching is based on partner distance to restaurant pickup, never the
customer's destination or partner rating. Sort eligible candidates by distance
for inspection. The user confirmed the broadcast policy: notify nearby eligible
partners and let the first valid acceptance win. This does not guarantee the
nearest partner wins; do not implement nearest-first sequential offers.

No general customer cancellation, reassignment after pickup, cash on delivery,
real money movement, coupons, or delivery ETA optimization in the initial scope.
Mock refunds are a small addition to the earlier schema notes that excluded
refunds: otherwise rejecting a paid order has an unexplained money outcome.

### Meaning of atomic payment

An asynchronous payment success cannot belong to a database transaction that
finished at checkout. Here the atomic checkout invariant is: stock reservation,
order/lines and the Pending payment record all exist together or none exist.
Webhook success atomically updates payment, order and event. Failure/expiry
atomically updates payment/order and releases stock. No database transaction is
held open awaiting a provider or a user action.

Document this interpretation prominently because the assignment says placement
must atomically reflect payment. If final successful payment at placement is
required instead, use a synchronous in-process mock decision inside checkout;
that is a different contract from the requested asynchronous webhook flow.

## The three correctness mechanisms

### Atomic checkout

Use `@Transactional` on a public service method called through Spring's proxy.
For each item, in deterministic ID order, issue a parameterized conditional update:

```sql
UPDATE menu_items
SET available_quantity = available_quantity - :quantity
WHERE id = :id AND restaurant_id = :restaurantId
  AND is_available = true AND available_quantity >= :quantity;
```

Require affected rows = 1; otherwise throw a rollback-triggering domain exception.
Keep stock, order/lines, pending payment, cart clearing and outbox insertion in
the same transaction. Read price snapshots while the affected item rows remain
locked. Admin stock adjustments must use the same database concurrency discipline.
An order-level `stock_released_at` guard prevents double release. Database
nonnegative checks remain a backstop. Never rely on Java `synchronized`.

For checkout and action POSTs (mock payment completion, owner accept/reject,
start-preparation/ready, offer acceptance, retry-assignment, pickup/deliver and
review), require Idempotency-Key. Store (actor, operation, key, request hash,
resource ID, original response) with a unique key, in the same transaction as the
successful mutation. Same key/same payload replays the original status/body;
changed payload returns 409. Concurrent duplicates serialize or resolve against
the unique record without duplicating effects. Consult it before revalidating
current state or requiring a nonempty cart. Keep records for the assignment
lifetime. A failed, rolled-back attempt has no successful stored response and may
be retried. PUT/DELETE retain their natural idempotent semantics.

Provider webhooks deduplicate by provider event ID rather than a customer's key;
queue consumers use (consumer, event ID). Refunds additionally have a unique
order/reason business key, so new request keys cannot refund the same charge twice.
Idempotency is not a replacement for stock checks, locks or legal state transitions.

### Assignment contention

Lock order then partner consistently. Add a partial unique index on
`orders(delivery_partner_id)` for active assigned states (Accepted, Preparing,
Ready for pickup, Out for delivery) as a second database defense. Claim with a
conditional update requiring `delivery_partner_id IS NULL` and eligible status.
One transaction includes partner availability, offer outcomes and outbox event.
Rollback a losing claim fully; translate expected contention to 409, not 500.
Two partners/one order and one partner/two orders are both required tests.

### Durable asynchronous fan-out

Each transactional service inserts `outbox_events` with event ID, type, order ID,
aggregate version, timestamp, schema version and JSON payload. A scheduled
publisher, independent of request threads, publishes pending rows to a durable
topic exchange `fooddelivery.events`. Use persistent messages, publisher confirms
and mandatory returns; mark published only after confirmation and no routing
failure. Broker outages leave rows pending for retry. Keep publisher work bounded;
no HTTP request waits for a broker or notification.

Queues and in-process listeners:

| Queue | Responsibility |
| --- | --- |
| `notifications.customer` | Customer-facing logs for relevant transitions |
| `notifications.restaurant` | Owner-facing logs for relevant transitions |
| `notifications.partner` | Offer and assigned-partner logs |
| `delivery.assignment` | Create offers on OrderAccepted |
| `payments.refund` | Complete idempotent mock refunds |

Use topic bindings to choose events each queue receives. Multiple consumers on
ONE queue divide work; separate queues are what make the same event reach
multiple subscriber types. Use one shared dead-letter queue with original queue
metadata and bounded retries (three attempts with backoff). A slow subscriber
cannot occupy another queue's listener thread. Do not create a queue per user.

ACK only after processing succeeds and its database transaction commits.
Deduplicate by unique (consumer name, event ID), with transactional effects in the
same transaction. Publisher crashes after broker acceptance can cause duplicates;
this is expected. For log-only sinks a crash can still duplicate a log; include
event ID and order version. Do not claim global ordering across queues. Tracking
GET reads authoritative database state; consumers must not regress state from
stale events. Notifications include version and status.

## API scope and validation

All paths below have `/api` prefix. Own-resource checks apply as well as roles.

| Actor | Endpoints / behavior |
| --- | --- |
| Any authenticated user | `GET /me`, `GET /cities`, `GET /cuisines` |
| Admin | POST/GET/PATCH/DELETE `/admin/cities`, `/admin/restaurants`, `/admin/delivery-partners` and ID variants |
| Owner | GET own restaurants; POST/GET/PATCH/DELETE `/restaurants/{id}/categories` and `/restaurants/{id}/menu-items` and ID variants; manage own opening hours |
| Customer | `GET /restaurants`, `GET /restaurants/{id}`, `GET /restaurants/{id}/menu-items` |
| Customer | GET/DELETE `/cart`; PUT `/cart/items/{itemId}` sets absolute quantity; DELETE item; PUT across restaurants atomically replaces old items |
| Customer | POST `/orders`; GET `/orders`, `/orders/{id}`; POST `/orders/{id}/review` |
| Owner | GET `/owner/orders`; POST `/orders/{id}/accept`, `/reject`, `/start-preparation`, `/ready` |
| Partner | PATCH `/delivery-partners/me/location`, `/availability`; GET `/delivery-offers`; POST `/delivery-offers/{id}/accept`; POST `/orders/{id}/pickup`, `/deliver` |
| Mock provider | POST `/payments/webhook`, authenticated with a dedicated HMAC secret, not customer credentials |
| Customer, demo profile only | POST `/mock/payments/{paymentId}/complete` for own payment, invokes mock provider/webhook contract |

List endpoints use page >= 0, size 1..100 (default 20), allowlisted sort fields
and deterministic ID tiebreakers. Validate nonblank/bounded names and reviews,
UUIDs, positive integer quantities, nonnegative money/stock, enums, ranges,
longitude -180..180 and latitude -90..90. Review stars 1..5, review <= 2,000 chars.
Ignore no unauthorized fields: use explicit DTOs without role, owner or totals
where the server should derive them. Audit actor IDs come from authentication.
Use a consistent ProblemDetail-style response: status, code, message, field errors,
request ID. 400 invalid input, 401 unauthenticated, 403 forbidden, 404 inaccessible
or missing resource, 409 stock/state/idempotency conflicts. Do not leak SQL errors.

DELETE deactivates referenced restaurants/items/partners/cities, preserving
history. Reject deactivating busy partners or cities with active restaurants;
deactivating a restaurant prevents new checkouts but existing orders can finish.
Category deletion requires no active items. Menu stock editing must not overwrite
concurrent checkout: use an explicit stock-delta operation or version precondition.

## Search semantics and seed data

Restaurant search: name substring, city, cuisine IDs (match any selected cuisine),
restaurant diet type, min/max cost for two, radius; sort distance ASC, rating DESC,
cost ASC/DESC or name. Diet type Non Veg means mixed/nonvegetarian restaurant,
not that every item is nonvegetarian. Distance requires explicit coordinates or
stored customer location; missing location is 400 when requested. Filter inactive
restaurants. Open-now filtering is optional; checkout enforces configured hours,
using Asia/Kolkata for the INR demo and covering overnight intervals.

Use geography(Point,4326), existing GiST indexes, `ST_DWithin` for meter-based
radius filtering, and `ST_Distance` for exact geography distance ordering of
filtered candidates. Construct points as longitude, latitude, not the reverse.
Do not compute all distances in Java. Small fixture tables can legitimately use
sequential scans; validate the index strategy with EXPLAIN on a larger fixture.

Menu search is scoped to one restaurant: name, category, diet, availability,
min/max item price; sort price, rating or display order. Distance and cost for two
are not meaningful item filters. Cuisine is currently attached to restaurants;
do not pretend there is per-item cuisine data. Adding item cuisine is optional
later and requires an explicit relationship/migration.

Demo seed: exactly 1 admin, 1 owner, 2 customers, 2 delivery partners; 1 city,
2 restaurants under the same demo owner, 2 categories and 4 menu items per
restaurant (8 items total), and 3–5 cuisine lookup names sourced from Swiggy.
Use deterministic IDs and an idempotent demo-only seeder with hashed passwords.
Do not put demo credentials/data into production migrations. Lookup cuisines can
use a separate seed migration. Use synthetic users, never real customer data.

Interpret the user's Swiggy request as catalog reference data. During the seed
task, record the public restaurant URL/access date and verified item/category
names; use clearly labeled demo prices/quantities, original short descriptions,
and no copied images. Suggested references: Paradise Biryani, Begumpet, and Chutneys, SP Road, Hyderabad.
Swiggy's food browse tiles mix dishes and cuisines; Biryani/Pizza are better
categories/tags than treating every tile as a cuisine. Candidate cuisine lookup:
North Indian, South Indian, Chinese, Italian, Hyderabadi; verify reference labels
when implementing. Do not claim an exact current Swiggy catalog was collected.

Both restaurants are seeded by default, with contrasting diet/cuisine, demo cost,
rating and location values to demonstrate sorting/filtering and cart replacement.
Mark seeded rating aggregates as demo fixtures, not actual restaurant ratings.
Fetch only the two public pages and minimal lookup labels required for this data;
no full catalog crawl, reviews, images or customer records. Use test-only extra
owners/users for authorization and load tests; retain six default accounts.

## Existing repository gaps

Only application bootstrap, V1–V11 migrations and schema tests currently exist.
README.md and AGENTS.md are empty. No functional API/auth/event code exists yet.
The AMQP/security/validation/JPA dependencies are already declared. Schema needs
credentials/activity, order states/version/deadlines, item-name snapshots,
idempotency, partner presence/offers, refund tracking and outbox/inbox tables.
Update docs/database.md: its current wording about payment in one transaction
and publishing after commit is too broad for asynchronous provider results and
does not cover durable event publication. Update docs/tables.md to match actual
migrations and new assumptions.

## Technical references

- [RabbitMQ Spring AMQP tutorial](https://www.rabbitmq.com/tutorials/tutorial-one-spring-amqp)
- [Publisher confirms and consumer acknowledgements](https://www.rabbitmq.com/docs/confirms)
- [RabbitMQ duplicate delivery and reliability](https://www.rabbitmq.com/docs/reliability)
- [PostGIS index-aware radius queries](https://postgis.net/documentation/tips/st-dwithin/)
- [PostGIS geography distance units](https://www.postgis.net/docs/manual-3.5/ST_DWithin.html)
- [Swiggy restaurant reference](https://www.swiggy.com/city/hyderabad/paradise-biryani-mayur-marg-begumpet-rest7411)

- [Swiggy second restaurant reference](https://www.swiggy.com/city/hyderabad/chutneys-sp-road-secunderabad-rest11160)
