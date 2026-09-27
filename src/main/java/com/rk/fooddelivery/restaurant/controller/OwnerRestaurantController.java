package com.rk.fooddelivery.restaurant.controller;

import com.rk.fooddelivery.admin.service.AdminCrudService;
import com.rk.fooddelivery.auth.CurrentUser;
import com.rk.fooddelivery.auth.Role;
import com.rk.fooddelivery.common.web.PageResponse;
import com.rk.fooddelivery.restaurant.dto.RestaurantDtos.HoursPatch;
import com.rk.fooddelivery.restaurant.dto.RestaurantDtos.RestaurantResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/restaurants")
public class OwnerRestaurantController {
    private final AdminCrudService service;
    private final CurrentUser current;

    public OwnerRestaurantController(AdminCrudService service, CurrentUser current) {
        this.service = service;
        this.current = current;
    }

    @GetMapping("/mine")
    public PageResponse<RestaurantResponse> mine(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.restaurants(current.requireRole(Role.RESTAURANT_OWNER).id(), page, size);
    }

    @PatchMapping("/{id}/hours")
    public RestaurantResponse hours(@PathVariable UUID id, @Valid @RequestBody HoursPatch request) {
        UUID ownerId = current.requireRole(Role.RESTAURANT_OWNER).id();
        return service.updateRestaurantHours(ownerId, id, request.hours());
    }
}
