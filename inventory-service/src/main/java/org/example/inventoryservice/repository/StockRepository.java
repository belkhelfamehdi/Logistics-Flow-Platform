package org.example.inventoryservice.repository;

import org.example.inventoryservice.model.StockEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepository extends JpaRepository<StockEntity, String> {
}
