package com.rk.fooddelivery.restaurant.service;

import com.rk.fooddelivery.auth.CurrentUser;
import com.rk.fooddelivery.auth.Role;
import com.rk.fooddelivery.common.web.PageResponse;
import com.rk.fooddelivery.restaurant.dto.RestaurantDtos.RestaurantResponse;
import com.rk.fooddelivery.restaurant.dto.RestaurantSearchRequest;
import com.rk.fooddelivery.restaurant.entity.Restaurant;
import com.rk.fooddelivery.restaurant.repository.RestaurantSearchRepository;
import com.rk.fooddelivery.user.repository.UserRepository;
import java.util.*;
import org.locationtech.jts.geom.Point;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RestaurantSearchService {
  private final RestaurantSearchRepository repository;
  private final CurrentUser current;
  private final UserRepository users;

  public RestaurantSearchService(
      RestaurantSearchRepository repository, CurrentUser current, UserRepository users) {
    this.repository = repository;
    this.current = current;
    this.users = users;
  }

  @Transactional(readOnly = true)
  public PageResponse<RestaurantResponse> search(RestaurantSearchRequest request) {
    var actor = current.require();
    if (request.minCostForTwo() != null
        && request.maxCostForTwo() != null
        && request.minCostForTwo().compareTo(request.maxCostForTwo()) > 0)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Minimum cost exceeds maximum cost");
    if ((request.latitude() == null) != (request.longitude() == null))
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Supply both latitude and longitude");
    if (request.spatial() && request.latitude() == null) {
      var location =
          actor.role() == Role.CUSTOMER
              ? users.findById(actor.id()).map(u -> u.getLocation()).orElse(null)
              : null;
      if (location == null)
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "A location is required for distance search");
      request = request.withLocation(location.getY(), location.getX());
    }
    var result = repository.search(request, actor);
    return PageResponse.of(
        result.getContent().stream().map(this::response).toList(),
        request.page(),
        request.size(),
        result.getTotalElements());
  }

  private RestaurantResponse response(Restaurant r) {
    Point p = r.getLocation();
    return new RestaurantResponse(
        r.getId(),
        r.getName(),
        r.getOwnerId(),
        r.getCityId(),
        r.getCostForTwo(),
        r.getDietType(),
        r.getAddressLine1(),
        r.getAddressLine2(),
        r.getDescription(),
        p.getY(),
        p.getX(),
        r.isActive());
  }
}
