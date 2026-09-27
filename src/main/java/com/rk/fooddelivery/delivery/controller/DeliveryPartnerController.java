package com.rk.fooddelivery.delivery.controller;
import com.rk.fooddelivery.auth.*;
import com.rk.fooddelivery.delivery.dto.PartnerDtos.*;
import com.rk.fooddelivery.delivery.service.PartnerPresenceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/delivery-partners/me") public class DeliveryPartnerController {
 private final CurrentUser current; private final PartnerPresenceService presence; public DeliveryPartnerController(CurrentUser c,PartnerPresenceService p){current=c;presence=p;}
 @PatchMapping("/availability") PartnerResponse availability(@Valid @RequestBody PresenceRequest r){return presence.updateAvailability(current.requireRole(Role.DELIVERY_PARTNER).id(),r);}
 @PatchMapping("/location") PartnerResponse location(@Valid @RequestBody LocationRequest r){return presence.updateLocationAndReturn(current.requireRole(Role.DELIVERY_PARTNER).id(),r);}
}
