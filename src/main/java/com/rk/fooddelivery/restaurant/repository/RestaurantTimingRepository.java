package com.rk.fooddelivery.restaurant.repository;

import com.rk.fooddelivery.restaurant.entity.RestaurantTiming;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantTimingRepository extends JpaRepository<RestaurantTiming, UUID> {
  List<RestaurantTiming> findByRestaurantId(UUID restaurantId);
}
