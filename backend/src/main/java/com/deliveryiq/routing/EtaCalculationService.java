package com.deliveryiq.routing;

import com.deliveryiq.tracking.Shipment;
import com.deliveryiq.tracking.ShipmentPriority;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
public class EtaCalculationService {

    private static final double CITY_AVG_KMH = 28.0;
    private static final double HIGHWAY_AVG_KMH = 55.0;

    private final PeakDayProfile peakDayProfile;

    public EtaCalculationService(PeakDayProfile peakDayProfile) {
        this.peakDayProfile = peakDayProfile;
    }

    public int estimateTravelMinutes(double distanceKm, LocalDate day) {
        double speed = distanceKm > 15 ? HIGHWAY_AVG_KMH : CITY_AVG_KMH;
        int base = (int) Math.ceil((distanceKm / speed) * 60.0);
        return peakDayProfile.adjustDurationMinutes(Math.max(base, 3), day);
    }

    public Instant calculateEta(Shipment shipment, Instant departAt, double remainingDistanceKm) {
        LocalDate day = departAt.atZone(ZoneOffset.UTC).toLocalDate();
        int travel = estimateTravelMinutes(remainingDistanceKm, day);
        int serviceBuffer = shipment.getPriority() == ShipmentPriority.CRITICAL ? 5 : 10;
        int priorityAdj = shipment.getPriority().weightBonusMinutes();
        long totalMinutes = travel + serviceBuffer + priorityAdj;
        return departAt.plusSeconds(Math.max(totalMinutes, 1) * 60L);
    }

    public BigDecimal delayRiskScore(Shipment shipment, Instant eta) {
        if (eta == null || shipment.getPromisedDelivery() == null) {
            return BigDecimal.ZERO;
        }
        long minutesLate = java.time.Duration.between(shipment.getPromisedDelivery(), eta).toMinutes();
        if (minutesLate <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(Math.min(minutesLate, 240))
                .divide(BigDecimal.valueOf(240), 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
}
