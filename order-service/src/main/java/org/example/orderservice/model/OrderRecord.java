package org.example.orderservice.model;

import java.time.Instant;

public record OrderRecord(
        Long id,
        String sku,
        int quantity,
        String customerName,
        String status,
        Instant createdAt
) {
}
