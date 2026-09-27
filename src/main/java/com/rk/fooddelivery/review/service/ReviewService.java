package com.rk.fooddelivery.review.service;

import com.rk.fooddelivery.common.error.*;
import com.rk.fooddelivery.order.repository.OrderRepository;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {
  private final OrderRepository orders;

  public ReviewService(OrderRepository orders) {
    this.orders = orders;
  }

  @Transactional
  public void submit(UUID customer, UUID orderId, int stars, String review) {
    if (stars < 1 || stars > 5) throw new DomainException("Stars must be between 1 and 5");
    var o =
        orders.findLockedById(orderId).orElseThrow(() -> new NotFoundException("Order not found"));
    if (!customer.equals(o.getCustomerId())) throw new NotFoundException("Order not found");
    try {
      o.review(stars, review);
    } catch (IllegalStateException e) {
      throw new DomainException(e.getMessage());
    }
  }
}
