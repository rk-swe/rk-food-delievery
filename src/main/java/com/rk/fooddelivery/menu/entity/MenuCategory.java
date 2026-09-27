package com.rk.fooddelivery.menu.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "menu_categories")
public class MenuCategory {
  @Id private UUID id;

  @Column(name = "restaurant_id", nullable = false)
  private UUID restaurantId;

  @Column(nullable = false, columnDefinition = "text")
  private String name;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  @Column(name = "item_count", nullable = false)
  private int itemCount;

  @Column(name = "created_by")
  private UUID createdBy;

  @Column(name = "updated_by")
  private UUID updatedBy;

  protected MenuCategory() {}

  public MenuCategory(UUID id, UUID restaurantId, String name, int sortOrder, UUID actor) {
    this.id = id;
    this.restaurantId = restaurantId;
    this.name = name;
    this.sortOrder = sortOrder;
    this.createdBy = actor;
    this.updatedBy = actor;
  }

  public UUID getId() {
    return id;
  }

  public UUID getRestaurantId() {
    return restaurantId;
  }

  public String getName() {
    return name;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public int getItemCount() {
    return itemCount;
  }

  public void patch(String name, Integer sortOrder, UUID actor) {
    if (name != null) this.name = name;
    if (sortOrder != null) this.sortOrder = sortOrder;
    updatedBy = actor;
  }

  public void incrementItemCount(int amount, UUID actor) {
    itemCount += amount;
    updatedBy = actor;
  }
}
