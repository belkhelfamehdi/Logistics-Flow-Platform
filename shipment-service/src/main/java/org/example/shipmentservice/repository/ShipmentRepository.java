package org.example.shipmentservice.repository;

import org.example.shipmentservice.model.ShipmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShipmentRepository extends JpaRepository<ShipmentEntity, Long> {

    Optional<ShipmentEntity> findByReservationId(Long reservationId);
}
