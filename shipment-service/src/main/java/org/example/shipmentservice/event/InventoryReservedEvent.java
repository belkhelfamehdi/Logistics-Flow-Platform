package org.example.shipmentservice.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InventoryReservedEvent(
        Long orderId,
        Long reservationId,
        String sku,
        int quantity,
        String status,
        boolean accepted,
        Instant timestamp
) {
}
