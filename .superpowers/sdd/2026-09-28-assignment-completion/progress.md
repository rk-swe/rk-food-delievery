# SDD ledger — plan: docs/superpowers/plans/2026-09-28-assignment-completion.md

Task 1: baseline `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home DB_URL=jdbc:postgresql://localhost:5432/fooddelivery_assignment_test DB_USERNAME=abcom DB_PASSWORD='' ./mvnw test` → 26 tests, 0 failures/errors.
Task 1: Ruling: task-start/task-done scripts are not executable in the checked-in skill copy, and their task extractor expects `Task N` headings while this approved plan uses numbered list headings — recorded task evidence manually; the implementation and verification sequence remains unchanged.
Task 1: Ruling: all Spring integration tests activate the `test` profile, whose datasource defaults only to `fooddelivery_assignment_test`; it does not fall back to `DB_URL`. Reusable destructive cleanup refuses any database whose name does not end with `_assignment_test`.
Task 1: RED observed: ApiErrorContractTest failed because the existing response exposed `error` and omitted the required `code` and `requestId` fields.
Task 1: verification before commit: `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./mvnw -Dtest=SchemaMigrationTests,ApiErrorContractTest test` → 27 tests, 0 failures/errors; `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./mvnw test` → 28 tests, 0 failures/errors.
Task 1: complete (commits e468b24..33fe759, tests: JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./mvnw -Dtest=SchemaMigrationTests,ApiErrorContractTest test → 27 tests, 0 failures/errors)

Task 2: RED observed: SecurityIntegrationTest failed because no PasswordEncoder or credential-backed authentication configuration existed.
Task 2: added V12 credentials/activity migration, BCrypt HTTP Basic database principal lookup, stateless `/api/**` security with API-scoped CSRF exemption, CurrentUser role/id contract, and authenticated `/api/me`. Security errors use the shared status/code/message/fieldErrors/requestId response shape.
Task 2: verification before commit: `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./mvnw -Dtest=SecurityIntegrationTest,SchemaMigrationTests test` → 32 tests, 0 failures/errors; `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./mvnw test` → 35 tests, 0 failures/errors.
