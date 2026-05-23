package org.example.shipmentservice.service;

import org.example.shipmentservice.model.ShipmentEntity;
import org.example.shipmentservice.model.ShipmentRecord;
import org.example.shipmentservice.repository.ShipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShipmentServiceTest {

    private ShipmentRepository repository;
    private ShipmentService service;

    @BeforeEach
    void setUp() {
        repository = mock(ShipmentRepository.class);
        service = new ShipmentService(repository);
    }

    @Test
    void createFromReservation_persistsNewShipmentWhenReservationUnseen() {
        when(repository.findByReservationId(42L)).thenReturn(Optional.empty());
        when(repository.save(any(ShipmentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShipmentRecord shipment = service.createFromReservation(1L, 42L, "SKU-001", 2, true);

        assertThat(shipment.orderId()).isEqualTo(1L);
        assertThat(shipment.reservationId()).isEqualTo(42L);
        assertThat(shipment.status()).isEqualTo("PREPARING");
        assertThat(shipment.carrier()).isEqualTo("DHL");
        verify(repository, times(1)).save(any(ShipmentEntity.class));
    }

    @Test
    void createFromReservation_isIdempotentByReservationId() {
        ShipmentEntity existing = new ShipmentEntity(1L, 42L, "SKU-001", 2, "PREPARING", "DHL", Instant.now());
        when(repository.findByReservationId(42L)).thenReturn(Optional.of(existing));

        ShipmentRecord first = service.createFromReservation(1L, 42L, "SKU-001", 2, true);
        ShipmentRecord second = service.createFromReservation(1L, 42L, "SKU-001", 2, true);

        assertThat(first.reservationId()).isEqualTo(42L);
        assertThat(second.reservationId()).isEqualTo(42L);
        verify(repository, never()).save(any(ShipmentEntity.class));
    }

    @Test
    void createFromReservation_rejectedReservationLandsOnHold() {
        when(repository.findByReservationId(99L)).thenReturn(Optional.empty());
        when(repository.save(any(ShipmentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShipmentRecord shipment = service.createFromReservation(7L, 99L, "SKU-002", 5, false);

        assertThat(shipment.status()).isEqualTo("ON_HOLD");
        assertThat(shipment.carrier()).isEqualTo("N/A");
    }

    @Test
    void updateStatus_movesPreparingToOutForDelivery() {
        ShipmentEntity entity = new ShipmentEntity(1L, 42L, "SKU-001", 2, "PREPARING", "DHL", Instant.now());
        when(repository.findById(10L)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);

        Optional<ShipmentRecord> updated = service.updateStatus(10L, "OUT_FOR_DELIVERY");

        assertThat(updated).isPresent();
        assertThat(updated.get().status()).isEqualTo("OUT_FOR_DELIVERY");
    }

    @Test
    void updateStatus_rejectsTransitionFromTerminalState() {
        ShipmentEntity entity = new ShipmentEntity(1L, 42L, "SKU-001", 2, "DELIVERED", "DHL", Instant.now());
        when(repository.findById(10L)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.updateStatus(10L, "OUT_FOR_DELIVERY"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void updateStatus_rejectsUnknownStatus() {
        assertThatThrownBy(() -> service.updateStatus(10L, "TELEPORTED"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown status");
    }

    @Test
    void updateStatus_returnsEmptyForMissingShipment() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThat(service.updateStatus(404L, "OUT_FOR_DELIVERY")).isEmpty();
    }
}
