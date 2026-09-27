# Development guidance

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
