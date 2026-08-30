package com.deliveryiq.analytics;

import com.deliveryiq.routing.DriverRepository;
import com.deliveryiq.routing.DriverStatus;
import com.deliveryiq.routing.RouteRepository;
import com.deliveryiq.tracking.ShipmentStatus;
import com.deliveryiq.tracking.TrackingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregates operational counters into depot-level delivery metrics.
 */
@Service
public class AnalyticsAggregator {

    private final TrackingRepository trackingRepository;
    private final RouteRepository routeRepository;
    private final DriverRepository driverRepository;
    private final DeliveryMetricRepository deliveryMetricRepository;
    private final KpiService kpiService;

    public AnalyticsAggregator(
            TrackingRepository trackingRepository,
            RouteRepository routeRepository,
            DriverRepository driverRepository,
            DeliveryMetricRepository deliveryMetricRepository,
            KpiService kpiService) {
        this.trackingRepository = trackingRepository;
        this.routeRepository = routeRepository;
        this.driverRepository = driverRepository;
        this.deliveryMetricRepository = deliveryMetricRepository;
        this.kpiService = kpiService;
    }

    @Transactional
    public DeliveryMetric aggregateForDepot(String depotCode, LocalDate day) {
        DeliveryMetric metric = deliveryMetricRepository
                .findByMetricDateAndDepotCode(day, depotCode)
                .orElseGet(DeliveryMetric::new);

        metric.setMetricDate(day);
        metric.setDepotCode(depotCode);
        metric.setTotalShipments((int) (
                trackingRepository.countByStatus(ShipmentStatus.DELIVERED)
                        + trackingRepository.countByStatus(ShipmentStatus.IN_TRANSIT)
                        + trackingRepository.countByStatus(ShipmentStatus.OUT_FOR_DELIVERY)
                        + trackingRepository.countByStatus(ShipmentStatus.DELAYED)
        ));

        KpiService.DelayedStats delayed = kpiService.delayedStats();
        metric.setDeliveredLate((int) delayed.delayedCount());
        metric.setDeliveredOnTime(Math.max(metric.getTotalShipments() - metric.getDeliveredLate(), 0));
        metric.setFailedDeliveries((int) trackingRepository.countByStatus(ShipmentStatus.FAILED));
        metric.setAvgDelayMinutes(delayed.avgDelayMinutes());
        metric.setAvgRouteEfficiency(kpiService.routeEfficiency());
        metric.setActiveDrivers((int) driverRepository.countByStatus(DriverStatus.ON_ROUTE));

        BigDecimal distance = routeRepository.findEfficientRoutes(BigDecimal.ZERO).stream()
                .filter(r -> depotCode.equals(r.getDepotCode()))
                .map(r -> r.getPlannedDistanceKm() != null ? r.getPlannedDistanceKm() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        metric.setTotalDistanceKm(distance);

        return deliveryMetricRepository.save(metric);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> trend(LocalDate from, String depot) {
        List<DeliveryMetric> rows = deliveryMetricRepository.findRecent(from, depot);
        Map<String, Object> payload = new HashMap<>();
        payload.put("from", from.toString());
        payload.put("depot", depot);
        payload.put("points", rows.stream().map(m -> Map.of(
                "date", m.getMetricDate().toString(),
                "onTimePercent", m.onTimePercent(),
                "delayed", m.getDeliveredLate(),
                "efficiency", m.getAvgRouteEfficiency() != null ? m.getAvgRouteEfficiency() : BigDecimal.ZERO
        )).toList());
        return payload;
    }
}
