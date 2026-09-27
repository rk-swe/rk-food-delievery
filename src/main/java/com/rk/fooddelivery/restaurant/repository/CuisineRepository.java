package com.rk.fooddelivery.restaurant.repository;

import com.rk.fooddelivery.restaurant.entity.Cuisine;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CuisineRepository extends JpaRepository<Cuisine, UUID> {
  @Query("select c from Cuisine c order by lower(c.name), c.id")
  List<Cuisine> findOrdered(Pageable pageable);
}
