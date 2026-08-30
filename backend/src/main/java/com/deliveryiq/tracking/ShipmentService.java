package com.deliveryiq.tracking;

import com.deliveryiq.common.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ShipmentService {

    private static final Logger log = LoggerFactory.getLogger(ShipmentService.class);

    private final TrackingRepository trackingRepository;
    private final TrackingEventRepository trackingEventRepository;

    public ShipmentService(TrackingRepository trackingRepository, TrackingEventRepository trackingEventRepository) {
        this.trackingRepository = trackingRepository;
        this.trackingEventRepository = trackingEventRepository;
    }

    public ShipmentResponse create(CreateShipmentRequest request) {
        Shipment shipment = new Shipment();
        shipment.setOriginAddress(request.originAddress());
        shipment.setDestinationAddress(request.destinationAddress());
        shipment.setOriginLat(request.originLat());
        shipment.setOriginLng(request.originLng());
        shipment.setDestinationLat(request.destinationLat());
        shipment.setDestinationLng(request.destinationLng());
        shipment.setWeightKg(request.weightKg());
        shipment.setVolumeM3(request.volumeM3());
        shipment.setCustomerName(request.customerName());
        shipment.setCustomerRef(request.customerRef());
        shipment.setPriority(request.priority() != null ? request.priority() : ShipmentPriority.STANDARD);
        shipment.setPromisedDelivery(request.promisedDelivery());
        shipment.setStatus(ShipmentStatus.CREATED);

        TrackingEvent created = TrackingEvent.of(
                shipment,
                "SHIPMENT_CREATED",
                ShipmentStatus.CREATED.name(),
                request.originAddress(),
                Instant.now()
        );
        shipment.addEvent(created);

        Shipment saved = trackingRepository.save(shipment);
        log.info("Created shipment {} for customer {}", saved.getTrackingNumber(), saved.getCustomerName());
        return ShipmentResponse.from(saved, true);
    }

    @Transactional(readOnly = true)
    public ShipmentResponse getById(UUID id) {
        Shipment shipment = trackingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + id));
        return ShipmentResponse.from(shipment, true);
    }

    @Transactional(readOnly = true)
    public ShipmentResponse track(String trackingNumber) {
        Shipment shipment = trackingRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Tracking number not found: " + trackingNumber));
        return ShipmentResponse.from(shipment, true);
    }

    @Transactional(readOnly = true)
    public Page<ShipmentResponse> list(ShipmentStatus status, String customer, Pageable pageable) {
        return trackingRepository.search(status, blankToNull(customer), pageable)
                .map(s -> ShipmentResponse.from(s, false));
    }

    @Transactional(readOnly = true)
    public List<TrackingEventResponse> timeline(UUID shipmentId, String eventType) {
        if (!trackingRepository.existsById(shipmentId)) {
            throw new ResourceNotFoundException("Shipment not found: " + shipmentId);
        }
        return trackingEventRepository.findTimeline(shipmentId, blankToNull(eventType))
                .stream()
                .map(TrackingEventResponse::from)
                .toList();
    }

    public ShipmentResponse updateStatus(UUID id, UpdateShipmentStatusRequest request) {
        Shipment shipment = trackingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + id));

        shipment.transitionTo(request.status());
        if (shipment.isDelayed(Instant.now()) && request.status() != ShipmentStatus.DELAYED
                && !request.status().isTerminal()) {
            shipment.setStatus(ShipmentStatus.DELAYED);
        }

        Instant when = request.occurredAt() != null ? request.occurredAt() : Instant.now();
        TrackingEvent event = TrackingEvent.of(
                shipment,
                "STATUS_UPDATE",
                shipment.getStatus().name(),
                request.locationLabel(),
                when
        );
        event.setLatitude(request.latitude());
        event.setLongitude(request.longitude());
        event.setNotes(request.notes());
        event.setSource("SYSTEM");
        shipment.addEvent(event);

        return ShipmentResponse.from(trackingRepository.save(shipment), true);
    }

    public void applyExternalEvent(UUID shipmentId, String eventType, String status, String location, Instant occurredAt) {
        Shipment shipment = trackingRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found for Kafka event: " + shipmentId));

        ShipmentStatus next = ShipmentStatus.valueOf(status);
        if (shipment.getStatus().canTransitionTo(next)) {
            shipment.transitionTo(next);
        }

        TrackingEvent event = TrackingEvent.of(shipment, eventType, status, location, occurredAt);
        event.setSource("KAFKA");
        shipment.addEvent(event);
        trackingRepository.save(shipment);
        log.debug("Applied Kafka tracking event {} to {}", eventType, shipment.getTrackingNumber());
    }

    @Transactional(readOnly = true)
    public List<ShipmentResponse> findDelayed() {
        List<ShipmentStatus> open = List.of(
                ShipmentStatus.CREATED,
                ShipmentStatus.PICKED_UP,
                ShipmentStatus.IN_TRANSIT,
                ShipmentStatus.OUT_FOR_DELIVERY,
                ShipmentStatus.DELAYED
        );
        return trackingRepository.findDelayedShipments(open, Instant.now()).stream()
                .map(s -> ShipmentResponse.from(s, false))
                .toList();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
