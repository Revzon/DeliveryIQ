package com.deliveryiq.analytics;

import com.deliveryiq.routing.Route;
import com.deliveryiq.routing.RouteRepository;
import com.deliveryiq.tracking.Shipment;
import com.deliveryiq.tracking.ShipmentPriority;
import com.deliveryiq.tracking.ShipmentStatus;
import com.deliveryiq.tracking.TrackingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KpiServiceTest {

    @Mock
    private TrackingRepository trackingRepository;

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private DeliveryMetricRepository deliveryMetricRepository;

    private KpiService kpiService;

    @BeforeEach
    void setUp() {
        kpiService = new KpiService(trackingRepository, routeRepository, deliveryMetricRepository);
    }

    @Test
    void onTimePercentUsesStoredMetricsWhenPresent() {
        DeliveryMetric day1 = metric(LocalDate.of(2026, 8, 1), 80, 20);
        DeliveryMetric day2 = metric(LocalDate.of(2026, 8, 2), 90, 10);
        when(deliveryMetricRepository.findByMetricDateBetweenOrderByMetricDateAsc(any(), any()))
                .thenReturn(List.of(day1, day2));

        BigDecimal percent = kpiService.onTimePercent(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 2));

        assertEquals(new BigDecimal("85.00"), percent);
    }

    @Test
    void routeEfficiencyAveragesScores() {
        Route r1 = route(new BigDecimal("80.00"));
        Route r2 = route(new BigDecimal("90.00"));
        when(routeRepository.findEfficientRoutes(BigDecimal.valueOf(50))).thenReturn(List.of(r1, r2));

        assertEquals(new BigDecimal("85.00"), kpiService.routeEfficiency());
    }

    @Test
    void delayedStatsCountsCriticalShipments() {
        Shipment delayedStandard = delayedShipment(ShipmentPriority.STANDARD);
        Shipment delayedCritical = delayedShipment(ShipmentPriority.CRITICAL);
        when(trackingRepository.findDelayedShipments(anyList(), any()))
                .thenReturn(List.of(delayedStandard, delayedCritical));

        KpiService.DelayedStats stats = kpiService.delayedStats();

        assertEquals(2, stats.delayedCount());
        assertEquals(1, stats.criticalCount());
        assertTrue(stats.avgDelayMinutes().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void liveOnTimePercentFallsBackWhenNoMetrics() {
        when(deliveryMetricRepository.findByMetricDateBetweenOrderByMetricDateAsc(any(), any()))
                .thenReturn(List.of());
        when(trackingRepository.countByStatus(ShipmentStatus.DELIVERED)).thenReturn(100L);
        when(trackingRepository.findDelayedShipments(anyList(), any())).thenReturn(List.of());

        BigDecimal percent = kpiService.onTimePercent(LocalDate.now().minusDays(7), LocalDate.now());
        assertEquals(new BigDecimal("100.00"), percent);
    }

    private DeliveryMetric metric(LocalDate date, int onTime, int late) {
        DeliveryMetric metric = new DeliveryMetric();
        metric.setMetricDate(date);
        metric.setDepotCode("KYIV-1");
        metric.setDeliveredOnTime(onTime);
        metric.setDeliveredLate(late);
        metric.setTotalShipments(onTime + late);
        return metric;
    }

    private Route route(BigDecimal score) {
        Route route = new Route();
        route.setDepotCode("KYIV-1");
        route.setEfficiencyScore(score);
        return route;
    }

    private Shipment delayedShipment(ShipmentPriority priority) {
        Shipment shipment = new Shipment();
        shipment.setPriority(priority);
        shipment.setStatus(ShipmentStatus.DELAYED);
        shipment.setPromisedDelivery(Instant.now().minus(3, ChronoUnit.HOURS));
        return shipment;
    }
}
