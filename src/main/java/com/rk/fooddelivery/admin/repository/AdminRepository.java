package com.rk.fooddelivery.admin.repository;

import com.rk.fooddelivery.city.dto.CityDtos.*;
import com.rk.fooddelivery.delivery.dto.PartnerDtos.*;
import com.rk.fooddelivery.restaurant.dto.RestaurantDtos.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AdminRepository {
  private final JdbcTemplate jdbc;

  public AdminRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public UUID insertCity(UUID actor, CityRequest request) {
    return jdbc.queryForObject(
        "INSERT INTO cities (name,state,country,currency,created_by,updated_by) VALUES (?,?,?,?,?,?) RETURNING id",
        UUID.class,
        request.name().trim(),
        request.state().trim(),
        request.country().trim(),
        request.currency(),
        actor,
        actor);
  }

  public void updateCity(UUID actor, UUID id, CityPatch request) {
    jdbc.update(
        "UPDATE cities SET name=COALESCE(?,name),state=COALESCE(?,state),country=COALESCE(?,country),currency=COALESCE(?,currency),updated_by=? WHERE id=?",
        trim(request.name()),
        trim(request.state()),
        trim(request.country()),
        request.currency(),
        actor,
        id);
  }

  public Boolean lockCity(UUID id) {
    return jdbc.query(
        "SELECT active FROM cities WHERE id=? FOR UPDATE",
        rs -> rs.next() ? rs.getBoolean(1) : null,
        id);
  }

  public boolean hasActiveRestaurants(UUID cityId) {
    return Boolean.TRUE.equals(
        jdbc.queryForObject(
            "SELECT exists(SELECT 1 FROM restaurants WHERE city_id=? AND active)",
            Boolean.class,
            cityId));
  }

  public void deactivateCity(UUID actor, UUID id) {
    jdbc.update("UPDATE cities SET active=false,updated_by=? WHERE id=?", actor, id);
  }

  public Optional<CityResponse> city(UUID id) {
    return jdbc.query(
        "SELECT id,name,state,country,currency,active FROM cities WHERE id=?",
        rs ->
            rs.next()
                ? Optional.of(
                    new CityResponse(
                        rs.getObject(1, UUID.class),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getString(4),
                        rs.getString(5),
                        rs.getBoolean(6)))
                : Optional.empty(),
        id);
  }

  public List<CityResponse> cities(boolean activeOnly, int limit, int offset) {
    return jdbc.query(
        "SELECT id,name,state,country,currency,active FROM cities WHERE (?=false OR active) ORDER BY lower(name),id LIMIT ? OFFSET ?",
        (rs, n) ->
            new CityResponse(
                rs.getObject(1, UUID.class),
                rs.getString(2),
                rs.getString(3),
                rs.getString(4),
                rs.getString(5),
                rs.getBoolean(6)),
        activeOnly,
        limit,
        offset);
  }

  public long cityCount(boolean activeOnly) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM cities WHERE (?=false OR active)", Long.class, activeOnly);
  }

  public UUID insertRestaurant(UUID actor, RestaurantRequest request) {
    return jdbc.queryForObject(
        "INSERT INTO restaurants (name,owner_id,city_id,cost_for_two,diet_type,address_line_1,address_line_2,description,location,created_by,updated_by) VALUES (?,?,?,?,?,?,?,?,ST_SetSRID(ST_MakePoint(?,?),4326)::geography,?,?) RETURNING id",
        UUID.class,
        request.name().trim(),
        request.ownerId(),
        request.cityId(),
        request.costForTwo(),
        request.dietType(),
        request.addressLine1().trim(),
        trim(request.addressLine2()),
        trim(request.description()),
        request.longitude(),
        request.latitude(),
        actor,
        actor);
  }

  public void updateRestaurant(UUID actor, UUID id, RestaurantPatch request) {
    jdbc.update(
        "UPDATE restaurants SET name=COALESCE(?,name),cost_for_two=COALESCE(?,cost_for_two),diet_type=COALESCE(?,diet_type),address_line_1=COALESCE(?,address_line_1),address_line_2=COALESCE(?,address_line_2),description=COALESCE(?,description),location=CASE WHEN ?::double precision IS NULL THEN location ELSE ST_SetSRID(ST_MakePoint(?::double precision,?::double precision),4326)::geography END,updated_by=? WHERE id=?",
        trim(request.name()),
        request.costForTwo(),
        request.dietType(),
        trim(request.addressLine1()),
        trim(request.addressLine2()),
        trim(request.description()),
        request.latitude(),
        request.longitude(),
        request.latitude(),
        actor,
        id);
  }

  public void deactivateRestaurant(UUID actor, UUID id) {
    jdbc.update("UPDATE restaurants SET active=false,updated_by=? WHERE id=?", actor, id);
  }

  public boolean isActiveRestaurantOwner(UUID id) {
    return Boolean.TRUE.equals(
        jdbc.queryForObject(
            "SELECT exists(SELECT 1 FROM users WHERE id=? AND role='restaurant_owner' AND active)",
            Boolean.class,
            id));
  }

  public boolean isRestaurantOwnedBy(UUID restaurantId, UUID ownerId) {
    return Boolean.TRUE.equals(
        jdbc.queryForObject(
            "SELECT exists(SELECT 1 FROM restaurants WHERE id=? AND owner_id=?)",
            Boolean.class,
            restaurantId,
            ownerId));
  }

  public void upsertRestaurantHours(UUID restaurantId, UUID ownerId, List<HoursRequest> hours) {
    for (HoursRequest hour : hours) {
      jdbc.update(
          "INSERT INTO restaurant_timings (restaurant_id,day,start_time,end_time,is_open,created_by,updated_by) VALUES (?,?,?,?,?,?,?) ON CONFLICT (restaurant_id,day) DO UPDATE SET start_time=EXCLUDED.start_time,end_time=EXCLUDED.end_time,is_open=EXCLUDED.is_open,updated_by=EXCLUDED.updated_by",
          restaurantId,
          hour.day(),
          hour.startTime(),
          hour.endTime(),
          hour.open(),
          ownerId,
          ownerId);
    }
  }

  public Optional<RestaurantResponse> restaurant(UUID id) {
    return jdbc.query(
        "SELECT id,name,owner_id,city_id,cost_for_two,diet_type,address_line_1,address_line_2,description,ST_Y(location::geometry),ST_X(location::geometry),active FROM restaurants WHERE id=?",
        rs -> rs.next() ? Optional.of(restaurant(rs)) : Optional.empty(),
        id);
  }

  public List<RestaurantResponse> restaurants(UUID ownerId, int limit, int offset) {
    String sql =
        "SELECT id,name,owner_id,city_id,cost_for_two,diet_type,address_line_1,address_line_2,description,ST_Y(location::geometry),ST_X(location::geometry),active FROM restaurants";
    return ownerId == null
        ? jdbc.query(
            sql + " ORDER BY lower(name),id LIMIT ? OFFSET ?",
            (rs, n) -> restaurant(rs),
            limit,
            offset)
        : jdbc.query(
            sql + " WHERE owner_id=? ORDER BY lower(name),id LIMIT ? OFFSET ?",
            (rs, n) -> restaurant(rs),
            ownerId,
            limit,
            offset);
  }

  public long restaurantCount(UUID ownerId) {
    return ownerId == null
        ? jdbc.queryForObject("SELECT count(*) FROM restaurants", Long.class)
        : jdbc.queryForObject(
            "SELECT count(*) FROM restaurants WHERE owner_id=?", Long.class, ownerId);
  }

  public UUID insertPartner(UUID actor, String name, String email, String phoneNumber) {
    return jdbc.queryForObject(
        "INSERT INTO users (name,email,phone_number,role,created_by,updated_by) VALUES (?,?,?,'delivery_partner',?,?) RETURNING id",
        UUID.class,
        name.trim(),
        email.trim(),
        phoneNumber,
        actor,
        actor);
  }

  public void insertCredentials(UUID userId, String username, String passwordHash) {
    jdbc.update(
        "INSERT INTO user_credentials (user_id,username,password_hash) VALUES (?,?,?)",
        userId,
        username.trim(),
        passwordHash);
  }

  public void updatePartner(UUID actor, UUID id, PartnerPatch request) {
    jdbc.update(
        "UPDATE users SET name=COALESCE(?,name),email=COALESCE(?,email),phone_number=COALESCE(?,phone_number),updated_by=? WHERE id=?",
        trim(request.name()),
        trim(request.email()),
        request.phoneNumber(),
        actor,
        id);
  }

  public boolean lockPartner(UUID id) {
    return Boolean.TRUE.equals(
        jdbc.query(
            "SELECT id FROM users WHERE id=? AND role='delivery_partner' FOR UPDATE",
            rs -> {
              return rs != null && rs.next();
            },
            id));
  }

  public boolean hasActiveDelivery(UUID partnerId) {
    return Boolean.TRUE.equals(
        jdbc.queryForObject(
            "SELECT exists(SELECT 1 FROM orders WHERE delivery_partner_id=? AND order_status IN ('Accepted','Preparing','Ready for pickup','Out for delivery'))",
            Boolean.class,
            partnerId));
  }

  public void deactivatePartner(UUID actor, UUID id) {
    jdbc.update("UPDATE users SET active=false,online=false,updated_by=? WHERE id=?", actor, id);
  }

  public boolean isActivePartner(UUID id) {
    return Boolean.TRUE.equals(
        jdbc.queryForObject(
            "SELECT exists(SELECT 1 FROM users WHERE id=? AND role='delivery_partner' AND active)",
            Boolean.class,
            id));
  }

  public void updatePartnerAvailability(UUID id, boolean online) {
    jdbc.update("UPDATE users SET online=?,updated_by=? WHERE id=?", online, id, id);
  }

  public void updatePartnerLocation(UUID id, double longitude, double latitude) {
    jdbc.update(
        "UPDATE users SET location=ST_SetSRID(ST_MakePoint(?,?),4326)::geography,location_updated_at=CURRENT_TIMESTAMP,updated_by=? WHERE id=?",
        longitude,
        latitude,
        id,
        id);
  }

  public Optional<PartnerResponse> partner(UUID id) {
    return jdbc.query(
        "SELECT id,name,email,phone_number,active,online,location_updated_at FROM users WHERE id=? AND role='delivery_partner'",
        rs -> rs.next() ? Optional.of(partner(rs)) : Optional.empty(),
        id);
  }

  public List<PartnerResponse> partners(int limit, int offset) {
    return jdbc.query(
        "SELECT id,name,email,phone_number,active,online,location_updated_at FROM users WHERE role='delivery_partner' ORDER BY lower(name),id LIMIT ? OFFSET ?",
        (rs, n) -> partner(rs),
        limit,
        offset);
  }

  public long partnerCount() {
    return jdbc.queryForObject(
        "SELECT count(*) FROM users WHERE role='delivery_partner'", Long.class);
  }

  private RestaurantResponse restaurant(ResultSet rs) throws SQLException {
    return new RestaurantResponse(
        rs.getObject(1, UUID.class),
        rs.getString(2),
        rs.getObject(3, UUID.class),
        rs.getObject(4, UUID.class),
        rs.getBigDecimal(5),
        rs.getString(6),
        rs.getString(7),
        rs.getString(8),
        rs.getString(9),
        rs.getDouble(10),
        rs.getDouble(11),
        rs.getBoolean(12));
  }

  private PartnerResponse partner(ResultSet rs) throws SQLException {
    var timestamp = rs.getTimestamp(7);
    return new PartnerResponse(
        rs.getObject(1, UUID.class),
        rs.getString(2),
        rs.getString(3),
        rs.getString(4),
        rs.getBoolean(5),
        rs.getBoolean(6),
        timestamp == null ? null : timestamp.toInstant());
  }

  private String trim(String value) {
    return value == null ? null : value.trim();
  }
}
