# Food Delivery Assignment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. This is a proposed plan, not authorization to implement it now.

**Goal:** Complete the assignment with demonstrable atomic checkout, safe delivery assignment, durable asynchronous notifications, and the requested REST flows.

**Architecture:** One Spring Boot application with PostgreSQL/PostGIS and a local RabbitMQ broker. Database transactions protect business state; a small transactional outbox connects committed changes to independent in-process queue listeners.

**Tech Stack:** Existing Java 25, Spring Boot 4.1.1, Maven wrapper, Flyway, JPA/JdbcTemplate, Spring Security/Validation/AMQP, PostgreSQL/PostGIS, RabbitMQ, JUnit and Awaitility for bounded eventual assertions.

**Spec:** [Assignment design](../specs/2026-09-28-assignment-design.md). Read its state machine and assumptions before executing any task.

## Global constraints

- Keep Java 25 and Spring Boot 4.1.1 from the existing pom unless the initial build proves a compatibility issue.
- Add new migrations after V11; never rewrite applied migrations.
- One Spring Boot JVM; no separate worker services, frontend, deployment or CI work.
- All money uses BigDecimal and a documented two-decimal rounding rule.
- Delivery is at least once; database effects are idempotent.
- The recording must be at most 10 minutes.
- Each numbered task ends in its own commit after its relevant checks pass.
- Preserve existing schema tests. Tests use dedicated real PostgreSQL/PostGIS and RabbitMQ instances; do not use H2 to prove locking/spatial behavior.

## Review focus

- Duplicate checkout after the cart was cleared returns the original order (task 7).
- Concurrent requests for one partner and different orders cannot both succeed (task 10).
- Payment expiry racing success cannot resurrect an order or leak inventory (task 8).
- Broker failure immediately after database commit cannot lose an update (tasks 6, 13).
- Cross-restaurant cart updates and ownership mistakes cannot mix data or bypass roles (tasks 2, 5).

## Folder and interface map

Use package-by-feature with conventional layers inside each feature:

```text
src/main/java/com/rk/fooddelivery/
  config/                  SecurityConfig, RabbitConfig, ClockConfig
  common/error/            ApiExceptionHandler, DomainException
  common/web/              PageResponse
  auth/                    CurrentUser, UserDetailsService adapter
  user/{entity,repository,service,dto}/
  city/{controller,service,repository,entity,dto}/
  restaurant/{controller,service,repository,entity,dto}/
  menu/{controller,service,repository,entity,dto}/
  cart/{controller,service,repository,entity,dto}/
  order/{controller,service,repository,entity,dto}/
  payment/{controller,service,repository,entity,dto}/
  delivery/{controller,service,repository,entity,dto}/
  review/{controller,service,repository,dto}/
  event/{outbox,inbox,listener,dto}/
  notification/            Customer/Restaurant/PartnerNotificationListener
  seed/                    DemoDataSeeder
src/main/resources/db/migration/  V12 onward, one migration per schema change
src/test/java/com/rk/fooddelivery/ feature tests and integration support
scripts/demo/              concurrent request scripts and SQL assertions
docs/demo/                 API collection, video script, reference data sources
```

Use repositories with native SQL/JdbcTemplate for spatial and guarded updates;
do not force awkward spatial/locking logic into derived JPA method names. Keep
entities, HTTP DTOs and events distinct. Paths below are relative to the above
Java package unless given in full. Each named service has one owning task.

Shared contracts: `CurrentUser.requireId(): UUID`, `CurrentUser.requireRole(Role)`;
`OrderResponse` includes order ID, state, payment/refund state, restaurant response state, assignment state,
partner ID, version, item snapshots and totals; `DomainEvent` includes UUID ID,
String type, UUID orderId, long version, Instant occurredAt, int schemaVersion,
and typed payload. `OutboxService.append(DomainEvent): void` joins its caller's
transaction. Use injected `Clock` for deadlines and tests.

## Commit-sized execution steps

For every task: write the named behavior tests, run them and observe the expected
failure, implement the listed change, rerun the tests, then commit only that task's
files. No knowingly broken application commits. File lists identify primary
classes; add their DTO/entity/repository counterparts in the mapped package.

### 1. Establish repeatable local verification and API conventions

Files: `pom.xml`, `src/test/.../support/IntegrationTestSupport.java`,
`common/error/ApiExceptionHandler.java`, `common/error/DomainException.java`,
`config/ClockConfig.java`, `README.md`, `AGENTS.md`.

- [ ] Run `./mvnw test` against the existing dedicated test DB and record baseline; check dependency/runtime compatibility rather than upgrading speculatively.
- [ ] Add integration-test configuration for a separate PostGIS database and broker vhost, bounded database connection pool, injected Clock, and reusable committed fixture/cleanup helpers.
- [ ] Add `ApiErrorContractTest`: invalid request => 400 with field errors; domain conflict => 409 without SQL details. Implement consistent errors and API conventions.
- [ ] Run `./mvnw -Dtest=SchemaMigrationTests,ApiErrorContractTest test`; expected zero failures. Document exact local prerequisite/start/test commands and the planning skills used in AGENTS.md.
- [ ] Commit `chore: establish local verification and API conventions`.

### 2. Authentication and role/ownership boundaries

Files: new credentials/activity migration; `config/SecurityConfig.java`,
`auth/CurrentUser.java`, `auth/DatabaseUserDetailsService.java`,
`user/controller/MeController.java`; `SecurityIntegrationTest.java`.
Produces `CurrentUser` contract and authenticated `/api/me`.

- [ ] Test missing credentials => 401, wrong role => 403, inactive account denied, hash verification, two roles sharing an email with distinct usernames.
- [ ] Add unique username/password_hash and active flag; implement HTTP Basic and BCrypt; explicitly scope CSRF/session policy to the stateless API. Configure webhook's separate signature authentication in task 8.
- [ ] Test user identity cannot be overridden by a request body; later owned-resource tests use the same principal accessor.
- [ ] Run `./mvnw -Dtest=SecurityIntegrationTest,SchemaMigrationTests test`; expected zero failures.
- [ ] Commit `feat: add basic authentication and role authorization`.

### 3. Admin CRUD and partner presence

Files: city/restaurant/user/delivery feature classes, new activity/presence
migration; `AdminCrudIntegrationTest.java`.
Produces city/restaurant/partner create/list/get/patch/deactivate APIs and
`PartnerPresenceService.updateLocation(UUID, LocationRequest): void`.

- [ ] Test city uniqueness, invalid coordinates, owner role validation, forbidden customer access, referenced-history preservation and busy-partner deactivation rejection.
- [ ] Implement DTO validation, admin CRUD, owner restaurant listing, cuisines lookup, opening hours, partner self location/online updates and location timestamp.
- [ ] Add version-aware stock/catalog mutation conventions for the following task; define deactivation behavior from the spec.
- [ ] Run `./mvnw -Dtest=AdminCrudIntegrationTest,SecurityIntegrationTest test`; expected zero failures.
- [ ] Commit `feat: manage cities restaurants and delivery partners`.

### 4. Menu/category management and discovery

Files: menu feature classes, `restaurant/repository/RestaurantSearchRepository.java`,
`restaurant/service/RestaurantSearchService.java`, `MenuSearchIntegrationTest.java`,
`RestaurantSearchIntegrationTest.java`, `MenuManagementIntegrationTest.java`.
Produces `RestaurantSearchService.search(RestaurantSearchRequest): PageResponse<RestaurantSummary>`
and `MenuService.search(UUID, MenuSearchRequest): PageResponse<MenuItemResponse>`.

- [ ] Test owner isolation, cross-restaurant category rejection, safe concurrent stock edits, availability/deactivation, positive quantities and category counts.
- [ ] Implement category/item CRUD and stock delta endpoint; preserve FK history and update category counts transactionally on moves/deactivation.
- [ ] Test combined name/cuisine/diet/cost filters, pagination tiebreakers, missing coordinates, known near/far points, radius boundary and item price/category filtering.
- [ ] Implement SQL PostGIS filtering/sorting and parameterized text search. Test that cuisine joins do not duplicate restaurant results. Item cuisine/distance filters are intentionally absent.
- [ ] Run `./mvnw -Dtest=MenuManagementIntegrationTest,RestaurantSearchIntegrationTest,MenuSearchIntegrationTest test`; expected zero failures.
- [ ] Commit `feat: manage menus and search restaurant catalogs`.

### 5. Single-restaurant cart

Files: cart feature classes, new cart-version migration; `CartIntegrationTest.java`.
Produces `CartService.setItem(UUID customerId, UUID itemId, int quantity): CartResponse`,
`clear(UUID): void`, `removeItem(UUID, UUID): CartResponse`, `get(UUID): CartResponse`.

- [ ] Test add/update/remove/clear, cross-restaurant replacement, concurrent first-cart creation, replacement racing another add, unavailable items and another customer's cart denial.
- [ ] Implement customer-row locking consistently; return cart version and `restaurantChanged` so replacement is visible to the caller. Do not decrement stock.
- [ ] Persist/increment cart version for each mutation; test stale checkout version in task 7.
- [ ] Run `./mvnw -Dtest=CartIntegrationTest test`; expected zero failures and no mixed-restaurant cart.
- [ ] Commit `feat: add transactional single restaurant carts`.

### 6. Durable events and independent notification listeners

Files: new outbox/inbox migration, `config/RabbitConfig.java`,
`event/outbox/OutboxService.java`, `OutboxPublisher.java`,
`event/inbox/InboxService.java`, `event/dto/DomainEvent.java`, notification listeners;
`EventDeliveryIntegrationTest.java`.
Produces outbox append contract, topic exchange, three notification queues,
assignment/refund queues, bounded retry/dead-letter behavior and structured logs.
The assignment queue handles OrderAccepted and DeliveryAssignmentRequested.

- [ ] Test rollback leaves no event, committed event reaches each intended queue, duplicate delivery has one committed consumer effect, a failing subscriber does not block another.
- [ ] Implement bounded outbox polling on its own scheduler, durable routing, JSON events, persistent messages, confirms/returns and retry. Keep scheduling separate from expiry jobs.
- [ ] Implement ACK-after-commit, consumer-scoped deduplication, dead-letter metadata and log fields (event ID, order ID/version, recipient, status). Restrict event deserialization to defined types.
- [ ] Run `./mvnw -Dtest=EventDeliveryIntegrationTest test` with real broker; expected zero failures, eventual assertions use deadlines rather than arbitrary sleeps.
- [ ] Commit `feat: publish durable order events and fan out notifications`.

### 7. Atomic checkout, idempotency and stock contention

Files: new order state/snapshot/idempotency migration;
`common/idempotency/IdempotencyService.java`; order feature classes,
`order/service/CheckoutService.java`, `menu/repository/StockRepository.java`,
`payment/repository/PaymentRepository.java`; `CheckoutConcurrencyTest.java`.
Consumes cart locking and outbox. Produces
`CheckoutService.place(UUID customerId, String idempotencyKey, CheckoutRequest): OrderResponse`
and `StockReservationService.releaseOnce(UUID orderId): void` within a caller transaction.

- [ ] Test rollback after one of several item decrements and after order insert: stock/cart unchanged; zero order/payment/event artifacts.
- [ ] Test 20 simultaneous distinct-customer checkouts against stock 5: exactly 5 successes, 15 stock conflicts, final stock 0, five complete order/payment groups. Use separate connections and a start barrier.
- [ ] Implement short transaction, sorted guarded updates, immutable server-priced snapshots, Pending payment, deadline, outbox and cart clearing; add idempotency hash/unique key.
- [ ] Implement actor/operation-scoped Idempotency-Key storage and original response replay for successful action POSTs; subsequent tasks apply it to their actions. Test changed-body conflict and concurrent duplicate behavior.
- [ ] Test simultaneous same-key retries => one order and one decrement; changed payload => 409; retry after cart clearing returns original response; overnight opening-hours validation.
- [ ] Run `./mvnw -Dtest=CheckoutConcurrencyTest,CartIntegrationTest test`; expected zero failures, no negative stock or partial orders.
- [ ] Commit `feat: atomically reserve stock and create idempotent orders`.

### 8. Mock payment webhook, expiry and refunds

Files: new webhook receipt/refund migration; payment feature classes,
`payment/service/PaymentWebhookService.java`, `MockPaymentProvider.java`,
`PaymentExpiryJob.java`, `MockRefundListener.java`; `PaymentIntegrationTest.java`.
Produces `PaymentWebhookService.handle(byte[] body, String signature): void`;
`RefundService.requestOnce(UUID orderId, String reason): void` participates in the current transaction.

- [ ] Test signature/amount/currency/reference validation, success, failure, duplicate webhook and conflicting terminal result. Test mock trigger can act only on the customer's own pending payment and is unavailable outside demo profile.
- [ ] Implement mock completion and HMAC webhook; lock order before payment consistently; update state/payment/outbox together. Persist event receipts for retries.
- [ ] Test expiry vs Success with a barrier, repeated expiry, failure release once, late Success => mock refund without order resurrection, duplicate refund request => one refund.
- [ ] Implement expiry using injected Clock and persisted deadline; mock refunds via outbox/consumer with durable refund state. No request waits for notifications.
- [ ] Run `./mvnw -Dtest=PaymentIntegrationTest,CheckoutConcurrencyTest test`; expected zero failures.
- [ ] Commit `feat: process mock payments expiry and refunds safely`.

### 9. Owner lifecycle and customer tracking

Files: `order/service/OrderLifecycleService.java`, `order/controller/OwnerOrderController.java`,
`order/controller/OrderQueryController.java`; `OrderLifecycleIntegrationTest.java`.
Produces `accept(UUID ownerId, UUID orderId)`, `reject(UUID, UUID, String reason)`,
`startPreparation(UUID, UUID)`, `markReady(UUID, UUID)`, each returning OrderResponse.

- [ ] Test unpaid acceptance denied, other owner denied, legal path Placed/Accepted/Preparing/Ready, invalid jumps, accept/reject race and repeated rejection without double stock release/refund.
- [ ] Implement guarded transitions with version increment and outbox. Add own-order lists and customer tracking including payment/refund, restaurant response and assignment separately. Require/replay action Idempotency-Key.
- [ ] Add RestaurantResponseDelayJob with injected Clock: Awaiting response becomes Delayed after 5 minutes, order stays Placed, no refund or stock release. Test no response, late accept/reject, decision-vs-delay race and one notification per transition.
- [ ] Run `./mvnw -Dtest=OrderLifecycleIntegrationTest,PaymentIntegrationTest test`; expected zero failures.
- [ ] Commit `feat: manage restaurant order lifecycle and tracking`.

### 10. Nearby offers and race-safe delivery assignment

Files: new assignment/offers/presence constraints migration,
`delivery/service/DeliveryOfferService.java`, `DeliveryAssignmentService.java`,
`DeliveryOfferExpiryJob.java`, `event/listener/OrderAcceptedListener.java`,
`delivery/controller/DeliveryOfferController.java`; `DeliveryAssignmentConcurrencyTest.java`.
Produces `createOffers(UUID orderId): void`, `listMine(UUID partnerId): List<DeliveryOfferResponse>`,
`acceptOffer(UUID partnerId, UUID offerId): OrderResponse`.

- [ ] Test nearby online/fresh/unoccupied qualification, stale/far/offline exclusion, duplicate OrderAccepted event, offer expiry and unauthorized acceptance.
- [ ] Verify distance is partner-to-restaurant pickup, not partner-to-customer; ratings do not influence matching. Use the user-confirmed policy: notify nearby eligible partners; first valid acceptance wins.
- [ ] Implement discovery from OrderAccepted, persistent offers/round/expiry and partner notification events; no assignment during mere notification.
- [ ] Test two partners on one order => one winner; one partner on two orders => one winner; repeated winning claim idempotent; acceptance vs offer expiry => one valid final outcome; stale-round acceptance rejected.
- [ ] Implement order-then-partner locking, conditional claim, partial unique active-partner index, availability and offer invalidation in one transaction; 409 for losers.
- [ ] Persist No partners available when discovery is empty and Offers unanswered after all offers expire. Keep cooking/payment/stock unchanged; notify on status changes.
- [ ] Add owner/admin POST /orders/{id}/retry-assignment with Idempotency-Key: one new round and DeliveryAssignmentRequested event, fresh eligibility query, only after prior offers expire. Test duplicate retry, newly available partner, stale offer round and delayed events against completed orders. No automatic dispatch cancellation/refund.
- [ ] Run `./mvnw -Dtest=DeliveryAssignmentConcurrencyTest,OrderLifecycleIntegrationTest test`; expected zero failures across repeated coordinated races.
- [ ] Commit `feat: offer nearby deliveries and assign a single winning partner`.

### 11. Pickup, delivery and delivered-order reviews

Files: `delivery/service/DeliveryLifecycleService.java`, review feature classes;
`DeliveryLifecycleIntegrationTest.java`, `ReviewIntegrationTest.java`.
Produces `pickup(UUID partnerId, UUID orderId): OrderResponse`, `deliver(UUID, UUID): OrderResponse`,
`ReviewService.submit(UUID customerId, UUID orderId, ReviewRequest): ReviewResponse`.

- [ ] Test pickup requires ready order + assigned partner, unauthorized transitions denied, deliver requires pickup, repeated delivery cannot free a newly occupied slot.
- [ ] Implement transitions, notifications and atomic slot release; test assignment-before-ready and ready-before-assignment paths.
- [ ] Test rating before delivery denied, other customer denied, stars bounds, review length, one review/order and concurrent restaurant aggregate updates.
- [ ] Implement order/partner review and correct restaurant sum/count update. Do not invent per-item feedback.
- [ ] Run `./mvnw -Dtest=DeliveryLifecycleIntegrationTest,ReviewIntegrationTest test`; expected zero failures.
- [ ] Commit `feat: complete deliveries and collect verified order reviews`.

### 12. Deterministic demonstration data and API collection

Files: `seed/DemoDataSeeder.java`, `src/main/resources/application-demo.properties`,
`docs/demo/catalog-sources.md`, `docs/demo/fooddelivery.http`;
`DemoSeedIntegrationTest.java`.

- [ ] Verify public reference catalog labels; record restaurant URL/date, copied factual names and which values are synthetic. Treat Swiggy dish tiles separately from cuisine lookup.
- [ ] Add the requested six fictional accounts, one city, two restaurants under the demo owner, two categories/four items per restaurant and 3–5 cuisine lookup entries and realistic nearby/far coordinates. Use demo-only deterministic IDs and password hashes; never reset stock on each normal restart.
- [ ] Test seeding twice is idempotent, no demo users outside demo profile, demo IDs/references valid and full paid-order happy path works.
- [ ] Add executable API requests for every role, webhook, failure/rejection, both seeded restaurants, search, cart replacement, unanswered restaurant decisions and unfilled delivery offers. Document local-only reset commands and credentials.
- [ ] Run `./mvnw -Dtest=DemoSeedIntegrationTest test`; expected zero failures.
- [ ] Commit `feat: seed demo catalog and document executable API examples`.

### 13. Failure recovery proofs and submission material

Files: `scripts/demo/checkout_race.py`, `scripts/demo/assignment_race.py`,
`scripts/demo/assert_invariants.sql`, `docs/demo/video-script.md`,
`README.md`, `AGENTS.md`, `docs/database.md`, `docs/tables.md`,
`AsyncRecoveryIntegrationTest.java`.

- [ ] Add HTTP race scripts using barriers/futures and machine-readable counts; use existing two customers for a stock-1 video race and larger test fixtures for load tests. No parent test transaction around concurrent workers.
- [ ] Add deterministic consumer-blocking test: latch blocks notification handling while HTTP request completes; release latch then assert eventual logs/records. Add broker-outage and publisher crash-window replay tests.
- [ ] Verify broker-down request succeeds with pending outbox, restart drains it, subscriber failure goes to DLQ after bounded retries, unaffected recipients continue. Redeliver to prove one assignment/refund effect.
- [ ] Run `./mvnw verify` with documented dedicated test DB/broker; all suites must pass. Run scripts and SQL assertions. Inspect SQL plans with realistic spatial fixtures; record results without treating tiny-table sequential scans as failure.
- [ ] Document architecture, atomic payment interpretation, state machine, API/auth setup, error conventions, assumptions, limitations, AI workflow and exact test/demo commands. Ensure AGENTS.md, used skills and raw development files are retained; exclude credentials and generated binaries.
- [ ] Rehearse the <=10-minute script below, then commit `test: prove concurrency recovery and document assignment demo`.

## Mandatory evidence matrix

| Requirement | Stimulus | Evidence / invariant |
| --- | --- | --- |
| No overselling | 20 checkouts, stock 5 | 5 orders + pending payments, 15 conflicts, stock 0 |
| Full atomicity | Fail second line/payment insertion | No order/lines/payment/outbox; all stock/cart unchanged |
| Request retries | Concurrent same idempotency key | One order and one reservation |
| One winner/order | Two eligible partner accepts together | One assignment, one 409, one assignment event |
| One active order/partner | One partner accepts two orders together | One winner, other order remains unassigned |
| Correct release | Duplicate payment failure/expiry/rejection | Stock restored at most once under policy |
| Waiting states | No restaurant response; no eligible/accepting partner | Delayed / No partners available / Offers unanswered; payment and stock unchanged |
| Action retries | Same key/payload across mutating POST actions | Same response, one transition/event; changed payload => 409 |
| Async/nonblocking | Hold notification listener with latch | HTTP completes before listener is released |
| Durable updates | Broker down during successful API transition | Outbox persisted; later delivery after restart |
| Subscriber isolation | Customer listener fails | Restaurant listener succeeds; customer retry/DLQ |
| Replay safety | Republish confirmed event after crash window | No duplicate business effect; log duplication allowed |
| Authorization | Wrong owner/customer/partner | No mutation, appropriate 403/404 |
| Geography | Known points, radius edge and stale presence | Correct meters, ordering and eligible offers |

Concurrency tests use committed fixtures and independent threads/connections.
Coordinate start barriers to induce overlap, join all futures with timeouts,
and assert persisted database state, not merely HTTP responses. Repeat races
with reset fixtures enough to detect intermittent failures. Use two application
instances against the same test database as an optional additional proof that
correctness does not depend on an in-memory lock. This is verification, not a
microservices deployment requirement.

## Video run of show (9 minutes 30 seconds)

- 0:00–0:45: assignment interpretation, scope and single-app architecture.
- 0:45–1:30: folder structure, schema and transaction/outbox boundaries.
- 1:30–2:15: role-based API request, search and cart restaurant switching.
- 2:15–3:45: checkout, mock webhook, owner accepts/prepares, nearby offer, partner
  accepts, ready/pickup/deliver, customer review; show correlated logs.
- 3:45–5:00: stock-1 race with two customers and DB assertions; show automated
  multi-item rollback test and larger contention test results.
- 5:00–6:15: two partners race; show one winner plus one-partner/two-orders test.
- 6:15–7:30: pause listener or broker, perform status change, show prompt API result
  and pending event; resume and show eventual fan-out and replay-safe effects.
- 7:30–8:30: tests for failure, rejection/refund, waiting states and idempotent retries; test summary.
- 8:30–9:30: assumptions/limitations, AI workflow/skills, incremental commits and
  repository documentation. Recording ends before 10:00.

Precreate IDs/tokens in the API collection and use split terminal panes for
requests, worker logs and invariant queries. Rehearse broker restart ahead of the
recording; use the blocked-listener proof live if startup consumes too much time.

## Priority if the deadline is tight

Protect all three correctness mechanisms, core lifecycle, auth/validation,
reviews, core tests and the recorded proof. Trim large seed catalogs, optional
open-now discovery, item-rating write APIs, coupons, elaborate matching and UI.
Do not replace database safety with JVM locks or claim reliable delivery from
plain background threads. Basic admin/menu CRUD and requested search/cart APIs
remain required in this proposed scope. Do not assume all breadth fits 48 hours:
after tasks 1–2, estimate from actual progress and reduce optional work early.
