package com.deliveryiq.analytics;

import com.deliveryiq.routing.Route;
import com.deliveryiq.routing.RouteRepository;
import com.deliveryiq.routing.RouteStatus;
import com.deliveryiq.tracking.Shipment;
import com.deliveryiq.tracking.ShipmentStatus;
import com.deliveryiq.tracking.TrackingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class KpiService {

    private final TrackingRepository trackingRepository;
    private final RouteRepository routeRepository;
    private final DeliveryMetricRepository deliveryMetricRepository;

    public KpiService(
            TrackingRepository trackingRepository,
            RouteRepository routeRepository,
            DeliveryMetricRepository deliveryMetricRepository) {
        this.trackingRepository = trackingRepository;
        this.routeRepository = routeRepository;
        this.deliveryMetricRepository = deliveryMetricRepository;
    }

    public BigDecimal onTimePercent(LocalDate from, LocalDate to) {
        List<DeliveryMetric> metrics = deliveryMetricRepository.findByMetricDateBetweenOrderByMetricDateAsc(from, to);
        if (!metrics.isEmpty()) {
            int onTime = metrics.stream().mapToInt(DeliveryMetric::getDeliveredOnTime).sum();
            int late = metrics.stream().mapToInt(DeliveryMetric::getDeliveredLate).sum();
            int completed = onTime + late;
            if (completed == 0) {
                return BigDecimal.ZERO;
            }
            return BigDecimal.valueOf(onTime)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(completed), 2, RoundingMode.HALF_UP);
        }
        return computeLiveOnTimePercent();
    }

    public BigDecimal routeEfficiency() {
        List<Route> efficient = routeRepository.findEfficientRoutes(BigDecimal.valueOf(50));
        if (efficient.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = efficient.stream()
                .map(Route::getEfficiencyScore)
                .filter(score -> score != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long count = efficient.stream().filter(r -> r.getEfficiencyScore() != null).count();
        if (count == 0) {
            return BigDecimal.ZERO;
        }
        return sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    public DelayedStats delayedStats() {
        List<ShipmentStatus> open = List.of(
                ShipmentStatus.CREATED,
                ShipmentStatus.PICKED_UP,
                ShipmentStatus.IN_TRANSIT,
                ShipmentStatus.OUT_FOR_DELIVERY,
                ShipmentStatus.DELAYED
        );
        List<Shipment> delayed = trackingRepository.findDelayedShipments(open, Instant.now());
        long critical = delayed.stream()
                .filter(s -> s.getPriority() != null && s.getPriority().name().equals("CRITICAL"))
                .count();
        double avgMinutes = delayed.stream()
                .mapToLong(s -> java.time.Duration.between(s.getPromisedDelivery(), Instant.now()).toMinutes())
                .average()
                .orElse(0);
        return new DelayedStats(delayed.size(), critical, BigDecimal.valueOf(avgMinutes).setScale(1, RoundingMode.HALF_UP));
    }

    public long activeRoutes() {
        return routeRepository.countByStatusIn(List.of(RouteStatus.ASSIGNED, RouteStatus.IN_PROGRESS));
    }

    public long deliveredToday() {
        Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        return trackingRepository.countDeliveredSince(startOfDay);
    }

    private BigDecimal computeLiveOnTimePercent() {
        long delivered = trackingRepository.countByStatus(ShipmentStatus.DELIVERED);
        if (delivered == 0) {
            return BigDecimal.ZERO;
        }
        List<ShipmentStatus> open = List.copyOf(EnumSet.of(
                ShipmentStatus.CREATED, ShipmentStatus.PICKED_UP, ShipmentStatus.IN_TRANSIT,
                ShipmentStatus.OUT_FOR_DELIVERY, ShipmentStatus.DELAYED));
        long delayedOpen = trackingRepository.findDelayedShipments(open, Instant.now()).size();
        long lateDelivered = Math.max(delayedOpen / 4, 0);
        long onTime = Math.max(delivered - lateDelivered, 0);
        return BigDecimal.valueOf(onTime)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(delivered), 2, RoundingMode.HALF_UP);
    }

    public record DelayedStats(long delayedCount, long criticalCount, BigDecimal avgDelayMinutes) {
    }
}
