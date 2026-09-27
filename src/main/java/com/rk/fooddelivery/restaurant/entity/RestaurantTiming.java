package com.rk.fooddelivery.restaurant.entity;

import jakarta.persistence.*;
import java.time.LocalTime;
import java.util.UUID;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "restaurant_timings")
public class RestaurantTiming implements Persistable<UUID> {
  @Id private UUID id;

  @Column(name = "restaurant_id", nullable = false)
  private UUID restaurantId;

  @Column(nullable = false)
  private String day;

  @Column(name = "start_time")
  private LocalTime startTime;

  @Column(name = "end_time")
  private LocalTime endTime;

  @Column(name = "is_open", nullable = false)
  private boolean open;

  @Column(name = "created_by")
  private UUID createdBy;

  @Column(name = "updated_by")
  private UUID updatedBy;

  @Transient private boolean newEntity = true;

  protected RestaurantTiming() {}

  public RestaurantTiming(
      UUID id,
      UUID restaurantId,
      String day,
      boolean open,
      LocalTime startTime,
      LocalTime endTime,
      UUID actor) {
    this.id = id;
    this.restaurantId = restaurantId;
    this.day = day;
    this.open = open;
    this.startTime = startTime;
    this.endTime = endTime;
    this.createdBy = actor;
    this.updatedBy = actor;
  }

  @Override
  public UUID getId() {
    return id;
  }

  @Override
  public boolean isNew() {
    return newEntity;
  }

  @PostLoad
  @PostPersist
  void persisted() {
    newEntity = false;
  }

  public UUID getRestaurantId() {
    return restaurantId;
  }

  public String getDay() {
    return day;
  }

  public void update(boolean open, LocalTime startTime, LocalTime endTime, UUID actor) {
    this.open = open;
    this.startTime = startTime;
    this.endTime = endTime;
    this.updatedBy = actor;
  }
}
