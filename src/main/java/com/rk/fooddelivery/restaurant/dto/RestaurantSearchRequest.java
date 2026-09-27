package com.rk.fooddelivery.restaurant.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record RestaurantSearchRequest(
    String name,
    String cuisine,
    String dietType,
    UUID cityId,
    List<UUID> cuisineIds,
    BigDecimal minCostForTwo,
    BigDecimal maxCostForTwo,
    String sort,
    Double latitude,
    Double longitude,
    Double radiusMeters,
    int page,
    int size) {
  public RestaurantSearchRequest {
    cuisineIds = cuisineIds == null ? List.of() : cuisineIds.stream().distinct().toList();
    sort = sort == null ? "name" : sort;
  }

  public boolean spatial() {
    return radiusMeters != null || sort.startsWith("distance");
  }

  public String sortField() {
    return sort.split(",")[0];
  }

  public boolean descending() {
    return sort.endsWith(",desc") || (sort.equals("rating"));
  }

  public RestaurantSearchRequest withLocation(double latitude, double longitude) {
    return new RestaurantSearchRequest(
        name,
        cuisine,
        dietType,
        cityId,
        cuisineIds,
        minCostForTwo,
        maxCostForTwo,
        sort,
        latitude,
        longitude,
        radiusMeters,
        page,
        size);
  }
}
