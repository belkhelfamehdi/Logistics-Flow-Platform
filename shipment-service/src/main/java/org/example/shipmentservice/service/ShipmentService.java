package org.example.shipmentservice.service;

import org.example.shipmentservice.model.ShipmentEntity;
import org.example.shipmentservice.model.ShipmentRecord;
import org.example.shipmentservice.repository.ShipmentRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    private final ShipmentRepository shipmentRepository;

    public ShipmentService(ShipmentRepository shipmentRepository) {
        this.shipmentRepository = shipmentRepository;
    }

    @Transactional
    public ShipmentRecord createFromReservation(
            Long orderId,
            Long reservationId,
            String sku,
            int quantity,
            boolean accepted
    ) {
        return shipmentRepository.findByReservationId(reservationId)
                .map(ShipmentEntity::toRecord)
                .orElseGet(() -> {
                    ShipmentEntity entity = new ShipmentEntity(
                            orderId,
                            reservationId,
                            sku,
                            quantity,
                            accepted ? "PREPARING" : "ON_HOLD",
                            accepted ? "DHL" : "N/A",
                            Instant.now()
                    );
                    return shipmentRepository.save(entity).toRecord();
                });
    }

    @Transactional(readOnly = true)
    public List<ShipmentRecord> allShipments() {
        return shipmentRepository.findAll(Sort.by("id")).stream()
                .map(ShipmentEntity::toRecord)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<ShipmentRecord> findById(Long id) {
        return shipmentRepository.findById(id).map(ShipmentEntity::toRecord);
    }

    @Transactional
    public Optional<ShipmentRecord> updateStatus(Long shipmentId, String newStatus) {
        String normalizedStatus = newStatus.trim().toUpperCase();

        if (!ALLOWED_TRANSITIONS.containsKey(normalizedStatus)) {
            throw new IllegalArgumentException("Unknown status: " + normalizedStatus);
        }

        Optional<ShipmentEntity> existing = shipmentRepository.findById(shipmentId);
        if (existing.isEmpty()) {
            return Optional.empty();
        }

        ShipmentEntity entity = existing.get();
        List<String> allowed = ALLOWED_TRANSITIONS.getOrDefault(entity.getStatus(), List.of());
        if (!allowed.contains(normalizedStatus)) {
            throw new IllegalStateException(
                    "Transition from " + entity.getStatus() + " to " + normalizedStatus + " is not allowed"
            );
        }

        entity.setStatus(normalizedStatus);
        return Optional.of(shipmentRepository.save(entity).toRecord());
    }
}
