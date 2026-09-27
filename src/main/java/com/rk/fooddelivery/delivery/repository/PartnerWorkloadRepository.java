package com.rk.fooddelivery.delivery.repository;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * PostgreSQL order occupancy is retained as a narrow native query until lifecycle entities exist.
 */
@Repository
public class PartnerWorkloadRepository {
  private final JdbcTemplate jdbc;

  public PartnerWorkloadRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public boolean hasActiveDelivery(UUID partnerId) {
    return Boolean.TRUE.equals(
        jdbc.queryForObject(
            "SELECT exists(SELECT 1 FROM orders WHERE delivery_partner_id=? AND order_status IN ('Accepted','Preparing','Ready for pickup','Out for delivery'))",
            Boolean.class,
            partnerId));
  }
}
