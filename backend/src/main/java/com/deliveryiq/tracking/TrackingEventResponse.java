package com.deliveryiq.tracking;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TrackingEventResponse(
        UUID id,
        String eventType,
        String status,
        String locationLabel,
        BigDecimal latitude,
        BigDecimal longitude,
        Instant occurredAt,
        Instant recordedAt,
        String source,
        String notes
) {
    public static TrackingEventResponse from(TrackingEvent event) {
        return new TrackingEventResponse(
                event.getId(),
                event.getEventType(),
                event.getStatus(),
                event.getLocationLabel(),
                event.getLatitude(),
                event.getLongitude(),
                event.getOccurredAt(),
                event.getRecordedAt(),
                event.getSource(),
                event.getNotes()
        );
    }
}
