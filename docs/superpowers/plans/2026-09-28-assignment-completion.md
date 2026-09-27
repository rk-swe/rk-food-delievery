# Food Delivery Assignment Implementation Plan

> **For agentic workers:** Tasks through 6 are committed to main. Resume at 7 after the user switches to gpt-5.6-terra and resumes execution. The implementation-first override immediately below governs remaining tasks and supersedes conflicting skill defaults and older execution prose. Read the existing spec and contracts once; do not restart planning. Checkboxes alone are not an execution-status source.

## Implementation-first override for tasks 7–13 — 2026-09-28

The user explicitly requested less process overhead: implement the remaining
features first, then write tests and fix all failures toward the end. This
changes execution order, not product scope or acceptance criteria. This section
and AGENTS.md override older TDD, RED/GREEN, per-task review, per-task full-suite,
worktree and commit-gating instructions in this plan and applicable skills.
Shared skill files remain unchanged. This planning turn stops after this update;
the user will switch models before implementation resumes.

### Implementation pass

1. Use one continuing gpt-5.6-terra implementer by default. Work directly on main,
   as authorized, preserving unrelated staged/unstaged changes. No new design
   approval, task brief/report generation, per-task reviewer, review package,
   or model escalation is required. Read only the current task and dependencies.
2. Implement 7 first, freezing order/payment states, DTOs, locking, idempotency,
   stock release and outbox contracts. Then implement 8, 9, 10, 11 and 12;
   prepare task 13's scripts/docs with the final integrated flow. Optional
   persistent lanes may overlap 8/9 with 10 after task 7's shared contracts
   compile. Do not add workers when handoff/integration would cost more time.
3. Preserve the concrete interfaces, dependency ordering, feature ownership and
   migration ordering below. If parallel lanes are used, isolate their worktrees
   and have one coordinator own shared files and integrate to main. Inspect the
   actual next unused migration version (main currently includes V16).
4. Run `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home
   ./mvnw -DskipTests compile` after each integrated task; fix compilation failures
   immediately. Commit that numbered task's implementation to main, without
   bundling unrelated staged changes. Record its commit, produced contracts,
   compile result and deferred test classes as "implemented; verification
   pending". Do not mark it verified or complete yet. No push is implied.
5. New tests and the numbered tasks' test commands are deferred to the validation
   pass. Do not run the full suite between implementation commits or require
   a failing test before writing production code. Existing tests remain intact.
   An earlier narrow diagnostic check is optional when it resolves a concrete
   integration uncertainty, rather than becoming a new task gate.

### Final test, fix and delivery pass

- Write and complete CheckoutConcurrencyTest, PaymentIntegrationTest,
  OrderLifecycleIntegrationTest, DeliveryAssignmentConcurrencyTest,
  DeliveryLifecycleIntegrationTest, ReviewIntegrationTest,
  DemoSeedIntegrationTest and AsyncRecoveryIntegrationTest against the original
  task requirements and mandatory evidence matrix. Include CartIntegrationTest
  and existing API/auth/event regressions. Retain all authorization, rollback,
  idempotency, concurrency, waiting-state, recovery and demo-isolation assertions.
  A green existing suite alone does not prove new feature coverage.
- Run the named focused tests in one batched command where practical. Fix real
  failures in the relevant feature, rerun affected tests, and continue until all
  acceptance cases pass. Do not delete, skip or weaken tests to manufacture green.
- Perform one consolidated self-review of the integrated behavior and contracts
  while validating. No mandatory independent reviewer/re-review loop. Keep the
  review limited to missing requirements and correctness; avoid optional refactors.
- Run `./mvnw verify` with the prescribed JAVA_HOME on the final tree without
  skipping tests. This includes the full test suite and Spotless check, replacing
  duplicate unchanged `test` plus `verify` runs. Fix formatting and all failures;
  rerun affected checks and final verify after corrections. Existing successful
  runs need not be repeated merely because the executor or checkout changed.
- Run task 13's race scripts, SQL invariants, spatial-plan inspection and demo
  rehearsal. Use only the dedicated assignment database and test RabbitMQ vhost,
  with one process/test slot at a time; isolate broker fault injection from other
  applications. Preserve real PostgreSQL/PostGIS/RabbitMQ integration behavior.
- Commit tests and fixes to main in coherent groups identified by numbered task;
  cross-task final verification/docs can belong to task 13. Update ledger status
  to verified/complete only with actual evidence for every required assertion.
  Verification-before-completion still applies to success claims. Report any
  genuine blocker honestly; do not claim unfinished tests or rehearsal succeeded.

The earlier execution schedules and test-first checklists below retain their
behavioral requirements and historical context, but their process timing is
superseded for tasks 7–13 by this override.

## REST/Hibernate revision — authorized prerequisite

Read [Resource APIs and Hibernate persistence](../specs/2026-09-28-rest-jpa-design.md).
The user requested resource-based routes and Swagger organization, Hibernate for
ordinary persistence, and consistent guidance for future agents. Tasks 3.1–3.6
in the authorized [prerequisite plan](2026-09-28-rest-jpa-refactor.md) are now
committed, followed by tasks 4–6. Preserve existing task numbers, commits,
verification evidence and the ledger; do not mark tasks 1–3 unimplemented.

The linked design defines replacement routes and permission scopes for current
features and future workflow resources. It overrides conflicting
endpoint examples below and in the preflight rulings. The assignment's locking,
idempotency, payment, stock and event invariants remain unchanged.

**Goal:** Complete the assignment with demonstrable atomic checkout, safe delivery assignment, durable asynchronous notifications, and the requested REST flows.

**Architecture:** One Spring Boot application with PostgreSQL/PostGIS and a local RabbitMQ broker. Database transactions protect business state; a small transactional outbox connects committed changes to independent in-process queue listeners.

**Target Tech Stack:** Existing Java 25, Spring Boot 4.1.1, Maven wrapper, Flyway, Hibernate/Spring Data JPA with narrowly scoped native queries, Spring Security/Validation/AMQP, PostgreSQL/PostGIS, RabbitMQ, JUnit and Awaitility for bounded eventual assertions.

**Spec:** [Assignment design](../specs/2026-09-28-assignment-design.md). Read its state machine and assumptions before executing any task.

## Global constraints

- Keep Java 25 and Spring Boot 4.1.1 from the existing pom unless the initial build proves a compatibility issue.
- Preserve applied V1–V13; allocate subsequent schema changes the next unused version; never rewrite applied migrations.
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
  user/{controller,entity,repository,service,dto}/
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
src/main/resources/db/migration/  V1–V13 preserved; next unused version per change
src/test/java/com/rk/fooddelivery/ feature tests and integration support
scripts/demo/              concurrent request scripts and SQL assertions
docs/demo/                 API collection, video script, reference data sources
```

Use Hibernate/Spring Data JPA for ordinary CRUD, lookup and pagination. Reserve
native queries for spatial and guarded atomic operations; document any remaining
JdbcTemplate exception inside a feature repository. Do not force awkward
spatial/locking logic into derived JPA method names. Keep
entities, HTTP DTOs and events distinct. Paths below are relative to the above
Java package unless given in full. Each named service has one owning task.

Shared contracts: `CurrentUser.requireId(): UUID`, `CurrentUser.requireRole(Role)`;
`OrderResponse` includes order ID, state, payment/refund state, restaurant response state, assignment state,
partner ID, version, item snapshots and totals; `DomainEvent` includes UUID ID,
String type, UUID orderId, long version, Instant occurredAt, int schemaVersion,
and typed payload. `OutboxService.append(DomainEvent): void` joins its caller's
transaction. Use injected `Clock` for deadlines and tests.

## Commit-sized execution steps

For every Maven invocation set `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home`.
Tests use only database `fooddelivery_assignment_test` and RabbitMQ vhost
`fooddelivery_assignment_test`. Each task runs its focused command **and** full
`./mvnw test` before commit, including tasks whose checklist below only names the
focused command. Keep the existing progress ledger current.

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

### 3.1–3.6. Resource REST and Hibernate prerequisites

Read and execute the authorized [REST/JPA refactor plan](2026-09-28-rest-jpa-refactor.md).
Its tasks are independently verified commits:

- 3.1: Hibernate users/credentials and real PostGIS mapping proof.
- 3.2: City/cuisine resource services and visibility.
- 3.3: Restaurant resources, ownership and transactional hours.
- 3.4: Partner CRUD/presence and removal of shared admin persistence.
- 3.5: OpenAPI resource metadata, full resource contract and guidance.
- 3.6: JWT token issuance, Swagger bearer authorization, removal of Basic authentication and authentication regression coverage.

Record these in the existing ledger without renumbering tasks 1–13. The original
completed task 3 is historical; its caller-ID presence signature is superseded
by principal-derived self service methods in prerequisite 3.4.

## Faster execution for tasks 4–13 — 2026-09-28 revision

**Planning assumption:** 3.6 is the completed prerequisite for this schedule;
this does not assert it is implemented now. Check its evidence once before
starting execution. Do not redo prerequisites that already have valid evidence.
The user authorized parallel work and continuation through task 13; no new
review checkpoint is required between tasks. Preserve gpt-6-astra for planning
and gpt-5.6-terra for implementation. Use fast mode only if the execution tool
exposes it; reasoning effort is not a speed-mode setting.

**Outcome:** retain all required APIs, races, recovery proofs and demo scope,
while overlapping independent implementation and avoiding repeated handoffs.
Task numbers and one numbered task per commit remain unchanged. The slices
below are working checkpoints within a task, not extra commits or review gates.
There is no measured runtime baseline for these remaining tasks, so do not
promise a fixed completion time or speedup.

### Dependency and integration schedule

Use at most three implementation workers plus the coordinator. Keep a worker
on a feature lane across related slices; send follow-ups instead of spawning a
fresh implementer/reviewer for each checklist item. Dispatch only ready work.

| Wave | Lane A | Lane B | Lane C | Required join |
| --- | --- | --- | --- | --- |
| 1, after verified 3.6 | 4 menu/search, then 5 cart | 6 events/notifications | 12 public catalog references and API request draft only | Integrate 4, 5, 6 before 7 |
| 2 | 7 checkout, shared order contracts and idempotency | Finish any wave-1 work; otherwise release worker | Continue 12 draft against frozen routes | Integrate and verify 7 before runtime work on 8/10 |
| 3 | 8 payment/refund, then 9 owner lifecycle | 10 offers/assignment using committed fixtures | 12 seeder after 4–7; 13 race-script/video-outline drafts | Integrate 8, 9, then 10; task 10's final command includes task 9 |
| 4, after 9 and 10 | 11 pickup/delivery | 11 reviews, in disjoint review files | Finish 12 API examples and 13 scripts | Integrate both task-11 slices in one task-11 commit |
| 5 | 12 full demo happy-path verification and commit | 13 recovery tests/docs on integrated 11; no broker disruption during another run | No extra worker unless a concrete independent item remains | Commit 12, then final verification and commit 13 |

Task 10 does not call task 8 or 9 services to arrange its tests: use committed
paid/Accepted/Preparing/Ready fixtures through the test helper. This permits
parallel implementation, not bypassing integration tests. Task 9 consumes
RefundService from task 8, so those tasks stay sequential. Reviews can be
implemented against a committed Delivered fixture while pickup/delivery is
being built, then the combined task verifies the real transition-to-review path.
Task 12/13 drafts are preparation only: never mark them complete early.

### Ownership and contracts that prevent parallel rework

- Coordinator alone integrates commits, assigns unused Flyway versions and edits
  shared build/configuration, shared entities/DTOs, test support and the ledger.
  Workers request a small shared change with its reason; they do not race to
  alter pom.xml, SecurityConfig, RabbitConfig, Order, OrderResponse or migrations.
- Give concurrent workers isolated worktrees. A worker owns its task's feature
  and test files; parallel task-11 workers own delivery and review respectively.
  Rebase dependent work onto the integrated prerequisite before continuing.
  Integrate one task patch at a time, run its final checks on that exact tree,
  then make its numbered commit. Do not commit a combined wave.
- Allocate migration versions in the integration order 4, 5, 6, 7, 8, 9, 10,
  11, 12, 13, skipping tasks with no migration. Reserve actual next-unused
  versions at dispatch; no duplicate numbers, edits to applied migrations, or
  lower-version migration introduced after a higher one was applied. Deferred
  workers may write migration drafts but wait for preceding migrations before
  database verification. Keep demo records out of production migrations.
- Task 4 owns menu entities, menu repository, stock-delta writes and restaurant
  search; task 5 consumes these mappings. Task 6 owns event envelope, routing,
  outbox/inbox and notification infrastructure. Do not duplicate that plumbing
  inside payment or delivery features.
- Before dispatching 8 and 10, task 7 freezes order/payment entity mappings,
  OrderResponse and the full state enums from the spec, including separate
  restaurant-response, assignment, refund and version fields. Its forward
  migration supplies the shared order columns needed by 8–11 (payment deadline,
  response timing, assignment round and stock-release guard). Task 8 owns
  webhook receipt/refund tables; task 10 owns offer tables and active-partner
  uniqueness. Later features mutate existing fields without editing shared
  mappings concurrently. Record actual Java types/methods in the ledger handoff.
- Freeze `OrderRepository.findLockedById(UUID): Optional<Order>` in task 7 and
  reuse it across 8–11. Lock order before payment/partner; cart and checkout lock
  customer first; reserve stock in sorted item-ID order. Task 7 owns
  `StockReservationService.releaseOnce(UUID orderId): void` and its tests.
  Native mutations flush dependent JPA writes and refresh affected managed
  entities before reuse. Add shared committed paid-order fixture builders in
  task 7 so later workers do not duplicate setup or call unfinished services.
- Task 7 supplies `IdempotencyService.execute(UUID actorId, String operation,
  String key, String requestHash, Supplier<StoredResponse> mutation): StoredResponse`,
  joining the caller transaction, plus `StoredResponse(int status, String body,
  String location)` (nullable location). Operation includes resource identity.
  Canonical request hashing and response serialization live in this one module.
  A duplicate replays before current-state/cart checks; failed work rolls back
  both business changes and receipt. Feature controllers derive actor IDs from
  CurrentUser; never accept trusted actor IDs in HTTP DTOs.
- Task 6 freezes DomainEvent and append/inbox contracts before checkout. Add
  feature-specific payloads in the owning feature, using its allowlisted event
  types. Assignment/refund listeners are added by 10/8; task 6 must not install
  placeholder consumers that ACK and discard those business events.
- Each feature owns its Swagger metadata and JWT role/ownership tests in its
  task. Preserve the public, HMAC-authenticated webhook exception in task 8;
  no other protected feature falls back to Basic. Extend resource tags for new
  features rather than applying the prerequisite's six-tag limit forever.

### Small working slices within each numbered task

Each row is a sequence of focused red/green cycles. Use the named test classes
in the task below; add methods there instead of another framework or harness.
Finish a slice, record its concrete result, and continue without a review wait.

| Task | Slice sequence | Completion evidence |
| --- | --- | --- |
| 4 | Owner category/item writes and stock delta → menu search → restaurant spatial search | Ownership, concurrent stock adjustment and combined filters/radius/pagination tests |
| 5 | Basic cart/version → restaurant replacement → concurrent first-cart/replacement | CartIntegrationTest; no stock reservation and no mixed restaurant |
| 6 | Transactional outbox → confirmed publish/routing → inbox/retry/subscriber isolation | EventDeliveryIntegrationTest with real RabbitMQ and rollback/dedup checks |
| 7 | Shared order/idempotency mappings → one checkout + snapshots → rollback/replay → stock contention | CheckoutConcurrencyTest including 20 customers/stock 5 and CartIntegrationTest |
| 8 | Signed webhook + receipts → failure/expiry/release → late success/refund | PaymentIntegrationTest and CheckoutConcurrencyTest; barrier-controlled expiry race |
| 9 | Owner decisions + tracking → preparation/readiness → delayed response job | OrderLifecycleIntegrationTest and PaymentIntegrationTest; decision/delay race |
| 10 | Eligibility/offers → atomic acceptance → expiry/retry rounds | DeliveryAssignmentConcurrencyTest; after task 9, integrated OrderLifecycleIntegrationTest |
| 11 | Pickup/delivery and review implementation in parallel → combined lifecycle proof | DeliveryLifecycleIntegrationTest and ReviewIntegrationTest; one commit |
| 12 | Early source/request drafts → deterministic seeder → executable full happy path | DemoSeedIntegrationTest after 11, including seed-twice and profile isolation |
| 13 | Early scripts/outline → recovery fault injection → final checks/rehearsal | AsyncRecoveryIntegrationTest, test, verify, race scripts and SQL invariants |

Keep the complete behavioral assertions in each task below. Smaller slices do
not remove atomicity, authorization, idempotency or failure-path requirements.
Do not add optional open-now search, per-item reviews, larger catalogs, UI,
deployment, generic workflow frameworks or unrelated refactors.

### Verification without duplicated work

- During a slice, run only its affected test class/method to observe RED and
  GREEN. Run the task's named focused command and full `./mvnw test` once after
  its final integrated change, before committing. Repeat only when code changes
  or a failure requires it. A new worker/context is not a reason to rerun an
  unchanged baseline. This retains the existing per-task verification rule.
- Only one Maven/integration/demo process may use
  `fooddelivery_assignment_test` and its RabbitMQ vhost at a time, including
  cleanup, migrations, scheduler tests and broker fault injection. Coordinator
  grants a single test slot and records its holder; other workers continue
  writing code, reviewing diffs or drafting docs. Worktrees do not isolate DBs.
  Stop the holder's application contexts/listeners before releasing the slot;
  broker fault injection also requires no other application using that broker.
  Use the prescribed JAVA_HOME for every Maven command and explicit test
  database/vhost configuration. Never use fooddelivery for tests.
- Workers report focused evidence; coordinator inspects the task diff and runs
  final focused/full checks in the integration checkout. No extra independent
  reviewer cycle for every slice. Do one cross-feature review before the final
  task-13 verification; fix findings with the relevant regression tests.
- Reuse the established Spring test profile, issued JWT helper, fixture cleanup
  and injected Clock. Background jobs/listeners start only when a test needs
  them; deliberate concurrency remains inside a single test run. Use barriers
  and bounded eventual assertions, not arbitrary sleeps or unbounded race loops.
- Task 13 runs focused AsyncRecoveryIntegrationTest, `./mvnw test` and
  `./mvnw verify` once on the final tree, followed by the documented scripts and
  SQL assertions. Keep all required checks; do not loop successful whole suites
  for reassurance. Record any genuinely necessary rerun and its cause.
- Ledger entries are compact: task/slice, owner, dependency, files, RED/GREEN
  command/result, final focused/full totals, commit, next ready task. Record
  actual elapsed time and test/setup wait so subsequent estimates have evidence.
  If a slice stalls, report the concrete failing check and reduce its scope to
  the next testable behavior; do not restart planning or silently broaden work.

### 4. Menu/category management and discovery

Files: menu feature classes, `restaurant/repository/RestaurantSearchRepository.java`,
`restaurant/service/RestaurantSearchService.java`, `MenuSearchIntegrationTest.java`,
`RestaurantSearchIntegrationTest.java`, `MenuManagementIntegrationTest.java`.
Produces `RestaurantSearchService.search(RestaurantSearchRequest): PageResponse<RestaurantSummary>`
and `MenuService.search(UUID, MenuSearchRequest): PageResponse<MenuItemResponse>`.

- [ ] Test owner isolation, cross-restaurant category rejection, safe concurrent stock edits, availability/deactivation, positive quantities and category counts.
- [ ] Implement category/item CRUD and stock delta endpoint; preserve FK history and update category counts transactionally on moves/deactivation.
- [ ] Test combined name/cuisine/diet/cost filters, pagination tiebreakers, missing coordinates, known near/far points, radius boundary and item price/category filtering.
- [ ] Implement PostGIS filtering/sorting behind the custom RestaurantSearchRepository; use JPA/JPQL for ordinary menu queries and parameterized text filters. Test that cuisine joins do not duplicate restaurant results. Item cuisine/distance filters are intentionally absent.
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
- [ ] Implement actor/resource/operation-scoped Idempotency-Key storage and original response replay for successful action POSTs; subsequent tasks apply it to their actions. Test changed-body conflict and concurrent duplicate behavior.
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
- [ ] Expose owner lists at GET `/api/restaurants/{restaurantId}/orders` with ownership checks and customer history at GET `/api/orders` scoped to the principal. Use POST `/api/orders/{id}/restaurant-decisions` with a validated accept/reject DTO and rejection reason; POST `/api/orders/{id}/preparation` and `/readiness` model subsequent transitions. Implement guarded transitions with version increment and outbox. Add own-order lists and customer tracking including payment/refund, restaurant response and assignment separately. Require/replay action Idempotency-Key.
- [ ] Add RestaurantResponseDelayJob with injected Clock: Awaiting response becomes Delayed after 5 minutes, order stays Placed, no refund or stock release. Test no response, late accept/reject, decision-vs-delay race and one notification per transition.
- [ ] Run `./mvnw -Dtest=OrderLifecycleIntegrationTest,PaymentIntegrationTest test`; expected zero failures.
- [ ] Commit `feat: manage restaurant order lifecycle and tracking`.

### 10. Nearby offers and race-safe delivery assignment

Files: new assignment/offers/presence constraints migration,
`delivery/service/DeliveryOfferService.java`, `DeliveryAssignmentService.java`,
`DeliveryOfferExpiryJob.java`, `event/listener/OrderAcceptedListener.java`,
`delivery/controller/DeliveryOfferController.java`; `DeliveryAssignmentConcurrencyTest.java`.
Produces `createOffers(UUID orderId): void`, `listMine(UUID partnerId): List<DeliveryOfferResponse>`,
`acceptOffer(UUID partnerId, UUID offerId, int round, String idempotencyKey): OrderResponse`.
The acceptance DTO supplies the offer round; authenticated identity supplies
partnerId. The shared task-7 idempotency boundary includes the round in its hash.

- [ ] Test nearby online/fresh/unoccupied qualification, stale/far/offline exclusion, duplicate OrderAccepted event, offer expiry and unauthorized acceptance.
- [ ] Verify distance is partner-to-restaurant pickup, not partner-to-customer; ratings do not influence matching. Use the user-confirmed policy: notify nearby eligible partners; first valid acceptance wins.
- [ ] Implement discovery from OrderAccepted, persistent offers/round/expiry and partner notification events; no assignment during mere notification.
- [ ] Test two partners on one order => one winner; one partner on two orders => one winner; repeated winning claim idempotent; acceptance vs offer expiry => one valid final outcome; stale-round acceptance rejected.
- [ ] Expose offer acceptance at POST `/api/delivery-offers/{id}/acceptances`, retaining actor/resource-scoped Idempotency-Key and offer-round validation. Implement order-then-partner locking, conditional claim, partial unique active-partner index, availability and offer invalidation in one transaction; 409 for losers.
- [ ] Persist No partners available when discovery is empty and Offers unanswered after all offers expire. Keep cooking/payment/stock unchanged; notify on status changes.
- [ ] Add owner/admin POST `/api/orders/{id}/assignment-attempts` with Idempotency-Key: one new round and DeliveryAssignmentRequested event, fresh eligibility query, only after prior offers expire. Test duplicate retry, newly available partner, stale offer round and delayed events against completed orders. No automatic dispatch cancellation/refund.
- [ ] Run `./mvnw -Dtest=DeliveryAssignmentConcurrencyTest,OrderLifecycleIntegrationTest test`; expected zero failures across repeated coordinated races.
- [ ] Commit `feat: offer nearby deliveries and assign a single winning partner`.

### 11. Pickup, delivery and delivered-order reviews

Files: `delivery/service/DeliveryLifecycleService.java`, review feature classes;
`DeliveryLifecycleIntegrationTest.java`, `ReviewIntegrationTest.java`.
Produces `pickup(UUID partnerId, UUID orderId): OrderResponse`, `deliver(UUID, UUID): OrderResponse`,
`ReviewService.submit(UUID customerId, UUID orderId, ReviewRequest): ReviewResponse`.

- [ ] Test pickup requires ready order + assigned partner, unauthorized transitions denied, deliver requires pickup, repeated delivery cannot free a newly occupied slot.
- [ ] Expose POST `/api/orders/{id}/pickup` and `/api/orders/{id}/delivery` with assigned-partner checks and idempotency. Implement transitions, notifications and atomic slot release; test assignment-before-ready and ready-before-assignment paths.
- [ ] Test rating before delivery denied, other customer denied, stars bounds, review length, one review/order and concurrent restaurant aggregate updates.
- [ ] Expose POST `/api/orders/{id}/reviews` for the owning customer. Implement order/partner review and correct restaurant sum/count update. Do not invent per-item feedback.
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
