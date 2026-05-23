package org.example.inventoryservice.event;

import java.time.Instant;

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
