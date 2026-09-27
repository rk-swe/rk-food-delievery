package com.rk.fooddelivery.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class CartDtos {
  private CartDtos() {}

  public record SetCartItemRequest(@NotNull @Min(1) Integer quantity) {}

  public record CartItemResponse(
      UUID menuItemId, String name, BigDecimal price, int quantity, UUID restaurantId) {}

  public record CartResponse(
      UUID restaurantId, long version, boolean restaurantChanged, List<CartItemResponse> items) {}
}
