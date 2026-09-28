package com.rk.fooddelivery.delivery.controller;

import com.rk.fooddelivery.auth.*;
import com.rk.fooddelivery.delivery.service.DeliveryOfferService;
import com.rk.fooddelivery.order.entity.Order;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Delivery Offers")
@RequestMapping("/api/delivery-offers")
public class DeliveryOfferController {
  private final DeliveryOfferService offers;
  private final CurrentUser current;

  public DeliveryOfferController(DeliveryOfferService offers, CurrentUser current) {
    this.offers = offers;
    this.current = current;
  }

  @PostMapping("/{id}/acceptances")
  public Map<String, Object> accept(@PathVariable UUID id, @RequestParam int round) {
    Order order = offers.acceptOffer(current.requireRole(Role.DELIVERY_PARTNER).id(), id, round);
    return Map.of("orderId", order.getId(), "status", order.getAssignmentStatus());
  }
}
