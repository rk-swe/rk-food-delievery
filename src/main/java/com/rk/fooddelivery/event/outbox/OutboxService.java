package com.rk.fooddelivery.event.outbox;

import com.rk.fooddelivery.event.dto.DomainEvent;
import com.rk.fooddelivery.event.dto.DomainEventType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxService {
  private final OutboxEventRepository events;
  public OutboxService(OutboxEventRepository events) {
    this.events = events;
  }

  @Transactional
  public void append(DomainEvent event) {
    if (!isAllowed(event.type())) {
      throw new IllegalArgumentException("Unsupported domain event type: " + event.type());
    }
    events.save(
        new OutboxEvent(
            event.id(),
            event.type().name(),
            event.aggregateId(),
            event.aggregateVersion(),
            event.payload(),
            event.occurredAt()));
  }

  private boolean isAllowed(DomainEventType type) {
    return type != null;
  }
}
