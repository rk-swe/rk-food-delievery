package com.rk.fooddelivery.restaurant.dto;
import java.math.BigDecimal;
public record RestaurantSearchRequest(String name,String cuisine,String dietType,BigDecimal maxCostForTwo,Double latitude,Double longitude,Double radiusMeters,int page,int size) {}
