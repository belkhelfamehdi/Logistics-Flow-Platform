package org.example.inventoryservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.inventoryservice.event.InventoryReservedEvent;
import org.example.inventoryservice.event.OrderCreatedEvent;
import org.example.inventoryservice.model.InventoryReservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class InventoryEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryEventProcessor.class);
    public static final String ORDER_TOPIC = "order-events";
    public static final String INVENTORY_TOPIC = "inventory-events";

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

    @KafkaListener(topics = ORDER_TOPIC, groupId = "inventory-group")
    public void onOrderCreated(String message) throws JsonProcessingException {
        OrderCreatedEvent event = objectMapper.readValue(message, OrderCreatedEvent.class);
        String sku = event.sku() == null ? "" : event.sku().trim().toUpperCase();

        InventoryReservation reservation = inventoryStateService.processOrderCreated(
                event.orderId(),
                sku,
                event.quantity()
        );
        publishInventoryReserved(reservation);
        LOGGER.info("Processed OrderCreatedEvent for orderId={}, status={}", event.orderId(), reservation.status());
    }

    private void publishInventoryReserved(InventoryReservation reservation) throws JsonProcessingException {
        InventoryReservedEvent event = new InventoryReservedEvent(
                reservation.orderId(),
                reservation.id(),
                reservation.sku(),
                reservation.quantity(),
                reservation.status(),
                "RESERVED".equals(reservation.status()),
                reservation.createdAt()
        );

        String message = objectMapper.writeValueAsString(event);
        kafkaTemplate.send(INVENTORY_TOPIC, String.valueOf(reservation.orderId()), message);
    }
}
