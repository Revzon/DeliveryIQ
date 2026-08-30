package com.deliveryiq.routing;

import com.deliveryiq.tracking.Shipment;
import com.deliveryiq.tracking.ShipmentPriority;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Nearest-neighbor style route optimizer with priority weighting and peak-day adjustments.
 */
@Service
public class RoutePlanner {

    private final PeakDayProfile peakDayProfile;
    private final EtaCalculationService etaCalculationService;
    private final RouteRepository routeRepository;

    public RoutePlanner(
            PeakDayProfile peakDayProfile,
            EtaCalculationService etaCalculationService,
            RouteRepository routeRepository) {
        this.peakDayProfile = peakDayProfile;
        this.etaCalculationService = etaCalculationService;
        this.routeRepository = routeRepository;
    }

    public OptimizedRouteResult optimize(String depotCode, List<Shipment> shipments, Instant startAt) {
        if (shipments == null || shipments.isEmpty()) {
            throw new IllegalArgumentException("Cannot optimize an empty shipment list");
        }

        List<Shipment> remaining = new ArrayList<>(shipments);
        remaining.sort(Comparator
                .comparing((Shipment s) -> s.getPriority() == ShipmentPriority.CRITICAL ? 0
                        : s.getPriority() == ShipmentPriority.EXPRESS ? 1 : 2)
                .thenComparing(Shipment::getPromisedDelivery));

        List<Shipment> ordered = new ArrayList<>();
        double currentLat = 50.4501;
        double currentLng = 30.5234;
        BigDecimal totalDistance = BigDecimal.ZERO;
        int totalMinutes = 0;
        LocalDate day = startAt.atZone(ZoneOffset.UTC).toLocalDate();

        while (!remaining.isEmpty()) {
            Shipment next = selectNext(remaining, currentLat, currentLng);
            remaining.remove(next);
            double destLat = coord(next.getDestinationLat(), currentLat + 0.01);
            double destLng = coord(next.getDestinationLng(), currentLng + 0.01);
            double legKm = haversineKm(currentLat, currentLng, destLat, destLng);
            int legMinutes = etaCalculationService.estimateTravelMinutes(legKm, day);

            totalDistance = totalDistance.add(BigDecimal.valueOf(legKm).setScale(2, RoundingMode.HALF_UP));
            totalMinutes += legMinutes + 8;
            next.setStopSequence(ordered.size() + 1);
            next.setEta(startAt.plusSeconds((totalMinutes) * 60L));
            ordered.add(next);
            currentLat = destLat;
            currentLng = destLng;
        }

        totalMinutes = peakDayProfile.adjustDurationMinutes(totalMinutes, day);

        Route route = new Route();
        route.setDepotCode(depotCode);
        route.setStatus(RouteStatus.PLANNED);
        route.setPlannedDistanceKm(totalDistance);
        route.setPlannedDurationMin(totalMinutes);
        route.setPlannedStart(startAt);
        route.setPlannedEnd(startAt.plusSeconds(totalMinutes * 60L));
        route.setStopCount(ordered.size());
        route.setEfficiencyScore(estimateEfficiency(ordered.size(), totalDistance));

        Route saved = routeRepository.save(route);
        for (Shipment shipment : ordered) {
            shipment.setRoute(saved);
        }

        return new OptimizedRouteResult(
                saved.getId(),
                saved.getRouteCode(),
                depotCode,
                ordered.stream().map(Shipment::getId).toList(),
                saved.getPlannedDistanceKm(),
                saved.getPlannedDurationMin(),
                saved.getEfficiencyScore(),
                saved.getPlannedStart(),
                saved.getPlannedEnd(),
                peakDayProfile.isPeakDay(day)
        );
    }

    private Shipment selectNext(List<Shipment> remaining, double lat, double lng) {
        return remaining.stream()
                .min(Comparator.comparingDouble(s -> {
                    double dLat = coord(s.getDestinationLat(), lat);
                    double dLng = coord(s.getDestinationLng(), lng);
                    double distance = haversineKm(lat, lng, dLat, dLng);
                    double priorityBias = s.getPriority() == ShipmentPriority.CRITICAL ? -5
                            : s.getPriority() == ShipmentPriority.EXPRESS ? -2 : 0;
                    return distance + priorityBias;
                }))
                .orElseThrow();
    }

    private BigDecimal estimateEfficiency(int stops, BigDecimal distanceKm) {
        if (stops == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal stopsPerKm = BigDecimal.valueOf(stops)
                .divide(distanceKm.max(BigDecimal.ONE), 4, RoundingMode.HALF_UP);
        return stopsPerKm.multiply(BigDecimal.valueOf(40))
                .min(new BigDecimal("98.00"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private double coord(BigDecimal value, double fallback) {
        return value != null ? value.doubleValue() : fallback;
    }

    public record OptimizedRouteResult(
            UUID routeId,
            String routeCode,
            String depotCode,
            List<UUID> shipmentIds,
            BigDecimal plannedDistanceKm,
            Integer plannedDurationMin,
            BigDecimal efficiencyScore,
            Instant plannedStart,
            Instant plannedEnd,
            boolean peakDay
    ) {
    }
}
