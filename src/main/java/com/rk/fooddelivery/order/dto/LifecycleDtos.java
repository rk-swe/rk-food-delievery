package com.rk.fooddelivery.order.dto; import jakarta.validation.constraints.*;
public final class LifecycleDtos { private LifecycleDtos(){} public record RestaurantDecisionRequest(@NotBlank String decision,String reason){} }
