package com.deliveryiq.tracking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Consumes scanner / telematics tracking events from Kafka and applies them to shipments.
 */
@Component
public class TrackingEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TrackingEventConsumer.class);

    private final ShipmentService shipmentService;
    private final ObjectMapper objectMapper;

    public TrackingEventConsumer(ShipmentService shipmentService, ObjectMapper objectMapper) {
        this.shipmentService = shipmentService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${deliveryiq.kafka.tracking-topic}", groupId = "deliveryiq-tracking")
    public void onTrackingEvent(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            UUID shipmentId = UUID.fromString(node.path("shipmentId").asText());
            String eventType = node.path("eventType").asText("LOCATION_PING");
            String status = node.path("status").asText();
            String location = node.path("locationLabel").asText(null);
            Instant occurredAt = node.hasNonNull("occurredAt")
                    ? Instant.parse(node.get("occurredAt").asText())
                    : Instant.now();

            if (status == null || status.isBlank()) {
                log.warn("Ignoring tracking event without status: {}", payload);
                return;
            }

            shipmentService.applyExternalEvent(shipmentId, eventType, status, location, occurredAt);
        } catch (Exception ex) {
            log.error("Failed to process tracking event payload: {}", payload, ex);
        }
    }
}
