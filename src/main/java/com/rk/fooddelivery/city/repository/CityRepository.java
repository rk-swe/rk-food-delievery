package com.rk.fooddelivery.city.repository;

import com.rk.fooddelivery.city.entity.City;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface CityRepository extends JpaRepository<City, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from City c where c.id = :id")
  Optional<City> findLockedById(UUID id);

  @Query(
      value =
          "select c from City c where (:activeOnly = false or c.active = true) order by lower(c.name), c.id",
      countQuery = "select count(c) from City c where (:activeOnly = false or c.active = true)")
  Page<City> findVisible(boolean activeOnly, Pageable pageable);

  boolean existsByNameIgnoreCaseAndStateIgnoreCaseAndCountryIgnoreCase(
      String name, String state, String country);

  @Query(
      value = "select exists(select 1 from restaurants where city_id = :cityId and active)",
      nativeQuery = true)
  boolean hasActiveRestaurants(UUID cityId);
}
