package org.example.shipmentservice.service;

import org.example.shipmentservice.model.ShipmentRecord;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ShipmentService {

    private static final Map<String, List<String>> ALLOWED_TRANSITIONS = Map.of(
            "PREPARING", List.of("OUT_FOR_DELIVERY", "ON_HOLD"),
            "OUT_FOR_DELIVERY", List.of("DELIVERED", "DELIVERY_FAILED"),
            "DELIVERY_FAILED", List.of("OUT_FOR_DELIVERY", "RETURNED"),
            "ON_HOLD", List.of("PREPARING", "CANCELED"),
            "RETURNED", List.of(),
            "CANCELED", List.of(),
            "DELIVERED", List.of()
    );

    private final AtomicLong shipmentIdSequence = new AtomicLong(0L);
    private final ConcurrentMap<Long, ShipmentRecord> shipments = new ConcurrentHashMap<>();

    public ShipmentRecord createFromReservation(
            Long orderId,
            Long reservationId,
            String sku,
            int quantity,
            boolean accepted
    ) {
        long shipmentId = shipmentIdSequence.incrementAndGet();
        ShipmentRecord shipment = new ShipmentRecord(
                shipmentId,
                orderId,
                reservationId,
                sku,
                quantity,
                accepted ? "PREPARING" : "ON_HOLD",
                accepted ? "DHL" : "N/A",
                Instant.now()
        );
        shipments.put(shipmentId, shipment);
        return shipment;
    }

    public List<ShipmentRecord> allShipments() {
        return shipments.values().stream()
                .sorted(Comparator.comparingLong(ShipmentRecord::id))
                .toList();
    }

    public Optional<ShipmentRecord> findById(Long id) {
        return Optional.ofNullable(shipments.get(id));
    }

    public Optional<ShipmentRecord> updateStatus(Long shipmentId, String newStatus) {
        String normalizedStatus = newStatus.trim().toUpperCase();

        if (!ALLOWED_TRANSITIONS.containsKey(normalizedStatus)) {
            throw new IllegalArgumentException("Unknown status: " + normalizedStatus);
        }

        return Optional.ofNullable(shipments.computeIfPresent(shipmentId, (id, existing) -> {
            List<String> allowed = ALLOWED_TRANSITIONS.getOrDefault(existing.status(), List.of());
            if (!allowed.contains(normalizedStatus)) {
                throw new IllegalStateException("Transition from " + existing.status() + " to " + normalizedStatus + " is not allowed");
            }

            return new ShipmentRecord(
                    existing.id(),
                    existing.orderId(),
                    existing.reservationId(),
                    existing.sku(),
                    existing.quantity(),
                    normalizedStatus,
                    existing.carrier(),
                    existing.createdAt()
            );
        }));
    }
}
