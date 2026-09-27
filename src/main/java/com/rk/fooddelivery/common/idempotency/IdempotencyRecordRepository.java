package com.rk.fooddelivery.common.idempotency;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, UUID> {
  Optional<IdempotencyRecord> findByActorIdAndOperationAndKey(
      UUID actorId, String operation, String key);
}
