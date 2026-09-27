package com.rk.fooddelivery.payment.service;

import com.rk.fooddelivery.common.error.DomainException;
import com.rk.fooddelivery.event.dto.*;
import com.rk.fooddelivery.event.outbox.OutboxService;
import com.rk.fooddelivery.order.entity.*;
import com.rk.fooddelivery.order.repository.OrderRepository;
import com.rk.fooddelivery.order.service.StockReservationService;
import com.rk.fooddelivery.payment.entity.*;
import com.rk.fooddelivery.payment.repository.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Clock;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class PaymentWebhookService {
  private final PaymentWebhookReceiptRepository receipts;
  private final PaymentRepository payments;
  private final OrderRepository orders;
  private final StockReservationService stock;
  private final OutboxService outbox;
  private final RefundService refunds;
  private final ObjectMapper json;
  private final Clock clock;
  private final String secret;

  public PaymentWebhookService(
      PaymentWebhookReceiptRepository receipts,
      PaymentRepository payments,
      OrderRepository orders,
      StockReservationService stock,
      OutboxService outbox,
      RefundService refunds,
      ObjectMapper json,
      Clock clock,
      @Value("${PAYMENT_WEBHOOK_SECRET:test-webhook-secret}") String secret) {
    this.receipts = receipts;
    this.payments = payments;
    this.orders = orders;
    this.stock = stock;
    this.outbox = outbox;
    this.refunds = refunds;
    this.json = json;
    this.clock = clock;
    this.secret = secret;
  }

  @Transactional
  public void handle(byte[] body, String signature) {
    if (!MessageDigest.isEqual(hmac(body), decode(signature)))
      throw new DomainException("Invalid webhook signature");
    try {
      var node = json.readTree(body);
      String eventId = node.path("eventId").asString();
      if (eventId.isBlank()) throw new DomainException("Webhook eventId is required");
      if (receipts.existsById(eventId)) return;
      UUID orderId = UUID.fromString(node.path("orderId").asString());
      String result = node.path("status").asString();
      Order order = orders.findLockedById(orderId).orElseThrow();
      Payment payment = payments.findFirstByOrderIdOrderByIdDesc(orderId).orElseThrow();
      if ("Success".equals(result)) {
        if (payment.status() == PaymentStatus.PENDING) {
          payment.complete(node.path("paymentId").asString());
          order.payment(PaymentStatus.SUCCESS);
          outbox.append(
              new DomainEvent(
                  UUID.randomUUID(),
                  DomainEventType.PAYMENT_SUCCEEDED,
                  orderId,
                  order.getVersion(),
                  clock.instant(),
                  Map.of()));
        } else if (payment.status() != PaymentStatus.SUCCESS) {
          refunds.requestOnce(orderId, "late-success");
        }
      } else if ("Failed".equals(result) && payment.status() == PaymentStatus.PENDING) {
        payment.fail("provider-failed");
        order.payment(PaymentStatus.FAILED);
        stock.releaseOnce(orderId);
        outbox.append(
            new DomainEvent(
                UUID.randomUUID(),
                DomainEventType.PAYMENT_FAILED,
                orderId,
                order.getVersion(),
                clock.instant(),
                Map.of()));
      }
      receipts.save(new PaymentWebhookReceipt(eventId, hash(body)));
    } catch (DomainException e) {
      throw e;
    } catch (Exception e) {
      throw new DomainException("Invalid payment webhook");
    }
  }

  private byte[] hmac(byte[] b) {
    try {
      var mac = javax.crypto.Mac.getInstance("HmacSHA256");
      mac.init(
          new javax.crypto.spec.SecretKeySpec(
              secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return mac.doFinal(b);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private byte[] decode(String s) {
    try {
      return java.util.HexFormat.of().parseHex(s);
    } catch (Exception e) {
      return new byte[0];
    }
  }

  private String hash(byte[] b) {
    try {
      return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
