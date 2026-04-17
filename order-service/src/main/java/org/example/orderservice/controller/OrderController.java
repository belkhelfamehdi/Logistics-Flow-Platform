package org.example.orderservice.controller;

import jakarta.validation.Valid;
import org.example.orderservice.model.OrderRecord;
import org.example.orderservice.model.OrderRequest;
import org.example.orderservice.service.OrderEventPublisher;
import org.example.orderservice.service.OrderStore;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderStore orderStore;
    private final OrderEventPublisher orderEventPublisher;

    public OrderController(OrderStore orderStore, OrderEventPublisher orderEventPublisher) {
        this.orderStore = orderStore;
        this.orderEventPublisher = orderEventPublisher;
    }

    @PostMapping
    public ResponseEntity<OrderRecord> createOrder(@Valid @RequestBody OrderRequest request) {
        OrderRecord created = orderStore.create(request);
        orderEventPublisher.publishOrderCreated(created);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public List<OrderRecord> listOrders() {
        return orderStore.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderRecord> findOrder(@PathVariable Long id) {
        return orderStore.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
