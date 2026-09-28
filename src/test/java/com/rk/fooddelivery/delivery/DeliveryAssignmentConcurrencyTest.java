package com.rk.fooddelivery.delivery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.rk.fooddelivery.support.IntegrationTestSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@AutoConfigureMockMvc
class DeliveryAssignmentConcurrencyTest extends IntegrationTestSupport {

  @Autowired MockMvc mvc;
  @Autowired PasswordEncoder passwords;

  private UUID restaurant;
  private UUID customer;

  @BeforeEach
  void fixture() {
    UUID owner = user("owner@delivery-race.test", "restaurant_owner", false);
    customer = user("customer@delivery-race.test", "customer", false);
    UUID city =
        jdbc.queryForObject(
            "INSERT INTO cities (name,state,country,currency) VALUES ('Pune','MH','India','INR') RETURNING id",
            UUID.class);
    restaurant =
        jdbc.queryForObject(
            "INSERT INTO restaurants (name,owner_id,city_id,cost_for_two,diet_type,address_line_1,location) VALUES ('Race Kitchen',?,?,100,'Veg','Road',ST_GeogFromText('POINT(73.85 18.52)')) RETURNING id",
            UUID.class,
            owner,
            city);
  }

  @Test
  void simultaneousAcceptancesForOneOrderLeaveExactlyOneAssignedPartner() throws Exception {
    UUID firstPartner = user("partner-one@delivery-race.test", "delivery_partner", true);
    UUID secondPartner = user("partner-two@delivery-race.test", "delivery_partner", true);
    UUID order = order();
    UUID firstOffer = offer(order, firstPartner);
    UUID secondOffer = offer(order, secondPartner);

    List<Integer> statuses =
        race(
            () -> accept(firstOffer, "partner-one@delivery-race.test"),
            () -> accept(secondOffer, "partner-two@delivery-race.test"));

    assertThat(statuses).containsExactlyInAnyOrder(200, 409);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM orders WHERE id=? AND delivery_partner_id IS NOT NULL",
                Integer.class,
                order))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM delivery_offers WHERE order_id=? AND status='Accepted'",
                Integer.class,
                order))
        .isEqualTo(1);
  }

  @Test
  void simultaneousAcceptancesForTwoOrdersLeavePartnerAssignedToOnlyOne() throws Exception {
    UUID partner = user("partner@delivery-race.test", "delivery_partner", true);
    UUID firstOrder = order();
    UUID secondOrder = order();
    UUID firstOffer = offer(firstOrder, partner);
    UUID secondOffer = offer(secondOrder, partner);

    List<Integer> statuses =
        race(
            () -> accept(firstOffer, "partner@delivery-race.test"),
            () -> accept(secondOffer, "partner@delivery-race.test"));

    assertThat(statuses).containsExactlyInAnyOrder(200, 409);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM orders WHERE delivery_partner_id=? AND order_status IN ('Accepted','Preparing','Ready for pickup','Out for delivery')",
                Integer.class,
                partner))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM orders WHERE id IN (?,?) AND delivery_partner_id IS NULL",
                Integer.class,
                firstOrder,
                secondOrder))
        .isEqualTo(1);
  }

  private int accept(UUID offer, String username) throws Exception {
    RequestPostProcessor partner = bearer(username, "secret");
    return mvc.perform(
            post("/api/delivery-offers/{id}/acceptances", offer).param("round", "1").with(partner))
        .andReturn()
        .getResponse()
        .getStatus();
  }

  private List<Integer> race(Callable<Integer> first, Callable<Integer> second) throws Exception {
    CyclicBarrier barrier = new CyclicBarrier(2);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      List<Future<Integer>> futures = new ArrayList<>();
      futures.add(
          executor.submit(
              () -> {
                barrier.await();
                return first.call();
              }));
      futures.add(
          executor.submit(
              () -> {
                barrier.await();
                return second.call();
              }));
      return List.of(futures.get(0).get(), futures.get(1).get());
    } finally {
      executor.shutdownNow();
    }
  }

  private UUID order() {
    return jdbc.queryForObject(
        "INSERT INTO orders (customer_id,restaurant_id,order_status,payment_status,sub_total_amount,total_amount,address_line_1,city,state,country,location) VALUES (?,?,'Accepted','Success',100,100,'Road','Pune','MH','India',ST_GeogFromText('POINT(73.85 18.52)')) RETURNING id",
        UUID.class,
        customer,
        restaurant);
  }

  private UUID offer(UUID order, UUID partner) {
    return jdbc.queryForObject(
        "INSERT INTO delivery_offers (order_id,partner_id,round,status,expires_at) VALUES (?,?,1,'Offered',CURRENT_TIMESTAMP + interval '1 minute') RETURNING id",
        UUID.class,
        order,
        partner);
  }

  private UUID user(String username, String role, boolean online) {
    UUID id =
        jdbc.queryForObject(
            "INSERT INTO users (name,email,phone_number,role,online) VALUES (?,?,?,?,?) RETURNING id",
            UUID.class,
            username,
            username,
            "+9197" + Math.abs(username.hashCode()),
            role,
            online);
    jdbc.update(
        "INSERT INTO user_credentials (user_id,username,password_hash) VALUES (?,?,?)",
        id,
        username,
        passwords.encode("secret"));
    return id;
  }
}
