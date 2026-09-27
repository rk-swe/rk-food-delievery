package com.rk.fooddelivery.delivery.entity;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "delivery_offers")
public class DeliveryOffer {
  @Id private UUID id;

  @Column(name = "order_id")
  private UUID orderId;

  @Column(name = "partner_id")
  private UUID partnerId;

  private int round;
  private String status = "Offered";

  @Column(name = "expires_at")
  private Instant expiresAt;

  protected DeliveryOffer() {}

  public DeliveryOffer(UUID order, UUID partner, int round, Instant expires) {
    id = UUID.randomUUID();
    orderId = order;
    partnerId = partner;
    this.round = round;
    expiresAt = expires;
  }

  public UUID getId() {
    return id;
  }

  public UUID getOrderId() {
    return orderId;
  }

  public UUID getPartnerId() {
    return partnerId;
  }

  public int getRound() {
    return round;
  }

  public boolean active(Instant now) {
    return "Offered".equals(status) && expiresAt.isAfter(now);
  }

  public void accept() {
    status = "Accepted";
  }
}
