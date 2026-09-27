package com.rk.fooddelivery.delivery.controller;

import com.rk.fooddelivery.delivery.dto.PartnerDtos.*;
import com.rk.fooddelivery.delivery.service.PartnerPresenceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me/delivery-partner")
public class PartnerPresenceController {
  private final PartnerPresenceService presence;

  public PartnerPresenceController(PartnerPresenceService presence) {
    this.presence = presence;
  }

  @PatchMapping("/availability")
  PartnerResponse availability(@Valid @RequestBody PresenceRequest request) {
    return presence.updateAvailability(request);
  }

  @PatchMapping("/location")
  PartnerResponse location(@Valid @RequestBody LocationRequest request) {
    return presence.updateLocation(request);
  }
}
