package org.example.inventoryservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.inventoryservice.model.InventoryReservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class InventoryEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryEventProcessor.class);
    private static final String LOGISTICS_TOPIC = "logistics-events";

    private final InventoryStateService inventoryStateService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public InventoryEventProcessor(
            InventoryStateService inventoryStateService,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper
    ) {
        this.inventoryStateService = inventoryStateService;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = LOGISTICS_TOPIC, groupId = "inventory-group")
    public void consume(String message) {
        try {
            Map<String, Object> payload = objectMapper.readValue(message, new TypeReference<>() {
            });
            String event = String.valueOf(payload.get("event"));

            if (!"ORDER_CREATED".equals(event)) {
                return;
            }

            Long orderId = toLong(payload.get("orderId"));
            String sku = String.valueOf(payload.get("sku")).trim().toUpperCase();
            int quantity = toInt(payload.get("quantity"));

            InventoryReservation reservation = inventoryStateService.processOrderCreated(orderId, sku, quantity);
            publishInventoryReserved(reservation);
            LOGGER.info("Processed ORDER_CREATED for orderId={}, status={}", orderId, reservation.status());
        } catch (Exception exception) {
            LOGGER.error("Failed to process logistics event: {}", message, exception);
        }
    }

    private void publishInventoryReserved(InventoryReservation reservation) throws JsonProcessingException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "INVENTORY_RESERVED");
        payload.put("orderId", reservation.orderId());
        payload.put("reservationId", reservation.id());
        payload.put("sku", reservation.sku());
        payload.put("quantity", reservation.quantity());
        payload.put("status", reservation.status());
        payload.put("accepted", "RESERVED".equals(reservation.status()));
        payload.put("timestamp", reservation.createdAt().toString());

        String message = objectMapper.writeValueAsString(payload);
        kafkaTemplate.send(LOGISTICS_TOPIC, String.valueOf(reservation.orderId()), message);
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
