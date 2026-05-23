package org.example.orderservice.event;

import java.time.Instant;

public record OrderCreatedEvent(
        Long orderId,
        String sku,
        int quantity,
        String customerName,
        Instant timestamp
) {
}
