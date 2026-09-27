package com.rk.fooddelivery.cart.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "cart_items")
public class CartItem {
  @Id private UUID id;

  @Column(name = "cart_id", nullable = false)
  private UUID cartId;

  @Column(name = "restaurant_id", nullable = false)
  private UUID restaurantId;

  @Column(name = "menu_item_id", nullable = false)
  private UUID menuItemId;

  @Column(nullable = false)
  private int quantity;

  protected CartItem() {}

  public CartItem(UUID id, UUID cartId, UUID restaurantId, UUID menuItemId, int quantity) {
    this.id = id;
    this.cartId = cartId;
    this.restaurantId = restaurantId;
    this.menuItemId = menuItemId;
    this.quantity = quantity;
  }

  public UUID getId() {
    return id;
  }

  public UUID getCartId() {
    return cartId;
  }

  public UUID getRestaurantId() {
    return restaurantId;
  }

  public UUID getMenuItemId() {
    return menuItemId;
  }

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }
}
