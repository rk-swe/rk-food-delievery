package com.rk.fooddelivery.event.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_deliveries")
class NotificationDelivery {
  @Id private UUID id = UUID.randomUUID();
  @Column(nullable = false) private String recipient;
  @Column(name = "event_id", nullable = false) private UUID eventId;
  @Column(name = "aggregate_id", nullable = false) private UUID aggregateId;
  @Column(name = "aggregate_version", nullable = false) private int aggregateVersion;
  @Column(name = "event_type", nullable = false) private String eventType;
  @Column(nullable = false) private String status = "DELIVERED";
  @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();

  protected NotificationDelivery() {}

  NotificationDelivery(String recipient, UUID eventId, UUID aggregateId, int aggregateVersion, String eventType) {
    this.recipient = recipient;
    this.eventId = eventId;
    this.aggregateId = aggregateId;
    this.aggregateVersion = aggregateVersion;
    this.eventType = eventType;
  }
}
