package com.rk.fooddelivery.cart.controller;

import com.rk.fooddelivery.auth.CurrentUser;
import com.rk.fooddelivery.auth.Role;
import com.rk.fooddelivery.cart.dto.CartDtos.CartResponse;
import com.rk.fooddelivery.cart.dto.CartDtos.SetCartItemRequest;
import com.rk.fooddelivery.cart.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@Tag(name = "Cart")
public class CartController {
  private final CartService carts;
  private final CurrentUser current;

  public CartController(CartService carts, CurrentUser current) {
    this.carts = carts;
    this.current = current;
  }

  @GetMapping
  @Operation(operationId = "getCart", summary = "Get the authenticated customer's cart")
  public CartResponse get() {
    return carts.get(customerId());
  }

  @PutMapping("/items/{itemId}")
  @Operation(
      operationId = "setCartItem",
      summary = "Set an item quantity in the authenticated customer's cart")
  public CartResponse setItem(
      @PathVariable UUID itemId, @Valid @RequestBody SetCartItemRequest request) {
    return carts.setItem(customerId(), itemId, request.quantity());
  }

  @DeleteMapping("/items/{itemId}")
  @Operation(
      operationId = "removeCartItem",
      summary = "Remove an item from the authenticated customer's cart")
  public CartResponse removeItem(@PathVariable UUID itemId) {
    return carts.removeItem(customerId(), itemId);
  }

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(operationId = "clearCart", summary = "Clear the authenticated customer's cart")
  public void clear() {
    carts.clear(customerId());
  }

  private UUID customerId() {
    return current.requireRole(Role.CUSTOMER).id();
  }
}
