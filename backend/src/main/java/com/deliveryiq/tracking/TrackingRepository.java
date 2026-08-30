package com.deliveryiq.tracking;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrackingRepository extends JpaRepository<Shipment, UUID> {

    Optional<Shipment> findByTrackingNumber(String trackingNumber);

    Page<Shipment> findByStatus(ShipmentStatus status, Pageable pageable);

    @Query("""
            SELECT s FROM Shipment s
            WHERE s.status IN :statuses
              AND s.promisedDelivery < :reference
            ORDER BY s.promisedDelivery ASC
            """)
    List<Shipment> findDelayedShipments(
            @Param("statuses") List<ShipmentStatus> statuses,
            @Param("reference") Instant reference);

    @Query("""
            SELECT s FROM Shipment s
            WHERE (:status IS NULL OR s.status = :status)
              AND (:customer IS NULL OR LOWER(s.customerName) LIKE LOWER(CONCAT('%', :customer, '%')))
            """)
    Page<Shipment> search(
            @Param("status") ShipmentStatus status,
            @Param("customer") String customer,
            Pageable pageable);

    long countByStatus(ShipmentStatus status);

    @Query("SELECT COUNT(s) FROM Shipment s WHERE s.status = com.deliveryiq.tracking.ShipmentStatus.DELIVERED AND s.actualDelivery >= :since")
    long countDeliveredSince(@Param("since") Instant since);
}
