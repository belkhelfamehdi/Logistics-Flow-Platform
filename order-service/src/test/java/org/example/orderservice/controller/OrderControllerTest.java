package org.example.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.orderservice.model.OrderRecord;
import org.example.orderservice.model.OrderRequest;
import org.example.orderservice.service.OrderEventPublisher;
import org.example.orderservice.service.OrderStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private StubOrderStore store;
    private StubPublisher publisher;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        store = new StubOrderStore();
        publisher = new StubPublisher();

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new OrderController(store, publisher))
                .setValidator(validator)
                .build();
    }

    @Test
    void createOrder_returns201AndPublishesEvent() throws Exception {
        store.next = new OrderRecord(1L, "SKU-001", 2, "Alice", "CREATED", Instant.parse("2026-01-01T10:00:00Z"));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderRequest("SKU-001", 2, "Alice"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.sku").value("SKU-001"))
                .andExpect(jsonPath("$.status").value("CREATED"));

        assertThat(publisher.published.get()).isEqualTo(store.next);
    }

    @Test
    void createOrder_rejectsNegativeQuantity() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderRequest("SKU-001", 0, "Alice"))))
                .andExpect(status().isBadRequest());

        assertThat(store.createCalls).isEqualTo(0);
        assertThat(publisher.published.get()).isNull();
    }

    @Test
    void createOrder_rejectsBlankSku() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderRequest("", 2, "Alice"))))
                .andExpect(status().isBadRequest());

        assertThat(store.createCalls).isEqualTo(0);
    }

    @Test
    void createOrder_rejectsBlankCustomerName() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderRequest("SKU-001", 2, ""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findOrder_returnsNotFoundForMissingId() throws Exception {
        mockMvc.perform(get("/orders/999"))
                .andExpect(status().isNotFound());
    }

    private static class StubOrderStore extends OrderStore {
        OrderRecord next;
        int createCalls;

        StubOrderStore() {
            super(null);
        }

        @Override
        public OrderRecord create(OrderRequest request) {
            createCalls++;
            return next;
        }

        @Override
        public List<OrderRecord> findAll() {
            return List.of();
        }

        @Override
        public Optional<OrderRecord> findById(Long id) {
            return Optional.empty();
        }
    }

    private static class StubPublisher extends OrderEventPublisher {
        final AtomicReference<OrderRecord> published = new AtomicReference<>();

        StubPublisher() {
            super(null, new ObjectMapper());
        }

        @Override
        public void publishOrderCreated(OrderRecord order) {
            published.set(order);
        }
    }
}
