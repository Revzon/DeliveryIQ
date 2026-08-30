package com.deliveryiq.tracking;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record UpdateShipmentStatusRequest(
        @NotNull ShipmentStatus status,
        String locationLabel,
        BigDecimal latitude,
        BigDecimal longitude,
        String notes,
        Instant occurredAt
) {
}
