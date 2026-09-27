package com.rk.fooddelivery.delivery.controller;

import com.rk.fooddelivery.delivery.dto.PartnerDtos.*;
import com.rk.fooddelivery.delivery.service.PartnerPresenceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/me/delivery-partner")
@Tag(name = "Delivery Partners")
public class PartnerPresenceController {
  private final PartnerPresenceService presence;

  public PartnerPresenceController(PartnerPresenceService presence) {
    this.presence = presence;
  }

  @PatchMapping("/availability")
  @Operation(operationId = "updateMyAvailability", summary = "Update my availability", description = "Requires the DELIVERY_PARTNER role.")
  PartnerResponse availability(@Valid @RequestBody PresenceRequest request) {
    return presence.updateAvailability(request);
  }

  @PatchMapping("/location")
  @Operation(operationId = "updateMyLocation", summary = "Update my location", description = "Requires the DELIVERY_PARTNER role.")
  PartnerResponse location(@Valid @RequestBody LocationRequest request) {
    return presence.updateLocation(request);
  }
}
