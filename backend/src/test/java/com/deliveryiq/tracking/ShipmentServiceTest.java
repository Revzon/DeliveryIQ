package com.deliveryiq.tracking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceTest {

    @Mock
    private TrackingRepository trackingRepository;

    @Mock
    private TrackingEventRepository trackingEventRepository;

    private ShipmentService shipmentService;

    @BeforeEach
    void setUp() {
        shipmentService = new ShipmentService(trackingRepository, trackingEventRepository);
    }

    @Test
    void createPersistsShipmentWithCreatedEvent() {
        when(trackingRepository.save(any(Shipment.class))).thenAnswer(inv -> {
            Shipment s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        CreateShipmentRequest request = new CreateShipmentRequest(
                "Depot Kyiv",
                "Client Lviv",
                BigDecimal.valueOf(50.45),
                BigDecimal.valueOf(30.52),
                BigDecimal.valueOf(49.84),
                BigDecimal.valueOf(24.03),
                BigDecimal.valueOf(12.5),
                BigDecimal.valueOf(0.4),
                "Acme Corp",
                "PO-100",
                ShipmentPriority.EXPRESS,
                Instant.now().plus(1, ChronoUnit.DAYS)
        );

        ShipmentResponse response = shipmentService.create(request);

        assertNotNull(response.id());
        assertEquals(ShipmentStatus.CREATED, response.status());
        assertEquals("Acme Corp", response.customerName());
        assertFalse(response.timeline().isEmpty());
        assertEquals("SHIPMENT_CREATED", response.timeline().getFirst().eventType());
    }

    @Test
    void updateStatusRejectsIllegalTransition() {
        Shipment shipment = baseShipment();
        shipment.setStatus(ShipmentStatus.DELIVERED);
        when(trackingRepository.findById(shipment.getId())).thenReturn(Optional.of(shipment));

        UpdateShipmentStatusRequest request = new UpdateShipmentStatusRequest(
                ShipmentStatus.IN_TRANSIT, "Hub", null, null, null, Instant.now());

        assertThrows(IllegalStateException.class, () -> shipmentService.updateStatus(shipment.getId(), request));
    }

    @Test
    void updateStatusAppendsTimelineEvent() {
        Shipment shipment = baseShipment();
        when(trackingRepository.findById(shipment.getId())).thenReturn(Optional.of(shipment));
        when(trackingRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));

        ShipmentResponse response = shipmentService.updateStatus(
                shipment.getId(),
                new UpdateShipmentStatusRequest(ShipmentStatus.PICKED_UP, "Dock A", null, null, "scanned", Instant.now())
        );

        assertEquals(ShipmentStatus.PICKED_UP, response.status());
        assertTrue(response.timeline().stream().anyMatch(e -> "STATUS_UPDATE".equals(e.eventType())));
    }

    @Test
    void listDelegatesToRepositorySearch() {
        Shipment shipment = baseShipment();
        when(trackingRepository.search(ShipmentStatus.IN_TRANSIT, "Acme", PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(shipment)));

        Page<ShipmentResponse> page = shipmentService.list(ShipmentStatus.IN_TRANSIT, "Acme", PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        assertEquals(shipment.getTrackingNumber(), page.getContent().getFirst().trackingNumber());
    }

    @Test
    void applyExternalEventUsesKafkaSource() {
        Shipment shipment = baseShipment();
        when(trackingRepository.findById(shipment.getId())).thenReturn(Optional.of(shipment));
        when(trackingRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));

        shipmentService.applyExternalEvent(
                shipment.getId(),
                "SCAN",
                ShipmentStatus.PICKED_UP.name(),
                "Gate 2",
                Instant.now()
        );

        ArgumentCaptor<Shipment> captor = ArgumentCaptor.forClass(Shipment.class);
        verify(trackingRepository).save(captor.capture());
        assertEquals("KAFKA", captor.getValue().getEvents().getLast().getSource());
        assertEquals(ShipmentStatus.PICKED_UP, captor.getValue().getStatus());
    }

    private Shipment baseShipment() {
        Shipment shipment = new Shipment();
        shipment.setId(UUID.randomUUID());
        shipment.setTrackingNumber("DIQ-TEST01");
        shipment.setStatus(ShipmentStatus.CREATED);
        shipment.setPriority(ShipmentPriority.STANDARD);
        shipment.setOriginAddress("A");
        shipment.setDestinationAddress("B");
        shipment.setWeightKg(BigDecimal.TEN);
        shipment.setCustomerName("Acme");
        shipment.setPromisedDelivery(Instant.now().plus(2, ChronoUnit.DAYS));
        return shipment;
    }
}
