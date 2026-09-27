package com.rk.fooddelivery.restaurant.controller;

import com.rk.fooddelivery.restaurant.dto.CuisineResponse;
import com.rk.fooddelivery.restaurant.service.CuisineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.*;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cuisines")
@Tag(name = "Cuisines")
public class CuisineController {
  private final CuisineService service;

  public CuisineController(CuisineService service) {
    this.service = service;
  }

  @GetMapping
  @Operation(
      operationId = "listCuisines",
      summary = "List cuisines",
      description = "Available to every authenticated account.")
  List<CuisineResponse> list(@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return service.list(size);
  }
}
