# Development guidance

## Requested REST and Hibernate revision

The user requested resource-oriented REST APIs and Hibernate wherever practical.
Read `docs/superpowers/specs/2026-09-28-rest-jpa-design.md` before further feature
work. It records the proposed route/authorization matrix, JPA/native-query
boundaries and verification requirements. The Astra-authored implementation plan
is `docs/superpowers/plans/2026-09-28-rest-jpa-refactor.md`, tasks 3.1–3.5.
The design and plan are pending user review; do not represent them as implemented
or continue task 4 under the old conventions. After review, implement these
prerequisite tasks first and record their evidence in the existing ledger.

For this revision the user selected `gpt-6-astra` for planning and
`gpt-5.6-terra` for implementation, requesting normal and fast mode respectively.
Preserve those model choices. Apply speed modes only when the execution tool
actually exposes them; do not equate reasoning effort with speed/service tier
or claim an unavailable mode was enabled.

Organize code by feature with controller, service, repository, entity and dto
packages where needed. Replace combined admin CRUD classes with feature-owned
components; admin is an authorization role, not a persistence boundary.

Use resource-oriented Swagger tags, explicit role/ownership enforcement and DTOs.
Prefer Hibernate/Spring Data JPA for ordinary persistence. Reserve native SQL for
documented spatial or atomic PostgreSQL operations behind feature repositories;
do not add SQL to controllers or services. Keep database columns snake_case and
Java/JSON fields camelCase. Flyway owns the schema; Hibernate validates it.

## Execution and verification

Use the approved plan at
`docs/superpowers/plans/2026-09-28-assignment-completion.md` task by task.
Use the `test-driven-development` skill before implementation and the
`verification-before-completion` skill before committing or reporting success.
For task execution, retain the plan ledger in
`.superpowers/sdd/2026-09-28-assignment-completion/progress.md`.

Set `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home`
for all Maven invocations. Tests must use the dedicated
`fooddelivery_assignment_test` PostgreSQL/PostGIS database and the local
RabbitMQ test vhost; never run assignment tests against `fooddelivery`.

Run `./mvnw test` before a task commit, plus the task's named focused test
command. Keep one numbered plan task per commit. Use real PostgreSQL/PostGIS
and RabbitMQ for integration behavior; do not substitute H2.
