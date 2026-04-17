package org.example.inventoryservice.controller;

import org.example.inventoryservice.model.InventoryReservation;
import org.example.inventoryservice.service.InventoryStateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryStateService inventoryStateService;

    public InventoryController(InventoryStateService inventoryStateService) {
        this.inventoryStateService = inventoryStateService;
    }

    @GetMapping
    public Map<String, Integer> stock() {
        return inventoryStateService.snapshotStock();
    }

    @GetMapping("/reservations")
    public List<InventoryReservation> reservations() {
        return inventoryStateService.allReservations();
    }

    @GetMapping("/{sku}")
    public Map<String, Object> stockForSku(@PathVariable String sku) {
        return Map.of(
                "sku", sku.toUpperCase(),
                "available", inventoryStateService.stockForSku(sku.toUpperCase())
        );
    }

    @PostMapping("/{sku}/restock")
    public ResponseEntity<Map<String, Object>> restock(
            @PathVariable String sku,
            @RequestParam(defaultValue = "1") int quantity
    ) {
        if (quantity <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "quantity must be greater than 0"));
        }

        int updated = inventoryStateService.restock(sku.toUpperCase(), quantity);
        return ResponseEntity.ok(Map.of(
                "sku", sku.toUpperCase(),
                "available", updated
        ));
    }
}
