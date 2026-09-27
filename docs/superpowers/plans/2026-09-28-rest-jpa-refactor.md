# Resource REST APIs and Hibernate Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Use test-driven-development before implementation and verification-before-completion before reporting success or committing.

**Goal:** Replace the implemented role-prefixed APIs and ordinary JDBC persistence with resource APIs and Hibernate while preserving authorization, schema and transactional behavior.

**Architecture:** Keep package-by-feature; controllers validate transport, services enforce role/ownership and own transactions, repositories persist entities, DTOs form the HTTP boundary. Spring Data JPA handles ordinary reads/writes; narrowly documented PostgreSQL-specific repository operations retain native SQL where necessary.

**Tech Stack:** Java 25, Spring Boot 4.1.1, Boot-managed Hibernate/Spring Data JPA, matching Hibernate Spatial, Flyway, PostgreSQL/PostGIS, Basic authentication, springdoc 3.1.0 and real database integration tests.

**Spec:** [Resource APIs and Hibernate persistence](../specs/2026-09-28-rest-jpa-design.md), with original assignment business invariants retained.

**Status:** Plan prepared for user review; no refactor implementation claimed. Execute 3.1–3.5 after review and before original task 4. Preserve original tasks 1–13 and their evidence. Planning model requested: Astra; implementation model requested: gpt-5.6-terra. The user requested normal planning speed and fast implementation speed; model tools do not expose a speed-tier control, so do not claim those speeds were set.

## Global Constraints

- Keep HTTP Basic, BCrypt, inactive-account denial and stateless session behavior.
- JSON stays camelCase; SQL columns stay snake_case with explicit entity mappings.
- Hibernate must validate schema, never create/update it. Preserve applied migrations V1–V13.
- Map DTOs within service transactions, keep open-in-view disabled, and use projections or fetch queries to avoid N+1 reads.
- No SQL in controllers, business services or authentication adapters.
- New resources return 201 with a Location header; reads/patches return 200; soft DELETE returns 204.
- Wrong-role operations return 403, unauthenticated requests 401.
- Inactive catalog and another owner's resources return 404 to callers who cannot view them.
- Resource GET filtering applies in the database before pagination and counting.
- Set `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home` for every Maven invocation.
- Use only `fooddelivery_assignment_test` and RabbitMQ vhost `fooddelivery_assignment_test` (local default port 5673); never test against `fooddelivery` or substitute H2.
- Run the task's focused command and full `./mvnw test` before its separate numbered-task commit; update `.superpowers/sdd/2026-09-28-assignment-completion/progress.md` with commands/results/commit.
- Do not add JWT, registration, seed data, new assignment workflows or a speculative schema version column.

## Review Focus

- Case-insensitive credential collisions must roll back the newly inserted partner as well as credentials (3.4).
- Filtering after pagination could disclose inactive rows or inflate totals; each role needs matching content/count predicates (3.2, 3.3).
- Geography can silently swap coordinates or use degrees; round-trip SRID/order and meter-distance assertions must use PostGIS (3.1, 3.3).
- Concurrent parent deactivation and child creation must serialize on the city row; concurrent hours updates must not lose unrelated days (3.3).
- Native writes and trigger timestamps can leave managed entities stale; reload after flush and prove audit/presence responses match stored values (3.1, 3.4).

---

## File and interface map

All Java paths below are relative to `src/main/java/com/rk/fooddelivery/`; test paths are relative to `src/test/java/com/rk/fooddelivery/`. Braced directories in this map describe structure, not additional empty packages to create.

| Feature | Files and responsibility |
| --- | --- |
| Shared identity | `auth/CurrentUser.java` remains principal authority; `auth/RoleConverter.java` maps Role to lowercase database strings |
| User persistence | `user/entity/User.java`, `UserCredential.java`; `user/repository/UserRepository.java`, `UserCredentialRepository.java`; credentials never cross HTTP boundary |
| Cities | `city/controller/CityController.java`, `city/service/CityService.java`, `city/repository/CityRepository.java`, `city/entity/City.java`, existing `city/dto/CityDtos.java` |
| Cuisines | Keep within existing restaurant feature: `restaurant/controller/CuisineController.java`, `restaurant/service/CuisineService.java`, `restaurant/repository/CuisineRepository.java`, `restaurant/entity/Cuisine.java`, `restaurant/dto/CuisineResponse.java` |
| Restaurants | `restaurant/controller/RestaurantController.java`, `OwnerRestaurantController.java`; `restaurant/service/RestaurantService.java`; `restaurant/repository/RestaurantRepository.java`, `RestaurantTimingRepository.java`; `restaurant/entity/Restaurant.java`, `RestaurantTiming.java`; existing `RestaurantDtos.java` |
| Partners | `delivery/controller/DeliveryPartnerController.java` becomes admin resource controller; new `PartnerPresenceController.java` hosts self routes; `delivery/service/DeliveryPartnerService.java`, existing `PartnerPresenceService.java`; `delivery/repository/PartnerWorkloadRepository.java` holds narrow existing-order occupancy query |
| Account/API docs | Existing `user/controller/MeController.java`; new `user/dto/MeResponse.java`; `config/OpenApiConfig.java`; `config/SecurityConfig.java` |
| Removed after migration | `admin/controller/AdminCrudController.java`, `admin/service/AdminCrudService.java`, `admin/repository/AdminRepository.java` |

Do not create a second delivery-partner entity/table: partners are `User` rows with role `DELIVERY_PARTNER`. Use scalar UUID owner/city/audit references, except a lazy credential-to-user association fetched explicitly during login. UUID IDs are application generated for new ordinary entities; credential ID is its user's UUID. Entity field access and constructors must work with Hibernate; do not use records as entities. Preserve database defaults deliberately by initializing active/online/rating fields and marking database-generated timestamps read-only, then flush/refresh when a response needs generated values. Map text columns as text, currency length 3, money precision 10/scale 2, rating precision 3/scale 2, `LocalTime` hours and `Instant` timestamptz. No cascading historical deletes.

`PageResponse` and all existing request/response DTO shapes remain unchanged, except moving nested account/cuisine response records to the named DTO files. Null PATCH fields retain their existing 'leave unchanged' meaning; trimming remains as today. Defaults: page 0, size 20, page >= 0, size 1–100. Sort in SQL/JPQL by `lower(name), id`; never sort/filter a page in memory.

## Service authorization and route contracts

Every public business service entry point below calls `CurrentUser` itself before data access. Controllers must not supply actor/owner IDs for authorization. Entity/repository helper methods do not become public HTTP entry points. Actor columns come from the authenticated principal.

| Service method / route | Permission and visibility |
| --- | --- |
| `CityService.list/get` — GET `/api/cities[/{id}]` | All authenticated; admin sees all, every other role sees active only |
| `CityService.create/patch/deactivate` — POST/PATCH/DELETE city resources | ADMIN only |
| `CuisineService.list` — GET `/api/cuisines` | All authenticated, size-limited list |
| `RestaurantService.list/get` — GET `/api/restaurants[/{id}]` | ADMIN all; RESTAURANT_OWNER own, including inactive; CUSTOMER/DELIVERY_PARTNER only active restaurants in active cities |
| `RestaurantService.mine` — GET `/api/me/restaurants` | RESTAURANT_OWNER only, same owned visibility |
| `RestaurantService.create/patch/deactivate` — restaurant mutations | ADMIN only; active owner and locked active city required on create |
| `RestaurantService.updateHours` — PATCH `/api/restaurants/{id}/hours` | RESTAURANT_OWNER only and matching owner; other owner's/missing ID => 404; admin => 403 |
| `DeliveryPartnerService` — `/api/delivery-partners[/{id}]` all verbs | ADMIN only, detail on a non-partner UUID => 404 |
| `PartnerPresenceService` — PATCH `/api/me/delivery-partner/location` or `/availability` | DELIVERY_PARTNER only; current principal ID, active account required |
| GET `/api/me` | Any authenticated role; DTO only, no password/hash |

Use HTTP Basic for all `/api/**`. Unknown legacy admin URLs must have no handler. Remove `/api/admin/**` special matcher only when all migrated service methods enforce permissions. Test removed routes as an admin: 404 (for legacy `/api/restaurants/mine`, 400 from UUID conversion is acceptable, but it must not invoke the former handler). OpenAPI contains none of the removed paths. No compatibility aliases.

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

### Task 3.5: OpenAPI security, contract audit and durable documentation

**Files:** Create `config/OpenApiConfig.java`, `user/dto/MeResponse.java`; modify all current feature controllers, `user/controller/MeController.java`, `src/main/resources/application.properties`, `README.md`, `AGENTS.md`, `docs/database.md`, `docs/tables.md` as needed for accurate implemented guidance. Test: `api/OpenApiContractIntegrationTest.java`, `api/ResourceRouteContractIntegrationTest.java`.

**Interfaces:** OpenAPI scheme name `basicAuth`, type HTTP, scheme basic, top-level security requirement `basicAuth: []`. Tags exactly `Account`, `Cities`, `Cuisines`, `Restaurants`, `Delivery Partners`; no fallback controller tags. Every operation has an explicit unique operationId, readable summary and role/visibility description. Alphabetical Swagger tag and operation ordering is deterministic; `/api/me/restaurants` belongs to Restaurants and partner self routes belong to Delivery Partners.

- [ ] Add `openApiDeclaresBasicSecurityAndResourceTags`: unauthenticated `/v3/api-docs` and Swagger UI are accessible; document declares HTTP Basic and protected operation security (inherited global requirement acceptable); every current operation uses only prescribed tags with non-empty permission text and globally unique operationId. Assert old admin/mine/presence paths absent.
- [ ] Add `allResourceRoutesHonorAuthenticationAndRemovedPathsStayRemoved`: parameterized route/HTTP method cases for every current operation, 401 anonymous, expected role matrix; admin requests to removed routes find no handler. Assert collection POST Location resolves through GET and no DTO exposes password, passwordHash or credential association. Assert invalid page/size contract at each paged controller.
- [ ] Run `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./mvnw -Dtest=OpenApiContractIntegrationTest,ResourceRouteContractIntegrationTest,SecurityIntegrationTest test`; confirm missing documentation assertions fail.
- [ ] Implement OpenAPI metadata and deterministic UI sorters, move MeResponse to DTO package and preserve its JSON. Document Basic Authorize usage using existing provisioned/test accounts; no default credentials or demo seed claimed.
- [ ] Update README's complete old-to-new route table, folder map, service ownership rules and JPA/native boundaries. Record actual spatial approach and any narrow native exceptions. Mark spec/plan implemented only after all tasks' verification evidence exists; original tasks 4–13 remain pending. Keep original ledger task history intact.
- [ ] Inspect production sources for residual `JdbcTemplate`, native SQL, AdminCrud references, `/api/admin` and old self URLs; every surviving native method must have a specific PostgreSQL reason in its feature repository. Inspect entity mappings and confirm V1–V13 unchanged with git diff. Run focused command and full suite per protocol; commit `docs: document and verify resource APIs and Hibernate boundaries`.

## Handoff and self-review

Coverage checked: route/permission matrix (3.2–3.4), auth and mappings (3.1), scalar CRUD and transactions (3.2–3.4), hours and locks (3.3), credential rollback and busy checks (3.4), Swagger and durable guidance (3.5). Five Review Focus conditions each have named integration assertions above. Every cross-task repository/service signature is declared before its consumer. No migration is expected; a necessary schema change must be a new forward migration with schema tests, never an edit to V1–V13.

This is a planning deliverable, pending user review under the writing-plans skill's execution handoff. Preserve the already requested implementation model; do not ask the user to choose it again. After review, execute 3.1–3.5, then resume original task 4 using the resource route conventions in the spec and updated original plan.
