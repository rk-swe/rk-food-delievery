package com.rk.fooddelivery.menu.repository;

import com.rk.fooddelivery.menu.entity.MenuCategory;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, UUID> {
  @Query(
      "select c from MenuCategory c where c.restaurantId=:restaurantId and (:includeInactive=true or c.active=true) order by c.sortOrder,c.id")
  org.springframework.data.domain.Page<MenuCategory> findByRestaurantIdOrdered(
      UUID restaurantId,
      boolean includeInactive,
      org.springframework.data.domain.Pageable pageable);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from MenuCategory c where c.id=:id")
  Optional<MenuCategory> findLockedById(UUID id);
}
