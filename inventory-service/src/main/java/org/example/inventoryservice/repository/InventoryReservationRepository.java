package org.example.inventoryservice.repository;

import org.example.inventoryservice.model.InventoryReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservationEntity, Long> {

    Optional<InventoryReservationEntity> findByOrderId(Long orderId);
}
