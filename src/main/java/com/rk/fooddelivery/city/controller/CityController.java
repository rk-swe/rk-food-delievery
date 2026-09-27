package com.rk.fooddelivery.city.controller;

import com.rk.fooddelivery.city.dto.CityDtos.*;
import com.rk.fooddelivery.city.service.CityService;
import com.rk.fooddelivery.common.web.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.net.URI;
import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cities")
@Tag(name = "Cities")
public class CityController {
  private final CityService service;

  public CityController(CityService service) {
    this.service = service;
  }

  @PostMapping
  @Operation(operationId = "createCity", summary = "Create a city", description = "Requires the ADMIN role.")
  ResponseEntity<CityResponse> create(@Valid @RequestBody CityRequest request) {
    CityResponse response = service.create(request);
    return ResponseEntity.created(URI.create("/api/cities/" + response.id())).body(response);
  }

  @GetMapping
  @Operation(operationId = "listCities", summary = "List cities", description = "Authenticated users see active cities; administrators see all cities.")
  public PageResponse<CityResponse> list(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return service.list(page, size);
  }

  @GetMapping("/{id}")
  @Operation(operationId = "getCity", summary = "Get a city", description = "Authenticated users see active cities; administrators see all cities.")
  CityResponse get(@PathVariable UUID id) {
    return service.get(id);
  }

  @PatchMapping("/{id}")
  @Operation(operationId = "patchCity", summary = "Update a city", description = "Requires the ADMIN role.")
  CityResponse patch(@PathVariable UUID id, @Valid @RequestBody CityPatch request) {
    return service.patch(id, request);
  }

  @DeleteMapping("/{id}")
  @Operation(operationId = "deactivateCity", summary = "Deactivate a city", description = "Requires the ADMIN role.")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void deactivate(@PathVariable UUID id) {
    service.deactivate(id);
  }
}
