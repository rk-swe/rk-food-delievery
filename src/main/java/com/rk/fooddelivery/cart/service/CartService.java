package com.rk.fooddelivery.cart.service;

import com.rk.fooddelivery.auth.CurrentUser;
import com.rk.fooddelivery.auth.Role;
import com.rk.fooddelivery.cart.dto.CartDtos.CartItemResponse;
import com.rk.fooddelivery.cart.dto.CartDtos.CartResponse;
import com.rk.fooddelivery.cart.entity.Cart;
import com.rk.fooddelivery.cart.entity.CartItem;
import com.rk.fooddelivery.cart.repository.CartItemRepository;
import com.rk.fooddelivery.cart.repository.CartRepository;
import com.rk.fooddelivery.common.error.NotFoundException;
import com.rk.fooddelivery.menu.entity.MenuItem;
import com.rk.fooddelivery.menu.repository.MenuItemRepository;
import com.rk.fooddelivery.user.entity.User;
import com.rk.fooddelivery.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {
  private final CartRepository carts;
  private final CartItemRepository cartItems;
  private final MenuItemRepository menuItems;
  private final UserRepository users;
  private final CurrentUser current;

  public CartService(
      CartRepository carts,
      CartItemRepository cartItems,
      MenuItemRepository menuItems,
      UserRepository users,
      CurrentUser current) {
    this.carts = carts;
    this.cartItems = cartItems;
    this.menuItems = menuItems;
    this.users = users;
    this.current = current;
  }

  @Transactional
  public CartResponse setItem(UUID customerId, UUID itemId, int quantity) {
    User customer = lockedCustomer(customerId);
    MenuItem item =
        menuItems
            .findById(itemId)
            .filter(candidate -> candidate.isAvailable())
            .orElseThrow(() -> new NotFoundException("Menu item not found"));
    Cart cart = carts.findByCustomerId(customerId).orElse(null);
    boolean restaurantChanged =
        cart != null && !cart.getRestaurantId().equals(item.getRestaurantId());
    if (cart == null) {
      cart = carts.save(new Cart(UUID.randomUUID(), customerId, item.getRestaurantId()));
    } else if (restaurantChanged) {
      cartItems.deleteByCartId(cart.getId());
      cartItems.flush();
      cart.changeRestaurant(item.getRestaurantId());
      carts.saveAndFlush(cart);
    }
    CartItem line = cartItems.findByCartIdAndMenuItemId(cart.getId(), itemId).orElse(null);
    if (line == null) {
      line =
          new CartItem(UUID.randomUUID(), cart.getId(), item.getRestaurantId(), itemId, quantity);
    }
    line.setQuantity(quantity);
    cartItems.save(line);
    customer.incrementCartVersion();
    return response(cart, customer.getCartVersion(), restaurantChanged);
  }

  @Transactional
  public CartResponse removeItem(UUID customerId, UUID itemId) {
    User customer = lockedCustomer(customerId);
    Cart cart = carts.findByCustomerId(customerId).orElse(null);
    if (cart == null) return empty(customer, false);
    cartItems.findByCartIdAndMenuItemId(cart.getId(), itemId).ifPresent(cartItems::delete);
    customer.incrementCartVersion();
    return response(cart, customer.getCartVersion(), false);
  }

  @Transactional
  public void clear(UUID customerId) {
    User customer = lockedCustomer(customerId);
    carts.findByCustomerId(customerId).ifPresent(cart -> cartItems.deleteByCartId(cart.getId()));
    customer.incrementCartVersion();
  }

  @Transactional(readOnly = true)
  public CartResponse get(UUID customerId) {
    User customer = customer(customerId);
    return carts
        .findByCustomerId(customerId)
        .map(cart -> response(cart, customer.getCartVersion(), false))
        .orElseGet(() -> empty(customer, false));
  }

  private User customer(UUID customerId) {
    var actor = current.requireRole(Role.CUSTOMER);
    if (!actor.id().equals(customerId))
      throw new AccessDeniedException("Customer does not own this cart");
    return users
        .findById(customerId)
        .orElseThrow(() -> new NotFoundException("Customer not found"));
  }

  private User lockedCustomer(UUID customerId) {
    var actor = current.requireRole(Role.CUSTOMER);
    if (!actor.id().equals(customerId))
      throw new AccessDeniedException("Customer does not own this cart");
    return users
        .findLockedById(customerId)
        .orElseThrow(() -> new NotFoundException("Customer not found"));
  }

  private CartResponse response(Cart cart, long version, boolean restaurantChanged) {
    List<CartItemResponse> items =
        cartItems.findByCartIdOrderByMenuItemId(cart.getId()).stream()
            .map(
                line -> {
                  MenuItem item = menuItems.findById(line.getMenuItemId()).orElseThrow();
                  return new CartItemResponse(
                      item.getId(),
                      item.getName(),
                      item.getPrice(),
                      line.getQuantity(),
                      item.getRestaurantId());
                })
            .toList();
    return new CartResponse(cart.getRestaurantId(), version, restaurantChanged, items);
  }

  private CartResponse empty(User customer, boolean restaurantChanged) {
    return new CartResponse(null, customer.getCartVersion(), restaurantChanged, List.of());
  }
}
