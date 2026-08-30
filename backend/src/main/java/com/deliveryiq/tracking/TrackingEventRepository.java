package com.deliveryiq.tracking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TrackingEventRepository extends JpaRepository<TrackingEvent, UUID> {

    List<TrackingEvent> findByShipmentIdOrderByOccurredAtAsc(UUID shipmentId);

    @Query("""
            SELECT e FROM TrackingEvent e
            WHERE e.shipment.id = :shipmentId
              AND (:eventType IS NULL OR e.eventType = :eventType)
            ORDER BY e.occurredAt DESC
            """)
    List<TrackingEvent> findTimeline(
            @Param("shipmentId") UUID shipmentId,
            @Param("eventType") String eventType);
}
