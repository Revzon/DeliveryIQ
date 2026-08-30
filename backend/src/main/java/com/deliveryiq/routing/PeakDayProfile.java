package com.deliveryiq.routing;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

/**
 * Peak-day traffic and demand multipliers used by route ETA calculations.
 */
@Component
public class PeakDayProfile {

    private final Map<DayOfWeek, BigDecimal> trafficMultiplier = new EnumMap<>(DayOfWeek.class);
    private final Map<DayOfWeek, BigDecimal> demandMultiplier = new EnumMap<>(DayOfWeek.class);

    public PeakDayProfile() {
        trafficMultiplier.put(DayOfWeek.MONDAY, new BigDecimal("1.15"));
        trafficMultiplier.put(DayOfWeek.TUESDAY, new BigDecimal("1.05"));
        trafficMultiplier.put(DayOfWeek.WEDNESDAY, new BigDecimal("1.05"));
        trafficMultiplier.put(DayOfWeek.THURSDAY, new BigDecimal("1.10"));
        trafficMultiplier.put(DayOfWeek.FRIDAY, new BigDecimal("1.25"));
        trafficMultiplier.put(DayOfWeek.SATURDAY, new BigDecimal("0.95"));
        trafficMultiplier.put(DayOfWeek.SUNDAY, new BigDecimal("0.85"));

        demandMultiplier.put(DayOfWeek.MONDAY, new BigDecimal("1.20"));
        demandMultiplier.put(DayOfWeek.TUESDAY, new BigDecimal("1.00"));
        demandMultiplier.put(DayOfWeek.WEDNESDAY, new BigDecimal("1.00"));
        demandMultiplier.put(DayOfWeek.THURSDAY, new BigDecimal("1.10"));
        demandMultiplier.put(DayOfWeek.FRIDAY, new BigDecimal("1.30"));
        demandMultiplier.put(DayOfWeek.SATURDAY, new BigDecimal("0.80"));
        demandMultiplier.put(DayOfWeek.SUNDAY, new BigDecimal("0.60"));
    }

    public BigDecimal trafficFactor(LocalDate date) {
        return trafficMultiplier.getOrDefault(date.getDayOfWeek(), BigDecimal.ONE);
    }

    public BigDecimal demandFactor(LocalDate date) {
        return demandMultiplier.getOrDefault(date.getDayOfWeek(), BigDecimal.ONE);
    }

    public int adjustDurationMinutes(int baseMinutes, LocalDate date) {
        BigDecimal adjusted = BigDecimal.valueOf(baseMinutes)
                .multiply(trafficFactor(date))
                .multiply(demandFactor(date).max(new BigDecimal("0.75")))
                .setScale(0, RoundingMode.HALF_UP);
        return Math.max(adjusted.intValue(), 5);
    }

    public boolean isPeakDay(LocalDate date) {
        return trafficFactor(date).compareTo(new BigDecimal("1.15")) >= 0;
    }
}
