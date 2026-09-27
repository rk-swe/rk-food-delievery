package com.rk.fooddelivery.menu.repository;

import com.rk.fooddelivery.menu.entity.MenuCategory;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, UUID> {
  @Query(
      "select c from MenuCategory c where c.restaurantId=:restaurantId order by c.sortOrder,c.id")
  List<MenuCategory> findByRestaurantIdOrdered(UUID restaurantId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from MenuCategory c where c.id=:id")
  Optional<MenuCategory> findLockedById(UUID id);
}
