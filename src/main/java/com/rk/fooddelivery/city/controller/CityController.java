package com.rk.fooddelivery.city.controller;

import com.rk.fooddelivery.city.dto.CityDtos.*;
import com.rk.fooddelivery.city.service.CityService;
import com.rk.fooddelivery.common.web.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cities")
public class CityController {
  private final CityService service;

  public CityController(CityService service) {
    this.service = service;
  }

  @PostMapping
  ResponseEntity<CityResponse> create(@Valid @RequestBody CityRequest request) {
    CityResponse response = service.create(request);
    return ResponseEntity.created(URI.create("/api/cities/" + response.id())).body(response);
  }

  @GetMapping
  public PageResponse<CityResponse> list(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return service.list(page, size);
  }

  @GetMapping("/{id}")
  CityResponse get(@PathVariable UUID id) {
    return service.get(id);
  }

  @PatchMapping("/{id}")
  CityResponse patch(@PathVariable UUID id, @Valid @RequestBody CityPatch request) {
    return service.patch(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void deactivate(@PathVariable UUID id) {
    service.deactivate(id);
  }
}
