package org.example.orderservice.service;

import org.example.orderservice.model.OrderEntity;
import org.example.orderservice.model.OrderRecord;
import org.example.orderservice.model.OrderRequest;
import org.example.orderservice.repository.OrderRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class OrderStore {

    private final OrderRepository orderRepository;

    public OrderStore(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public OrderRecord create(OrderRequest request) {
        OrderEntity entity = new OrderEntity(
                request.sku().trim().toUpperCase(),
                request.quantity(),
                request.customerName().trim(),
                "CREATED",
                Instant.now()
        );
        return orderRepository.save(entity).toRecord();
    }

    @Transactional(readOnly = true)
    public List<OrderRecord> findAll() {
        return orderRepository.findAll(Sort.by("id")).stream()
                .map(OrderEntity::toRecord)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<OrderRecord> findById(Long id) {
        return orderRepository.findById(id).map(OrderEntity::toRecord);
    }
}
