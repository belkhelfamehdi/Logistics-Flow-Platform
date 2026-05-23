package org.example.orderservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.orderservice.event.OrderCreatedEvent;
import org.example.orderservice.model.OrderRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderEventPublisher.class);
    public static final String ORDER_TOPIC = "order-events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OrderEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishOrderCreated(OrderRecord order) {
        OrderCreatedEvent event = new OrderCreatedEvent(
                order.id(),
                order.sku(),
                order.quantity(),
                order.customerName(),
                order.createdAt()
        );

        try {
            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(ORDER_TOPIC, String.valueOf(order.id()), message)
                    .whenComplete((result, exception) -> {
                        if (exception != null) {
                            LOGGER.error("Failed to publish OrderCreatedEvent for orderId={}", order.id(), exception);
                        } else {
                            LOGGER.info("Published OrderCreatedEvent for orderId={}", order.id());
                        }
                    });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize OrderCreatedEvent", exception);
        }
    }
}
