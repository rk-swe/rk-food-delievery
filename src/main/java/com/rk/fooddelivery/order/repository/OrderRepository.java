package com.rk.fooddelivery.order.repository;
import com.rk.fooddelivery.order.entity.Order; import jakarta.persistence.LockModeType; import java.util.*; import org.springframework.data.jpa.repository.*;
public interface OrderRepository extends JpaRepository<Order,UUID>{ @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select o from Order o where o.id=:id") Optional<Order> findLockedById(UUID id); List<Order> findByCustomerIdOrderByIdDesc(UUID customerId); List<Order> findByRestaurantIdOrderByIdDesc(UUID restaurantId); }
