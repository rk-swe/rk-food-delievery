package com.rk.fooddelivery.event.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "outbox_events")
class OutboxEvent {
  @Id private UUID id;

  @Column(name = "event_type", nullable = false)
  private String eventType;

  @Column(name = "aggregate_id", nullable = false)
  private UUID aggregateId;

  @Column(name = "aggregate_version", nullable = false)
  private int aggregateVersion;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> payload;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @Column(name = "published_at")
  private Instant publishedAt;

  @Column(name = "publish_attempts", nullable = false)
  private int publishAttempts;

  @Column(name = "last_error")
  private String lastError;

  protected OutboxEvent() {}

  OutboxEvent(
      UUID id,
      String eventType,
      UUID aggregateId,
      int aggregateVersion,
      Map<String, Object> payload,
      Instant occurredAt) {
    this.id = id;
    this.eventType = eventType;
    this.aggregateId = aggregateId;
    this.aggregateVersion = aggregateVersion;
    this.payload = payload;
    this.occurredAt = occurredAt;
  }

  UUID id() {
    return id;
  }

  String eventType() {
    return eventType;
  }

  UUID aggregateId() {
    return aggregateId;
  }

  int aggregateVersion() {
    return aggregateVersion;
  }

  Map<String, Object> payload() {
    return payload;
  }

  Instant occurredAt() {
    return occurredAt;
  }

  void publishedAt(Instant value) {
    publishedAt = value;
  }

  void failed(String error) {
    publishAttempts++;
    lastError = error;
  }
}
