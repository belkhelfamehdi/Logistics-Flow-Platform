package org.example.shipmentservice.controller;

import jakarta.validation.Valid;
import org.example.shipmentservice.model.ShipmentRecord;
import org.example.shipmentservice.model.ShipmentStatusUpdateRequest;
import org.example.shipmentservice.service.ShipmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/shipments")
public class ShipmentController {

    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @GetMapping
    public List<ShipmentRecord> allShipments() {
        return shipmentService.allShipments();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShipmentRecord> shipmentById(@PathVariable Long id) {
        return shipmentService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateShipmentStatus(
            @PathVariable Long id,
            @Valid @RequestBody ShipmentStatusUpdateRequest request,
            @RequestHeader(value = "X-Authenticated-Roles", required = false) String rolesHeader
    ) {
        Set<String> roles = parseRoles(rolesHeader);
        String targetStatus = request.status().trim().toUpperCase();

        if (!isAllowedForStatusUpdate(roles, targetStatus)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Role is not allowed to apply this status"));
        }

        try {
            return shipmentService.updateStatus(id, targetStatus)
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    private static Set<String> parseRoles(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) {
            return Set.of();
        }

        return Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());
    }

    private static boolean isAllowedForStatusUpdate(Set<String> roles, String status) {
        if (roles.contains("ADMIN")) {
            return true;
        }

        Set<String> deliveryStatuses = Set.of("OUT_FOR_DELIVERY", "DELIVERED", "DELIVERY_FAILED");
        Set<String> operationsStatuses = Set.of("PREPARING", "ON_HOLD", "RETURNED", "CANCELED");

        if (roles.contains("DELIVERY") && deliveryStatuses.contains(status)) {
            return true;
        }

        return roles.contains("OPS") && operationsStatuses.contains(status);
    }
}
