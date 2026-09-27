# Development guidance

Current handoff: prerequisites 3.1–3.6 and tasks 4–6 are already committed to
main through `ac0db5e`. Do not repeat them. Remaining tasks 7–13 use the
implementation-first exception under "Execution and verification" below.

## Requested REST and Hibernate revision

The user requested resource-oriented REST APIs and Hibernate wherever practical.
Read `docs/superpowers/specs/2026-09-28-rest-jpa-design.md` before further feature
work. It records the proposed route/authorization matrix, JPA/native-query
boundaries and verification requirements. The Astra-authored implementation plan
is `docs/superpowers/plans/2026-09-28-rest-jpa-refactor.md`, tasks 3.1–3.6.
The revision includes POST `/api/auth/tokens`, 30-minute JWTs and Swagger bearer
authorization with JWT-only protected APIs, replacing Basic authentication (task 3.6).
The user has authorized this revision. Implement prerequisite tasks 3.1–3.6
immediately, integrating and verifying in order, and record their evidence in
the existing ledger. Follow the refactor plan's faster 3.3–3.6 schedule:
independent restaurant/partner implementation and JWT preparation may overlap
in isolated worktrees, with shared-file edits and database tests serialized. Do not
continue task 4 under the old conventions. Once task 3.6 is complete and
verified, continue the remaining assignment tasks through task 13 without
waiting for a further review checkpoint.

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
For remaining tasks 7–13, the user's latest instruction is implementation first,
then write tests and fix all failures near the end. Follow the plan's
"Implementation-first override for tasks 7–13" ahead of older execution rules.
This is an assignment-scoped exception to skill defaults: no mandatory TDD/RED
cycle, per-task reviewer, repeated planning/approval, or full test suite before
each implementation commit. Do not rewrite the shared skill files.

Use gpt-5.6-terra for implementation after the user switches models and resumes.
Default to one continuous inline implementer in the main checkout; the user
authorized incremental commits to main. Preserve unrelated staged and unstaged
changes, and stage only owned files. Independent lanes remain optional (8/9
alongside 10 after task 7); use them only when they reduce elapsed time. Use
isolated worktrees only for concurrent lanes, with one coordinator owning shared
contracts, migrations and integration. Do not spawn per-task implementers or
reviewers by default, or automatically escalate models on routine fixes.

Compile after integrating each numbered task, commit it to main, and record
"implemented; verification pending" in the ledger. Keep one numbered task per
implementation commit. Write the required tests after the implementation pass,
run the named coverage and full final verification, and fix failures before
claiming completion. The verification-before-completion skill still governs
verified/passing/completed claims; compilation is not behavioral verification.
Perform one consolidated self-review during final validation, not review loops.
Keep all REST/Hibernate/JWT, atomicity, ownership and recovery requirements.
Serialize all tests using the shared assignment database/vhost.
For task execution, retain the plan ledger in
`.superpowers/sdd/2026-09-28-assignment-completion/progress.md`.

Set `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home`
for all Maven invocations. Tests must use the dedicated
`fooddelivery_assignment_test` PostgreSQL/PostGIS database and the local
RabbitMQ test vhost; never run assignment tests against `fooddelivery`.

For tasks 7–13 run `./mvnw -DskipTests compile` before implementation commits.
At the final validation phase, run all required named tests (they may be batched),
then `./mvnw verify` without skipping tests; it includes the full test lifecycle
and formatting checks, so an additional unchanged `./mvnw test` is unnecessary.
Fix failures and rerun affected tests, then verify the final tree. Use real
PostgreSQL/PostGIS and RabbitMQ; do not substitute H2 or weaken assertions.
