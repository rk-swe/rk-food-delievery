# Food Delivery Assignment

## Architecture revision

The user requested resource-based REST APIs and Hibernate persistence. The
[revision design](docs/superpowers/specs/2026-09-28-rest-jpa-design.md) documents
the proposed URL mappings, authorization, Swagger grouping and JPA/native SQL
boundaries. Astra prepared the
[implementation plan](docs/superpowers/plans/2026-09-28-rest-jpa-refactor.md),
with tasks 3.1–3.5 preceding original task 4. Plan review and implementation are pending: the current
application still exposes the existing admin-prefixed routes and uses JDBC.
Follow [AGENTS.md](AGENTS.md) and the updated assignment plan before continuing
feature development. This notice does not claim the proposed endpoints exist.

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
