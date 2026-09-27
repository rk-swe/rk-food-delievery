package com.rk.fooddelivery.order.controller;

import com.rk.fooddelivery.auth.*;
import com.rk.fooddelivery.order.dto.*;
import com.rk.fooddelivery.order.dto.LifecycleDtos.*;
import com.rk.fooddelivery.order.service.OrderLifecycleService;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class OwnerOrderController {
  private final OrderLifecycleService orders;
  private final CurrentUser current;

  public OwnerOrderController(OrderLifecycleService orders, CurrentUser current) {
    this.orders = orders;
    this.current = current;
  }

  @GetMapping("/api/restaurants/{restaurantId}/orders")
  public List<OrderDtos.OrderResponse> list(@PathVariable UUID restaurantId) {
    return orders.restaurantOrders(current.requireRole(Role.RESTAURANT_OWNER).id(), restaurantId);
  }

  @PostMapping("/api/orders/{id}/restaurant-decisions")
  public OrderDtos.OrderResponse decide(
      @PathVariable UUID id, @Valid @RequestBody RestaurantDecisionRequest request) {
    return orders.decide(
        current.requireRole(Role.RESTAURANT_OWNER).id(), id, request.decision(), request.reason());
  }

  @PostMapping("/api/orders/{id}/preparation")
  public OrderDtos.OrderResponse prepare(@PathVariable UUID id) {
    return orders.startPreparation(current.requireRole(Role.RESTAURANT_OWNER).id(), id);
  }

  @PostMapping("/api/orders/{id}/readiness")
  public OrderDtos.OrderResponse ready(@PathVariable UUID id) {
    return orders.markReady(current.requireRole(Role.RESTAURANT_OWNER).id(), id);
  }
}
