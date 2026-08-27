package com.example.order.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final RestTemplate restTemplate;
    private final String paymentServiceUrl;

    @Autowired
    public OrderController(
            RestTemplate restTemplate,
            @Value("${app.services.payment-url:http://localhost:8082}") String paymentServiceUrl) {
        this.restTemplate = restTemplate;
        this.paymentServiceUrl = paymentServiceUrl;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "service", "order-service",
                "status", "UP",
                "timestamp", Instant.now().toString()
        ));
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> processOrder(@RequestBody Map<String, Object> payload) {
        String requestId = (String) payload.getOrDefault("requestId", "unknown");

        System.out.println(
                "timestamp=" + Instant.now()
                        + " service=order-service"
                        + " event_type=request_forwarded"
                        + " request_id=" + requestId
                        + " status_code=200"
                        + " message=Forwarding checkout request to payment-service"
        );

        try {
            payload.put("merchantCode", "M" + payload.get("orderId"));

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    paymentServiceUrl + "/api/payments",
                    payload,
                    Map.class
            );

            return ResponseEntity
                    .status(response.getStatusCode())
                    .body(response.getBody());

        } catch (Exception e) {
            System.out.println(
                    "timestamp=" + Instant.now()
                            + " service=order-service"
                            + " event_type=downstream_failure"
                            + " request_id=" + requestId
                            + " status_code=500"
                            + " error_code=DOWNSTREAM_PAYMENT_FAILURE"
                            + " message=payment-service request failed"
            );

            throw new RuntimeException("Payment service failed", e);
        }
    }
}
