package com.deliveryiq.analytics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryMetricRepository extends JpaRepository<DeliveryMetric, UUID> {

    Optional<DeliveryMetric> findByMetricDateAndDepotCode(LocalDate metricDate, String depotCode);

    List<DeliveryMetric> findByMetricDateBetweenOrderByMetricDateAsc(LocalDate from, LocalDate to);

    @Query("""
            SELECT m FROM DeliveryMetric m
            WHERE m.metricDate >= :from
              AND (:depot IS NULL OR m.depotCode = :depot)
            ORDER BY m.metricDate DESC
            """)
    List<DeliveryMetric> findRecent(
            @Param("from") LocalDate from,
            @Param("depot") String depot);
}
