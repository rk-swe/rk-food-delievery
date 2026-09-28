package com.rk.fooddelivery.payment.entity;

import com.rk.fooddelivery.order.entity.PaymentStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {
  @Id private UUID id;

  @Column(name = "order_id")
  private UUID orderId;

  private String status;

  @SuppressWarnings("unused") // Persisted by Hibernate through field access.
  private String provider;

  @Column(name = "payment_method")
  private String paymentMethod;

  @Column(name = "provider_payment_id")
  private String providerPaymentId;

  private BigDecimal amount;

  @SuppressWarnings("unused") // Persisted by Hibernate through field access.
  private String currency;

  @Column(name = "expires_at")
  private Instant expiresAt;

  @Column(name = "status_reason")
  private String statusReason;

  protected Payment() {}

  public Payment(UUID id, UUID orderId, String method, BigDecimal amount, Instant expiresAt) {
    this.id = id;
    this.orderId = orderId;
    this.status = PaymentStatus.PENDING.value();
    this.provider = "Mock";
    this.paymentMethod = method;
    this.amount = amount;
    this.currency = "INR";
    this.expiresAt = expiresAt;
  }

  public UUID getId() {
    return id;
  }

  public UUID getOrderId() {
    return orderId;
  }

  public PaymentStatus status() {
    return PaymentStatus.from(status);
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void complete(String providerId) {
    status = PaymentStatus.SUCCESS.value();
    providerPaymentId = providerId;
  }

  public void fail(String reason) {
    status = PaymentStatus.FAILED.value();
    statusReason = reason;
  }
}
