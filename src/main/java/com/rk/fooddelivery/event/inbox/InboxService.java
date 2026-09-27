package com.rk.fooddelivery.event.inbox;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InboxService {
  private final InboxEntryRepository entries;

  public InboxService(InboxEntryRepository entries) {
    this.entries = entries;
  }

  @Transactional
  public boolean recordOnce(String consumer, UUID eventId) {
    return entries.insertIfAbsent(consumer, eventId) == 1;
  }
}
