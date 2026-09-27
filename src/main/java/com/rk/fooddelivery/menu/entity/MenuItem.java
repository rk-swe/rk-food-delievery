package com.rk.fooddelivery.menu.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "menu_items")
public class MenuItem {
  @Id private UUID id;

  @Column(name = "restaurant_id", nullable = false)
  private UUID restaurantId;

  @Column(name = "category_id", nullable = false)
  private UUID categoryId;

  @Column(nullable = false, columnDefinition = "text")
  private String name;

  @Column(columnDefinition = "text")
  private String description;

  @Column(name = "diet_type", nullable = false, columnDefinition = "text")
  private String dietType;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal price;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  @Column(name = "is_available", nullable = false)
  private boolean available = true;

  @Column(name = "available_quantity", nullable = false)
  private int availableQuantity;

  @Column(name = "created_by")
  private UUID createdBy;

  @Column(name = "updated_by")
  private UUID updatedBy;

  protected MenuItem() {}

  public MenuItem(
      UUID id,
      UUID restaurantId,
      UUID categoryId,
      String name,
      String description,
      String dietType,
      BigDecimal price,
      int sortOrder,
      int availableQuantity,
      UUID actor) {
    this.id = id;
    this.restaurantId = restaurantId;
    this.categoryId = categoryId;
    this.name = name;
    this.description = description;
    this.dietType = dietType;
    this.price = price;
    this.sortOrder = sortOrder;
    this.availableQuantity = availableQuantity;
    this.createdBy = actor;
    this.updatedBy = actor;
  }

  public UUID getId() {
    return id;
  }

  public UUID getRestaurantId() {
    return restaurantId;
  }

  public UUID getCategoryId() {
    return categoryId;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getDietType() {
    return dietType;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public boolean isAvailable() {
    return available;
  }

  public int getAvailableQuantity() {
    return availableQuantity;
  }

  public void patch(
      UUID categoryId,
      String name,
      String description,
      String dietType,
      BigDecimal price,
      Integer sortOrder,
      Boolean available,
      UUID actor) {
    if (categoryId != null) this.categoryId = categoryId;
    if (name != null) this.name = name;
    if (description != null) this.description = description;
    if (dietType != null) this.dietType = dietType;
    if (price != null) this.price = price;
    if (sortOrder != null) this.sortOrder = sortOrder;
    if (available != null) this.available = available;
    updatedBy = actor;
  }

  public void adjustStock(int delta, UUID actor) {
    if (availableQuantity + delta < 0)
      throw new IllegalArgumentException("Insufficient available quantity");
    availableQuantity += delta;
    updatedBy = actor;
  }

  public void deactivate(UUID actor) {
    available = false;
    updatedBy = actor;
  }
}
