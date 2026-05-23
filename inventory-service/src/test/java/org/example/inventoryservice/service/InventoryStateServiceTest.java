package org.example.inventoryservice.service;

import org.example.inventoryservice.model.InventoryReservation;
import org.example.inventoryservice.model.InventoryReservationEntity;
import org.example.inventoryservice.model.StockEntity;
import org.example.inventoryservice.repository.InventoryReservationRepository;
import org.example.inventoryservice.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InventoryStateServiceTest {

    private StockRepository stockRepository;
    private InventoryReservationRepository reservationRepository;
    private InventoryStateService service;

    @BeforeEach
    void setUp() {
        stockRepository = mock(StockRepository.class);
        reservationRepository = mock(InventoryReservationRepository.class);
        service = new InventoryStateService(stockRepository, reservationRepository);
    }

    @Test
    void processOrderCreated_reservesWhenStockSufficient() {
        StockEntity stock = new StockEntity("SKU-001", 10);
        when(reservationRepository.findByOrderId(1L)).thenReturn(Optional.empty());
        when(stockRepository.findById("SKU-001")).thenReturn(Optional.of(stock));
        when(reservationRepository.save(any(InventoryReservationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InventoryReservation reservation = service.processOrderCreated(1L, "SKU-001", 3);

        assertThat(reservation.status()).isEqualTo("RESERVED");
        assertThat(reservation.orderId()).isEqualTo(1L);
        assertThat(stock.getAvailable()).isEqualTo(7);
        verify(stockRepository).save(stock);
    }

    @Test
    void processOrderCreated_rejectsWhenStockInsufficient() {
        StockEntity stock = new StockEntity("SKU-001", 2);
        when(reservationRepository.findByOrderId(1L)).thenReturn(Optional.empty());
        when(stockRepository.findById("SKU-001")).thenReturn(Optional.of(stock));
        when(reservationRepository.save(any(InventoryReservationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InventoryReservation reservation = service.processOrderCreated(1L, "SKU-001", 5);

        assertThat(reservation.status()).isEqualTo("REJECTED_NO_STOCK");
        assertThat(stock.getAvailable()).isEqualTo(2);
        verify(stockRepository, never()).save(any(StockEntity.class));
    }

    @Test
    void processOrderCreated_rejectsWhenSkuUnknown() {
        when(reservationRepository.findByOrderId(1L)).thenReturn(Optional.empty());
        when(stockRepository.findById("SKU-XXX")).thenReturn(Optional.empty());
        when(reservationRepository.save(any(InventoryReservationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InventoryReservation reservation = service.processOrderCreated(1L, "SKU-XXX", 1);

        assertThat(reservation.status()).isEqualTo("REJECTED_NO_STOCK");
    }

    @Test
    void processOrderCreated_isIdempotentByOrderId() {
        InventoryReservationEntity existing = new InventoryReservationEntity(
                1L, "SKU-001", 3, "RESERVED", Instant.now()
        );
        when(reservationRepository.findByOrderId(1L)).thenReturn(Optional.of(existing));

        InventoryReservation first = service.processOrderCreated(1L, "SKU-001", 3);
        InventoryReservation second = service.processOrderCreated(1L, "SKU-001", 3);

        assertThat(first.status()).isEqualTo("RESERVED");
        assertThat(second.status()).isEqualTo("RESERVED");
        verify(stockRepository, never()).findById(any());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void restock_incrementsExistingStock() {
        StockEntity stock = new StockEntity("SKU-001", 5);
        when(stockRepository.findById("SKU-001")).thenReturn(Optional.of(stock));
        when(stockRepository.save(stock)).thenReturn(stock);

        int updated = service.restock("SKU-001", 8);

        assertThat(updated).isEqualTo(13);
        assertThat(stock.getAvailable()).isEqualTo(13);
    }

    @Test
    void restock_createsRowForUnknownSku() {
        when(stockRepository.findById("SKU-NEW")).thenReturn(Optional.empty());
        when(stockRepository.save(any(StockEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int updated = service.restock("SKU-NEW", 10);

        assertThat(updated).isEqualTo(10);
    }
}
