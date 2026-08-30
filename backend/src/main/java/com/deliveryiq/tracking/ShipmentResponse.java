package com.deliveryiq.tracking;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ShipmentResponse(
        UUID id,
        String trackingNumber,
        ShipmentStatus status,
        ShipmentPriority priority,
        String originAddress,
        String destinationAddress,
        BigDecimal weightKg,
        String customerName,
        String customerRef,
        UUID routeId,
        UUID driverId,
        Instant promisedDelivery,
        Instant actualDelivery,
        Instant eta,
        Integer stopSequence,
        Instant createdAt,
        Instant updatedAt,
        List<TrackingEventResponse> timeline
) {
    public static ShipmentResponse from(Shipment shipment, boolean includeTimeline) {
        List<TrackingEventResponse> events = includeTimeline
                ? shipment.getEvents().stream().map(TrackingEventResponse::from).toList()
                : List.of();
        return new ShipmentResponse(
                shipment.getId(),
                shipment.getTrackingNumber(),
                shipment.getStatus(),
                shipment.getPriority(),
                shipment.getOriginAddress(),
                shipment.getDestinationAddress(),
                shipment.getWeightKg(),
                shipment.getCustomerName(),
                shipment.getCustomerRef(),
                shipment.getRoute() != null ? shipment.getRoute().getId() : null,
                shipment.getDriver() != null ? shipment.getDriver().getId() : null,
                shipment.getPromisedDelivery(),
                shipment.getActualDelivery(),
                shipment.getEta(),
                shipment.getStopSequence(),
                shipment.getCreatedAt(),
                shipment.getUpdatedAt(),
                events
        );
    }
}
