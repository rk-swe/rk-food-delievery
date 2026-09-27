package com.rk.fooddelivery.delivery.repository;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Native PostGIS write keeps coordinate construction and timestamp authority in PostgreSQL. */
@Repository
public class PartnerPresenceRepository {
  private final JdbcTemplate jdbc;

  public PartnerPresenceRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public void updateLocation(UUID id, double longitude, double latitude) {
    jdbc.update(
        "UPDATE users SET location=ST_SetSRID(ST_MakePoint(?,?),4326)::geography, location_updated_at=CURRENT_TIMESTAMP, updated_by=? WHERE id=?",
        longitude,
        latitude,
        id,
        id);
  }
}
