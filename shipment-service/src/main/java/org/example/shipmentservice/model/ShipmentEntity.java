package org.example.shipmentservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "shipments", uniqueConstraints = @UniqueConstraint(columnNames = "reservation_id"))
public class ShipmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "reservation_id", nullable = false, unique = true)
    private Long reservationId;

    @Column(nullable = false, length = 64)
    private String sku;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(nullable = false, length = 64)
    private String carrier;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ShipmentEntity() {
    }

    public ShipmentEntity(Long orderId, Long reservationId, String sku, int quantity, String status, String carrier, Instant createdAt) {
        this.orderId = orderId;
        this.reservationId = reservationId;
        this.sku = sku;
        this.quantity = quantity;
        this.status = status;
        this.carrier = carrier;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public String getSku() {
        return sku;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCarrier() {
        return carrier;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public ShipmentRecord toRecord() {
        return new ShipmentRecord(id, orderId, reservationId, sku, quantity, status, carrier, createdAt);
    }
}
