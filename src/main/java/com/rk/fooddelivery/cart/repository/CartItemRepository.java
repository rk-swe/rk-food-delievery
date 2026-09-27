package com.rk.fooddelivery.cart.repository;

import com.rk.fooddelivery.cart.entity.CartItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
  List<CartItem> findByCartIdOrderByMenuItemId(UUID cartId);

  Optional<CartItem> findByCartIdAndMenuItemId(UUID cartId, UUID menuItemId);

  void deleteByCartId(UUID cartId);
}
