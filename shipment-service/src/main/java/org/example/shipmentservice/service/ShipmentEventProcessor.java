package org.example.shipmentservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.shipmentservice.event.InventoryReservedEvent;
import org.example.shipmentservice.model.ShipmentRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class ShipmentEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShipmentEventProcessor.class);
    public static final String INVENTORY_TOPIC = "inventory-events";

    private final ShipmentService shipmentService;
    private final ObjectMapper objectMapper;

    public ShipmentEventProcessor(ShipmentService shipmentService, ObjectMapper objectMapper) {
        this.shipmentService = shipmentService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = INVENTORY_TOPIC, groupId = "shipment-group")
    public void onInventoryReserved(String message) throws JsonProcessingException {
        InventoryReservedEvent event = objectMapper.readValue(message, InventoryReservedEvent.class);
        String sku = event.sku() == null ? "" : event.sku().trim().toUpperCase();

        ShipmentRecord shipment = shipmentService.createFromReservation(
                event.orderId(),
                event.reservationId(),
                sku,
                event.quantity(),
                event.accepted()
        );

        LOGGER.info("Created shipment id={} for orderId={}, status={}", shipment.id(), event.orderId(), shipment.status());
    }
}
