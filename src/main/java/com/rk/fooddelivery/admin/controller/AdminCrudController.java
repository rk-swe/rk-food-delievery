package com.rk.fooddelivery.admin.controller;

import com.rk.fooddelivery.admin.service.AdminCrudService;
import com.rk.fooddelivery.auth.*;
import com.rk.fooddelivery.city.dto.CityDtos.*;
import com.rk.fooddelivery.common.web.PageResponse;
import com.rk.fooddelivery.delivery.dto.PartnerDtos.*;
import com.rk.fooddelivery.restaurant.dto.RestaurantDtos.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminCrudController {
  private final AdminCrudService service;
  private final CurrentUser current;

  public AdminCrudController(AdminCrudService service, CurrentUser current) {
    this.service = service;
    this.current = current;
  }

  private UUID admin() {
    return current.requireRole(Role.ADMIN).id();
  }

  @PostMapping("/cities")
  @ResponseStatus(HttpStatus.CREATED)
  CityResponse createCity(@Valid @RequestBody CityRequest r) {
    return service.createCity(admin(), r);
  }

  @GetMapping("/cities")
  PageResponse<CityResponse> cities(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    admin();
    return service.cities(false, page, size);
  }

  @GetMapping("/cities/{id}")
  CityResponse city(@PathVariable UUID id) {
    admin();
    return service.city(id);
  }

  @PatchMapping("/cities/{id}")
  CityResponse patchCity(@PathVariable UUID id, @Valid @RequestBody CityPatch r) {
    return service.patchCity(admin(), id, r);
  }

  @DeleteMapping("/cities/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void deactivateCity(@PathVariable UUID id) {
    service.deactivateCity(admin(), id);
  }

  @PostMapping("/restaurants")
  @ResponseStatus(HttpStatus.CREATED)
  RestaurantResponse createRestaurant(@Valid @RequestBody RestaurantRequest r) {
    return service.createRestaurant(admin(), r);
  }

  @GetMapping("/restaurants")
  PageResponse<RestaurantResponse> restaurants(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    admin();
    return service.restaurants(null, page, size);
  }

  @GetMapping("/restaurants/{id}")
  RestaurantResponse restaurant(@PathVariable UUID id) {
    admin();
    return service.restaurant(id);
  }

  @PatchMapping("/restaurants/{id}")
  RestaurantResponse patchRestaurant(@PathVariable UUID id, @Valid @RequestBody RestaurantPatch r) {
    return service.patchRestaurant(admin(), id, r);
  }

  @DeleteMapping("/restaurants/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void deactivateRestaurant(@PathVariable UUID id) {
    service.deactivateRestaurant(admin(), id);
  }

  @PostMapping("/delivery-partners")
  @ResponseStatus(HttpStatus.CREATED)
  PartnerResponse createPartner(@Valid @RequestBody PartnerRequest r) {
    return service.createPartner(admin(), r);
  }

  @GetMapping("/delivery-partners")
  PageResponse<PartnerResponse> partners(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    admin();
    return service.partners(page, size);
  }

  @GetMapping("/delivery-partners/{id}")
  PartnerResponse partner(@PathVariable UUID id) {
    admin();
    return service.partner(id);
  }

  @PatchMapping("/delivery-partners/{id}")
  PartnerResponse patchPartner(@PathVariable UUID id, @Valid @RequestBody PartnerPatch r) {
    return service.patchPartner(admin(), id, r);
  }

  @DeleteMapping("/delivery-partners/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void deactivatePartner(@PathVariable UUID id) {
    service.deactivatePartner(admin(), id);
  }
}
