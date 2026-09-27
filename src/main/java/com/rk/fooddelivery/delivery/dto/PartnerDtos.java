package com.rk.fooddelivery.delivery.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

public final class PartnerDtos {
    private PartnerDtos() {}
    public record PartnerRequest(@NotBlank @Size(max=160) String name, @NotBlank @Email @Size(max=254) String email,
        @NotBlank @Pattern(regexp="^\\+[1-9][0-9]{1,14}$") String phoneNumber, @NotBlank @Size(min=3,max=100) String username,
        @NotBlank @Size(min=8,max=200) String password) {}
    public record PartnerPatch(@Size(max=160) @Pattern(regexp=".*\\S.*", message="must not be blank") String name, @Email @Size(max=254) @Pattern(regexp=".*\\S.*", message="must not be blank") String email, @Pattern(regexp="^\\+[1-9][0-9]{1,14}$") String phoneNumber) {}
    public record PresenceRequest(boolean online) {}
    public record LocationRequest(@NotNull @DecimalMin(value="-90.0") @DecimalMax(value="90.0") Double latitude,
        @NotNull @DecimalMin(value="-180.0") @DecimalMax(value="180.0") Double longitude) {}
    public record PartnerResponse(UUID id, String name, String email, String phoneNumber, boolean active, boolean online, Instant locationUpdatedAt) {}
}
