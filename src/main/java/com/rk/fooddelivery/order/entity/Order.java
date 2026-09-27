package com.rk.fooddelivery.order.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "orders")
public class Order {
  @Id private UUID id;

  @Column(name = "customer_id", nullable = false)
  private UUID customerId;

  @Column(name = "restaurant_id", nullable = false)
  private UUID restaurantId;

  @Column(name = "order_status", nullable = false)
  private String orderStatus;

  @Column(name = "payment_status", nullable = false)
  private String paymentStatus;

  @Column(name = "delivery_partner_id")
  private UUID deliveryPartnerId;

  @Column(name = "sub_total_amount", nullable = false)
  private BigDecimal subTotal;

  @Column(name = "delivery_fee", nullable = false)
  private BigDecimal deliveryFee = BigDecimal.ZERO;

  @Column(name = "platform_fee", nullable = false)
  private BigDecimal platformFee = BigDecimal.ZERO;

  @Column(name = "tax_percent", nullable = false)
  private BigDecimal taxPercent = BigDecimal.ZERO;

  @Column(name = "tax_amount", nullable = false)
  private BigDecimal taxAmount = BigDecimal.ZERO;

  @Column(name = "discount_amount", nullable = false)
  private BigDecimal discountAmount = BigDecimal.ZERO;

  @Column(name = "total_amount", nullable = false)
  private BigDecimal totalAmount;

  @Column(name = "address_line_1", nullable = false)
  private String addressLine1;

  @Column(nullable = false)
  private String city;

  @Column(nullable = false)
  private String state;

  @Column(nullable = false)
  private String country;

  @JdbcTypeCode(SqlTypes.GEOGRAPHY)
  @Column(columnDefinition = "geography(Point,4326)")
  private Point location;

  @Column(name = "version", nullable = false)
  private int version;

  @Column(name = "payment_deadline_at")
  private Instant paymentDeadlineAt;

  @Column(name = "restaurant_response_at")
  private Instant restaurantResponseAt;

  @Column(name = "stock_released_at")
  private Instant stockReleasedAt;

  @Column(name = "assignment_status", nullable = false)
  private String assignmentStatus = "Not started";

  @Column(name = "assignment_round", nullable = false)
  private int assignmentRound;

  protected Order() {}

  public Order(
      UUID id,
      UUID customerId,
      UUID restaurantId,
      BigDecimal subTotal,
      String addressLine1,
      String city,
      String state,
      String country,
      Point location,
      Instant deadline) {
    this.id = id;
    this.customerId = customerId;
    this.restaurantId = restaurantId;
    this.subTotal = subTotal;
    this.totalAmount = subTotal;
    this.addressLine1 = addressLine1;
    this.city = city;
    this.state = state;
    this.country = country;
    this.location = location;
    this.orderStatus = OrderStatus.PLACED.value();
    this.paymentStatus = PaymentStatus.PENDING.value();
    this.paymentDeadlineAt = deadline;
  }

  public UUID getId() {
    return id;
  }

  public UUID getCustomerId() {
    return customerId;
  }

  public UUID getRestaurantId() {
    return restaurantId;
  }

  public UUID getDeliveryPartnerId() {
    return deliveryPartnerId;
  }

  public int getVersion() {
    return version;
  }

  public BigDecimal getTotalAmount() {
    return totalAmount;
  }

  public OrderStatus status() {
    return OrderStatus.from(orderStatus);
  }

  public PaymentStatus paymentStatus() {
    return PaymentStatus.from(paymentStatus);
  }

  public Instant getPaymentDeadlineAt() {
    return paymentDeadlineAt;
  }

  public boolean stockReleased() {
    return stockReleasedAt != null;
  }

  public void transition(OrderStatus next) {
    this.orderStatus = next.value();
    version++;
  }

  public void payment(PaymentStatus next) {
    this.paymentStatus = next.value();
    version++;
  }

  public boolean releaseStock(Instant at) {
    if (stockReleasedAt != null) return false;
    stockReleasedAt = at;
    version++;
    return true;
  }

  public void assign(UUID partner) {
    deliveryPartnerId = partner;
    assignmentStatus = "Assigned";
    assignmentRound++;
    version++;
  }

  public void assignmentStatus(String value) {
    assignmentStatus = value;
    version++;
  }

  public String getAssignmentStatus() {
    return assignmentStatus;
  }

  public int getAssignmentRound() {
    return assignmentRound;
  }

  public void review(int rating, String text) {
    if (status() != OrderStatus.DELIVERED)
      throw new IllegalStateException("Order is not delivered");
    if (orderRating != null) throw new IllegalStateException("Order already reviewed");
    orderRating = rating;
    orderRatingReview = text;
    version++;
  }

  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.SMALLINT)
  @Column(name = "order_rating")
  private Integer orderRating;

  @Column(name = "order_rating_review")
  private String orderRatingReview;
}
