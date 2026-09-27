package com.rk.fooddelivery.event.inbox;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface InboxEntryRepository extends JpaRepository<InboxEntry, InboxEntryId> {
  @Modifying
  @Query(
      value =
          "insert into inbox_entries (consumer, event_id, received_at) values (:consumer, :eventId, current_timestamp) on conflict do nothing",
      nativeQuery = true)
  int insertIfAbsent(String consumer, UUID eventId);
}
