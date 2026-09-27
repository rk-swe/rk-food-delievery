package com.rk.fooddelivery.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.rk.fooddelivery.payment.service.PaymentWebhookService;
import com.rk.fooddelivery.support.IntegrationTestSupport;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PaymentIntegrationTest extends IntegrationTestSupport {

  @Autowired PaymentWebhookService webhooks;

  @Test
  void validWebhookIsIdempotentAndMarksPaymentSuccessful() throws Exception {
    UUID customer = user("customer");
    UUID owner = user("restaurant_owner");
    UUID city =
        jdbc.queryForObject(
            "INSERT INTO cities (name,state,country,currency) VALUES ('Pune','MH','India','INR') RETURNING id",
            UUID.class);
    UUID restaurant =
        jdbc.queryForObject(
            "INSERT INTO restaurants (name,owner_id,city_id,cost_for_two,diet_type,address_line_1,location) VALUES ('Payments',?,?,100,'Veg','Road',ST_GeogFromText('POINT(73.85 18.52)')) RETURNING id",
            UUID.class,
            owner,
            city);
    UUID order =
        jdbc.queryForObject(
            "INSERT INTO orders (customer_id,restaurant_id,order_status,payment_status,sub_total_amount,total_amount,address_line_1,city,state,country,location,payment_deadline_at) VALUES (?,?,'Placed','Pending',100,100,'Road','Pune','MH','India',ST_GeogFromText('POINT(73.85 18.52)'),CURRENT_TIMESTAMP + interval '15 minutes') RETURNING id",
            UUID.class,
            customer,
            restaurant);
    jdbc.update(
        "INSERT INTO payments (order_id,status,provider,payment_method,amount) VALUES (?,'Pending','Mock','UPI',100)",
        order);
    byte[] body =
        ("{\"eventId\":\"payment-success-1\",\"orderId\":\""
                + order
                + "\",\"status\":\"Success\",\"paymentId\":\"mock-1\"}")
            .getBytes(StandardCharsets.UTF_8);

    webhooks.handle(body, signature(body));
    webhooks.handle(body, signature(body));

    assertThat(
            jdbc.queryForObject(
                "SELECT payment_status FROM orders WHERE id=?", String.class, order))
        .isEqualTo("Success");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM payment_webhook_receipts", Integer.class))
        .isEqualTo(1);
  }

  private UUID user(String role) {
    return jdbc.queryForObject(
        "INSERT INTO users (name,email,phone_number,role) VALUES (?,?,?,?) RETURNING id",
        UUID.class,
        role,
        role + "@payment.test",
        role.equals("customer") ? "+919900000001" : "+919900000002",
        role);
  }

  private String signature(byte[] body) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(
        new SecretKeySpec("test-webhook-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    return HexFormat.of().formatHex(mac.doFinal(body));
  }
}
