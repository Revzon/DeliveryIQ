package com.deliveryiq.analytics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "delivery_metrics")
public class DeliveryMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;

    @NotBlank
    @Column(name = "depot_code", nullable = false, length = 64)
    private String depotCode;

    @Column(name = "total_shipments", nullable = false)
    private int totalShipments;

    @Column(name = "delivered_on_time", nullable = false)
    private int deliveredOnTime;

    @Column(name = "delivered_late", nullable = false)
    private int deliveredLate;

    @Column(name = "failed_deliveries", nullable = false)
    private int failedDeliveries;

    @Column(name = "avg_delay_minutes", precision = 10, scale = 2)
    private BigDecimal avgDelayMinutes;

    @Column(name = "avg_route_efficiency", precision = 5, scale = 2)
    private BigDecimal avgRouteEfficiency;

    @Column(name = "total_distance_km", precision = 12, scale = 2)
    private BigDecimal totalDistanceKm;

    @Column(name = "active_drivers", nullable = false)
    private int activeDrivers;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public BigDecimal onTimePercent() {
        int completed = deliveredOnTime + deliveredLate;
        if (completed == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(deliveredOnTime)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(completed), 2, RoundingMode.HALF_UP);
    }

    public void recordDelivery(boolean onTime, long delayMinutes) {
        totalShipments++;
        if (onTime) {
            deliveredOnTime++;
        } else {
            deliveredLate++;
            BigDecimal delay = BigDecimal.valueOf(delayMinutes);
            if (avgDelayMinutes == null) {
                avgDelayMinutes = delay;
            } else {
                avgDelayMinutes = avgDelayMinutes
                        .multiply(BigDecimal.valueOf(deliveredLate - 1L))
                        .add(delay)
                        .divide(BigDecimal.valueOf(deliveredLate), 2, RoundingMode.HALF_UP);
            }
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public LocalDate getMetricDate() { return metricDate; }
    public void setMetricDate(LocalDate metricDate) { this.metricDate = metricDate; }
    public String getDepotCode() { return depotCode; }
    public void setDepotCode(String depotCode) { this.depotCode = depotCode; }
    public int getTotalShipments() { return totalShipments; }
    public void setTotalShipments(int totalShipments) { this.totalShipments = totalShipments; }
    public int getDeliveredOnTime() { return deliveredOnTime; }
    public void setDeliveredOnTime(int deliveredOnTime) { this.deliveredOnTime = deliveredOnTime; }
    public int getDeliveredLate() { return deliveredLate; }
    public void setDeliveredLate(int deliveredLate) { this.deliveredLate = deliveredLate; }
    public int getFailedDeliveries() { return failedDeliveries; }
    public void setFailedDeliveries(int failedDeliveries) { this.failedDeliveries = failedDeliveries; }
    public BigDecimal getAvgDelayMinutes() { return avgDelayMinutes; }
    public void setAvgDelayMinutes(BigDecimal avgDelayMinutes) { this.avgDelayMinutes = avgDelayMinutes; }
    public BigDecimal getAvgRouteEfficiency() { return avgRouteEfficiency; }
    public void setAvgRouteEfficiency(BigDecimal avgRouteEfficiency) { this.avgRouteEfficiency = avgRouteEfficiency; }
    public BigDecimal getTotalDistanceKm() { return totalDistanceKm; }
    public void setTotalDistanceKm(BigDecimal totalDistanceKm) { this.totalDistanceKm = totalDistanceKm; }
    public int getActiveDrivers() { return activeDrivers; }
    public void setActiveDrivers(int activeDrivers) { this.activeDrivers = activeDrivers; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
