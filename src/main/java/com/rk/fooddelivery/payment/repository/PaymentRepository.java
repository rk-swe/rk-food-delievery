package com.rk.fooddelivery.payment.repository;
import com.rk.fooddelivery.payment.entity.Payment; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface PaymentRepository extends JpaRepository<Payment,UUID>{ Optional<Payment> findFirstByOrderIdOrderByIdDesc(UUID orderId); }
