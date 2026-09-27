package com.rk.fooddelivery.restaurant.controller;

import com.rk.fooddelivery.common.web.PageResponse;
import com.rk.fooddelivery.restaurant.dto.RestaurantDtos.HoursPatch;
import com.rk.fooddelivery.restaurant.dto.RestaurantDtos.RestaurantResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Restaurants")
public class OwnerRestaurantController {
  private final com.rk.fooddelivery.restaurant.service.RestaurantService service;

  public OwnerRestaurantController(com.rk.fooddelivery.restaurant.service.RestaurantService service) {
    this.service = service;
  }

  @GetMapping("/api/me/restaurants")
  @Operation(operationId = "listMyRestaurants", summary = "List my restaurants", description = "Requires the RESTAURANT_OWNER role.")
  public PageResponse<RestaurantResponse> mine(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return service.mine(page, size);
  }

  @PatchMapping("/api/restaurants/{id}/hours")
  @Operation(operationId = "updateRestaurantHours", summary = "Update restaurant hours", description = "Requires ownership and the RESTAURANT_OWNER role.")
  public RestaurantResponse hours(@PathVariable java.util.UUID id, @Valid @RequestBody HoursPatch request) {
    return service.updateHours(id, request.hours());
  }
}
