package com.rk.fooddelivery.review.controller;

import com.rk.fooddelivery.auth.*;
import com.rk.fooddelivery.review.service.ReviewService;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class ReviewController {
  record Request(@Min(1) @Max(5) int stars, @Size(max = 2000) String review) {}

  private final ReviewService reviews;
  private final CurrentUser current;

  public ReviewController(ReviewService reviews, CurrentUser current) {
    this.reviews = reviews;
    this.current = current;
  }

  @PostMapping("/{id}/reviews")
  @ResponseStatus(HttpStatus.CREATED)
  public void submit(@PathVariable UUID id, @RequestBody Request request) {
    reviews.submit(current.requireRole(Role.CUSTOMER).id(), id, request.stars(), request.review());
  }
}
