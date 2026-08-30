package com.deliveryiq.routing;

import com.deliveryiq.tracking.Shipment;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "routes")
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Column(name = "route_code", nullable = false, unique = true, length = 32)
    private String routeCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RouteStatus status = RouteStatus.PLANNED;

    @NotBlank
    @Column(name = "depot_code", nullable = false, length = 64)
    private String depotCode;

    @Column(name = "planned_distance_km", precision = 10, scale = 2)
    private BigDecimal plannedDistanceKm;

    @Column(name = "planned_duration_min")
    private Integer plannedDurationMin;

    @Column(name = "efficiency_score", precision = 5, scale = 2)
    private BigDecimal efficiencyScore;

    @Column(name = "planned_start")
    private Instant plannedStart;

    @Column(name = "planned_end")
    private Instant plannedEnd;

    @Column(name = "actual_start")
    private Instant actualStart;

    @Column(name = "actual_end")
    private Instant actualEnd;

    @Column(name = "stop_count", nullable = false)
    private int stopCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL)
    @OrderBy("stopSequence ASC")
    private List<Shipment> shipments = new ArrayList<>();

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (routeCode == null || routeCode.isBlank()) {
            routeCode = "RT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void assignDriver(Driver assigned) {
        if (status != RouteStatus.PLANNED && status != RouteStatus.ASSIGNED) {
            throw new IllegalStateException("Cannot assign driver to route in status " + status);
        }
        this.driver = assigned;
        this.status = RouteStatus.ASSIGNED;
    }

    public void recalculateEfficiency(BigDecimal actualDistanceKm, int actualDurationMin) {
        if (plannedDistanceKm == null || plannedDurationMin == null || plannedDurationMin == 0) {
            efficiencyScore = BigDecimal.ZERO;
            return;
        }
        BigDecimal distanceRatio = plannedDistanceKm.divide(actualDistanceKm.max(BigDecimal.ONE), 4, RoundingMode.HALF_UP);
        BigDecimal durationRatio = BigDecimal.valueOf(plannedDurationMin)
                .divide(BigDecimal.valueOf(Math.max(actualDurationMin, 1)), 4, RoundingMode.HALF_UP);
        efficiencyScore = distanceRatio.add(durationRatio)
                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .min(new BigDecimal("100.00"));
    }

    public boolean overlaps(Instant start, Instant end) {
        if (plannedStart == null || plannedEnd == null || start == null || end == null) {
            return false;
        }
        return plannedStart.isBefore(end) && start.isBefore(plannedEnd);
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getRouteCode() { return routeCode; }
    public void setRouteCode(String routeCode) { this.routeCode = routeCode; }
    public Driver getDriver() { return driver; }
    public void setDriver(Driver driver) { this.driver = driver; }
    public RouteStatus getStatus() { return status; }
    public void setStatus(RouteStatus status) { this.status = status; }
    public String getDepotCode() { return depotCode; }
    public void setDepotCode(String depotCode) { this.depotCode = depotCode; }
    public BigDecimal getPlannedDistanceKm() { return plannedDistanceKm; }
    public void setPlannedDistanceKm(BigDecimal plannedDistanceKm) { this.plannedDistanceKm = plannedDistanceKm; }
    public Integer getPlannedDurationMin() { return plannedDurationMin; }
    public void setPlannedDurationMin(Integer plannedDurationMin) { this.plannedDurationMin = plannedDurationMin; }
    public BigDecimal getEfficiencyScore() { return efficiencyScore; }
    public void setEfficiencyScore(BigDecimal efficiencyScore) { this.efficiencyScore = efficiencyScore; }
    public Instant getPlannedStart() { return plannedStart; }
    public void setPlannedStart(Instant plannedStart) { this.plannedStart = plannedStart; }
    public Instant getPlannedEnd() { return plannedEnd; }
    public void setPlannedEnd(Instant plannedEnd) { this.plannedEnd = plannedEnd; }
    public Instant getActualStart() { return actualStart; }
    public void setActualStart(Instant actualStart) { this.actualStart = actualStart; }
    public Instant getActualEnd() { return actualEnd; }
    public void setActualEnd(Instant actualEnd) { this.actualEnd = actualEnd; }
    public int getStopCount() { return stopCount; }
    public void setStopCount(int stopCount) { this.stopCount = stopCount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
    public List<Shipment> getShipments() { return shipments; }
}
