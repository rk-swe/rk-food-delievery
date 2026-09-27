package com.rk.fooddelivery.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.rk.fooddelivery.order.entity.OrderStatus;
import com.rk.fooddelivery.order.service.OrderLifecycleService;
import com.rk.fooddelivery.support.IntegrationTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class OrderLifecycleIntegrationTest extends IntegrationTestSupport {

  @Autowired OrderLifecycleService lifecycle;

  @Test
  void paidOrderMovesThroughOwnerDecisionPreparationAndReadiness() {
    UUID owner = user("restaurant_owner", "+919900000011");
    UUID customer = user("customer", "+919900000012");
    UUID city =
        jdbc.queryForObject(
            "INSERT INTO cities (name,state,country,currency) VALUES ('Pune','MH','India','INR') RETURNING id",
            UUID.class);
    UUID restaurant =
        jdbc.queryForObject(
            "INSERT INTO restaurants (name,owner_id,city_id,cost_for_two,diet_type,address_line_1,location) VALUES ('Lifecycle',?,?,100,'Veg','Road',ST_GeogFromText('POINT(73.85 18.52)')) RETURNING id",
            UUID.class,
            owner,
            city);
    UUID order =
        jdbc.queryForObject(
            "INSERT INTO orders (customer_id,restaurant_id,order_status,payment_status,sub_total_amount,total_amount,address_line_1,city,state,country,location) VALUES (?,?,'Placed','Success',100,100,'Road','Pune','MH','India',ST_GeogFromText('POINT(73.85 18.52)')) RETURNING id",
            UUID.class,
            customer,
            restaurant);

    assertThat(lifecycle.decide(owner, order, "accept", null).orderStatus())
        .isEqualTo(OrderStatus.ACCEPTED.value());
    assertThat(lifecycle.startPreparation(owner, order).orderStatus())
        .isEqualTo(OrderStatus.PREPARING.value());
    assertThat(lifecycle.markReady(owner, order).orderStatus())
        .isEqualTo(OrderStatus.READY_FOR_PICKUP.value());
  }

  private UUID user(String role, String phone) {
    return jdbc.queryForObject(
        "INSERT INTO users (name,email,phone_number,role) VALUES (?,?,?,?) RETURNING id",
        UUID.class,
        role,
        role + "@lifecycle.test",
        phone,
        role);
  }
}
