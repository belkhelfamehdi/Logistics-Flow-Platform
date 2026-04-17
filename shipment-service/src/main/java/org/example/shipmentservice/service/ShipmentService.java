package org.example.shipmentservice.service;

import org.example.shipmentservice.model.ShipmentRecord;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ShipmentService {

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
}
