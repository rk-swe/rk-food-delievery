# Food Delivery Assignment — Evaluator Guide

Thank you for reviewing this assignment. Follow the steps below from the repository root to configure the application, explore the APIs, load sample data, and review the test coverage.

## 1. Fill in `.env`

Copy the example if you do not already have a local `.env` file:

```bash
cp .env.example .env
```

Fill in every setting using `.env.example` as the template:

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`: your PostgreSQL/PostGIS database connection.
- `JWT_SECRET`: a Base64-encoded secret containing at least 32 random bytes. Generate a value with `openssl rand -base64 32` and paste it into `.env`.
- `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`, `RABBITMQ_VHOST`: your RabbitMQ connection.

You need Java 25, a running PostgreSQL database with PostGIS available, and RabbitMQ. Set `JAVA_HOME` to your Java 25 installation. Flyway applies the database migrations on startup.

For the supplied local setup, `rk-assignment-rabbit` exposes AMQP on `localhost:5673`; its management UI is at [http://localhost:15673](http://localhost:15673), with local credentials `guest` / `guest`. Change the example values to match your machine. Keep `.env` private; Git ignores it.

## 2. Run the application

If your VS Code launch configuration loads `.env`, run using that configuration. For a terminal launch, export the file's values first—the application does not automatically read `.env`:

```bash
set -a
source .env
set +a
./mvnw spring-boot:run
```

The application starts on port 8080. Keep the same JWT secret across restarts to preserve the validity of unexpired tokens.

## 3. Open Swagger

- [Swagger UI](http://localhost:8080/swagger-ui/index.html)
- [OpenAPI JSON](http://localhost:8080/v3/api-docs)

After loading the sample accounts in the next step, call `POST /api/auth/tokens` with a demo username and password. Copy the returned `accessToken`, click **Authorize** in Swagger, and paste the raw token into `bearerAuth`. Protected APIs require this token; it expires after 30 minutes.

## 4. Load lookup and sample data

Stop the running application, then start it with the `demo` profile in the same configured terminal:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

This starts the application and loads cities, cuisines, restaurants, menu categories, menu items, and demo accounts into the database specified by `DB_URL`. It is not a separate seed-only command. Existing seeded records are retained on subsequent runs.

All demo accounts use the password `DemoPass!2026`:

| Role             | Example username       |
| ---------------- | ---------------------- |
| Admin            | `admin@demo.local`     |
| Restaurant owner | `owner1@demo.local`    |
| Customer         | `customer1@demo.local` |
| Delivery partner | `partner1@demo.local`  |

Example request body for `POST /api/auth/tokens`:

```json
{ "username": "customer1@demo.local", "password": "DemoPass!2026" }
```

## 5. Verify the three concurrency scenarios

The required concurrency checks are:

| Scenario                                                | Expected result                                                                         | Current automated coverage                                                                           |
| ------------------------------------------------------- | --------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------- |
| Multiple customers check out the same limited stock     | With 20 simultaneous checkouts and stock 5: exactly 5 orders, 15 conflicts, and stock 0 | `CheckoutConcurrencyTest.twentySimultaneousCheckoutsAgainstStockFiveCreateOnlyFiveOrders`            |
| Two partners accept the same order simultaneously       | Exactly one assignment succeeds; the other request receives a conflict                  | `DeliveryAssignmentConcurrencyTest.simultaneousAcceptancesForOneOrderLeaveExactlyOneAssignedPartner` |
| One partner accepts two different orders simultaneously | Only one order is assigned to that partner; the other remains unassigned                | `DeliveryAssignmentConcurrencyTest.simultaneousAcceptancesForTwoOrdersLeavePartnerAssignedToOnlyOne` |

Each test uses independent HTTP requests on concurrent threads, synchronized by a start barrier, against the real PostgreSQL/PostGIS database and RabbitMQ-backed Spring application.

Tests require the dedicated `fooddelivery_assignment_test` PostgreSQL/PostGIS database and RabbitMQ vhost. They clear test data; do not point them at your application database. Create the test database with your PostgreSQL tooling and ensure its user can run the PostGIS migrations.

For the local RabbitMQ container, create the test vhost once if it does not exist:

```bash
docker exec rk-assignment-rabbit rabbitmqctl add_vhost fooddelivery_assignment_test
docker exec rk-assignment-rabbit rabbitmqctl set_permissions -p fooddelivery_assignment_test guest '.*' '.*' '.*'
```

In a separate terminal, set Java 25 and the dedicated test connection values. Replace the database username and password below with your local credentials:

```bash
export TEST_DB_URL=jdbc:postgresql://localhost:5432/fooddelivery_assignment_test
export TEST_DB_USERNAME=abcom
export TEST_DB_PASSWORD=''
export RABBITMQ_HOST=localhost
export RABBITMQ_PORT=5673
export RABBITMQ_USERNAME=guest
export RABBITMQ_PASSWORD=guest
export RABBITMQ_VHOST=fooddelivery_assignment_test

# Run all three concurrency scenarios
./mvnw -Dtest=CheckoutConcurrencyTest,DeliveryAssignmentConcurrencyTest test
```

Run the full existing test lifecycle and formatting checks with:

```bash
./mvnw verify
```

Run test commands sequentially because they share the dedicated database and vhost.
