package com.rk.fooddelivery.event.inbox;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "inbox_entries")
class InboxEntry {
  @EmbeddedId private InboxEntryId id;

  @SuppressWarnings("unused") // Persisted by Hibernate through field access.
  private Instant receivedAt = Instant.now();

  protected InboxEntry() {}

  InboxEntry(InboxEntryId id) {
    this.id = id;
  }
}
