package com.deliveryiq.routing;

import com.deliveryiq.common.BusinessConflictException;
import com.deliveryiq.common.ResourceNotFoundException;
import com.deliveryiq.tracking.Shipment;
import com.deliveryiq.tracking.TrackingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DriverAssignmentService {

    private final DriverRepository driverRepository;
    private final RouteRepository routeRepository;
    private final TrackingRepository trackingRepository;

    public DriverAssignmentService(
            DriverRepository driverRepository,
            RouteRepository routeRepository,
            TrackingRepository trackingRepository) {
        this.driverRepository = driverRepository;
        this.routeRepository = routeRepository;
        this.trackingRepository = trackingRepository;
    }

    public Route assignDriver(UUID routeId, UUID driverId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found: " + routeId));
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found: " + driverId));

        detectConflicts(driver, route);

        BigDecimal totalWeight = route.getShipments().stream()
                .map(Shipment::getWeightKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (!driver.canCarry(totalWeight, route.getStopCount())) {
            throw new BusinessConflictException(
                    "Driver " + driver.getEmployeeCode() + " cannot carry " + totalWeight + " kg across "
                            + route.getStopCount() + " stops");
        }

        route.assignDriver(driver);
        driver.markOnRoute();

        for (Shipment shipment : route.getShipments()) {
            shipment.setDriver(driver);
            trackingRepository.save(shipment);
        }

        return routeRepository.save(route);
    }

    public Driver autoAssign(UUID routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found: " + routeId));

        BigDecimal totalWeight = route.getShipments().stream()
                .map(Shipment::getWeightKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Driver> candidates = driverRepository.findAvailableAtDepot(route.getDepotCode());
        Driver selected = candidates.stream()
                .filter(d -> d.canCarry(totalWeight, route.getStopCount()))
                .filter(d -> findConflicts(d.getId(), route).isEmpty())
                .findFirst()
                .orElseThrow(() -> new BusinessConflictException(
                        "No available driver at depot " + route.getDepotCode() + " for route " + route.getRouteCode()));

        assignDriver(routeId, selected.getId());
        return selected;
    }

    public List<Route> detectConflicts(Driver driver, Route candidate) {
        List<Route> conflicts = findConflicts(driver.getId(), candidate);
        if (!conflicts.isEmpty()) {
            throw new BusinessConflictException(
                    "Driver " + driver.getEmployeeCode() + " has " + conflicts.size()
                            + " overlapping active route(s)");
        }
        return conflicts;
    }

    private List<Route> findConflicts(UUID driverId, Route candidate) {
        if (candidate.getPlannedStart() == null || candidate.getPlannedEnd() == null) {
            return List.of();
        }
        return routeRepository.findConflicts(
                driverId,
                List.of(RouteStatus.ASSIGNED, RouteStatus.IN_PROGRESS),
                candidate.getPlannedStart(),
                candidate.getPlannedEnd()
        ).stream().filter(r -> !r.getId().equals(candidate.getId())).toList();
    }
}
