package com.rk.fooddelivery.restaurant.service;

import com.rk.fooddelivery.auth.CurrentUser;
import com.rk.fooddelivery.common.web.PageResponse;
import com.rk.fooddelivery.restaurant.dto.RestaurantDtos.RestaurantResponse;
import com.rk.fooddelivery.restaurant.dto.RestaurantSearchRequest;
import com.rk.fooddelivery.restaurant.entity.Restaurant;
import com.rk.fooddelivery.restaurant.repository.RestaurantSearchRepository;
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

  public RestaurantSearchService(RestaurantSearchRepository repository, CurrentUser current) {
    this.repository = repository;
    this.current = current;
  }

  @Transactional(readOnly = true)
  public PageResponse<RestaurantResponse> search(RestaurantSearchRequest request) {
    current.require();
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
