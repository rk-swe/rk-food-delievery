package com.rk.fooddelivery.delivery.repository;

import com.rk.fooddelivery.delivery.entity.DeliveryOffer;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryOfferRepository extends JpaRepository<DeliveryOffer, UUID> {
  List<DeliveryOffer> findByPartnerIdAndStatus(UUID partner, String status);
}
