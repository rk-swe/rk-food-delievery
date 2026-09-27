package com.rk.fooddelivery.payment.repository;

import com.rk.fooddelivery.payment.entity.PaymentRefund;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRefundRepository extends JpaRepository<PaymentRefund, UUID> {
  boolean existsByOrderIdAndReason(UUID orderId, String reason);
}
