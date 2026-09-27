package com.rk.fooddelivery.cart.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "carts")
public class Cart {
  @Id private UUID id;

  @Column(name = "customer_id", nullable = false)
  private UUID customerId;

  @Column(name = "restaurant_id", nullable = false)
  private UUID restaurantId;

  protected Cart() {}

  public Cart(UUID id, UUID customerId, UUID restaurantId) {
    this.id = id;
    this.customerId = customerId;
    this.restaurantId = restaurantId;
  }

  public UUID getId() {
    return id;
  }

  public UUID getCustomerId() {
    return customerId;
  }

  public UUID getRestaurantId() {
    return restaurantId;
  }

  public void changeRestaurant(UUID restaurantId) {
    this.restaurantId = restaurantId;
  }
}
