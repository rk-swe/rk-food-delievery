# Resource REST APIs and Hibernate Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Use test-driven-development before implementation and verification-before-completion before reporting success or committing.

**Goal:** Replace the implemented role-prefixed APIs and ordinary JDBC persistence with resource APIs and Hibernate, then add JWT issuance and Swagger bearer authorization as the sole protected-API authentication mechanism while preserving authorization, schema and transactional behavior.

**Architecture:** Keep package-by-feature; controllers validate transport, services enforce role/ownership and own transactions, repositories persist entities, DTOs form the HTTP boundary. Spring Data JPA handles ordinary reads/writes; narrowly documented PostgreSQL-specific repository operations retain native SQL where necessary.

**Tech Stack:** Java 25, Spring Boot 4.1.1, Boot-managed Hibernate/Spring Data JPA, matching Hibernate Spatial, Flyway, PostgreSQL/PostGIS, Spring Security JWT bearer authentication (replacing existing Basic authentication in 3.6), springdoc 3.1.0 and real database integration tests.

**Spec:** [Resource APIs and Hibernate persistence](../specs/2026-09-28-rest-jpa-design.md), with original assignment business invariants retained.

**Status:** Authorized implementation plan; this document does not establish implementation status. Consult the execution ledger. Execute and integrate 3.1–3.6 in order before original task 4, allowing the independent preparation below. Preserve original tasks 1–13 and their evidence. Planning model requested: gpt-6-astra; implementation model requested: gpt-5.6-terra. The user requested normal planning speed and fast implementation speed; model tools do not expose a speed-tier control, so do not claim those speeds were set.

## Global Constraints

- Keep BCrypt, inactive-account denial and stateless session behavior. Existing HTTP Basic remains only during intermediate tasks; task 3.6 replaces it with JWT bearer authentication on protected routes; only POST `/api/auth/tokens` is public within the current API scope.
- JSON stays camelCase; SQL columns stay snake_case with explicit entity mappings.
- Hibernate must validate schema, never create/update it. Preserve applied migrations V1–V13.
- Map DTOs within service transactions, keep open-in-view disabled, and use projections or fetch queries to avoid N+1 reads.
- No SQL in controllers, business services or authentication adapters.
- New persisted resources return 201 with a Location header; reads/patches return 200; soft DELETE returns 204. Transient token issuance returns 200 without Location.
- Wrong-role operations return 403, unauthenticated requests 401.
- Inactive catalog and another owner's resources return 404 to callers who cannot view them.
- Resource GET filtering applies in the database before pagination and counting.
- Set `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home` for every Maven invocation.
- Use only `fooddelivery_assignment_test` and RabbitMQ vhost `fooddelivery_assignment_test` (local default port 5673); never test against `fooddelivery` or substitute H2.
- Run the task's focused command and full `./mvnw test` before its separate numbered-task commit; update `.superpowers/sdd/2026-09-28-assignment-completion/progress.md` with commands/results/commit.
- JWT scope is issuance plus bearer validation only: no registration, refresh tokens, logout/revocation system, seed data, new assignment workflows or speculative schema version column.
- Task 3.6 uses HS256 with a required base64 `JWT_SECRET` decoding to at least 32 random bytes, 1800-second lifetime, issuer `fooddelivery` and audience `fooddelivery-api`. Never ship a production fallback secret or log credentials/tokens.
- Token claims contain only UUID subject and validation metadata; roles, authorities, names, emails, passwords and hashes never enter the token. Reload active user/current role via JPA on every bearer request.

## Review Focus

- Case-insensitive credential collisions must roll back the newly inserted partner as well as credentials (3.4).
- Filtering after pagination could disclose inactive rows or inflate totals; each role needs matching content/count predicates (3.2, 3.3).
- Geography can silently swap coordinates or use degrees; round-trip SRID/order and meter-distance assertions must use PostGIS (3.1, 3.3).
- Concurrent parent deactivation and child creation must serialize on the city row; concurrent hours updates must not lose unrelated days (3.3).
- Native writes and trigger timestamps can leave managed entities stale; reload after flush and prove audit/presence responses match stored values (3.1, 3.4).

---

## File and interface map

All Java paths below are relative to `src/main/java/com/rk/fooddelivery/`; test paths are relative to `src/test/java/com/rk/fooddelivery/`. Braced directories in this map describe structure, not additional empty packages to create.

| Feature                    | Files and responsibility                                                                                                                                                                                                                                                                                                                 |
| -------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Shared identity            | `auth/CurrentUser.java` remains principal authority; `auth/RoleConverter.java` maps Role to lowercase database strings                                                                                                                                                                                                                   |
| User persistence           | `user/entity/User.java`, `UserCredential.java`; `user/repository/UserRepository.java`, `UserCredentialRepository.java`; credentials never cross HTTP boundary                                                                                                                                                                            |
| Cities                     | `city/controller/CityController.java`, `city/service/CityService.java`, `city/repository/CityRepository.java`, `city/entity/City.java`, existing `city/dto/CityDtos.java`                                                                                                                                                                |
| Cuisines                   | Keep within existing restaurant feature: `restaurant/controller/CuisineController.java`, `restaurant/service/CuisineService.java`, `restaurant/repository/CuisineRepository.java`, `restaurant/entity/Cuisine.java`, `restaurant/dto/CuisineResponse.java`                                                                               |
| Restaurants                | `restaurant/controller/RestaurantController.java`, `OwnerRestaurantController.java`; `restaurant/service/RestaurantService.java`; `restaurant/repository/RestaurantRepository.java`, `RestaurantTimingRepository.java`; `restaurant/entity/Restaurant.java`, `RestaurantTiming.java`; existing `RestaurantDtos.java`                     |
| Partners                   | `delivery/controller/DeliveryPartnerController.java` becomes admin resource controller; new `PartnerPresenceController.java` hosts self routes; `delivery/service/DeliveryPartnerService.java`, existing `PartnerPresenceService.java`; `delivery/repository/PartnerWorkloadRepository.java` holds narrow existing-order occupancy query |
| Token authentication (3.6) | `auth/controller/AuthTokenController.java`, `auth/dto/TokenRequest.java`, `TokenResponse.java`; `auth/service/AuthTokenService.java`, `BearerUserService.java`; `auth/JwtUserAuthenticationConverter.java`; `config/JwtConfig.java`                                                                                                      |
| Account/API docs           | Existing `user/controller/MeController.java`; new `user/dto/MeResponse.java`; `config/OpenApiConfig.java`; `config/SecurityConfig.java`                                                                                                                                                                                                  |
| Removed after migration    | `admin/controller/AdminCrudController.java`, `admin/service/AdminCrudService.java`, `admin/repository/AdminRepository.java`                                                                                                                                                                                                              |

Do not create a second delivery-partner entity/table: partners are `User` rows with role `DELIVERY_PARTNER`. Use scalar UUID owner/city/audit references, except a lazy credential-to-user association fetched explicitly during login. UUID IDs are application generated for new ordinary entities; credential ID is its user's UUID. Entity field access and constructors must work with Hibernate; do not use records as entities. Preserve database defaults deliberately by initializing active/online/rating fields and marking database-generated timestamps read-only, then flush/refresh when a response needs generated values. Map text columns as text, currency length 3, money precision 10/scale 2, rating precision 3/scale 2, `LocalTime` hours and `Instant` timestamptz. No cascading historical deletes.

`PageResponse` and all existing request/response DTO shapes remain unchanged, except moving nested account/cuisine response records to the named DTO files. Null PATCH fields retain their existing 'leave unchanged' meaning; trimming remains as today. Defaults: page 0, size 20, page >= 0, size 1–100. Sort in SQL/JPQL by `lower(name), id`; never sort/filter a page in memory.

## Service authorization and route contracts

Every protected business service entry point below calls `CurrentUser` itself before data access. Public token issuance instead authenticates the submitted credentials through the existing authentication provider. Controllers must not supply actor/owner IDs for authorization. Entity/repository helper methods do not become public HTTP entry points. Actor columns come from the authenticated principal.

| Service method / route                                                                  | Permission and visibility                                                                                               |
| --------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| `CityService.list/get` — GET `/api/cities[/{id}]`                                       | All authenticated; admin sees all, every other role sees active only                                                    |
| `CityService.create/patch/deactivate` — POST/PATCH/DELETE city resources                | ADMIN only                                                                                                              |
| `CuisineService.list` — GET `/api/cuisines`                                             | All authenticated, size-limited list                                                                                    |
| `RestaurantService.list/get` — GET `/api/restaurants[/{id}]`                            | ADMIN all; RESTAURANT_OWNER own, including inactive; CUSTOMER/DELIVERY_PARTNER only active restaurants in active cities |
| `RestaurantService.mine` — GET `/api/me/restaurants`                                    | RESTAURANT_OWNER only, same owned visibility                                                                            |
| `RestaurantService.create/patch/deactivate` — restaurant mutations                      | ADMIN only; active owner and locked active city required on create                                                      |
| `RestaurantService.updateHours` — PATCH `/api/restaurants/{id}/hours`                   | RESTAURANT_OWNER only and matching owner; other owner's/missing ID => 404; admin => 403                                 |
| `DeliveryPartnerService` — `/api/delivery-partners[/{id}]` all verbs                    | ADMIN only, detail on a non-partner UUID => 404                                                                         |
| `PartnerPresenceService` — PATCH `/api/me/delivery-partner/location` or `/availability` | DELIVERY_PARTNER only; current principal ID, active account required                                                    |
| `AuthTokenService.issue(TokenRequest)` — POST `/api/auth/tokens` (3.6)                  | Public credential exchange; authenticate active existing account with BCrypt, no existing Basic/Bearer header required  |
| GET `/api/me`                                                                           | Any authenticated role; DTO only, no password/hash                                                                      |

Through 3.5, use HTTP Basic for all `/api/**`. After 3.6, protected API routes accept only Bearer; POST `/api/auth/tokens` alone permits anonymous credential submission. Preserve every role/ownership rule under bearer authentication; reject Basic-only protected requests with 401. Unknown legacy admin URLs must have no handler. Remove `/api/admin/**` special matcher only when all migrated service methods enforce permissions. Test removed routes as an admin: 404 (for legacy `/api/restaurants/mine`, 400 from UUID conversion is acceptable, but it must not invoke the former handler). OpenAPI contains none of the removed paths. No compatibility aliases.

## Execution protocol shared by every task

- [ ] Read spec, this plan, original ledger and current diff; preserve unrelated edits. Use an isolated worktree if required by the execution skill, without losing the reviewed plan.
- [ ] Note that existing integration fixtures truncate catalog/users, including manually provisioned demo accounts in the shared dedicated test database; preserve any wanted fixture recipe before testing and re-provision explicitly afterward. Restart a running development application after compilation before verifying its live routes.
- [ ] Confirm active test profile and exact database/vhost; strengthen the existing cleanup guard to require `current_database() = 'fooddelivery_assignment_test'` in 3.1. Do not start a suite with production override variables.
- [ ] Write named tests first and run the focused command below; observe intended failing assertions or missing production interfaces before implementation. Existing regression tests must remain; migrate their route assertions alongside their owning feature.
- [ ] Implement only this task, rerun its focused command and `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./mvnw test`; both must finish with zero failures/errors.
- [ ] Review task diff, update ledger with actual evidence, stage only its files and commit the task's named message. Never commit a deliberately broken intermediate application.

The `J25` notation is not used in commands: each command includes the required environment explicitly. Test DB cleanup is shared; do not run Maven suites concurrently. Concurrency tests use committed fixtures, separate transactions/connections, bounded latches and futures, and persisted-state assertions.

### Task 3.1: Hibernate user persistence, credential lookup and geography proof

**Files:** Create user entities/repositories and `auth/RoleConverter.java` from map; modify `auth/DatabaseUserDetailsService.java`, `pom.xml`, `src/test/java/com/rk/fooddelivery/support/IntegrationTestSupport.java`. Test: `user/UserPersistenceIntegrationTest.java`, existing `auth/SecurityIntegrationTest.java`, `SchemaMigrationTests.java`. Retain `application.properties` validation/open-in-view settings.

**Interfaces:** `UserRepository extends JpaRepository<User, UUID>` exposes `Optional<User> findLockedById(UUID id)` with PESSIMISTIC_WRITE and `boolean existsByIdAndRoleAndActiveTrue(UUID id, Role role)`. `UserCredentialRepository extends JpaRepository<UserCredential, UUID>` exposes `Optional<UserCredential> findWithUserByUsernameIgnoreCase(String username)` using an explicit fetch join. `RoleConverter implements AttributeConverter<Role,String>`. `DatabaseUserDetailsService.loadUserByUsername(String): UserDetails` keeps its existing contract.

- [ ] Add `credentialsLoadCaseInsensitivelyWithoutExtraUserQuery`: persist User/credential in a committed transaction, clear context, lookup uppercase username; assert matching ID/role/hash and one joined credential/user select. Retain unknown-user, bad-password, inactive-user and `/api/me` secrecy tests.
- [ ] Add `userMappingPreservesDefaultsAuditAndNullableGeography`: new user has active=true, online=false, null location; reload preserves actor UUIDs and non-null timestamps; update in a later transaction preserves createdAt and advances updatedAt. Assert role database value is `delivery_partner`, not Java enum name.
- [ ] Add `geographyRoundTripsLongitudeLatitudeAndMeters`: persist Point x=77.5946, y=12.9716, SRID=4326; use test-only JDBC assertions for ST_X/ST_Y/ST_SRID and approximately 111 meters for a nearby 0.001-degree latitude difference. Null location remains valid. Add rollback assertion after a JPA user write.
- [ ] Run `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./mvnw -Dtest=UserPersistenceIntegrationTest,SecurityIntegrationTest,SchemaMigrationTests test`; expect new behavior/mapping tests to fail before implementation.
- [ ] Add Boot-version-aligned `org.hibernate.orm:hibernate-spatial`; use JTS Point and Hibernate's supported geography JDBC type against the existing `geography(Point,4326)` column. Inspect the resolved Hibernate dependency/API locally before selecting annotations; do not independently upgrade Hibernate. Implement mappings, joined credential lookup and exact DB cleanup guard.
- [ ] If geography mapping cannot pass the real test, document concrete compatibility evidence before adopting the spec's native fallback. Implement only spatial conversion/write SQL in feature custom repositories, retain scalar JPA CRUD, and preserve non-null restaurant insertion atomically (a custom native spatial insert is an explicitly documented exception if necessary). Update the file map/ledger with the actual fallback; no silent nullable-column migration or whole-CRUD JDBC fallback. Apply the same strategy consistently in 3.3/3.4.
- [ ] Run focused command and full suite per protocol; commit `refactor: map users and credentials with Hibernate`.

### Task 3.2: City and cuisine resource services

**Files:** City/Cuisine files in map; remove only city handlers/methods from the three admin classes as references disappear. Modify `admin/AdminCrudIntegrationTest.java` city route assertions. Test: `city/CityResourceIntegrationTest.java`, `restaurant/CuisineResourceIntegrationTest.java`.

**Interfaces:** `CityService.create(CityRequest): CityResponse`, `patch(UUID,CityPatch): CityResponse`, `deactivate(UUID): void`, `get(UUID): CityResponse`, `list(int page,int size): PageResponse<CityResponse>`. `CityRepository extends JpaRepository<City,UUID>` exposes `findLockedById(UUID): Optional<City>` with PESSIMISTIC_WRITE and `findVisible(boolean activeOnly, Pageable): Page<City>` with explicit matching count query. `CuisineService.list(int size): List<CuisineResponse>`; `CuisineRepository extends JpaRepository<Cuisine,UUID>` exposes `findOrdered(Pageable): List<Cuisine>`. The restaurant existence guard temporarily remains a repository dependency until replaced by `RestaurantRepository.existsByCityIdAndActiveTrue(UUID)` in 3.3; no SQL moves into CityService.

- [ ] Add `cityRoleMatrixAndLocation`: admin POST => 201 and `/api/cities/{id}` Location; GET/PATCH => 200, DELETE => 204; other authenticated roles cannot mutate (403), anonymous => 401. Call CityService directly under a customer SecurityContext and assert AccessDeniedException/no write.
- [ ] Add `cityVisibilityFiltersContentAndTotalBeforePaging`: mixed inactive/active and mixed-case same-name fixtures, size=1; non-admin total counts active only, stable UUID tiebreaks; inactive detail => 404, admin detail => 200. Invalid page/size => 400. Case-insensitive duplicate create/patch => existing 409 contract and no partial update.
- [ ] Add `cuisinesUseOrderedDtos`: anonymous 401; each authenticated role receives size-limited camelCase `imageUrl`, deterministic name/UUID order, no persistence fields; size 0/101 => 400. Retain city historical-row/deactivation-conflict tests.
- [ ] Run `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./mvnw -Dtest=CityResourceIntegrationTest,CuisineResourceIntegrationTest,AdminCrudIntegrationTest test`; confirm intended failures.
- [ ] Implement JPA entities/repositories and transactional services; extend CityController to all resource verbs and move CuisineController SQL into CuisineRepository. Flush city writes within the existing conflict-translation boundary and rethrow without continuing the failed transaction. Remove corresponding old handlers immediately; keep unrelated admin code until its task.
- [ ] Run focused command and full suite per protocol; commit `refactor: expose city and cuisine resource services`.

## Faster execution for 3.3–3.6 — 2026-09-28 revision

The user requested shorter execution for these tasks. Preserve all named
assertions below, but work in small red/green slices and overlap independent
feature files. This section overrides repeated setup and all-tests-first
interpretations of the execution protocol; it does not waive task verification.
No new approval or per-slice review gate is needed. Do not mark prerequisites
complete by assumption or restart already verified work.

### Schedule and file ownership

Start after verified 3.1/3.2, using their actual User/City mappings, spatial
strategy, CurrentUser and test helpers. Check their evidence once. Up to three
workers use isolated worktrees; the coordinator owns integration and the shared
test slot. Retain a worker across related slices rather than starting a fresh
agent/reviewer for each test.

| Stage | Primary lane                                            | Independent parallel lane                                                                                           | Integration gate                                               |
| ----- | ------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------- |
| A     | 3.3 restaurant implementation                           | 3.4 partner service/repository/new tests; 3.5 route/operation metadata inventory and documentation draft            | Commit verified 3.3 first                                      |
| B     | 3.4 integrate partner routes and retire admin code      | 3.5 prepare contract tests; 3.6 JWT configuration/validator tests and new auth-only files on a private branch       | Commit verified 3.4 before whole-route audit                   |
| C     | 3.5 annotations, resource audit and documentation       | 3.6 continue isolated JWT implementation and fail-closed configuration tests                                        | Commit verified 3.5 with current Basic runtime                 |
| D     | 3.6 integrate JWT security and shared token test helper | After helper is stable, migrate disjoint feature test files to bearer in parallel; prepare final Swagger/login docs | Merge all 3.6 slices, run its focused/full checks, commit once |

Parallel preparation never changes the integrated authentication mode early.
Do not merge unfinished JWT configuration that would make a 3.3–3.5 application
require JWT_SECRET or break its existing tests. Task 3.6 may run its isolated
configuration tests early; its full API contract waits for integrated 3.5.

- Restaurant worker owns restaurant entities/repositories/services/controllers
  and RestaurantResourceIntegrationTest/RestaurantConcurrencyIntegrationTest.
  Partner worker owns delivery feature files and its two named test classes.
- Coordinator applies shared changes sequentially: CityService guard, additions
  to UserRepository/UserCredentialRepository, AdminCrudController/AdminCrudService/
  AdminRepository removal, AdminCrudIntegrationTest route migration, SecurityConfig,
  pom.xml, application properties, IntegrationTestSupport and ledger. Workers
  supply precise patches instead of editing these concurrently. Remove restaurant
  admin methods in 3.3; delete the remaining admin classes only in 3.4.
- Task 3.5 applies controller annotations after their owning feature is integrated.
  Its early inventory fixes paths, role descriptions, tags and unique operationIds;
  it does not create another controller set or duplicate the role matrix.
  OpenApiConfig and shared contract tests have one writer at a time: 3.5, then 3.6.
- The 3.6 auth worker owns new auth files and JwtConfig. Coordinator integrates
  its dependency/security/property/repository patches together, then exposes a
  single helper that obtains real tokens through POST /api/auth/tokens. Partition
  migration of existing test call sites by file only after that helper works.
  Keep explicit Basic-rejection tests; never replace real auth with mocked principals.
- No schema migration is expected. Do not reopen the proven geography choice,
  add a parallel auth framework or expand token scope. Any necessary forward
  migration requires concrete failing evidence and coordinator-assigned version.

### Small checkpoints, without additional commits

Use the exact interfaces and named assertions in the task sections below.
For each checkpoint write its failing test, run it, implement and rerun it.
Do not wait to author every test for a whole task before getting its first slice
green. Checkpoints are progress units, not independent completion claims.

| Task | Checkpoints in execution order                                                                                                                                                             | Final named test command remains                                                                                                                      |
| ---- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------- |
| 3.3  | JPA create/patch/spatial round-trip → role-filtered lists/detail/mine → hours update → city/hour races and legacy route removal                                                            | RestaurantResourceIntegrationTest, RestaurantConcurrencyIntegrationTest, AdminCrudIntegrationTest                                                     |
| 3.4  | Partner CRUD/credential rollback → principal-derived presence/timestamps → busy/deactivation races → delete admin remnants                                                                 | DeliveryPartnerResourceIntegrationTest, PartnerPresenceIntegrationTest, AdminCrudIntegrationTest, SecurityIntegrationTest                             |
| 3.5  | Resource tags/operation metadata → parameterized route/role/Location audit → native-query boundary scan and accurate docs                                                                  | OpenApiContractIntegrationTest, ResourceRouteContractIntegrationTest, SecurityIntegrationTest                                                         |
| 3.6  | Fail-closed secret and JWT validators → token issuance/current-account bearer identity → atomic security/helper cutover → parallel test-client migration → final bearer OpenAPI/docs audit | JwtAuthenticationIntegrationTest, JwtConfigurationTest, SecurityIntegrationTest, OpenApiContractIntegrationTest, ResourceRouteContractIntegrationTest |

### Reduce repeated setup and test overhead

- During development use `./mvnw -Dtest=ClassName#methodName test` for the affected
  assertion, or its class when methods are parameterized/fixtures are shared.
  Set the required JAVA_HOME on every invocation. A missing interface or intended
  failing assertion supplies RED evidence; infrastructure failure does not.
- At task integration run its exact focused command below and full `./mvnw test`
  once after the last change, then record evidence and make its numbered commit.
  Repeat only for changed code, a failure or a concrete unresolved concern.
  Do not rerun an unchanged baseline merely because a worker/context changed.
- Serialize every DB/broker test run, cleanup and live-app check on
  fooddelivery_assignment_test and its dedicated vhost. Worktrees share these
  resources. Coordinator records the test-slot holder; close its application
  contexts/listeners before handoff. Other workers continue implementation or
  documentation while the slot is occupied. Never switch to the ordinary DB.
- Reuse existing fixtures, Spring context configuration and injected Clock.
  Parameterize JWT rejection cases within one compatible context; use isolated
  configuration tests for invalid secrets, avoiding a full DB-backed application
  restart per malformed secret/token. Retain actual bearer/filter integration
  coverage and real PostGIS race tests; no timing sleeps or H2 substitutions.
- Extend 3.5's existing route matrix in 3.6 by changing its authentication helper
  and adding token/security assertions; do not rebuild the same matrix twice.
  Write resource documentation in 3.5 and add the login instructions in 3.6;
  do not spend time polishing a temporary Basic Swagger workflow.
- Coordinator reviews each task diff once. Do one final cross-feature auth and
  persistence review before 3.6's final verification; no fresh reviewer per slice.
  Record slice result, actual command/totals, elapsed time, wait/blocker and next
  step in the existing ledger. If a slice stalls, identify the failing assertion
  and make the next behavior smaller instead of restarting the whole task.

This schedule reduces repeated handoffs and overlaps independent code work.
Shared-file integration and final database verification remain sequential.
No measured runtime baseline supports a fixed finish-time promise.

### Task 3.3: Restaurant resources, owner visibility and transactional hours

**Files:** Restaurant files in map; modify `city/service/CityService.java` to use the restaurant existence query; remove restaurant methods from admin classes; migrate restaurant paths in `admin/AdminCrudIntegrationTest.java`. Test: `restaurant/RestaurantResourceIntegrationTest.java`, `restaurant/RestaurantConcurrencyIntegrationTest.java`.

**Interfaces:** `RestaurantService.create(RestaurantRequest): RestaurantResponse`, `patch(UUID,RestaurantPatch): RestaurantResponse`, `deactivate(UUID): void`, `get(UUID): RestaurantResponse`, `list(int,int): PageResponse<RestaurantResponse>`, `mine(int,int): PageResponse<RestaurantResponse>`, `updateHours(UUID,List<HoursRequest>): RestaurantResponse`. `RestaurantRepository extends JpaRepository<Restaurant,UUID>` exposes `findVisible(Role role,UUID actorId,Pageable): Page<Restaurant>`, `findVisibleById(UUID id,Role role,UUID actorId): Optional<Restaurant>`, `findLockedById(UUID): Optional<Restaurant>`, `existsByCityIdAndActiveTrue(UUID): boolean`. Query visibility predicates follow the matrix exactly. `RestaurantTimingRepository extends JpaRepository<RestaurantTiming,UUID>` exposes `findByRestaurantId(UUID): List<RestaurantTiming>`.

- [ ] Add `restaurantRoleAndVisibilityMatrix`: each role exercises list/detail/create/patch/delete/hours/mine; verify admin all, owner own including inactive, customers/partners active-in-active-city only. Size=1 total/count tests include another owner's and inactive fixtures. Wrong-owner detail/hours => 404; wrong-role mutation => 403; direct customer service mutation is denied. New POST includes Location; legacy admin restaurant handlers disappear.
- [ ] Add `restaurantJpaPreservesSpatialAndScalarPatch`: actual database round-trip coordinates, cost precision, description/address nulls, audit actor; paired coordinate updates succeed, one-coordinate PATCH retains existing 409 behavior, invalid ranges => 400; null patch fields preserve values. No new owner/city reassignment fields.
- [ ] Add `hoursPatchPreservesOtherDaysAndLastDuplicateWins`: Monday/Tuesday fixtures, patch Monday twice => one row with final Monday entry, unchanged Tuesday; closed hours nulls and overnight windows valid, malformed times rejected. Duplicate-day last-entry behavior preserves current sequential upsert semantics.
- [ ] Add `restaurantCreationRacesCityDeactivation`: hold city lock in one transaction and start the competitor; once released, either creation wins and deletion gets 409, or deletion wins and creation gets 409. Never active restaurant in inactive city. Add `concurrentHoursPatchesKeepDistinctDays`: separate connections patch different days and both rows survive.
- [ ] Run `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./mvnw -Dtest=RestaurantResourceIntegrationTest,RestaurantConcurrencyIntegrationTest,AdminCrudIntegrationTest test`; confirm intended failures.
- [ ] Implement mappings and service/query predicates. Lock city before restaurant creation and city deactivation; validate owner via UserRepository. Lock restaurant before hours read-modify-save, allowing ordinary JPA upsert under that shared row lock. Flush all hours in one transaction; map unchanged RestaurantResponse contract. Use geography strategy proven in 3.1. Do not eagerly load hours/cuisines per list row.
- [ ] Mount owner collection at `/api/me/restaurants` and hours at `/api/restaurants/{id}/hours` using method-level absolute mappings in OwnerRestaurantController; RestaurantController owns common resource verbs. Replace City's temporary guard dependency and remove migrated admin methods.
- [ ] Run focused command and full suite per protocol; commit `refactor: migrate restaurant resources and hours to Hibernate`.

### Task 3.4: Partner resources and self presence; retire shared admin persistence

**Files:** Delivery files in map; extend `user/repository/UserRepository.java`; delete all three admin production files after final usages are removed. Migrate remaining paths in `admin/AdminCrudIntegrationTest.java` (retain historical regression test name). Test: `delivery/DeliveryPartnerResourceIntegrationTest.java`, `delivery/PartnerPresenceIntegrationTest.java`.

**Interfaces:** `DeliveryPartnerService.create(PartnerRequest): PartnerResponse`, `patch(UUID,PartnerPatch): PartnerResponse`, `deactivate(UUID): void`, `get(UUID): PartnerResponse`, `list(int,int): PageResponse<PartnerResponse>`. `PartnerPresenceService.updateLocation(LocationRequest): PartnerResponse`, `updateAvailability(PresenceRequest): PartnerResponse`; remove caller-ID overloads and update all callers. `UserRepository.findByIdAndRole(UUID,Role): Optional<User>`, `findByRoleOrdered(Role,Pageable): Page<User>`. `PartnerWorkloadRepository.hasActiveDelivery(UUID): boolean` encapsulates the existing native orders existence query until original lifecycle tasks map their order model; this narrow repository exception prevents mapping future order scope prematurely.

- [ ] Add `partnerDirectoryIsAdminOnly`: CRUD list/detail prohibited for owner/customer/partner; anonymous 401, admin valid results and Location on create; non-partner UUID => 404. Direct non-admin service calls fail before querying/mutating.
- [ ] Add `duplicateUsernameRollsBackUserAndCredentials`: case-insensitive username collision => 409, unchanged user/credential counts, no orphan user; invalid/duplicate role-scoped email/phone keeps current error contract. New partner can log in with BCrypt; password/hash never appears in any HTTP DTO.
- [ ] Add `presenceUsesPrincipalAndServerTimestamp`: only current partner can mutate its location/online state, no target ID in route; other roles => 403; geographic order preserved; locationUpdatedAt is server assigned, response equals persisted value, other partner untouched. Empty/null optional user location remains supported.
- [ ] Add `busyPartnerCannotDeactivateAndHistorySurvives`: orders in currently schema-valid Accepted, Preparing or Out for delivery yield 409; retain Ready for pickup in the workload predicate, but defer its fixture case until the original lifecycle migration permits that status (do not change V7 or add a migration just for this refactor); free partner deactivation sets active=false/online=false without removing user/credentials/history. Lock the partner in an independent transaction, begin deactivation, update busy state under that lock, then release; deactivation must recheck after lock acquisition. Add `presenceRacingDeactivationCannotBringInactivePartnerOnline` under same row lock.
- [ ] Run `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./mvnw -Dtest=DeliveryPartnerResourceIntegrationTest,PartnerPresenceIntegrationTest,AdminCrudIntegrationTest,SecurityIntegrationTest test`; confirm intended failures.
- [ ] Implement transactional user+credential writes, explicit flush for translated 409 errors and partner row locks on presence/deactivation. Native workload query retains exact listed statuses; no new assignment state model. Use one Spring transaction; when fallback native spatial writes are used flush before query and refresh managed User before returning DTO. Keep database timestamp authority for presence/audit values.
- [ ] Move self controller to `/api/me/delivery-partner`; remove all residual AdminCrud/AdminRepository references and obsolete service signatures. Remove the old admin security matcher; retain authenticated `/api/**` and explicit service guards.
- [ ] Run focused command and full suite per protocol; commit `refactor: secure partner resources and persist presence with Hibernate`.

### Task 3.5: Resource OpenAPI metadata, contract audit and documentation

**Files:** Create `config/OpenApiConfig.java`, `user/dto/MeResponse.java`; modify all current feature controllers, `user/controller/MeController.java`, `src/main/resources/application.properties`, `README.md`, `AGENTS.md`, `docs/database.md`, `docs/tables.md` as needed for accurate implemented guidance. Test: `api/OpenApiContractIntegrationTest.java`, `api/ResourceRouteContractIntegrationTest.java`.

**Interfaces:** Existing Basic runtime authentication remains temporarily for this resource audit; do not add a Basic Swagger scheme or promote Basic as the final workflow. Task 3.6 declares the final bearer-only security contract and repeats the audit. Tags exactly `Account`, `Cities`, `Cuisines`, `Restaurants`, `Delivery Partners`; no fallback controller tags. Every operation has an explicit unique operationId, readable summary and role/visibility description. Alphabetical Swagger tag and operation ordering is deterministic; `/api/me/restaurants` belongs to Restaurants and partner self routes belong to Delivery Partners.

- [ ] Add `openApiDeclaresResourceTags`: unauthenticated `/v3/api-docs` and Swagger UI are accessible; every current operation uses only prescribed tags with non-empty permission text and globally unique operationId. Assert old admin/mine/presence paths absent.
- [ ] Add `allResourceRoutesHonorAuthenticationAndRemovedPathsStayRemoved`: parameterized route/HTTP method cases for every current operation, 401 anonymous, expected role matrix; admin requests to removed routes find no handler. Assert collection POST Location resolves through GET and no DTO exposes password, passwordHash or credential association. Assert invalid page/size contract at each paged controller.
- [ ] Run `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./mvnw -Dtest=OpenApiContractIntegrationTest,ResourceRouteContractIntegrationTest,SecurityIntegrationTest test`; confirm missing documentation assertions fail.
- [ ] Implement OpenAPI metadata and deterministic UI sorters, move MeResponse to DTO package and preserve its JSON. Defer the final login/Authorize instructions to 3.6; no default credentials or demo seed claimed.
- [ ] Update README's complete old-to-new route table, folder map, service ownership rules and JPA/native boundaries. Record actual spatial approach and any narrow native exceptions. Mark only this task complete here; mark spec/plan implemented only after 3.6 and all tasks' verification evidence exists; original tasks 4–13 remain pending. Keep original ledger task history intact.
- [ ] Inspect production sources for residual `JdbcTemplate`, native SQL, AdminCrud references, `/api/admin` and old self URLs; every surviving native method must have a specific PostgreSQL reason in its feature repository. Inspect entity mappings and confirm V1–V13 unchanged with git diff. Run focused command and full suite per protocol; commit `docs: document and verify resource APIs and Hibernate boundaries`.

### Task 3.6: JWT credential exchange and Swagger bearer authorization

**Files:** Create `auth/controller/AuthTokenController.java`, `auth/dto/TokenRequest.java`, `auth/dto/TokenResponse.java`, `auth/service/AuthTokenService.java`, `auth/service/BearerUserService.java`, `auth/JwtUserAuthenticationConverter.java`, `config/JwtConfig.java`. Modify `config/SecurityConfig.java`, `config/OpenApiConfig.java`, `user/repository/UserCredentialRepository.java`, `pom.xml`, `src/main/resources/application.properties`, `src/test/resources/application-test.properties`, `README.md`, `.env.example`, `AGENTS.md`, and the existing progress ledger. Modify `support/IntegrationTestSupport.java` and all existing integration test call sites using Basic to obtain issued JWTs while preserving their existing business assertions. Test: create `auth/JwtAuthenticationIntegrationTest.java`, `auth/JwtConfigurationTest.java`; extend `auth/SecurityIntegrationTest.java`, `api/OpenApiContractIntegrationTest.java`, `api/ResourceRouteContractIntegrationTest.java`.

**Interfaces:** `TokenRequest(String username, String password)` is a validated request record with nonblank fields; never trim or echo passwords. `TokenResponse(String accessToken, String tokenType, long expiresIn)` returns exactly `tokenType="Bearer"`, `expiresIn=1800`. `AuthTokenController.issue(TokenRequest): TokenResponse` exposes POST `/api/auth/tokens`, operationId `issueAuthToken`, tag `Authentication`, security `[]`, status 200 without Location. `AuthTokenService.issue(TokenRequest): TokenResponse` authenticates via Spring Security `AuthenticationManager` and the existing BCrypt/JPA username provider, then issues the signed JWT with injected `Clock`. `UserCredentialRepository.findWithUserByUserId(UUID): Optional<UserCredential>` fetch-joins the current account for bearer lookup; `BearerUserService.loadActiveUser(UUID): AuthenticatedUser` maps it inside a read-only transaction, rejects missing/inactive accounts with an authentication failure and returns no password material in the bearer principal. `JwtUserAuthenticationConverter.convert(Jwt): AbstractAuthenticationToken` uses that principal and its current authorities, preserving `CurrentUser` and `/api/me`; it never derives permissions from token claims. `JwtConfig` provides supported Spring Security `JwtEncoder` and `JwtDecoder` beans; inject the existing `Clock` from `config/ClockConfig.java` rather than declaring a second clock bean.

- [ ] Add `validCredentialsIssueThirtyMinuteJwt`: anonymous JSON username/password POST for each active role returns 200, no Location, exactly the three camelCase response fields above, `Cache-Control: no-store`, no cookie/session; decoded signature-valid claims have UUID `sub`, `iss=fooddelivery`, `aud` containing `fooddelivery-api`, required `iat`/`exp` with `exp-iat=1800`, and header `alg=HS256`. Assert no name/email/username/password/hash/role/authority claims. `caseInsensitiveUsernameStillAuthenticates` preserves the existing lookup behavior.
- [ ] Add `invalidCredentialExchangeUsesExistingErrors`: unknown username, incorrect password and inactive user receive the same 401 `UNAUTHENTICATED` envelope without account-existence details or token; missing/blank fields or malformed JSON receive existing 400 validation envelopes. A supplied Basic header cannot substitute for the request body credentials or grant authentication. Do not log body credentials or returned tokens.
- [ ] Add `bearerUsesCurrentDatabaseIdentity`: issued token reaches `/api/me` as the existing `AuthenticatedUser` shape without credential leakage; update the stored role and assert the same token gets the updated authorities on its next request; deactivate/delete the account after issuance and assert 401 on its next request. Verify no HTTP session is created. Migrate existing Basic-auth test helpers and requests throughout the test suite to obtain JWTs through the real token endpoint, retaining every role/inactive/ownership assertion. Add `basicOnlyProtectedRequestsAreRejected`: valid username/password in a Basic header returns 401 on protected routes. No Basic fallback remains.
- [ ] Add `invalidBearerTokensReturnUnauthenticated`: parameterize malformed/tampered signatures, expired token, future `nbf` when present, wrong/missing issuer, wrong/missing audience, unsigned token, unsupported algorithm, missing/invalid UUID subject, missing expiration, missing/invalid/future issued-at and expiry not after issuance. Each returns the existing 401 envelope, not 500. A valid bearer identity with the wrong role returns the existing 403 envelope. Use deterministic test clocks for expiry boundaries and require `exp` later than the validation instant (zero expiry leeway).
- [ ] Add `jwtConfigurationFailsClosed`: context startup fails clearly for absent, blank, malformed base64 or fewer-than-32-decoded-byte secrets; a correctly encoded 32-byte key starts. Error output must not disclose the secret. Keep one explicitly test-only fixed secret in `application-test.properties`; configuration-failure tests supply their own property sets so the test fixture cannot mask missing production configuration.
- [ ] Extend `openApiDeclaresResourceTags` into `openApiDeclaresBearerSecurity`: sole authentication scheme `bearerAuth` (HTTP/bearer, bearerFormat JWT), security array `[{"bearerAuth":[]}]`; assert `basicAuth` is absent. Token POST has explicit empty security and `Authentication` tag; all protected operations inherit or declare the bearer requirement. Assert Swagger/API docs remain public, unique operation IDs, precise permission descriptions and all six prescribed tags; old URLs remain absent.
- [ ] Extend `allResourceRoutesHonorAuthenticationAndRemovedPathsStayRemoved` to run the existing complete protected route/role/ownership matrix under freshly issued bearer credentials, preserving 401 anonymous, 403 wrong-role and 404 ownership/visibility outcomes. Exclude token POST from the blanket anonymous-401 expectation and test its public exchange separately. Keep its transient 200/no-Location contract out of persisted-resource POST assertions.
- [ ] Run `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./mvnw -Dtest=JwtAuthenticationIntegrationTest,JwtConfigurationTest,SecurityIntegrationTest,OpenApiContractIntegrationTest,ResourceRouteContractIntegrationTest test`; observe intended failures for absent issuance/bearer/configuration behavior before implementation.
- [ ] Inspect the local Boot-managed dependency graph and use its supported Spring Security OAuth2 resource-server/JOSE encoder and decoder dependencies; record resolved versions in the ledger. Implement HS256 signing and validation using those libraries, an explicit HS256 allowlist, required claim validators, fixed issuer/audience and required environment-backed secret. Do not hand-roll token parsing or cryptography, independently upgrade Spring Security, persist tokens or introduce a schema migration.
- [ ] Implement the DTO/controller/service/converter interfaces above. Permit only the POST token matcher before the protected `/api/**` matcher. Remove/disable `httpBasic`, add resource-server bearer support and route login and bearer failures through the existing JSON authentication/denial handlers. Keep BCrypt through the existing provider, stateless sessions and existing API CSRF treatment. Add `Cache-Control: no-store` to issuance responses. Map successful bearer authentication to `AuthenticatedUser` with current database authorities rather than Spring's default JWT principal.
- [ ] Implement final OpenAPI metadata and document the exact workflow: set `JWT_SECRET` to a freshly generated base64 key (for example `openssl rand -base64 32`), export it before startup, use an existing provisioned username/password in public POST `/api/auth/tokens`, copy only `accessToken`, paste it into Swagger Authorize → `bearerAuth`, then call `/api/me` and role-protected resources. Include curl login/bearer examples, the response shape, 30-minute expiry/relogin, JWT-only authentication, rejection of Basic-only protected calls and no default admin/secret claim. Keep Swagger persistent browser authorization disabled. `.env.example` contains only a placeholder; clarify the application requires exported variables and does not automatically load `.env`.
- [ ] Run the focused command above and `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./mvnw test` against the dedicated real test database/vhost; require zero failures/errors. Review the final complete route/authentication/OpenAPI contract and secret/claim handling, record task 3.6 commands/results in the existing ledger, update durable guidance to describe implemented behavior only after evidence passes, and commit `feat: add JWT login and Swagger bearer authorization`. Resume original task 4 only after 3.1–3.6 are complete.

## Handoff and self-review

Coverage checked: route/permission matrix (3.2–3.4), auth and mappings (3.1), scalar CRUD and transactions (3.2–3.4), hours and locks (3.3), credential rollback and busy checks (3.4), resource Swagger metadata and durable guidance (3.5), JWT issuance/validation, live principal/role lookup, final JWT-only route/Swagger audit and secret configuration (3.6). Five Review Focus conditions each have named integration assertions above. Every cross-task repository/service signature is declared before its consumer. No migration is expected; a necessary schema change must be a new forward migration with schema tests, never an edit to V1–V13.

The user has authorized execution and the faster 3.3–3.6 schedule above. Preserve the requested implementation model; do not ask the user to choose it again. Integrate and verify 3.1–3.6 in order, then continue original tasks 4–13 using their updated parallel schedule and resource conventions. This planning revision is not implementation evidence.
