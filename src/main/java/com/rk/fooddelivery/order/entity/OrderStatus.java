package com.rk.fooddelivery.order.entity;

public enum OrderStatus {
  PLACED("Placed"),
  ACCEPTED("Accepted"),
  REJECTED("Rejected"),
  PREPARING("Preparing"),
  READY_FOR_PICKUP("Ready for pickup"),
  OUT_FOR_DELIVERY("Out for delivery"),
  DELIVERED("Delivered"),
  CANCELLED("Cancelled");
  private final String value;

  OrderStatus(String value) {
    this.value = value;
  }

  public String value() {
    return value;
  }

  public static OrderStatus from(String value) {
    for (OrderStatus status : values()) if (status.value.equals(value)) return status;
    throw new IllegalArgumentException("Unknown order status");
  }
}
