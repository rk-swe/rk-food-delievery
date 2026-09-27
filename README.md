# Food Delivery Assignment

## Architecture revision

The application exposes resource-based APIs backed by Hibernate. Catalog,
restaurant, delivery-partner and account APIs use feature-owned controllers,
services and repositories. Protected `/api/**` routes require a JWT bearer
token; HTTP Basic is rejected. The old `/api/admin/**` routes are removed.

The target Java layout groups code by feature:

```text
com.rk.fooddelivery/
  auth/          Authentication and current principal
  user/          User and credential entities/repositories; account API
  city/          City controller, service, repository, entity and DTOs
  restaurant/    Restaurant, cuisine and opening-hours components
  delivery/      Partner administration and self-presence components
  config/        Security and OpenAPI configuration
  common/        Shared errors and pagination
```

Feature packages contain `controller`, `service`, `repository`, `entity` and
`dto` subpackages where needed. The refactor replaces the combined admin CRUD
classes with feature-owned components. It remains one application and database.

## Authentication and Swagger

Set a base64-encoded secret containing at least 32 random bytes before starting
the application:

```sh
export JWT_SECRET="$(openssl rand -base64 32)"
```

Exchange an existing username and password for a 30-minute access token, then
send only that token as a bearer credential:

```sh
TOKEN=$(curl -sS -X POST http://localhost:8080/api/auth/tokens \
  -H 'Content-Type: application/json' \
  -d '{"username":"your-user","password":"your-password"}' | jq -r .accessToken)
curl http://localhost:8080/api/me -H "Authorization: Bearer $TOKEN"
```

Open Swagger UI at `/swagger-ui/index.html`, use **Authorize**, choose
`bearerAuth`, and paste the raw `accessToken`. Tokens expire after 30 minutes;
there is no refresh endpoint. The application does not load `.env` files and
ships no default secret or account.

## Local verification

Use Java 25 and the dedicated local PostGIS database only. Do not point these
commands at the existing `fooddelivery` database.

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home
export TEST_DB_URL=jdbc:postgresql://localhost:5432/fooddelivery_assignment_test
export TEST_DB_USERNAME=abcom
export TEST_DB_PASSWORD=''
./mvnw test
./mvnw -Dtest=SchemaMigrationTests,ApiErrorContractTest test
```

Integration tests use the `test` profile, a bounded Hikari pool, and the
`fooddelivery_assignment_test` RabbitMQ vhost on `localhost:5673`. Create the
vhost once in the local broker container before running broker-backed tests:

```sh
docker exec rk-assignment-rabbit rabbitmqctl add_vhost fooddelivery_assignment_test
docker exec rk-assignment-rabbit rabbitmqctl set_permissions -p fooddelivery_assignment_test guest '.*' '.*' '.*'
```

Override `TEST_DB_URL`, `TEST_DB_USERNAME`, `TEST_DB_PASSWORD`,
`RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`, or
`RABBITMQ_VHOST` when the dedicated test services run elsewhere.

## API errors

Validation failures return HTTP 400 with `VALIDATION_FAILED`, a stable message,
`requestId`, and `fieldErrors`. Domain conflicts return HTTP 409 with
`CONFLICT` and `requestId`; responses do not expose database or SQL exception
details.

## Catalog discovery and menu management

`GET /api/restaurants` supports `name`, `cityId`, repeated/comma-separated
`cuisineIds` (match any), `dietType`, `minCostForTwo`, `maxCostForTwo`, and
`radiusMeters`. The existing `cuisine` name-substring filter is also supported.
`sort` accepts `name` (default), `rating` (descending by default), `cost,asc`,
`cost,desc`, or `distance` (ascending). Name/rating accept explicit `asc` or
`desc`. Every ordering ends with the restaurant UUID to stabilize pagination.
Distance/radius require both `latitude` and `longitude`, or the authenticated
customer's stored location. Coordinates are decimal degrees and radius is meters.
Admins can inspect inactive restaurants; owners see only their own restaurants,
including inactive ones; customers and partners see active restaurants in active
cities. Result and count queries enforce the same visibility.

`GET /api/restaurants/{id}/menu-items` supports `name`, `categoryId`, `dietType`,
`minPrice`, `maxPrice`, and `available`. `sort` accepts `price`, `rating`, or
`displayOrder` (default), optionally followed by `,asc` or `,desc`; rating defaults
to descending. UUIDs break ties. Public callers cannot expose unavailable items
using `available=false`; owners and admins may inspect them. Collection endpoints
accept `page` (default 0) and `size` (default 20, maximum 100).

Owners create/patch categories and items under their restaurant and adjust stock
with `POST /api/menu-items/{id}/stock-adjustments` using a nonzero integer `delta`.
Item DELETE deactivates the item. Category DELETE requires no active items and
soft-deactivates the category, preserving historical item references. Inactive
categories are hidden from public listings and reject new items and item
reactivation. Category counts track available items and update transactionally
when an item moves or changes availability. Duplicate category names return 409.
