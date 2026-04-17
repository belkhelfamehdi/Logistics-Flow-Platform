package org.example.shipmentservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.shipmentservice.model.ShipmentRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ShipmentEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShipmentEventProcessor.class);
    private static final String LOGISTICS_TOPIC = "logistics-events";

    private final ShipmentService shipmentService;
    private final ObjectMapper objectMapper;

    public ShipmentEventProcessor(ShipmentService shipmentService, ObjectMapper objectMapper) {
        this.shipmentService = shipmentService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = LOGISTICS_TOPIC, groupId = "shipment-group")
    public void consume(String message) {
        try {
            Map<String, Object> payload = objectMapper.readValue(message, new TypeReference<>() {
            });
            String event = String.valueOf(payload.get("event"));

            if (!"INVENTORY_RESERVED".equals(event)) {
                return;
            }

            Long orderId = toLong(payload.get("orderId"));
            Long reservationId = toLong(payload.get("reservationId"));
            String sku = String.valueOf(payload.get("sku")).trim().toUpperCase();
            int quantity = toInt(payload.get("quantity"));
            boolean accepted = Boolean.parseBoolean(String.valueOf(payload.get("accepted")));

            ShipmentRecord shipment = shipmentService.createFromReservation(
                    orderId,
                    reservationId,
                    sku,
                    quantity,
                    accepted
            );

            LOGGER.info("Created shipment id={} for orderId={}, status={}", shipment.id(), orderId, shipment.status());
        } catch (Exception exception) {
            LOGGER.error("Failed to process inventory event: {}", message, exception);
        }
    }

    private static Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private static int toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }
}
