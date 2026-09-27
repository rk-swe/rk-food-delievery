package com.rk.fooddelivery.restaurant.controller;

import com.rk.fooddelivery.common.web.PageResponse;
import com.rk.fooddelivery.restaurant.dto.RestaurantDtos.*;
import com.rk.fooddelivery.restaurant.dto.RestaurantSearchRequest;
import com.rk.fooddelivery.restaurant.service.RestaurantSearchService;
import com.rk.fooddelivery.restaurant.service.RestaurantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/restaurants")
@Tag(name = "Restaurants")
public class RestaurantController {
  private final RestaurantService service;
  private final RestaurantSearchService searchService;

  public RestaurantController(RestaurantService service, RestaurantSearchService searchService) {
    this.service = service;
    this.searchService = searchService;
  }

  @PostMapping
  @Operation(
      operationId = "createRestaurant",
      summary = "Create a restaurant",
      description = "Requires the ADMIN role.")
  public ResponseEntity<RestaurantResponse> create(@Valid @RequestBody RestaurantRequest request) {
    RestaurantResponse response = service.create(request);
    return ResponseEntity.created(URI.create("/api/restaurants/" + response.id())).body(response);
  }

  @GetMapping
  @Operation(
      operationId = "listRestaurants",
      summary = "Search restaurants",
      description =
          "Admins see all restaurants; owners see their own; customers and partners see active restaurants in active cities. Sort: name, rating, cost, distance with optional asc/desc direction; distance requires coordinates or stored customer location.")
  public PageResponse<RestaurantResponse> list(
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String cuisine,
      @RequestParam(required = false) @Pattern(regexp = "Veg|Non Veg") String dietType,
      @RequestParam(required = false) UUID cityId,
      @RequestParam(required = false) java.util.List<UUID> cuisineIds,
      @RequestParam(required = false) @DecimalMin("0.0") java.math.BigDecimal minCostForTwo,
      @RequestParam(defaultValue = "name")
          @Pattern(
              regexp = "name(,asc|,desc)?|rating(,asc|,desc)?|cost(,asc|,desc)?|distance(,asc)?")
          String sort,
      @RequestParam(required = false) @DecimalMin("0.0") java.math.BigDecimal maxCostForTwo,
      @RequestParam(required = false) @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
      @RequestParam(required = false) @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
      @RequestParam(required = false) @DecimalMin("0.0") Double radiusMeters,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return searchService.search(
        new RestaurantSearchRequest(
            name,
            cuisine,
            dietType,
            cityId,
            cuisineIds,
            minCostForTwo,
            maxCostForTwo,
            sort,
            latitude,
            longitude,
            radiusMeters,
            page,
            size));
  }

  @GetMapping("/{id}")
  @Operation(
      operationId = "getRestaurant",
      summary = "Get a restaurant",
      description = "Visibility depends on the authenticated role.")
  public RestaurantResponse get(@PathVariable UUID id) {
    return service.get(id);
  }

  @PatchMapping("/{id}")
  @Operation(
      operationId = "patchRestaurant",
      summary = "Update a restaurant",
      description = "Requires the ADMIN role.")
  public RestaurantResponse patch(
      @PathVariable UUID id, @Valid @RequestBody RestaurantPatch request) {
    return service.patch(id, request);
  }

  @DeleteMapping("/{id}")
  @Operation(
      operationId = "deactivateRestaurant",
      summary = "Deactivate a restaurant",
      description = "Requires the ADMIN role.")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deactivate(@PathVariable UUID id) {
    service.deactivate(id);
  }
}
