package com.deliveryiq.routing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "drivers")
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Column(name = "employee_code", nullable = false, unique = true, length = 32)
    private String employeeCode;

    @NotBlank
    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String phone;
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DriverStatus status = DriverStatus.AVAILABLE;

    @NotBlank
    @Column(name = "home_depot", nullable = false, length = 64)
    private String homeDepot;

    @NotNull
    @Positive
    @Column(name = "vehicle_capacity_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal vehicleCapacityKg = new BigDecimal("1000.00");

    @Column(name = "max_stops", nullable = false)
    private int maxStops = 25;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

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

    public boolean canCarry(BigDecimal weightKg, int stopCount) {
        return status.canAcceptAssignment()
                && weightKg.compareTo(vehicleCapacityKg) <= 0
                && stopCount <= maxStops;
    }

    public void markOnRoute() {
        if (!status.canAcceptAssignment()) {
            throw new IllegalStateException("Driver " + employeeCode + " cannot start a route in status " + status);
        }
        status = DriverStatus.ON_ROUTE;
    }

    public void markAvailable() {
        status = DriverStatus.AVAILABLE;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public DriverStatus getStatus() { return status; }
    public void setStatus(DriverStatus status) { this.status = status; }
    public String getHomeDepot() { return homeDepot; }
    public void setHomeDepot(String homeDepot) { this.homeDepot = homeDepot; }
    public BigDecimal getVehicleCapacityKg() { return vehicleCapacityKg; }
    public void setVehicleCapacityKg(BigDecimal vehicleCapacityKg) { this.vehicleCapacityKg = vehicleCapacityKg; }
    public int getMaxStops() { return maxStops; }
    public void setMaxStops(int maxStops) { this.maxStops = maxStops; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
