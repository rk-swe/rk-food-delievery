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
