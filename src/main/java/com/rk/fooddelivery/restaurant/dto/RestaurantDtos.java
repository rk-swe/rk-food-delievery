package com.rk.fooddelivery.restaurant.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public final class RestaurantDtos {
  private RestaurantDtos() {}

  public record RestaurantRequest(
      @NotBlank @Size(max = 160) String name,
      @NotNull UUID ownerId,
      @NotNull UUID cityId,
      @NotNull @DecimalMin("0.00") BigDecimal costForTwo,
      @NotBlank @Pattern(regexp = "Veg|Non Veg") String dietType,
      @NotBlank @Size(max = 500) String addressLine1,
      @Size(max = 500) String addressLine2,
      @Size(max = 2000) String description,
      @NotNull @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0") Double latitude,
      @NotNull @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") Double longitude) {}

  public record RestaurantPatch(
      @Size(max = 160) @Pattern(regexp = ".*\\S.*", message = "must not be blank") String name,
      @DecimalMin("0.00") BigDecimal costForTwo,
      @Pattern(regexp = "Veg|Non Veg") String dietType,
      @Size(max = 500) @Pattern(regexp = ".*\\S.*", message = "must not be blank")
          String addressLine1,
      @Size(max = 500) String addressLine2,
      @Size(max = 2000) String description,
      @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0") Double latitude,
      @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") Double longitude) {}

  public record RestaurantResponse(
      UUID id,
      String name,
      UUID ownerId,
      UUID cityId,
      BigDecimal costForTwo,
      String dietType,
      String addressLine1,
      String addressLine2,
      String description,
      double latitude,
      double longitude,
      boolean active) {}

  public record HoursPatch(@NotEmpty List<@Valid HoursRequest> hours) {}

  public record HoursRequest(
      @NotBlank @Pattern(regexp = "Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday")
          String day,
      boolean open,
      LocalTime startTime,
      LocalTime endTime) {
    @AssertTrue(
        message = "open hours need distinct start and end times, while closed hours omit them")
    public boolean isValidTimes() {
      return open
          ? startTime != null && endTime != null && !startTime.equals(endTime)
          : startTime == null && endTime == null;
    }
  }
}
