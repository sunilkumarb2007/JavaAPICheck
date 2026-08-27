package com.example.gateway.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@CrossOrigin(origins = "*")
public class GatewayController {

    private final RestTemplate restTemplate;
    private final String orderServiceUrl;

    @Autowired
    public GatewayController(
            RestTemplate restTemplate,
            @Value("${app.services.order-url:http://localhost:8081}") String orderServiceUrl) {
        this.restTemplate = restTemplate;
        this.orderServiceUrl = orderServiceUrl;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "service", "gateway",
                "status", "UP",
                "timestamp", Instant.now().toString()
        ));
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestBody Map<String, Object> payload) {
        String requestId = "req-" + UUID.randomUUID().toString().substring(0, 8);

        System.out.println(
                "timestamp=" + Instant.now()
                        + " service=api-gateway"
                        + " event_type=request_received"
                        + " request_id=" + requestId
                        + " method=POST"
                        + " path=/checkout"
        );

        payload.put("requestId", requestId);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    orderServiceUrl + "/orders/checkout",
                    payload,
                    Map.class
            );

            return ResponseEntity
                    .status(response.getStatusCode())
                    .body(response.getBody());

        } catch (Exception e) {
            System.out.println(
                    "timestamp=" + Instant.now()
                            + " service=api-gateway"
                            + " event_type=request_failed"
                            + " request_id=" + requestId
                            + " status_code=500"
                            + " error_code=HTTP_500"
                            + " message=Gateway observed downstream failure"
            );

            return ResponseEntity.status(500).body(Map.of(
                    "status", "FAILED",
                    "message", "Checkout failed in downstream service",
                    "errorCode", "HTTP_500",
                    "requestId", requestId
            ));
        }
    }
}
