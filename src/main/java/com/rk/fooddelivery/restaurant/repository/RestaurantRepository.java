package com.rk.fooddelivery.restaurant.repository;

import com.rk.fooddelivery.restaurant.entity.Restaurant;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;

public interface RestaurantRepository extends JpaRepository<Restaurant, UUID> {
  @Query("select r from Restaurant r order by lower(r.name), r.id")
  Page<Restaurant> findAllOrdered(Pageable pageable);

  @Query("select r from Restaurant r where r.ownerId=:ownerId order by lower(r.name), r.id")
  Page<Restaurant> findByOwnerIdOrdered(UUID ownerId, Pageable pageable);

  @Query(
      "select r from Restaurant r where r.active=true and exists (select c from com.rk.fooddelivery.city.entity.City c where c.id=r.cityId and c.active=true) order by lower(r.name),r.id")
  Page<Restaurant> findPublicVisible(Pageable pageable);

  @Query(
      "select r from Restaurant r where r.id=:id and r.active=true and exists (select c from com.rk.fooddelivery.city.entity.City c where c.id=r.cityId and c.active=true)")
  Optional<Restaurant> findPublicVisibleById(UUID id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from Restaurant r where r.id=:id")
  Optional<Restaurant> findLockedById(UUID id);

  boolean existsByCityIdAndActiveTrue(UUID cityId);
}
