package com.rk.fooddelivery.restaurant.controller;

import com.rk.fooddelivery.common.web.PageResponse;
import com.rk.fooddelivery.restaurant.dto.RestaurantDtos.HoursPatch;
import com.rk.fooddelivery.restaurant.dto.RestaurantDtos.RestaurantResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;

@RestController
public class OwnerRestaurantController {
  private final com.rk.fooddelivery.restaurant.service.RestaurantService service;

  public OwnerRestaurantController(com.rk.fooddelivery.restaurant.service.RestaurantService service) {
    this.service = service;
  }

  @GetMapping("/api/me/restaurants")
  public PageResponse<RestaurantResponse> mine(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return service.mine(page, size);
  }

  @PatchMapping("/api/restaurants/{id}/hours")
  public RestaurantResponse hours(@PathVariable java.util.UUID id, @Valid @RequestBody HoursPatch request) {
    return service.updateHours(id, request.hours());
  }
}
