# Food Delivery Assignment

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
