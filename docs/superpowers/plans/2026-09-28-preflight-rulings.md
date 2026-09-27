# Astra preflight rulings

Revision notice: the user has requested resource-oriented routes and Hibernate.
Read [the REST/JPA proposal](../specs/2026-09-28-rest-jpa-design.md) before further
implementation. Its route replacements are pending review; once approved,
assignment-attempt resources supersede the retry-assignment URL below. Locking,
idempotency and business-state rulings remain applicable.

Reviewed with `gpt-6-astra` before implementation. These resolve omissions in the
approved design/plan without expanding its scope. Execution uses `gpt-5.6-terra`.

1. Action POST contracts carry Idempotency-Key, including mock completion, owner
   lifecycle, delivery, assignment and review. Include target resource in request
   hashes. Offer acceptance also requires its round. Retry route is
   `/api/orders/{id}/retry-assignment`.
2. Persist cart version on the customer row, not a disposable cart row. Return a
   version for empty carts; increment on changes and checkout clear.
3. Distinguish locally expired payments (Failed with `expired` reason) from
   provider-declared failure. Only the former permits late provider success with
   refund and no order resurrection. Hash webhook payloads; changed content under
   the same provider event ID is a conflict.
4. Accepting an order sets assignment Searching and its round. Initial/retry
   discovery events carry round; consumers require matching Searching round,
   paid eligible order state and no partner. Delayed events cannot reopen offers.
5. Prefer deriving partner occupancy from active orders and a partial unique
   index; online preference is separate. If an explicit slot is used, release it
   only for the matching order. Replay successful idempotent requests before
   checking current lifecycle state.
6. New migrations add credentials/activity, persistent cart version, location
   freshness, order states/version/deadlines/round/stock-release/currency, item-name
   snapshots, webhook receipts/refunds, offers, idempotency, outbox/inbox and an
   exact restaurant rating sum. Active-partner uniqueness includes Ready for pickup.
7. A separate credentials table preserves existing users schema fixtures and
   prevents credentialless accounts from logging in. Keep original schema-test
   assertions; update fixture construction only where new invariants require it.
8. Multiple events may share order/version, e.g. offers to several partners.
   Deduplication uses event UUID and per-consumer inbox identity. Business effects
   and outbox are one transaction, consumer ACK follows commit. Never recover a
   PostgreSQL unique violation by continuing in an already-aborted transaction.
9. Checkout revalidates active city/restaurant, hours and availability under
   compatible catalog locks. Opening hours consider previous-day overnight slots.

The scope remains: nearby-partner broadcast/first valid acceptance, explicit
waiting states without automatic dispatch cancellation, six users, two demo
restaurants, and 13 separately verified implementation commits.

## Local execution environment

- Isolated branch: `codex/assignment-completion`.
- Java: `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home`.
- Dedicated test database: `fooddelivery_assignment_test` on localhost:5432;
  local role abcom with local trust. Do not use the existing fooddelivery database.
- Local broker container: `rk-assignment-rabbit`, AMQP localhost:5673,
  management localhost:15673, development-only guest credentials.
- Container is only a local test dependency; no application deployment work.
