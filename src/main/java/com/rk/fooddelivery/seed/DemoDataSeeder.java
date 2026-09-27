package com.rk.fooddelivery.seed;

import com.rk.fooddelivery.auth.Role;
import com.rk.fooddelivery.user.entity.User;
import com.rk.fooddelivery.user.repository.UserRepository;
import java.util.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("demo")
public class DemoDataSeeder implements CommandLineRunner {
  private final UserRepository users;

  public DemoDataSeeder(UserRepository users) {
    this.users = users;
  }

  public void run(String... args) {
    seed("00000000-0000-0000-0000-000000000001", "Demo Admin", "admin@demo.local", Role.ADMIN);
    seed(
        "00000000-0000-0000-0000-000000000002",
        "Demo Owner",
        "owner@demo.local",
        Role.RESTAURANT_OWNER);
    seed(
        "00000000-0000-0000-0000-000000000003",
        "Demo Customer One",
        "customer1@demo.local",
        Role.CUSTOMER);
    seed(
        "00000000-0000-0000-0000-000000000004",
        "Demo Customer Two",
        "customer2@demo.local",
        Role.CUSTOMER);
    seed(
        "00000000-0000-0000-0000-000000000005",
        "Demo Partner Near",
        "partner1@demo.local",
        Role.DELIVERY_PARTNER);
    seed(
        "00000000-0000-0000-0000-000000000006",
        "Demo Partner Far",
        "partner2@demo.local",
        Role.DELIVERY_PARTNER);
  }

  private void seed(String id, String name, String email, Role role) {
    UUID uuid = UUID.fromString(id);
    if (!users.existsById(uuid))
      users.save(new User(uuid, name, email, "+910000" + id.substring(id.length() - 4), role));
  }
}
