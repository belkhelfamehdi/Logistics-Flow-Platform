package org.example.orderservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.orderservice.model.OrderRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class OrderEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderEventPublisher.class);
    private static final String LOGISTICS_TOPIC = "logistics-events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OrderEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishOrderCreated(OrderRecord order) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "ORDER_CREATED");
        payload.put("orderId", order.id());
        payload.put("sku", order.sku());
        payload.put("quantity", order.quantity());
        payload.put("customerName", order.customerName());
        payload.put("timestamp", order.createdAt().toString());

        try {
            String message = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(LOGISTICS_TOPIC, String.valueOf(order.id()), message);
            LOGGER.info("Published ORDER_CREATED event for orderId={}", order.id());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize ORDER_CREATED event", exception);
        }
    }
}
