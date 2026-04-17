package org.example.orderservice.service;

import org.example.orderservice.model.OrderRecord;
import org.example.orderservice.model.OrderRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class OrderStore {

    private final AtomicLong idSequence = new AtomicLong(0L);
    private final ConcurrentMap<Long, OrderRecord> orders = new ConcurrentHashMap<>();

    public OrderRecord create(OrderRequest request) {
        long id = idSequence.incrementAndGet();
        OrderRecord order = new OrderRecord(
                id,
                request.sku().trim().toUpperCase(),
                request.quantity(),
                request.customerName().trim(),
                "CREATED",
                Instant.now()
        );
        orders.put(id, order);
        return order;
    }

    public List<OrderRecord> findAll() {
        return orders.values().stream()
                .sorted(Comparator.comparingLong(OrderRecord::id))
                .toList();
    }

    public Optional<OrderRecord> findById(Long id) {
        return Optional.ofNullable(orders.get(id));
    }
}
