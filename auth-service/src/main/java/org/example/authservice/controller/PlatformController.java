package org.example.authservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/platform")
public class PlatformController {

    @GetMapping("/admin/home")
    public Map<String, String> adminHome() {
        return Map.of(
                "platform", "ADMIN",
                "message", "Administrative platform ready"
        );
    }

    @GetMapping("/ops/home")
    public Map<String, String> operationsHome() {
        return Map.of(
                "platform", "OPS",
                "message", "Operations platform ready"
        );
    }

    @GetMapping("/delivery/home")
    public Map<String, String> deliveryHome() {
        return Map.of(
                "platform", "DELIVERY",
                "message", "Delivery platform ready"
        );
    }
}