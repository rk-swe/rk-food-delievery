package com.rk.fooddelivery.order.service;

import com.rk.fooddelivery.auth.CurrentUser;
import com.rk.fooddelivery.auth.Role;
import com.rk.fooddelivery.cart.entity.Cart;
import com.rk.fooddelivery.cart.repository.CartItemRepository;
import com.rk.fooddelivery.cart.repository.CartRepository;
import com.rk.fooddelivery.common.error.DomainException;
import com.rk.fooddelivery.common.error.NotFoundException;
import com.rk.fooddelivery.event.dto.DomainEvent;
import com.rk.fooddelivery.event.dto.DomainEventType;
import com.rk.fooddelivery.event.outbox.OutboxService;
import com.rk.fooddelivery.menu.entity.MenuItem;
import com.rk.fooddelivery.menu.repository.MenuItemRepository;
import com.rk.fooddelivery.order.dto.OrderDtos.*;
import com.rk.fooddelivery.order.entity.Order;
import com.rk.fooddelivery.order.entity.OrderItem;
import com.rk.fooddelivery.order.repository.OrderItemRepository;
import com.rk.fooddelivery.order.repository.OrderRepository;
import com.rk.fooddelivery.payment.entity.Payment;
import com.rk.fooddelivery.payment.repository.PaymentRepository;
import com.rk.fooddelivery.restaurant.repository.RestaurantRepository;
import com.rk.fooddelivery.user.entity.User;
import com.rk.fooddelivery.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service public class CheckoutService {
  private final CartRepository carts; private final CartItemRepository cartItems; private final MenuItemRepository menu;
  private final OrderRepository orders; private final OrderItemRepository orderItems; private final PaymentRepository payments;
  private final UserRepository users; private final RestaurantRepository restaurants; private final OutboxService outbox; private final CurrentUser current; private final Clock clock;
  public CheckoutService(CartRepository carts,CartItemRepository cartItems,MenuItemRepository menu,OrderRepository orders,OrderItemRepository orderItems,PaymentRepository payments,UserRepository users,RestaurantRepository restaurants,OutboxService outbox,CurrentUser current,Clock clock){this.carts=carts;this.cartItems=cartItems;this.menu=menu;this.orders=orders;this.orderItems=orderItems;this.payments=payments;this.users=users;this.restaurants=restaurants;this.outbox=outbox;this.current=current;this.clock=clock;}
  @Transactional public OrderResponse place(UUID customerId,String idempotencyKey,CheckoutRequest request){
    if(!current.requireRole(Role.CUSTOMER).id().equals(customerId)) throw new org.springframework.security.access.AccessDeniedException("Customer does not own checkout");
    if(idempotencyKey==null||idempotencyKey.isBlank()) throw new DomainException("Idempotency-Key is required");
    User customer=users.findLockedById(customerId).orElseThrow(()->new NotFoundException("Customer not found"));
    if(customer.getCartVersion()!=request.cartVersion()) throw new DomainException("Cart version is stale");
    Cart cart=carts.findByCustomerId(customerId).orElseThrow(()->new DomainException("Cart is empty"));
    var lines=cartItems.findByCartIdOrderByMenuItemId(cart.getId()); if(lines.isEmpty())throw new DomainException("Cart is empty");
    UUID orderId=UUID.randomUUID(); BigDecimal total=BigDecimal.ZERO; List<OrderItem> snapshots=new ArrayList<>();
    for(var line:lines){MenuItem item=menu.findById(line.getMenuItemId()).orElseThrow(()->new NotFoundException("Menu item not found")); if(menu.reserve(item.getId(),cart.getRestaurantId(),line.getQuantity())!=1)throw new DomainException("Insufficient stock"); total=total.add(item.getPrice().multiply(BigDecimal.valueOf(line.getQuantity()))); snapshots.add(new OrderItem(UUID.randomUUID(),orderId,item.getId(),line.getQuantity(),item.getPrice()));}
    var restaurant=restaurants.findById(cart.getRestaurantId()).orElseThrow(()->new NotFoundException("Restaurant not found")); Instant deadline=clock.instant().plus(Duration.ofMinutes(15));
    Order order=new Order(orderId,customerId,cart.getRestaurantId(),total,request.addressLine1(),request.city(),request.state(),request.country(),restaurant.getLocation(),deadline); orders.save(order); orderItems.saveAll(snapshots); payments.save(new Payment(UUID.randomUUID(),orderId,request.paymentMethod(),total,deadline)); cartItems.deleteByCartId(cart.getId()); customer.incrementCartVersion();
    outbox.append(new DomainEvent(UUID.randomUUID(),DomainEventType.ORDER_PLACED,orderId,order.getVersion(),clock.instant(),Map.of("customerId",customerId.toString(),"restaurantId",cart.getRestaurantId().toString())));
    return response(order,snapshots);
  }
  @Transactional(readOnly=true) public OrderResponse get(UUID id){Order order=orders.findById(id).orElseThrow(()->new NotFoundException("Order not found")); return response(order,orderItems.findByOrderId(id));}
  private OrderResponse response(Order order,List<OrderItem> lines){return new OrderResponse(order.getId(),order.getCustomerId(),order.getRestaurantId(),order.status().value(),order.paymentStatus().value(),order.getTotalAmount(),order.getVersion(),order.getPaymentDeadlineAt(),order.getAssignmentStatus(),lines.stream().map(l->new OrderItemResponse(l.getMenuItemId(),l.getQuantity(),null)).toList());}
}
