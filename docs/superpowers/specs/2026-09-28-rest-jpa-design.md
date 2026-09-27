# Resource APIs and Hibernate persistence

Status: proposed implementation design for user review. The user requested
resource-oriented REST APIs, Hibernate wherever practical, and durable guidance
for future agents. Application code has not yet been migrated.

## Scope and precedence

Apply this revision to the implemented authentication/catalog/presence features
before continuing assignment task 4. Preserve the completed-task history in the
existing ledger. Once approved, this document supersedes conflicting route and
persistence guidance in the original assignment design, plan and preflight
rulings; their business invariants remain mandatory.

The change includes resource-based controllers and Swagger tags, usable Swagger
Basic authorization, JPA persistence for ordinary CRUD and credential lookup,
updated role/ownership tests, and documentation. It does not add JWT, registration,
demo seeding, or the remaining assignment features.

## API design

Use plural resource nouns, HTTP methods for CRUD, nested resources for ownership
relationships, and authenticated identity for self resources. JSON stays camelCase;
SQL columns stay snake_case with explicit entity mappings. Roles are permissions,
not URL namespaces. Remove the old admin routes when replacing them: this is a
local assignment with no declared external clients, so the proposal does not
maintain duplicate legacy routes. Document the breaking mappings in README.

| Current route | Replacement | Access and behavior |
| --- | --- | --- |
| `/api/me` | `/api/me` | Authenticated account DTO; never return credential entities |
| `/api/admin/cities` and ID routes | `/api/cities` and `/api/cities/{id}` | Admin POST/PATCH/DELETE; authenticated GET; admins can inspect inactive cities, other roles see only active cities |
| `/api/admin/restaurants` and ID routes | `/api/restaurants` and `/api/restaurants/{id}` | Admin CRUD; owner GET is restricted to their restaurants; customer/partner GET sees active restaurants in active cities |
| `/api/restaurants/mine` | `/api/me/restaurants` | Owner-only collection, identity derived from principal |
| `/api/restaurants/{id}/hours` | Same route | Owner-only PATCH for that restaurant; keep existing partial-day update semantics |
| `/api/admin/delivery-partners` and ID routes | `/api/delivery-partners` and `/api/delivery-partners/{id}` | Admin-only CRUD, including list/detail; never expose a partner directory to customers |
| `/api/delivery-partners/me/location` | `/api/me/delivery-partner/location` | Delivery partner PATCH of own location |
| `/api/delivery-partners/me/availability` | `/api/me/delivery-partner/availability` | Delivery partner PATCH of own availability |
| `/api/cuisines` | Same route | Authenticated GET |

Resource GET filtering applies in the database before pagination and counting.
Inactive catalog and another owner's resources return 404 to callers who cannot
view them. Wrong-role operations return 403, unauthenticated requests 401. Keep
existing validation and conflict response contracts. New resources return 201
with a Location header; reads/patches return 200; soft DELETE returns 204.
Preserve active-city and busy-partner deactivation checks and historical rows.

Split the combined admin controller into feature controllers. Services enforce
roles and ownership so removing `/api/admin/**` cannot silently remove admin
protection. All `/api/**` routes still require authentication. Keep HTTP Basic,
BCrypt, inactive-account denial and stateless session behavior.

Swagger tags: Account, Cities, Cuisines, Restaurants, Delivery Partners. Give
each operation a readable summary and permission description. Declare HTTP Basic
as an OpenAPI security scheme and attach the security requirement to protected
operations. Keep Swagger and OpenAPI documents accessible without credentials.
Use deterministic tag/operation ordering and verify there are no controller-name
fallback tags or duplicate operation IDs.

## Hibernate design

Prefer Spring Data JPA repositories backed by Hibernate for entity lookup,
credential lookup, ordinary inserts/updates, pagination, existence checks and
relationships. Move persistence into feature repositories; remove the shared
AdminRepository/AdminCrudService structure as responsibilities move into city,
restaurant, user/credential, cuisine and delivery feature services.

Map users, user_credentials, cities, cuisines, restaurants and restaurant_timings
to entities. Keep DTOs separate. Map existing UUID keys, numeric precision,
nullable columns, database defaults, audit actor columns and trigger-managed
timestamps deliberately. Do not introduce cascading deletes of historical data,
or an entity version column without a forward Flyway migration. Hibernate must
validate schema, never create/update it. Preserve applied migrations V1–V13.

Use lazy associations only where useful; UUID references are acceptable when an
object relationship adds no value. Map DTOs within service transactions, keep
open-in-view disabled, and use projections or fetch queries to avoid N+1 reads.
Use JPQL/derived queries for ordinary filters and counts. Preserve deterministic
case-insensitive name ordering with UUID tiebreakers and page limits.

For spatial storage, prefer Hibernate Spatial with a PostGIS geography mapping
if compatible with the project's Hibernate version. Prove SRID 4326, longitude /
latitude order, nullable user location and meter-based distance semantics with
real PostGIS tests. If that mapping is impractical, use a documented custom
repository with native PostGIS queries for spatial columns; ordinary scalar CRUD
must still use JPA. Avoid using DTOs as persistence entities.

Native SQL remains appropriate for ST_DWithin/distance queries, partial-index
operations, conflict-safe upserts, SKIP LOCKED outbox polling and guarded stock /
assignment updates. Prefer native queries behind JPA custom repositories; retain
JdbcTemplate only for an explicit PostgreSQL-specific need, documented at the
method. No SQL in controllers, business services or authentication adapters.

Service transactions remain the business boundary. Preserve row lock order and
use JPA pessimistic locks for ordinary entity locking. When mixing entity writes
with native queries, flush before dependent SQL and refresh/clear affected
managed state before reusing it. Use one Spring-managed database transaction.
Flush writes inside the intended exception-translation boundary so uniqueness
violations still become 409; never catch a constraint violation and continue
using an aborted transaction. Database constraints remain authoritative.

## Guidance for subsequent assignment tasks

Do not introduce new role-prefixed routes. Owner order lists belong at
`GET /api/restaurants/{restaurantId}/orders` with ownership checks; customer
history remains `GET /api/orders` scoped to the authenticated customer.
Future workflow operations should model business resources, not generic writes
to unrestricted order status: `POST /api/orders/{id}/restaurant-decisions`,
`POST /api/orders/{id}/preparation`, `POST /api/orders/{id}/readiness`,
`POST /api/orders/{id}/assignment-attempts`,
`POST /api/delivery-offers/{id}/acceptances`,
`POST /api/orders/{id}/pickup`, `POST /api/orders/{id}/delivery`, and
`POST /api/orders/{id}/reviews`. These supersede planned accept/reject/ready/
retry-assignment/deliver action URLs without changing their state machines.
Restaurant decisions use a validated accept/reject DTO, including rejection
reason. Retain actor/resource-scoped Idempotency-Key and offer-round checks.
Mock provider webhook/demo operations retain their separate authentication and
profile rules; they must not become unrestricted status-update endpoints.

## Alternatives and verification

Recommended: resource routes plus JPA by default, with narrow native exceptions.
A Swagger-only rename would leave the requested URL and persistence changes
undone. Forcing all spatial/concurrency operations through entity read-modify-save
would obscure the atomic SQL required by the assignment.

Implementation must first add failing tests for the new route/role matrix,
removed routes, Swagger security and tags, pagination visibility and JPA behavior.
Retain existing schema, Basic-auth, ownership, soft-delete and conflict tests.
Add meaningful regression coverage for credential atomicity, timestamp/audit
mapping, hours upsert, location coordinates and transaction rollback. Exercise
the city-create/deactivate and partner-busy checks with real database locks.

After spec approval, revise the existing implementation plan with separately
numbered prerequisite tasks before task 4, preserving original task numbers and
recording each task in the existing ledger. Each task must run its focused tests
and the full `./mvnw test` before committing, with the prescribed JAVA_HOME and
dedicated PostgreSQL/PostGIS database and RabbitMQ vhost. Do not claim the
refactor is implemented based only on these documentation changes.
