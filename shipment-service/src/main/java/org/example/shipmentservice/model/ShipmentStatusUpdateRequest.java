package main.java.org.example.shipmentservice.model;

import jakarta.validation.constraints.NotBlank;

public record ShipmentStatusUpdateRequest(
        @NotBlank String status
) {
}