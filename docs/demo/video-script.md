# Demo run

1. Start the application with the `demo` profile and open `/swagger-ui.html`.
2. Show the seeded accounts and credentials from `docs/demo/README.md`; issue a customer JWT with
   `customer1@demo.local` and `DemoPass!2026`.
3. Paste the token into Swagger bearer authorization, list restaurants, then open Spice Route's menu.
4. Add Paneer Tikka to the cart, read the cart version, then submit checkout with an idempotency key.
5. Issue owner and partner tokens to demonstrate the restaurant decision, preparation, readiness,
   delivery-offer acceptance, pickup, delivery, and customer review endpoints.
6. Run `scripts/demo/assert_invariants.sql` against the demo database after the flow.
