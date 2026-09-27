package com.rk.fooddelivery.seed;

import com.rk.fooddelivery.auth.Role;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("demo")
public class DemoDataSeeder implements CommandLineRunner {
  /**
   * This profile is deliberately deterministic: it is safe to restart and gives a video demo stable
   * identifiers, inventory and accounts. It is never enabled by the normal application profile.
   */
  private final JdbcTemplate jdbc;

  private final PasswordEncoder passwords;

  public DemoDataSeeder(JdbcTemplate jdbc, PasswordEncoder passwords) {
    this.jdbc = jdbc;
    this.passwords = passwords;
  }

  @Transactional
  public void run(String... args) {
    seedUsers();
    seedCatalog();
  }

  private void seedUsers() {
    String passwordHash = passwords.encode("DemoPass!2026");
    user("001", "Demo Admin", "admin@demo.local", Role.ADMIN, false, null, passwordHash);
    user(
        "002",
        "Demo Owner One",
        "owner1@demo.local",
        Role.RESTAURANT_OWNER,
        false,
        null,
        passwordHash);
    user(
        "003",
        "Demo Owner Two",
        "owner2@demo.local",
        Role.RESTAURANT_OWNER,
        false,
        null,
        passwordHash);
    user(
        "004",
        "Demo Customer One",
        "customer1@demo.local",
        Role.CUSTOMER,
        false,
        point(73.8567, 18.5204),
        passwordHash);
    user(
        "005",
        "Demo Customer Two",
        "customer2@demo.local",
        Role.CUSTOMER,
        false,
        point(73.8610, 18.5250),
        passwordHash);
    user(
        "006",
        "Demo Customer Three",
        "customer3@demo.local",
        Role.CUSTOMER,
        false,
        point(73.8500, 18.5150),
        passwordHash);
    user(
        "007",
        "Demo Partner Near",
        "partner1@demo.local",
        Role.DELIVERY_PARTNER,
        true,
        point(73.8570, 18.5210),
        passwordHash);
    user(
        "008",
        "Demo Partner West",
        "partner2@demo.local",
        Role.DELIVERY_PARTNER,
        true,
        point(73.8480, 18.5180),
        passwordHash);
    user(
        "009",
        "Demo Partner Offline",
        "partner3@demo.local",
        Role.DELIVERY_PARTNER,
        false,
        point(73.8300, 18.5000),
        passwordHash);
    user(
        "010",
        "Demo Partner Mumbai",
        "partner4@demo.local",
        Role.DELIVERY_PARTNER,
        true,
        point(72.8777, 19.0760),
        passwordHash);
  }

  private void user(
      String suffix,
      String name,
      String email,
      Role role,
      boolean online,
      String location,
      String passwordHash) {
    UUID id = id(suffix);
    if (location == null) {
      jdbc.update(
          """
          INSERT INTO users (id,name,email,phone_number,role,online)
          VALUES (?,?,?,?,?,?) ON CONFLICT (id) DO NOTHING
          """,
          id,
          name,
          email,
          "+91990000" + suffix,
          role.databaseValue(),
          online);
    } else {
      jdbc.update(
          """
          INSERT INTO users (id,name,email,phone_number,role,online,location,location_updated_at)
          VALUES (?,?,?,?,?,?,ST_GeogFromText(?),CURRENT_TIMESTAMP)
          ON CONFLICT (id) DO NOTHING
          """,
          id,
          name,
          email,
          "+91990000" + suffix,
          role.databaseValue(),
          online,
          location);
    }
    jdbc.update(
        """
        INSERT INTO user_credentials (user_id,username,password_hash)
        VALUES (?,?,?) ON CONFLICT (user_id) DO NOTHING
        """,
        id,
        email,
        passwordHash);
  }

  private void seedCatalog() {
    UUID admin = id("001");
    UUID pune = id("101");
    UUID mumbai = id("102");
    city(pune, "Pune", "Maharashtra", admin);
    city(mumbai, "Mumbai", "Maharashtra", admin);
    cuisine(id("201"), "Indian", admin);
    cuisine(id("202"), "Italian", admin);
    cuisine(id("203"), "Asian", admin);
    cuisine(id("204"), "Desserts", admin);
    restaurant(
        id("301"),
        "Spice Route",
        id("002"),
        pune,
        "Veg",
        "Koregaon Park",
        point(73.8567, 18.5204),
        admin);
    restaurant(
        id("302"),
        "Pasta House",
        id("002"),
        pune,
        "Non Veg",
        "Viman Nagar",
        point(73.8780, 18.5679),
        admin);
    restaurant(
        id("303"),
        "Wok & Bowl",
        id("003"),
        pune,
        "Non Veg",
        "Baner",
        point(73.7980, 18.5590),
        admin);
    restaurant(
        id("304"),
        "Bombay Bites",
        id("003"),
        mumbai,
        "Veg",
        "Bandra West",
        point(72.8420, 19.0596),
        admin);
    linkCuisine(id("301"), id("201"));
    linkCuisine(id("302"), id("202"));
    linkCuisine(id("303"), id("203"));
    linkCuisine(id("304"), id("201"));
    linkCuisine(id("304"), id("204"));
    menu(
        id("401"),
        id("301"),
        "Starters",
        "Paneer Tikka",
        "Smoky grilled paneer",
        "Veg",
        249,
        25,
        admin);
    menu(id("402"), id("301"), "Mains", "Butter Naan", "Fresh tandoor bread", "Veg", 55, 60, admin);
    menu(
        id("403"),
        id("301"),
        "Mains",
        "Dal Makhani",
        "Slow-cooked black lentils",
        "Veg",
        275,
        20,
        admin);
    menu(
        id("404"),
        id("302"),
        "Pasta",
        "Penne Arrabbiata",
        "Tomato, basil and chilli",
        "Veg",
        349,
        18,
        admin);
    menu(
        id("405"),
        id("302"),
        "Pasta",
        "Chicken Alfredo",
        "Creamy parmesan sauce",
        "Non Veg",
        429,
        15,
        admin);
    menu(
        id("406"),
        id("302"),
        "Desserts",
        "Tiramisu",
        "Coffee mascarpone dessert",
        "Veg",
        220,
        12,
        admin);
    menu(
        id("407"),
        id("303"),
        "Bowls",
        "Chicken Ramen",
        "Miso broth and noodles",
        "Non Veg",
        399,
        20,
        admin);
    menu(
        id("408"),
        id("303"),
        "Bowls",
        "Tofu Rice Bowl",
        "Sesame vegetables and tofu",
        "Veg",
        319,
        22,
        admin);
    menu(
        id("409"),
        id("303"),
        "Sides",
        "Chilli Edamame",
        "Garlic and sea salt",
        "Veg",
        179,
        30,
        admin);
    menu(
        id("410"),
        id("304"),
        "Street Food",
        "Vada Pav",
        "Mumbai potato slider",
        "Veg",
        65,
        50,
        admin);
    menu(
        id("411"),
        id("304"),
        "Desserts",
        "Gulab Jamun",
        "Warm milk dumplings",
        "Veg",
        110,
        35,
        admin);
  }

  private void city(UUID id, String name, String state, UUID actor) {
    jdbc.update(
        "INSERT INTO cities (id,name,state,country,currency,created_by,updated_by) VALUES (?,?,?,'India','INR',?,?) ON CONFLICT (id) DO NOTHING",
        id,
        name,
        state,
        actor,
        actor);
  }

  private void cuisine(UUID id, String name, UUID actor) {
    jdbc.update(
        "INSERT INTO cuisines (id,name,created_by,updated_by) VALUES (?,?,?,?) ON CONFLICT (id) DO NOTHING",
        id,
        name,
        actor,
        actor);
  }

  private void restaurant(
      UUID id,
      String name,
      UUID owner,
      UUID city,
      String diet,
      String address,
      String location,
      UUID actor) {
    jdbc.update(
        "INSERT INTO restaurants (id,name,owner_id,city_id,cost_for_two,diet_type,address_line_1,description,location,created_by,updated_by) VALUES (?,?,?,?,?,?,?,?,ST_GeogFromText(?),?,?) ON CONFLICT (id) DO NOTHING",
        id,
        name,
        owner,
        city,
        600,
        diet,
        address,
        "Demo restaurant for the API walkthrough",
        location,
        actor,
        actor);
  }

  private void linkCuisine(UUID restaurant, UUID cuisine) {
    jdbc.update(
        "INSERT INTO restaurant_cuisines (restaurant_id,cuisine_id) VALUES (?,?) ON CONFLICT (restaurant_id,cuisine_id) DO NOTHING",
        restaurant,
        cuisine);
  }

  private void menu(
      UUID item,
      UUID restaurant,
      String categoryName,
      String name,
      String description,
      String diet,
      int price,
      int stock,
      UUID actor) {
    UUID category =
        UUID.nameUUIDFromBytes((restaurant + ":" + categoryName).getBytes(StandardCharsets.UTF_8));
    jdbc.update(
        "INSERT INTO menu_categories (id,restaurant_id,name,created_by,updated_by) VALUES (?,?,?,?,?) ON CONFLICT DO NOTHING",
        category,
        restaurant,
        categoryName,
        actor,
        actor);
    UUID actualCategory =
        jdbc.queryForObject(
            "SELECT id FROM menu_categories WHERE restaurant_id=? AND lower(name)=lower(?)",
            UUID.class,
            restaurant,
            categoryName);
    jdbc.update(
        "INSERT INTO menu_items (id,restaurant_id,category_id,name,description,diet_type,price,is_available,available_quantity,created_by,updated_by) VALUES (?,?,?,?,?,?,?,true,?,?,?) ON CONFLICT (id) DO NOTHING",
        item,
        restaurant,
        actualCategory,
        name,
        description,
        diet,
        price,
        stock,
        actor,
        actor);
  }

  private static UUID id(String suffix) {
    return UUID.fromString(
        String.format("00000000-0000-0000-0000-%012d", Integer.parseInt(suffix)));
  }

  private static String point(double longitude, double latitude) {
    return "POINT(" + longitude + " " + latitude + ")";
  }
}
