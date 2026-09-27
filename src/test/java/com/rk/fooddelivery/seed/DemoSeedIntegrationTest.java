package com.rk.fooddelivery.seed;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles({"test", "demo"})
class DemoSeedIntegrationTest {

  @Autowired JdbcTemplate jdbc;
  @Autowired PasswordEncoder passwords;

  @Test
  void demoProfileSeedsUsableAccountsAndABroadCatalog() {
    assertThat(count("users")).isGreaterThanOrEqualTo(10);
    assertThat(count("user_credentials")).isGreaterThanOrEqualTo(10);
    assertThat(count("restaurants")).isGreaterThanOrEqualTo(4);
    assertThat(count("menu_items")).isGreaterThanOrEqualTo(11);
    String hash =
        jdbc.queryForObject(
            "SELECT password_hash FROM user_credentials WHERE username = 'customer1@demo.local'",
            String.class);
    assertThat(passwords.matches("DemoPass!2026", hash)).isTrue();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM users WHERE role='delivery_partner' AND online",
                Integer.class))
        .isGreaterThanOrEqualTo(3);
  }

  @AfterEach
  void cleanup() {
    jdbc.execute(
        """
        TRUNCATE TABLE notification_deliveries, inbox_entries, outbox_events,
        payment_refunds, payment_webhook_receipts, idempotency_records, delivery_offers,
        order_items, payments, orders, cart_items, carts, menu_items, menu_categories,
        restaurant_cuisines, restaurant_timings, restaurants, cuisines, coupons, cities, users
        RESTART IDENTITY CASCADE
        """);
  }

  private int count(String table) {
    return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
  }
}
