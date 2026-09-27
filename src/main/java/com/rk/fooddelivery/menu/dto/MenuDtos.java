package com.rk.fooddelivery.menu.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public final class MenuDtos {
  private MenuDtos() {}

  public record CategoryRequest(
      @NotBlank @Size(max = 160) String name, @NotNull @Min(0) Integer sortOrder) {}

  public record CategoryPatch(
      @Size(max = 160) @Pattern(regexp = ".*\\S.*") String name, @Min(0) Integer sortOrder) {}

  public record CategoryResponse(
      UUID id, UUID restaurantId, String name, int sortOrder, int itemCount) {}

  public record ItemRequest(
      @NotNull UUID categoryId,
      @NotBlank @Size(max = 160) String name,
      @Size(max = 2000) String description,
      @NotBlank @Pattern(regexp = "Veg|Non Veg") String dietType,
      @NotNull @DecimalMin("0.00") BigDecimal price,
      @Min(0) Integer sortOrder,
      @NotNull @Min(0) Integer availableQuantity) {}

  public record ItemPatch(
      UUID categoryId,
      @Size(max = 160) @Pattern(regexp = ".*\\S.*") String name,
      @Size(max = 2000) String description,
      @Pattern(regexp = "Veg|Non Veg") String dietType,
      @DecimalMin("0.00") BigDecimal price,
      @Min(0) Integer sortOrder,
      Boolean available) {}

  public record StockAdjustmentRequest(@NotNull Integer delta) {
    @AssertTrue(message = "delta must not be zero")
    public boolean isNonZero() {
      return delta == null || delta != 0;
    }
  }

  public record MenuItemResponse(
      UUID id,
      UUID restaurantId,
      UUID categoryId,
      String name,
      String description,
      String dietType,
      BigDecimal price,
      int sortOrder,
      boolean available,
      int availableQuantity) {}

  public record MenuSearchRequest(
      UUID categoryId,
      String dietType,
      BigDecimal minPrice,
      BigDecimal maxPrice,
      String name,
      int page,
      int size) {}
}
