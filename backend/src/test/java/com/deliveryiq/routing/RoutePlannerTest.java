package com.deliveryiq.routing;

import com.deliveryiq.tracking.Shipment;
import com.deliveryiq.tracking.ShipmentPriority;
import com.deliveryiq.tracking.ShipmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoutePlannerTest {

    @Mock
    private EtaCalculationService etaCalculationService;

    @Mock
    private RouteRepository routeRepository;

    private RoutePlanner routePlanner;

    @BeforeEach
    void setUp() {
        PeakDayProfile profile = new PeakDayProfile();
        routePlanner = new RoutePlanner(profile, etaCalculationService, routeRepository);
    }

    @Test
    void optimizeOrdersStopsAndPersistsRoute() {
        when(etaCalculationService.estimateTravelMinutes(anyDouble(), any())).thenReturn(12);
        when(routeRepository.save(any(Route.class))).thenAnswer(inv -> {
            Route route = inv.getArgument(0);
            route.setId(java.util.UUID.randomUUID());
            return route;
        });

        Shipment near = shipment("near", 50.46, 30.53, ShipmentPriority.STANDARD);
        Shipment far = shipment("far", 50.55, 30.70, ShipmentPriority.STANDARD);
        Shipment critical = shipment("crit", 50.60, 30.80, ShipmentPriority.CRITICAL);

        RoutePlanner.OptimizedRouteResult result = routePlanner.optimize(
                "KYIV-1",
                List.of(far, near, critical),
                Instant.parse("2026-08-28T06:00:00Z")
        );

        assertEquals(3, result.shipmentIds().size());
        assertEquals("KYIV-1", result.depotCode());
        assertTrue(result.plannedDistanceKm().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(result.plannedDurationMin() > 0);
        assertEquals(1, critical.getStopSequence());

        ArgumentCaptor<Route> captor = ArgumentCaptor.forClass(Route.class);
        org.mockito.Mockito.verify(routeRepository).save(captor.capture());
        assertEquals(3, captor.getValue().getStopCount());
        assertEquals(RouteStatus.PLANNED, captor.getValue().getStatus());
    }

    @Test
    void optimizeRejectsEmptyList() {
        assertThrows(IllegalArgumentException.class,
                () -> routePlanner.optimize("KYIV-1", List.of(), Instant.now()));
    }

    @Test
    void haversineIsSymmetricAndPositive() {
        double a = RoutePlanner.haversineKm(50.45, 30.52, 49.84, 24.03);
        double b = RoutePlanner.haversineKm(49.84, 24.03, 50.45, 30.52);
        assertEquals(a, b, 0.001);
        assertTrue(a > 400);
        assertFalse(Double.isNaN(a));
    }

    private Shipment shipment(String name, double lat, double lng, ShipmentPriority priority) {
        Shipment s = new Shipment();
        s.setId(java.util.UUID.randomUUID());
        s.setTrackingNumber("DIQ-" + name);
        s.setStatus(ShipmentStatus.CREATED);
        s.setPriority(priority);
        s.setOriginAddress("Depot");
        s.setDestinationAddress(name);
        s.setDestinationLat(BigDecimal.valueOf(lat));
        s.setDestinationLng(BigDecimal.valueOf(lng));
        s.setWeightKg(BigDecimal.TEN);
        s.setCustomerName(name);
        s.setPromisedDelivery(Instant.now().plus(1, ChronoUnit.DAYS));
        return s;
    }
}
