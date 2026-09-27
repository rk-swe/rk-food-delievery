package com.rk.fooddelivery.restaurant.repository;

import com.rk.fooddelivery.restaurant.dto.RestaurantSearchRequest;
import com.rk.fooddelivery.restaurant.entity.Restaurant;
import jakarta.persistence.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

@Repository
public class RestaurantSearchRepositoryImpl implements RestaurantSearchRepository {
  @PersistenceContext private EntityManager entityManager;

  public Page<Restaurant> searchPublic(RestaurantSearchRequest r) {
    String filters =
        " r.active=true and exists (select 1 from cities city where city.id=r.city_id and city.active=true)"
            + text("r.name", "name", r.name())
            + cuisine(r.cuisine())
            + eq("r.diet_type", "dietType", r.dietType())
            + le("r.cost_for_two", "maxCost", r.maxCostForTwo())
            + distance(r);
    String from = " from restaurants r ";
    String sql = "select r.*" + from + " where" + filters + " order by lower(r.name),r.id";
    Query query = entityManager.createNativeQuery(sql, Restaurant.class);
    bind(query, r);
    query.setFirstResult(r.page() * r.size());
    query.setMaxResults(r.size());
    @SuppressWarnings("unchecked")
    List<Restaurant> content = query.getResultList();
    Query count =
        entityManager.createNativeQuery("select count(distinct r.id)" + from + " where" + filters);
    bind(count, r);
    Number total = (Number) count.getSingleResult();
    return new PageImpl<>(content, PageRequest.of(r.page(), r.size()), total.longValue());
  }

  private String text(String field, String key, String value) {
    return value == null
        ? ""
        : " and lower(" + field + ") like lower(concat('%',:" + key + ",'%'))";
  }

  private String cuisine(String value) {
    return value == null
        ? ""
        : " and exists (select 1 from restaurant_cuisines rc join cuisines c on c.id=rc.cuisine_id where rc.restaurant_id=r.id and lower(c.name) like lower(concat('%',:cuisine,'%')))";
  }

  private String eq(String field, String key, Object value) {
    return value == null ? "" : " and " + field + "=:" + key;
  }

  private String le(String field, String key, Object value) {
    return value == null ? "" : " and " + field + "<=:" + key;
  }

  private String distance(RestaurantSearchRequest r) {
    if (r.latitude() == null && r.longitude() == null && r.radiusMeters() == null) return "";
    if (r.latitude() == null || r.longitude() == null || r.radiusMeters() == null)
      throw new IllegalArgumentException(
          "latitude, longitude and radiusMeters must be supplied together");
    return " and ST_DWithin(r.location,ST_SetSRID(ST_MakePoint(:longitude,:latitude),4326)::geography,:radiusMeters)";
  }

  private void bind(Query q, RestaurantSearchRequest r) {
    if (r.name() != null) q.setParameter("name", r.name());
    if (r.cuisine() != null) q.setParameter("cuisine", r.cuisine());
    if (r.dietType() != null) q.setParameter("dietType", r.dietType());
    if (r.maxCostForTwo() != null) q.setParameter("maxCost", r.maxCostForTwo());
    if (r.latitude() != null) {
      q.setParameter("latitude", r.latitude());
      q.setParameter("longitude", r.longitude());
      q.setParameter("radiusMeters", r.radiusMeters());
    }
  }
}
