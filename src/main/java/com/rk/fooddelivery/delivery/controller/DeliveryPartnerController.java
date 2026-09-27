package com.rk.fooddelivery.delivery.controller;

import com.rk.fooddelivery.delivery.dto.PartnerDtos.*;
import com.rk.fooddelivery.delivery.service.DeliveryPartnerService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/delivery-partners")
public class DeliveryPartnerController {
  private final DeliveryPartnerService partners;

  public DeliveryPartnerController(DeliveryPartnerService partners) {
    this.partners = partners;
  }

  @PostMapping
  ResponseEntity<PartnerResponse> create(@Valid @RequestBody PartnerRequest request) {
    PartnerResponse response = partners.create(request);
    return ResponseEntity.created(URI.create("/api/delivery-partners/" + response.id())).body(response);
  }

  @GetMapping
  com.rk.fooddelivery.common.web.PageResponse<PartnerResponse> list(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return partners.list(page, size);
  }

  @GetMapping("/{id}")
  PartnerResponse get(@PathVariable UUID id) {
    return partners.get(id);
  }

  @PatchMapping("/{id}")
  PartnerResponse patch(@PathVariable UUID id, @Valid @RequestBody PartnerPatch request) {
    return partners.patch(id, request);
  }

  @DeleteMapping("/{id}")
  ResponseEntity<Void> deactivate(@PathVariable UUID id) {
    partners.deactivate(id);
    return ResponseEntity.noContent().build();
  }
}
