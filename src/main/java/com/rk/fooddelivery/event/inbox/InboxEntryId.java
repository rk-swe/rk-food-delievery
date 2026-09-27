package com.rk.fooddelivery.event.inbox;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
class InboxEntryId implements Serializable {
  private String consumer;
  private UUID eventId;

  protected InboxEntryId() {}

  InboxEntryId(String consumer, UUID eventId) {
    this.consumer = consumer;
    this.eventId = eventId;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof InboxEntryId that
        && consumer.equals(that.consumer)
        && eventId.equals(that.eventId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(consumer, eventId);
  }
}
