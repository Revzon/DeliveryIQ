package com.deliveryiq.routing;

import com.deliveryiq.cache.CacheService;
import com.deliveryiq.common.ResourceNotFoundException;
import com.deliveryiq.tracking.Shipment;
import com.deliveryiq.tracking.TrackingRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    private final RoutePlanner routePlanner;
    private final DriverAssignmentService driverAssignmentService;
    private final TrackingRepository trackingRepository;
    private final RouteRepository routeRepository;
    private final CacheService cacheService;

    public RouteController(
            RoutePlanner routePlanner,
            DriverAssignmentService driverAssignmentService,
            TrackingRepository trackingRepository,
            RouteRepository routeRepository,
            CacheService cacheService) {
        this.routePlanner = routePlanner;
        this.driverAssignmentService = driverAssignmentService;
        this.trackingRepository = trackingRepository;
        this.routeRepository = routeRepository;
        this.cacheService = cacheService;
    }

    @GetMapping("/optimize")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    public RoutePlanner.OptimizedRouteResult optimize(
            @RequestParam @NotBlank String depotCode,
            @RequestParam @NotEmpty List<UUID> shipmentIds,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startAt) {

        String cacheKey = "route-opt:" + depotCode + ":" + shipmentIds.hashCode();
        return cacheService.getOrLoad(cacheKey, RoutePlanner.OptimizedRouteResult.class, () -> {
            List<Shipment> shipments = shipmentIds.stream()
                    .map(id -> trackingRepository.findById(id)
                            .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + id)))
                    .toList();
            return routePlanner.optimize(depotCode, shipments, startAt != null ? startAt : Instant.now());
        }, cacheService.routeTtlSeconds());
    }

    @PostMapping("/{routeId}/assign/{driverId}")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    public RouteBoardItem assign(@PathVariable UUID routeId, @PathVariable UUID driverId) {
        Route route = driverAssignmentService.assignDriver(routeId, driverId);
        cacheService.evictByPrefix("route-opt:");
        return RouteBoardItem.from(route);
    }

    @PostMapping("/{routeId}/auto-assign")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    public RouteBoardItem autoAssign(@PathVariable UUID routeId) {
        driverAssignmentService.autoAssign(routeId);
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found: " + routeId));
        return RouteBoardItem.from(route);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ANALYST', 'ADMIN')")
    public List<RouteBoardItem> board(@RequestParam(required = false) RouteStatus status) {
        List<Route> routes = status != null
                ? routeRepository.findByStatus(status)
                : routeRepository.findAll();
        return routes.stream().map(RouteBoardItem::from).toList();
    }

    public record RouteBoardItem(
            UUID id,
            String routeCode,
            RouteStatus status,
            String depotCode,
            UUID driverId,
            String driverName,
            Integer stopCount,
            Instant plannedStart,
            Instant plannedEnd,
            java.math.BigDecimal plannedDistanceKm,
            Integer plannedDurationMin,
            java.math.BigDecimal efficiencyScore
    ) {
        static RouteBoardItem from(Route route) {
            return new RouteBoardItem(
                    route.getId(),
                    route.getRouteCode(),
                    route.getStatus(),
                    route.getDepotCode(),
                    route.getDriver() != null ? route.getDriver().getId() : null,
                    route.getDriver() != null ? route.getDriver().getFullName() : null,
                    route.getStopCount(),
                    route.getPlannedStart(),
                    route.getPlannedEnd(),
                    route.getPlannedDistanceKm(),
                    route.getPlannedDurationMin(),
                    route.getEfficiencyScore()
            );
        }
    }
}
