package com.rk.fooddelivery.event.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record DomainEvent(
    UUID id,
    DomainEventType type,
    UUID aggregateId,
    int aggregateVersion,
    Instant occurredAt,
    Map<String, Object> payload) {

  public DomainEvent {
    if (id == null || type == null || aggregateId == null || occurredAt == null || payload == null) {
      throw new IllegalArgumentException("A domain event requires its envelope fields");
    }
    if (aggregateVersion < 0) {
      throw new IllegalArgumentException("aggregateVersion must not be negative");
    }
    payload = Map.copyOf(payload);
  }

  public static DomainEvent orderAccepted(
      UUID eventId, UUID orderId, int orderVersion, Map<String, Object> payload) {
    return new DomainEvent(
        eventId, DomainEventType.ORDER_ACCEPTED, orderId, orderVersion, Instant.now(), payload);
  }
}
