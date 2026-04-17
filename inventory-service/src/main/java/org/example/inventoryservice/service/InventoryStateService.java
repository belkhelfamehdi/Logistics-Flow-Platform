package org.example.inventoryservice.service;

import org.example.inventoryservice.model.InventoryReservation;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class InventoryStateService {

    private final AtomicLong reservationIdSequence = new AtomicLong(0L);
    private final ConcurrentMap<String, Integer> stockBySku = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, InventoryReservation> reservations = new ConcurrentHashMap<>();

    public InventoryStateService() {
        stockBySku.put("SKU-001", 40);
        stockBySku.put("SKU-002", 20);
        stockBySku.put("SKU-003", 60);
    }

    public synchronized InventoryReservation processOrderCreated(Long orderId, String sku, int quantity) {
        int available = stockBySku.getOrDefault(sku, 0);
        boolean accepted = available >= quantity;

        if (accepted) {
            stockBySku.put(sku, available - quantity);
        }

        String status = accepted ? "RESERVED" : "REJECTED_NO_STOCK";
        long reservationId = reservationIdSequence.incrementAndGet();
        InventoryReservation reservation = new InventoryReservation(
                reservationId,
                orderId,
                sku,
                quantity,
                status,
                Instant.now()
        );
        reservations.put(reservationId, reservation);
        return reservation;
    }

    public synchronized int restock(String sku, int quantity) {
        int current = stockBySku.getOrDefault(sku, 0);
        int updated = current + quantity;
        stockBySku.put(sku, updated);
        return updated;
    }

    public Map<String, Integer> snapshotStock() {
        return new LinkedHashMap<>(new java.util.TreeMap<>(stockBySku));
    }

    public Integer stockForSku(String sku) {
        return stockBySku.getOrDefault(sku, 0);
    }

    public List<InventoryReservation> allReservations() {
        return reservations.values().stream()
                .sorted(Comparator.comparingLong(InventoryReservation::id))
                .toList();
    }
}
