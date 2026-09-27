package com.rk.fooddelivery.restaurant.controller;

import com.rk.fooddelivery.restaurant.dto.CuisineResponse;
import com.rk.fooddelivery.restaurant.service.CuisineService;
import jakarta.validation.constraints.*;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cuisines")
public class CuisineController {
  private final CuisineService service;

  public CuisineController(CuisineService service) {
    this.service = service;
  }

  @GetMapping
  List<CuisineResponse> list(@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return service.list(size);
  }
}
