package com.rk.fooddelivery.city.controller;

import com.rk.fooddelivery.admin.service.AdminCrudService;
import com.rk.fooddelivery.auth.CurrentUser;
import com.rk.fooddelivery.city.dto.CityDtos.CityResponse;
import com.rk.fooddelivery.common.web.PageResponse;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cities")
public class CityController {
  private final AdminCrudService service;
  private final CurrentUser current;

  public CityController(AdminCrudService s, CurrentUser c) {
    service = s;
    current = c;
  }

  @GetMapping
  public PageResponse<CityResponse> list(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    current.require();
    return service.cities(true, page, size);
  }
}
