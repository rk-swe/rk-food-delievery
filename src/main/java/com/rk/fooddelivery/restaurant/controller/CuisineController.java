package com.rk.fooddelivery.restaurant.controller;

import com.rk.fooddelivery.auth.CurrentUser;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cuisines")
public class CuisineController {
  private final JdbcTemplate jdbc;
  private final CurrentUser current;

  public CuisineController(JdbcTemplate j, CurrentUser c) {
    jdbc = j;
    current = c;
  }

  record CuisineResponse(UUID id, String name, String imageUrl) {}

  @GetMapping
  List<CuisineResponse> list(@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    current.require();
    return jdbc.query(
        "SELECT id,name,image_url FROM cuisines ORDER BY lower(name),id LIMIT ?",
        (rs, n) ->
            new CuisineResponse(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3)),
        size);
  }
}
