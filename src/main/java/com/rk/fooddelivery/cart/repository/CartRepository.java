package com.rk.fooddelivery.cart.repository;

import com.rk.fooddelivery.cart.entity.Cart;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, UUID> {
  Optional<Cart> findByCustomerId(UUID customerId);
}
