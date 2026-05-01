package org.example.shipmentservice.model;

import jakarta.validation.constraints.NotBlank;

public record ShipmentStatusUpdateRequest(
        @NotBlank String status
) {
}