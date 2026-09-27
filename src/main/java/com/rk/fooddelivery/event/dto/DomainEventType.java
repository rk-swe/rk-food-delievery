package com.rk.fooddelivery.event.dto;

public enum DomainEventType {
  ORDER_ACCEPTED("order.accepted"),
  DELIVERY_ASSIGNMENT_REQUESTED("delivery.assignment.requested"),
  PAYMENT_REFUND_REQUESTED("payment.refund.requested");

  private final String routingKey;

  DomainEventType(String routingKey) {
    this.routingKey = routingKey;
  }

  public String routingKey() {
    return routingKey;
  }
}
