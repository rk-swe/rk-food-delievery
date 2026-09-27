package com.rk.fooddelivery.restaurant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "cuisines")
public class Cuisine {
  @Id private UUID id;

  @Column(nullable = false)
  private String name;

  @Column(name = "image_url")
  private String imageUrl;

  protected Cuisine() {}

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getImageUrl() {
    return imageUrl;
  }
}
