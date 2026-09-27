package com.rk.fooddelivery.restaurant.repository;

import com.rk.fooddelivery.auth.AuthenticatedUser;
import com.rk.fooddelivery.auth.Role;
import com.rk.fooddelivery.restaurant.dto.RestaurantSearchRequest;
import com.rk.fooddelivery.restaurant.entity.Restaurant;
import jakarta.persistence.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

@Repository
public class RestaurantSearchRepositoryImpl implements RestaurantSearchRepository {
  @PersistenceContext private EntityManager entityManager;

  public Page<Restaurant> search(RestaurantSearchRequest request, AuthenticatedUser actor) {
    return request.spatial() ? spatialSearch(request, actor) : ordinarySearch(request, actor);
  }

  // Ordinary scalar filtering, visibility, pagination and counting all execute through JPQL.
  private Page<Restaurant> ordinarySearch(RestaurantSearchRequest r, AuthenticatedUser actor) {
    Map<String, Object> parameters = new HashMap<>();
    String filters = filters(r, actor, false, parameters);
    String from = " from Restaurant r where " + filters;
    var results =
        entityManager.createQuery(
            "select r" + from + " order by " + order(r, false), Restaurant.class);
    var count = entityManager.createQuery("select count(r)" + from, Long.class);
    parameters.forEach(
        (key, value) -> {
          results.setParameter(key, value);
          count.setParameter(key, value);
        });
    return new PageImpl<>(
        results.setFirstResult(r.page() * r.size()).setMaxResults(r.size()).getResultList(),
        PageRequest.of(r.page(), r.size()),
        count.getSingleResult());
  }

  /**
   * PostGIS geography is needed only for a radius or distance sort. ST_DWithin uses the geography
   * GiST index for meter-based radius filtering; ST_Distance orders exact distances. All ordinary
   * searches use JPQL above. Parameters and visibility are identical in both paths.
   */
  private Page<Restaurant> spatialSearch(RestaurantSearchRequest r, AuthenticatedUser actor) {
    Map<String, Object> parameters = new HashMap<>();
    String filters = filters(r, actor, true, parameters);
    String point = "ST_SetSRID(ST_MakePoint(:longitude,:latitude),4326)::geography";
    if (r.radiusMeters() != null) {
      filters += " and ST_DWithin(r.location," + point + ",:radiusMeters)";
      parameters.put("radiusMeters", r.radiusMeters());
    }
    String from = " from restaurants r where " + filters;
    String ordering =
        r.sortField().equals("distance")
            ? "ST_Distance(r.location," + point + ") asc,r.id"
            : order(r, true);
    Query results =
        entityManager.createNativeQuery(
            "select r.*" + from + " order by " + ordering, Restaurant.class);
    Query count = entityManager.createNativeQuery("select count(*)" + from);
    parameters.forEach(
        (key, value) -> {
          results.setParameter(key, value);
          count.setParameter(key, value);
        });
    results.setParameter("latitude", r.latitude()).setParameter("longitude", r.longitude());
    if (r.radiusMeters() != null)
      count.setParameter("latitude", r.latitude()).setParameter("longitude", r.longitude());
    @SuppressWarnings("unchecked")
    List<Restaurant> content =
        results.setFirstResult(r.page() * r.size()).setMaxResults(r.size()).getResultList();
    return new PageImpl<>(
        content,
        PageRequest.of(r.page(), r.size()),
        ((Number) count.getSingleResult()).longValue());
  }

  private String filters(
      RestaurantSearchRequest r, AuthenticatedUser actor, boolean sql, Map<String, Object> p) {
    String city = sql ? "r.city_id" : "r.cityId";
    String cost = sql ? "r.cost_for_two" : "r.costForTwo";
    StringBuilder where = new StringBuilder("1=1");
    if (actor.role() == Role.RESTAURANT_OWNER) {
      where.append(" and ").append(sql ? "r.owner_id" : "r.ownerId").append("=:ownerId");
      p.put("ownerId", actor.id());
    } else if (actor.role() != Role.ADMIN) {
      where
          .append(" and r.active=true and exists (select 1 from ")
          .append(sql ? "cities" : "City")
          .append(" c where c.id=")
          .append(city)
          .append(" and c.active=true)");
    }
    if (r.name() != null) {
      where.append(" and lower(r.name) like :name escape '!' ");
      p.put("name", like(r.name()));
    }
    if (r.cityId() != null) {
      where.append(" and ").append(city).append("=:cityId");
      p.put("cityId", r.cityId());
    }
    if (r.dietType() != null) {
      where.append(" and ").append(sql ? "r.diet_type" : "r.dietType").append("=:dietType");
      p.put("dietType", r.dietType());
    }
    if (r.minCostForTwo() != null) {
      where.append(" and ").append(cost).append(">=:minCost");
      p.put("minCost", r.minCostForTwo());
    }
    if (r.maxCostForTwo() != null) {
      where.append(" and ").append(cost).append("<=:maxCost");
      p.put("maxCost", r.maxCostForTwo());
    }
    if (r.cuisine() != null || !r.cuisineIds().isEmpty()) {
      where.append(
          sql
              ? " and exists (select 1 from restaurant_cuisines rc join cuisines cu on cu.id=rc.cuisine_id where rc.restaurant_id=r.id"
              : " and exists (select 1 from Restaurant cr join cr.cuisines cu where cr.id=r.id");
      if (r.cuisine() != null) {
        where.append(" and lower(cu.name) like :cuisine escape '!' ");
        p.put("cuisine", like(r.cuisine()));
      }
      if (!r.cuisineIds().isEmpty()) {
        where.append(" and cu.id in (:cuisineIds)");
        p.put("cuisineIds", r.cuisineIds());
      }
      where.append(")");
    }
    return where.toString();
  }

  private String order(RestaurantSearchRequest r, boolean sql) {
    String field =
        switch (r.sortField()) {
          case "name" -> "lower(r.name)";
          case "rating" -> sql ? "r.average_rating" : "r.averageRating";
          case "cost" -> sql ? "r.cost_for_two" : "r.costForTwo";
          default -> throw new IllegalArgumentException("Unsupported restaurant sort");
        };
    return field + (r.descending() ? " desc" : " asc") + ",r.id";
  }

  private String like(String value) {
    return "%"
        + value.toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_")
        + "%";
  }
}
