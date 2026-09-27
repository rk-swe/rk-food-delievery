package com.rk.fooddelivery.delivery.service;

import com.rk.fooddelivery.common.error.*;
import com.rk.fooddelivery.delivery.entity.*;
import com.rk.fooddelivery.delivery.repository.*;
import com.rk.fooddelivery.order.entity.*;
import com.rk.fooddelivery.order.repository.*;
import com.rk.fooddelivery.user.repository.UserRepository;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryOfferService {
  private final DeliveryOfferRepository offers;
  private final OrderRepository orders;
  private final UserRepository users;
  private final Clock clock;

  public DeliveryOfferService(
      DeliveryOfferRepository offers, OrderRepository orders, UserRepository users, Clock clock) {
    this.offers = offers;
    this.orders = orders;
    this.users = users;
    this.clock = clock;
  }

  @Transactional
  public void createOffers(UUID orderId) {
    Order o = orders.findLockedById(orderId).orElseThrow();
    if (o.paymentStatus() != PaymentStatus.SUCCESS || o.status() == OrderStatus.REJECTED) return;
    var partners =
        users.findAll().stream()
            .filter(
                u ->
                    u.getRole() == com.rk.fooddelivery.auth.Role.DELIVERY_PARTNER
                        && u.isActive()
                        && u.isOnline())
            .toList();
    if (partners.isEmpty()) {
      o.assignmentStatus("No partners available");
      return;
    }
    int round = o.getAssignmentRound() + 1;
    partners.forEach(
        p ->
            offers.save(
                new DeliveryOffer(orderId, p.getId(), round, clock.instant().plusSeconds(60))));
    o.assignmentStatus("Searching");
  }

  @Transactional
  public Order acceptOffer(UUID partner, UUID offerId, int round) {
    DeliveryOffer offer =
        offers.findById(offerId).orElseThrow(() -> new NotFoundException("Offer not found"));
    if (!offer.getPartnerId().equals(partner)
        || offer.getRound() != round
        || !offer.active(clock.instant())) throw new DomainException("Offer is not active");
    Order order = orders.findLockedById(offer.getOrderId()).orElseThrow();
    if (order.getDeliveryPartnerId() != null) throw new DomainException("Order already assigned");
    offer.accept();
    order.assign(partner);
    return order;
  }
}
