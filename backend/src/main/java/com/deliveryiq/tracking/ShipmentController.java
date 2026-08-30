package com.deliveryiq.tracking;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {

    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    public ResponseEntity<ShipmentResponse> create(@Valid @RequestBody CreateShipmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(shipmentService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ANALYST', 'ADMIN', 'DRIVER')")
    public Page<ShipmentResponse> list(
            @RequestParam(required = false) ShipmentStatus status,
            @RequestParam(required = false) String customer,
            @PageableDefault(size = 20) Pageable pageable) {
        return shipmentService.list(status, customer, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ANALYST', 'ADMIN', 'DRIVER', 'CUSTOMER')")
    public ShipmentResponse get(@PathVariable UUID id) {
        return shipmentService.getById(id);
    }

    @GetMapping("/track/{trackingNumber}")
    public ShipmentResponse track(@PathVariable String trackingNumber) {
        return shipmentService.track(trackingNumber);
    }

    @GetMapping("/{id}/timeline")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ANALYST', 'ADMIN', 'DRIVER', 'CUSTOMER')")
    public List<TrackingEventResponse> timeline(
            @PathVariable UUID id,
            @RequestParam(required = false) String eventType) {
        return shipmentService.timeline(id, eventType);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN', 'DRIVER')")
    public ShipmentResponse updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateShipmentStatusRequest request) {
        return shipmentService.updateStatus(id, request);
    }

    @GetMapping("/delayed")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ANALYST', 'ADMIN')")
    public List<ShipmentResponse> delayed() {
        return shipmentService.findDelayed();
    }
}
