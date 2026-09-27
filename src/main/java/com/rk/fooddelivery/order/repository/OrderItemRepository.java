package com.rk.fooddelivery.order.repository;
import com.rk.fooddelivery.order.entity.OrderItem; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface OrderItemRepository extends JpaRepository<OrderItem,UUID>{ List<OrderItem> findByOrderId(UUID orderId); }
