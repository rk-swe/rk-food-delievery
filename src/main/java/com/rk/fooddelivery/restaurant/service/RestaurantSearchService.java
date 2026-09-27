package com.rk.fooddelivery.restaurant.service;

import com.rk.fooddelivery.auth.CurrentUser;
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
    if (request.radiusMeters() != null && request.latitude() == null && request.longitude() == null) {
      var location = users.findById(actor.id()).map(u -> u.getLocation()).orElse(null);
      if (location == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A location is required for radius search");
      }
      request = new RestaurantSearchRequest(request.name(), request.cuisine(), request.dietType(), request.maxCostForTwo(), location.getY(), location.getX(), request.radiusMeters(), request.page(), request.size());
    }
    if (request.hasIncompleteCoordinates())
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "latitude, longitude and radiusMeters must be supplied together");
    var result = repository.searchPublic(request);
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
