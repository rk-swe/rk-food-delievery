package com.rk.fooddelivery.order.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items")
public class OrderItem {
  @Id private UUID id;

  @Column(name = "order_id")
  private UUID orderId;

  @Column(name = "menu_item_id")
  private UUID menuItemId;

  private int quantity;

  @Column(name = "unit_price")
  private BigDecimal unitPrice;

  @Column(name = "sub_total")
  private BigDecimal subTotal;

  protected OrderItem() {}

  public OrderItem(UUID id, UUID orderId, UUID itemId, int quantity, BigDecimal price) {
    this.id = id;
    this.orderId = orderId;
    this.menuItemId = itemId;
    this.quantity = quantity;
    this.unitPrice = price;
    this.subTotal = price.multiply(BigDecimal.valueOf(quantity));
  }

  public UUID getMenuItemId() {
    return menuItemId;
  }

  public int getQuantity() {
    return quantity;
  }
}
