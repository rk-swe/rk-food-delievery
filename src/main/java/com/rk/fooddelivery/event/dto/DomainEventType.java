package com.rk.fooddelivery.event.dto;

public enum DomainEventType {
  ORDER_PLACED("order.placed"),
  ORDER_ACCEPTED("order.accepted"),
  ORDER_READY("order.ready"),
  ORDER_DELIVERED("order.delivered"),
  PAYMENT_SUCCEEDED("payment.succeeded"),
  PAYMENT_FAILED("payment.failed"),
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
