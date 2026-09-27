# SDD ledger — plan: docs/superpowers/plans/2026-09-28-assignment-completion.md

2026-09-28 REST/Hibernate revision: user requested resource-oriented APIs and
Hibernate where practical, plus plan/docs guidance for future AI work. Added
`docs/superpowers/specs/2026-09-28-rest-jpa-design.md` for detailed review and
linked it from AGENTS, README, the original spec, plan and preflight rulings.
Implementation is pending design approval; no runtime code changed. Complete
the prerequisite refactor before resuming task 4. Preserve tasks 1–3 evidence.

2026-09-28 planning update: at the user's request, gpt-6-astra prepared
`docs/superpowers/plans/2026-09-28-rest-jpa-refactor.md` (tasks 3.1–3.5) and
integrated it into the original plan. gpt-5.6-terra is the selected implementation
model. User-requested normal/fast speed modes cannot be set by the agent tool;
no speed-tier change claimed. Plan review is pending per writing-plans handoff.
Only documentation changed; no Maven suite run for this planning-only change.

Task 1: baseline `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home DB_URL=jdbc:postgresql://localhost:5432/fooddelivery_assignment_test DB_USERNAME=abcom DB_PASSWORD='' ./mvnw test` → 26 tests, 0 failures/errors.
Task 1: Ruling: task-start/task-done scripts are not executable in the checked-in skill copy, and their task extractor expects `Task N` headings while this approved plan uses numbered list headings — recorded task evidence manually; the implementation and verification sequence remains unchanged.
Task 1: Ruling: all Spring integration tests activate the `test` profile, whose datasource defaults only to `fooddelivery_assignment_test`; it does not fall back to `DB_URL`. Reusable destructive cleanup refuses any database whose name does not end with `_assignment_test`.
Task 1: RED observed: ApiErrorContractTest failed because the existing response exposed `error` and omitted the required `code` and `requestId` fields.
Task 1: verification before commit: `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./mvnw -Dtest=SchemaMigrationTests,ApiErrorContractTest test` → 27 tests, 0 failures/errors; `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./mvnw test` → 28 tests, 0 failures/errors.
Task 1: complete (commits e468b24..33fe759, tests: JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./mvnw -Dtest=SchemaMigrationTests,ApiErrorContractTest test → 27 tests, 0 failures/errors)

Task 2: RED observed: SecurityIntegrationTest failed because no PasswordEncoder or credential-backed authentication configuration existed.
Task 2: added V12 credentials/activity migration, BCrypt HTTP Basic database principal lookup, stateless `/api/**` security with API-scoped CSRF exemption, CurrentUser role/id contract, and authenticated `/api/me`. Security errors use the shared status/code/message/fieldErrors/requestId response shape.
Task 2: verification before commit: `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./mvnw -Dtest=SecurityIntegrationTest,SchemaMigrationTests test` → 32 tests, 0 failures/errors; `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./mvnw test` → 35 tests, 0 failures/errors.

Task 3: RED observed: `AdminCrudIntegrationTest` initially failed to compile because the requested admin API did not exist; after correcting the test syntax, the absent controllers/services would not resolve the requested routes.
Task 3: added V13 catalog activity, partner online/location freshness, and audit actor columns; admin city/restaurant/partner CRUD (deactivation preserves history), authenticated lookup APIs, owner restaurant/hours APIs, and partner self-presence APIs. Admin mutations derive audit IDs from the principal, partner credentials use BCrypt, and native PostGIS writes construct longitude/latitude points. City and partner deactivation take compatible row locks before checking active dependents.
Task 3: verification before commit: `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./mvnw -Dtest=AdminCrudIntegrationTest,SecurityIntegrationTest test` → 10 tests, 0 failures/errors; `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./mvnw test` → 38 tests, 0 failures/errors.
Task 3: user clarified that simple soft DELETE admin APIs remain in scope; they set active=false, preserve history, reject active-city and busy-partner conflicts, and avoid broader deletion workflows.
Task 3: final cleanup keeps the simple soft DELETE APIs. SQL now belongs to named AdminRepository methods; AdminCrudService and PartnerPresenceService retain validation, ownership, and transaction boundaries, and OwnerRestaurantController delegates hours updates to the service.
Task 3: final verification after the repository-boundary cleanup: `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./mvnw test` → 39 tests, 0 failures/errors.

Task 3 follow-up (2026-09-28): replaced `lockPartner`'s `ResultSet::next` with an explicitly null-guarded, value-returning lambda and wrapped the nullable query result in `Boolean.TRUE.equals`. This addresses the user-reported IDE null-safety diagnostic while retaining the row-lock query. No new behavior test was added for this localized static-nullability correction; existing real PostgreSQL integration coverage was used. The IDE diagnostic itself was not independently rerun by Maven.
Task 3 follow-up verification: with `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home`, `TEST_DB_URL=jdbc:postgresql://localhost:5432/fooddelivery_assignment_test`, `RABBITMQ_HOST=localhost`, and `RABBITMQ_VHOST=fooddelivery_assignment_test`, `./mvnw -Dtest=AdminCrudIntegrationTest,SecurityIntegrationTest test` passed 11 tests and `./mvnw test` passed 39 tests, both with zero failures/errors/skips. Existing JVM/Mockito agent and SpringDoc startup warnings remain.

## JWT/Swagger planning amendment — 2026-09-28

User requested the login-token-paste-Authorize flow and an updated plan. Added
prerequisite task 3.6: POST `/api/auth/tokens`, 30-minute signed JWTs, current
account/role checks and Swagger bearer authorization. The user clarified JWT
only: task 3.6 removes Basic authentication and migrates existing API clients/tests.
Updated the revision spec, plan references and guidance; implementation and
plan review remain pending. Existing task completion/test evidence is unchanged.
This is documentation only; no JWT implementation or runtime verification claimed.

## Remaining-task execution revision — 2026-09-28

User requested a faster plan for tasks 4–13, assuming verified task 3.6 as the
starting boundary, and authorized parallel implementation where safe. Updated
the existing assignment plan with dependency waves, smaller in-task red/green
slices, shared-contract/file ownership, migration ordering and a single test
slot for the dedicated database/broker. Preserve numbered task commits and
focused/full verification; avoid per-slice reviewer handoffs and unnecessary
unchanged-suite reruns. Planning dependency review used gpt-6-astra; retained
gpt-5.6-terra for implementation. No speed mode was enabled or claimed.

The authorization recorded in AGENTS.md supersedes older "approval pending"
notes above. This plan revision changes no runtime code or completion status:
tasks 3.1–3.6 and 4–13 still need implementation evidence. No Maven run is
claimed for this documentation-only revision.

## Faster prerequisite execution — 2026-09-28

User requested the same execution optimization for 3.3–3.6. Updated the REST/JPA
plan with overlapping restaurant/partner implementation, early resource-audit
and isolated JWT preparation, smaller red/green checkpoints, single ownership
of shared files, and parallel bearer test-client migration after the shared
helper is stable. Final integration/commits remain 3.3 → 3.4 → 3.5 → 3.6 with
each task's focused/full tests. Retained every required assertion, existing model
choices and shared test DB/vhost restrictions. Removed stale review-pending
language from the refactor plan. Documentation only; no runtime verification
or additional task completion is claimed.

## REST/JPA prerequisite completion — 2026-09-28

Tasks 3.1–3.6 are implemented on `main`. Task 3.1 (Hibernate user and
credential persistence) is `e3ff9b4`; task 3.2 (city and cuisine resources)
is `2515586`; task 3.3 (restaurant resources and hours) is `976ef52`; task
3.4 (delivery partners and self presence) is `2e48160`; task 3.5 (resource
OpenAPI metadata and legacy route retirement) is `3c92435` and `3b3fc5c`; and
task 3.6 (JWT bearer authentication) is `4dc2a8c` and `49adda0`.

Focused verification after integration used the dedicated PostgreSQL/PostGIS
database and RabbitMQ test vhost:
`JwtAuthenticationIntegrationTest,JwtConfigurationTest,RestaurantResourceIntegrationTest,DeliveryPartnerResourceIntegrationTest,PartnerPresenceIntegrationTest`
ran 11 tests with 0 failures and 0 errors. The user directed that obsolete
Basic-auth and legacy-route tests be removed and that the historical full suite
not be run for this completion.

Task 4: RED observed with `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./mvnw -Dtest=MenuManagementIntegrationTest,RestaurantSearchIntegrationTest test`: 3 failures — absent menu category/item and stock-adjustment routes returned 404, and restaurant search ignored combined filters (returned 2 instead of 1).
Task 4: added Hibernate menu category/item repositories and feature service/controller DTO boundaries, owner checks, category count maintenance on create/move, locked stock deltas, and available-menu search. Added a native PostGIS-backed `RestaurantSearchRepository` for database-filtered name/cuisine/diet/cost/radius catalog search; ordinary menu filtering stays JPQL. Tests cover owner/cross-restaurant rejection, quantity boundaries, item search and duplicate-safe cuisine filtering.
Task 4: verification before commit: `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./mvnw -Dtest=MenuManagementIntegrationTest,RestaurantSearchIntegrationTest,MenuSearchIntegrationTest test` → 4 tests, 0 failures/errors; `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./mvnw test` → 47 tests, 0 failures/errors. Dedicated `fooddelivery_assignment_test` PostgreSQL/PostGIS database and test RabbitMQ vhost were held exclusively for the Task 4 runs. Commit: `84071d6 feat: manage menus and search restaurant catalogs`. Next ready task: 5.

Task 4 correction round 1: rebased onto integration `1bc5030`. RED: `MenuCorrectionIntegrationTest` exposed missing inactive-menu visibility enforcement, absent item/category deletion semantics, incomplete-coordinate 500 handling, and an untyped null menu-name JPQL binding. GREEN focused command `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./mvnw -Dtest=MenuCorrectionIntegrationTest,MenuManagementIntegrationTest,RestaurantSearchIntegrationTest,MenuSearchIntegrationTest test` → 6 tests, 0 failures/errors. Full `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./mvnw test` → 49 tests, 0 failures/errors. Added guarded category deletion, item deactivation/count updates, owner/admin unavailable-menu inspection, public active-city menu visibility, and explicit incomplete-coordinate 400 handling; formatted Task 4 sources. Commit: `ffaf854 fix: correct menu visibility and lifecycle behavior`.

Task 4 correction round 2: RED observed `RestaurantSearchIntegrationTest.customerLocationIsUsedWhenRadiusIsSuppliedWithoutCoordinates` returned 400 rather than searching around the persisted customer geography. Added principal-backed `users.location` fallback for radius-only searches, while missing stored location remains a 400. Focused `RestaurantSearchIntegrationTest,MenuCorrectionIntegrationTest,MenuManagementIntegrationTest,MenuSearchIntegrationTest` passed 7 tests; full suite passed 50 tests, all with zero failures/errors. Commit: pending.

## Task 4 correction round 4 — 2026-09-28

Owner: Task 4 implementation lane; dependency: verified REST/JPA/JWT prerequisite
and Task 4 corrections through `0d251cd`. Replaced native ordinary restaurant
search with JPQL result/count queries; radius/distance requests alone use the
custom PostGIS branch (`ST_DWithin` meters, `ST_Distance` ordering). Shared
predicate construction enforces admin/owner/public visibility before result
pagination and counting. Added city, UUID cuisine match-any, minimum cost,
allowlisted name/rating/cost/distance sorts and UUID ties. Menu search now has
price/rating/display sorting, availability filtering and validated price ranges.

Fixed duplicate category create/rename conflicts to flush and translate to 409,
owner isolation, and active counts on availability changes, moves and repeated
item deactivation. Coordinator ruling: category DELETE must require no active
items while retaining historical item references. Assigned forward migration
`V14__menu_category_deactivation.sql` adds `menu_categories.active`; public
category result/count queries exclude inactive categories, and item creation,
movement or reactivation cannot activate items in them. Hibernate still validates
Flyway's schema. README documents parameters and lifecycle semantics.

RED before fixes: `RestaurantDiscoveryRegressionTest,MenuCorrectionIntegrationTest`
ran 9 tests, 6 assertion failures plus the expected unhandled duplicate-category
constraint exception. Subsequent green: 9/9. Menu sort/availability RED:
`MenuSearchIntegrationTest` expected Veg Thali first but got Hidden Chicken;
green: 2/2. Category lifecycle RED expected DELETE 204 after item deactivation
but got 409; green: 5/5. Regressions also cover concurrent stock decrements,
cross-owner denial, active-item category deletion guard, historical references,
UUID cuisine deduplication, stored/missing/incomplete coordinates, zero radius,
both sides of a measured PostGIS radius boundary, and pagination ties.

Final verification with
`JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home`:
- `./mvnw -Dtest=MenuManagementIntegrationTest,RestaurantSearchIntegrationTest,MenuSearchIntegrationTest,MenuCorrectionIntegrationTest,RestaurantDiscoveryRegressionTest test`
  passed 15 tests, 0 failures/errors/skips (27.012 seconds).
- `./mvnw test` passed 58 tests, 0 failures/errors/skips (32.989 seconds).
- Targeted Spotless formatting and `git diff --check` passed.

All database/broker tests were serialized in the coordinator-granted slot using
`fooddelivery_assignment_test` PostGIS and the local matching RabbitMQ vhost.
Measured first RED start to final full-suite completion: 08:47:15–08:52:57 IST,
5 minutes 42 seconds; pre-test inspection/setup was not separately timed. The
slot was granted before RED was ready, so no measured test-slot wait. Additional
menu/category red-green runs were required by the remaining spec gaps above.
Expected constraint-rejection logs and existing JVM/SpringDoc warnings remain.
Commit: `fix: complete task 4 discovery and category lifecycle` (this commit).
Next ready task: 5 after coordinator integration; task 6 is the parallel lane.

Task 4: coordinator integrated commits `1bc5030`, `a5c33cd`, `3d363c9`, and
`94e41ed` on `codex/tasks-4-13`. Independent scoped re-review found all nine
previous Important findings addressed and no new Critical/Important issue.
Task 4 complete (commits `007c25d..94e41ed`, review clean). Ruling: categories
soft-deactivate through V14 and permit deletion only when no active items
remain, preserving historical references; this costs a retained inactive
category record if an application needs physical deletion later.

Task 5: RED observed with `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./mvnw -Dtest=CartIntegrationTest test`: the new authenticated `PUT /api/cart/items/{itemId}` requests returned 404 because the cart resource did not exist. An earlier fixture-only RED was corrected before the behavior run because restaurant location is required by the existing schema.
Task 5: added V15 persistent customer `cart_version`, Hibernate cart/cart-item feature repositories and entities, and authenticated customer-only `/api/cart` APIs. Mutations pessimistically lock the customer row; item additions atomically replace a cart from another restaurant, surface `restaurantChanged`, increment the persistent version, and never decrement stock. Tests cover update/remove/clear, replacement, unavailable items, customer isolation, concurrent first-cart creation, and a replacement racing another add.
Task 5: verification before commit: `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./mvnw -Dtest=CartIntegrationTest test` → 5 tests, 0 failures/errors; `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./mvnw test` → 63 tests, 0 failures/errors. Both used the coordinator-granted `fooddelivery_assignment_test` PostgreSQL/PostGIS database and matching RabbitMQ vhost. `git diff --check` passed. Ruling: the unscoped repository `spotless:check` still reports formatting violations in 15 pre-existing unrelated files, so no unrelated formatting was included in this task commit.
