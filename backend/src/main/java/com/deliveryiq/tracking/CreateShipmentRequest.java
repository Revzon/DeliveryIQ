package com.deliveryiq.tracking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateShipmentRequest(
        @NotBlank String originAddress,
        @NotBlank String destinationAddress,
        BigDecimal originLat,
        BigDecimal originLng,
        BigDecimal destinationLat,
        BigDecimal destinationLng,
        @NotNull @Positive BigDecimal weightKg,
        BigDecimal volumeM3,
        @NotBlank String customerName,
        String customerRef,
        ShipmentPriority priority,
        @NotNull Instant promisedDelivery
) {
}
