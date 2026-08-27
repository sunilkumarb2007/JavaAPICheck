package com.example.gateway.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

import org.springframework.web.bind.annotation.CrossOrigin;

@RestController
@CrossOrigin(origins = "*") // Allow GitHub Pages and Localtunnel to call this backend
public class GatewayController {

    private final RestTemplate restTemplate;
    private final String ORDER_SERVICE_URL = "http://localhost:8081";

    @Autowired
    public GatewayController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestBody Map<String, Object> payload) {
        String requestId = "req-" + UUID.randomUUID().toString().substring(0, 8);
        System.out.println("timestamp=" + java.time.Instant.now() + " service=api-gateway event_type=request_received request_id=" + requestId + " method=POST path=/checkout");

        try {
            // Forward to order service
            payload.put("requestId", requestId);
            ResponseEntity<Map> response = restTemplate.postForEntity(ORDER_SERVICE_URL + "/orders/checkout", payload, Map.class);
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            System.out.println("timestamp=" + java.time.Instant.now() + " service=api-gateway event_type=request_failed request_id=" + requestId + " status_code=500 error_code=HTTP_500 message=Gateway observed downstream HTTP 500");
            return ResponseEntity.status(500).body(Map.of(
                "status", "FAILED",
                "message", "Checkout failed in downstream service",
                "errorCode", "HTTP_500"
            ));
        }
    }
}
