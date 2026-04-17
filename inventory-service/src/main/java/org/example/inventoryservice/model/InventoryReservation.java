package org.example.inventoryservice.model;

import java.time.Instant;

public record InventoryReservation(
        Long id,
        Long orderId,
        String sku,
        int quantity,
        String status,
        Instant createdAt
) {
}
