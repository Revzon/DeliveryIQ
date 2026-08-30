package com.deliveryiq.tracking;

import com.deliveryiq.routing.Driver;
import com.deliveryiq.routing.Route;
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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "shipments")
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Column(name = "tracking_number", nullable = false, unique = true, length = 48)
    private String trackingNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ShipmentStatus status = ShipmentStatus.CREATED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ShipmentPriority priority = ShipmentPriority.STANDARD;

    @NotBlank
    @Column(name = "origin_address", nullable = false)
    private String originAddress;

    @NotBlank
    @Column(name = "destination_address", nullable = false)
    private String destinationAddress;

    @Column(name = "origin_lat", precision = 10, scale = 7)
    private BigDecimal originLat;

    @Column(name = "origin_lng", precision = 10, scale = 7)
    private BigDecimal originLng;

    @Column(name = "destination_lat", precision = 10, scale = 7)
    private BigDecimal destinationLat;

    @Column(name = "destination_lng", precision = 10, scale = 7)
    private BigDecimal destinationLng;

    @NotNull
    @Positive
    @Column(name = "weight_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "volume_m3", precision = 10, scale = 3)
    private BigDecimal volumeM3;

    @NotBlank
    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "customer_ref", length = 64)
    private String customerRef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @NotNull
    @Column(name = "promised_delivery", nullable = false)
    private Instant promisedDelivery;

    @Column(name = "actual_delivery")
    private Instant actualDelivery;

    private Instant eta;

    @Column(name = "stop_sequence")
    private Integer stopSequence;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("occurredAt ASC")
    private List<TrackingEvent> events = new ArrayList<>();

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (trackingNumber == null || trackingNumber.isBlank()) {
            trackingNumber = "DIQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void transitionTo(ShipmentStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new IllegalStateException("Cannot transition shipment from " + status + " to " + next);
        }
        this.status = next;
        if (next == ShipmentStatus.DELIVERED) {
            this.actualDelivery = Instant.now();
        }
    }

    public boolean isDelayed(Instant reference) {
        if (status == ShipmentStatus.DELIVERED) {
            return actualDelivery != null && actualDelivery.isAfter(promisedDelivery);
        }
        return reference.isAfter(promisedDelivery) && !status.isTerminal();
    }

    public void addEvent(TrackingEvent event) {
        events.add(event);
        event.setShipment(this);
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
    public ShipmentStatus getStatus() { return status; }
    public void setStatus(ShipmentStatus status) { this.status = status; }
    public ShipmentPriority getPriority() { return priority; }
    public void setPriority(ShipmentPriority priority) { this.priority = priority; }
    public String getOriginAddress() { return originAddress; }
    public void setOriginAddress(String originAddress) { this.originAddress = originAddress; }
    public String getDestinationAddress() { return destinationAddress; }
    public void setDestinationAddress(String destinationAddress) { this.destinationAddress = destinationAddress; }
    public BigDecimal getOriginLat() { return originLat; }
    public void setOriginLat(BigDecimal originLat) { this.originLat = originLat; }
    public BigDecimal getOriginLng() { return originLng; }
    public void setOriginLng(BigDecimal originLng) { this.originLng = originLng; }
    public BigDecimal getDestinationLat() { return destinationLat; }
    public void setDestinationLat(BigDecimal destinationLat) { this.destinationLat = destinationLat; }
    public BigDecimal getDestinationLng() { return destinationLng; }
    public void setDestinationLng(BigDecimal destinationLng) { this.destinationLng = destinationLng; }
    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }
    public BigDecimal getVolumeM3() { return volumeM3; }
    public void setVolumeM3(BigDecimal volumeM3) { this.volumeM3 = volumeM3; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerRef() { return customerRef; }
    public void setCustomerRef(String customerRef) { this.customerRef = customerRef; }
    public Route getRoute() { return route; }
    public void setRoute(Route route) { this.route = route; }
    public Driver getDriver() { return driver; }
    public void setDriver(Driver driver) { this.driver = driver; }
    public Instant getPromisedDelivery() { return promisedDelivery; }
    public void setPromisedDelivery(Instant promisedDelivery) { this.promisedDelivery = promisedDelivery; }
    public Instant getActualDelivery() { return actualDelivery; }
    public void setActualDelivery(Instant actualDelivery) { this.actualDelivery = actualDelivery; }
    public Instant getEta() { return eta; }
    public void setEta(Instant eta) { this.eta = eta; }
    public Integer getStopSequence() { return stopSequence; }
    public void setStopSequence(Integer stopSequence) { this.stopSequence = stopSequence; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
    public List<TrackingEvent> getEvents() { return events; }
}
