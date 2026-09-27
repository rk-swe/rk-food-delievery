package com.rk.fooddelivery.city.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public final class CityDtos {
  private CityDtos() {}

  public record CityRequest(
      @NotBlank @Size(max = 120) String name,
      @NotBlank @Size(max = 120) String state,
      @NotBlank @Size(max = 120) String country,
      @NotBlank
          @Pattern(regexp = "^[A-Z]{3}$", message = "must be a three-letter uppercase currency")
          String currency) {}

  public record CityPatch(
      @Size(max = 120) @Pattern(regexp = ".*\\S.*", message = "must not be blank") String name,
      @Size(max = 120) @Pattern(regexp = ".*\\S.*", message = "must not be blank") String state,
      @Size(max = 120) @Pattern(regexp = ".*\\S.*", message = "must not be blank") String country,
      @Pattern(regexp = "^[A-Z]{3}$", message = "must be a three-letter uppercase currency")
          String currency) {}

  public record CityResponse(
      UUID id, String name, String state, String country, String currency, boolean active) {}
}
