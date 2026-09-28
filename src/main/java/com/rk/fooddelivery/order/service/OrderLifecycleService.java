package com.rk.fooddelivery.order.service;

import com.rk.fooddelivery.common.error.*;
import com.rk.fooddelivery.event.dto.*;
import com.rk.fooddelivery.event.outbox.OutboxService;
import com.rk.fooddelivery.order.dto.OrderDtos.*;
import com.rk.fooddelivery.order.entity.*;
import com.rk.fooddelivery.order.repository.*;
import com.rk.fooddelivery.payment.service.RefundService;
import com.rk.fooddelivery.restaurant.repository.RestaurantRepository;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderLifecycleService {
  private final OrderRepository orders;
  private final OrderItemRepository items;
  private final RestaurantRepository restaurants;
  private final StockReservationService stock;
  private final RefundService refunds;
  private final OutboxService outbox;
  private final Clock clock;

  public OrderLifecycleService(
      OrderRepository orders,
      OrderItemRepository items,
      RestaurantRepository restaurants,
      StockReservationService stock,
      RefundService refunds,
      OutboxService outbox,
      Clock clock) {
    this.orders = orders;
    this.items = items;
    this.restaurants = restaurants;
    this.stock = stock;
    this.refunds = refunds;
    this.outbox = outbox;
    this.clock = clock;
  }

  @Transactional
  public OrderResponse decide(UUID ownerId, UUID orderId, String decision, String reason) {
    Order order = owned(ownerId, orderId);
    if (order.paymentStatus() != PaymentStatus.SUCCESS)
      throw new DomainException("Paid payment is required");
    if (order.status() != OrderStatus.PLACED)
      throw new DomainException("Order cannot be decided in its current state");
    if ("accept".equalsIgnoreCase(decision)) {
      order.transition(OrderStatus.ACCEPTED);
      outbox.append(event(DomainEventType.ORDER_ACCEPTED, order));
    } else if ("reject".equalsIgnoreCase(decision)) {
      if (reason == null || reason.isBlank())
        throw new DomainException("Rejection reason is required");
      order.transition(OrderStatus.REJECTED);
      stock.releaseOnce(orderId);
      refunds.requestOnce(orderId, "restaurant-rejected");
    } else throw new DomainException("Decision must be accept or reject");
    return response(order);
  }

  @Transactional
  public OrderResponse startPreparation(UUID ownerId, UUID orderId) {
    Order order = owned(ownerId, orderId);
    transition(order, OrderStatus.ACCEPTED, OrderStatus.PREPARING);
    return response(order);
  }

  @Transactional
  public OrderResponse markReady(UUID ownerId, UUID orderId) {
    Order order = owned(ownerId, orderId);
    transition(order, OrderStatus.PREPARING, OrderStatus.READY_FOR_PICKUP);
    outbox.append(event(DomainEventType.ORDER_READY, order));
    return response(order);
  }

  @Transactional(readOnly = true)
  public List<OrderResponse> customerOrders(UUID customer) {
    return orders.findByCustomerIdOrderByIdDesc(customer).stream().map(this::response).toList();
  }

  @Transactional(readOnly = true)
  public List<OrderResponse> restaurantOrders(UUID owner, UUID restaurant) {
    if (!restaurants.findById(restaurant).filter(r -> r.getOwnerId().equals(owner)).isPresent())
      throw new NotFoundException("Restaurant not found");
    return orders.findByRestaurantIdOrderByIdDesc(restaurant).stream().map(this::response).toList();
  }

  private Order owned(UUID owner, UUID orderId) {
    Order order =
        orders.findLockedById(orderId).orElseThrow(() -> new NotFoundException("Order not found"));
    if (!restaurants
        .findById(order.getRestaurantId())
        .filter(r -> r.getOwnerId().equals(owner))
        .isPresent()) throw new NotFoundException("Order not found");
    return order;
  }

  private void transition(Order o, OrderStatus from, OrderStatus to) {
    if (o.status() != from) throw new DomainException("Illegal order transition");
    o.transition(to);
  }

  private DomainEvent event(DomainEventType type, Order o) {
    return new DomainEvent(
        UUID.randomUUID(),
        type,
        o.getId(),
        o.getVersion(),
        clock.instant(),
        Map.of("orderId", o.getId().toString()));
  }

  private OrderResponse response(Order o) {
    return new OrderResponse(
        o.getId(),
        o.getCustomerId(),
        o.getRestaurantId(),
        o.status().value(),
        o.paymentStatus().value(),
        o.getTotalAmount(),
        o.getVersion(),
        o.getPaymentDeadlineAt(),
        o.getAssignmentStatus(),
        items.findByOrderId(o.getId()).stream()
            .map(i -> new OrderItemResponse(i.getMenuItemId(), i.getQuantity(), null))
            .toList());
  }
}
