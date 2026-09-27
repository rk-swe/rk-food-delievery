package com.rk.fooddelivery.menu.repository;

import com.rk.fooddelivery.menu.entity.MenuItem;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select i from MenuItem i where i.id=:id")
  Optional<MenuItem> findLockedById(UUID id);

  @Query(
      "select i from MenuItem i where i.restaurantId=:restaurantId and (:categoryId is null or i.categoryId=:categoryId) and (:dietType is null or i.dietType=:dietType) and (:minPrice is null or i.price>=:minPrice) and (:maxPrice is null or i.price<=:maxPrice) and (cast(:name as string) is null or lower(i.name) like lower(concat('%',:name,'%'))) and (:includeUnavailable=true or i.available=true) and (:available is null or i.available=:available)")
  Page<MenuItem> search(
      UUID restaurantId,
      UUID categoryId,
      String dietType,
      java.math.BigDecimal minPrice,
      java.math.BigDecimal maxPrice,
      String name,
      boolean includeUnavailable,
      Boolean available,
      Pageable pageable);

  boolean existsByCategoryIdAndAvailableTrue(UUID categoryId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      "update MenuItem i set i.availableQuantity=i.availableQuantity-:quantity where i.id=:id and i.restaurantId=:restaurantId and i.available=true and i.availableQuantity>=:quantity")
  int reserve(UUID id, UUID restaurantId, int quantity);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("update MenuItem i set i.availableQuantity=i.availableQuantity+:quantity where i.id=:id")
  int restore(UUID id, int quantity);
}
