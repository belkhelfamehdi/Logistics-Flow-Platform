package org.example.inventoryservice.service;

import org.example.inventoryservice.model.InventoryReservation;
import org.example.inventoryservice.model.InventoryReservationEntity;
import org.example.inventoryservice.model.StockEntity;
import org.example.inventoryservice.repository.InventoryReservationRepository;
import org.example.inventoryservice.repository.StockRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class InventoryStateService {

    private final StockRepository stockRepository;
    private final InventoryReservationRepository reservationRepository;

    public InventoryStateService(StockRepository stockRepository, InventoryReservationRepository reservationRepository) {
        this.stockRepository = stockRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional
    public InventoryReservation processOrderCreated(Long orderId, String sku, int quantity) {
        return reservationRepository.findByOrderId(orderId)
                .map(InventoryReservationEntity::toRecord)
                .orElseGet(() -> persistReservation(orderId, sku, quantity));
    }

    private InventoryReservation persistReservation(Long orderId, String sku, int quantity) {
        StockEntity stock = stockRepository.findById(sku).orElse(null);
        int available = stock != null ? stock.getAvailable() : 0;
        boolean accepted = stock != null && available >= quantity;

        if (accepted) {
            stock.setAvailable(available - quantity);
            stockRepository.save(stock);
        }

        InventoryReservationEntity entity = new InventoryReservationEntity(
                orderId,
                sku,
                quantity,
                accepted ? "RESERVED" : "REJECTED_NO_STOCK",
                Instant.now()
        );
        return reservationRepository.save(entity).toRecord();
    }

    @Transactional
    public int restock(String sku, int quantity) {
        StockEntity stock = stockRepository.findById(sku)
                .orElseGet(() -> new StockEntity(sku, 0));
        stock.setAvailable(stock.getAvailable() + quantity);
        return stockRepository.save(stock).getAvailable();
    }

    @Transactional(readOnly = true)
    public Map<String, Integer> snapshotStock() {
        Map<String, Integer> sorted = new TreeMap<>();
        for (StockEntity entity : stockRepository.findAll()) {
            sorted.put(entity.getSku(), entity.getAvailable());
        }
        return new LinkedHashMap<>(sorted);
    }

    @Transactional(readOnly = true)
    public Integer stockForSku(String sku) {
        return stockRepository.findById(sku).map(StockEntity::getAvailable).orElse(0);
    }

    @Transactional(readOnly = true)
    public List<InventoryReservation> allReservations() {
        return reservationRepository.findAll(Sort.by("id")).stream()
                .map(InventoryReservationEntity::toRecord)
                .toList();
    }
}
