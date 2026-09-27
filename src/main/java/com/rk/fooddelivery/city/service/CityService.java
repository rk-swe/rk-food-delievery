package com.rk.fooddelivery.city.service;

import com.rk.fooddelivery.auth.CurrentUser;
import com.rk.fooddelivery.auth.Role;
import com.rk.fooddelivery.city.dto.CityDtos.CityPatch;
import com.rk.fooddelivery.city.dto.CityDtos.CityRequest;
import com.rk.fooddelivery.city.dto.CityDtos.CityResponse;
import com.rk.fooddelivery.city.entity.City;
import com.rk.fooddelivery.city.repository.CityRepository;
import com.rk.fooddelivery.common.error.DomainException;
import com.rk.fooddelivery.common.error.NotFoundException;
import com.rk.fooddelivery.common.web.PageResponse;
import com.rk.fooddelivery.restaurant.repository.RestaurantRepository;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CityService {
  private final CityRepository cities;
  private final CurrentUser current;
  private final RestaurantRepository restaurants;

  public CityService(CityRepository cities, CurrentUser current, RestaurantRepository restaurants) {
    this.cities = cities;
    this.current = current;
    this.restaurants = restaurants;
  }

  @Transactional
  public CityResponse create(CityRequest request) {
    UUID actor = current.requireRole(Role.ADMIN).id();
    try {
      City city =
          new City(
              UUID.randomUUID(),
              trim(request.name()),
              trim(request.state()),
              trim(request.country()),
              request.currency(),
              actor);
      cities.saveAndFlush(city);
      return response(city);
    } catch (DataIntegrityViolationException exception) {
      throw new DomainException("A city with this name, state and country already exists");
    }
  }

  @Transactional
  public CityResponse patch(UUID id, CityPatch request) {
    UUID actor = current.requireRole(Role.ADMIN).id();
    City city = cities.findById(id).orElseThrow(() -> new NotFoundException("City not found"));
    city.patch(
        trim(request.name()),
        trim(request.state()),
        trim(request.country()),
        request.currency(),
        actor);
    try {
      cities.saveAndFlush(city);
      return response(city);
    } catch (DataIntegrityViolationException exception) {
      throw new DomainException("A city with this name, state and country already exists");
    }
  }

  @Transactional
  public void deactivate(UUID id) {
    UUID actor = current.requireRole(Role.ADMIN).id();
    City city =
        cities.findLockedById(id).orElseThrow(() -> new NotFoundException("City not found"));
    if (!city.isActive()) return;
    if (hasActiveRestaurants(id)) throw new DomainException("City has active restaurants");
    city.deactivate(actor);
  }

  @Transactional(readOnly = true)
  public CityResponse get(UUID id) {
    boolean activeOnly = current.require().role() != Role.ADMIN;
    City city = cities.findById(id).orElseThrow(() -> new NotFoundException("City not found"));
    if (activeOnly && !city.isActive()) throw new NotFoundException("City not found");
    return response(city);
  }

  @Transactional(readOnly = true)
  public PageResponse<CityResponse> list(int page, int size) {
    boolean activeOnly = current.require().role() != Role.ADMIN;
    var result =
        cities.findVisible(
            activeOnly, PageRequest.of(page, size, Sort.by("name").ascending().and(Sort.by("id"))));
    return PageResponse.of(
        result.map(this::response).getContent(), page, size, result.getTotalElements());
  }

  private boolean hasActiveRestaurants(UUID cityId) {
    return restaurants.existsByCityIdAndActiveTrue(cityId);
  }

  private CityResponse response(City city) {
    return new CityResponse(
        city.getId(),
        city.getName(),
        city.getState(),
        city.getCountry(),
        city.getCurrency(),
        city.isActive());
  }

  private String trim(String value) {
    return value == null ? null : value.trim();
  }
}
