# Demo API walkthrough

Run the application with the `demo` profile. It seeds repeatable data in the configured database and
does not run unless that profile is explicit:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/fooddelivery_assignment_test
export DB_USERNAME=abcom
export DB_PASSWORD=''
export SPRING_RABBITMQ_HOST=localhost
export SPRING_RABBITMQ_PORT=5673
export SPRING_RABBITMQ_VIRTUAL_HOST=fooddelivery_assignment_test
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

For a real development database, set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, RabbitMQ settings and a
new base64-encoded `JWT_SECRET` with at least 32 decoded random bytes. The checked-in demo JWT secret
is only for the `demo` profile and must never be used outside a local demo.

Every seeded account uses `DemoPass!2026`. Login names are email addresses:

| Role | Email |
| --- | --- |
| Admin | `admin@demo.local` |
| Restaurant owner | `owner1@demo.local`, `owner2@demo.local` |
| Customer | `customer1@demo.local`, `customer2@demo.local`, `customer3@demo.local` |
| Online delivery partner | `partner1@demo.local`, `partner2@demo.local`, `partner4@demo.local` |
| Offline delivery partner | `partner3@demo.local` |

`POST /api/auth/tokens` accepts `{"email":"customer1@demo.local","password":"DemoPass!2026"}`.
The legacy `username` JSON field remains an alias while existing clients migrate. Passwords are never
stored in `users`: the dedicated `user_credentials.password_hash` column stores a BCrypt hash, which
includes a random salt. There is deliberately no registration endpoint in this assignment; accounts
are provisioned by an administrator or seeded for a demo.

The returned JWT is HS256-signed. Its payload has only `sub` (the user UUID), `iss` (`fooddelivery`),
`aud` (`fooddelivery-api`), `iat`, and `exp`. Roles and passwords are excluded; the application loads
the current active user and role from the database on every bearer request. Tokens expire exactly 30
minutes (1,800 seconds) after issuance. Paste the raw `accessToken` into Swagger's bearer authorization
or send `Authorization: Bearer <accessToken>`.

The four seeded restaurants have stable IDs `...0301` through `...0304`; the eleven menu items are
`...0401` through `...0411`. Use [fooddelivery.http](fooddelivery.http) for a copyable video-demo flow.
