package com.rk.fooddelivery.payment.entity;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "payment_refunds")
public class PaymentRefund {
  @Id private UUID id;

  @Column(name = "order_id")
  private UUID orderId;

  @SuppressWarnings("unused") // Persisted by Hibernate through field access.
  private String reason;

  @SuppressWarnings("unused") // Persisted by Hibernate through field access.
  private String status = "Pending";

  @Column(name = "completed_at")
  private Instant completedAt;

  protected PaymentRefund() {}

  public PaymentRefund(UUID orderId, String reason) {
    id = UUID.randomUUID();
    this.orderId = orderId;
    this.reason = reason;
  }

  public void complete() {
    status = "Completed";
    completedAt = Instant.now();
  }
}
