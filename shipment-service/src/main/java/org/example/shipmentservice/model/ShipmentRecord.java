package org.example.shipmentservice.model;

import java.time.Instant;

public record ShipmentRecord(
        Long id,
        Long orderId,
        Long reservationId,
        String sku,
        int quantity,
        String status,
        String carrier,
        Instant createdAt
) {
}
