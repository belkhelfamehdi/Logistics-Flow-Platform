package org.example.inventoryservice.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderCreatedEvent(
        Long orderId,
        String sku,
        int quantity,
        String customerName,
        Instant timestamp
) {
}
