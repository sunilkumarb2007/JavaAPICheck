package com.example.order.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final RestTemplate restTemplate;
    private final String PAYMENT_SERVICE_URL = "http://localhost:8082";

    @Autowired
    public OrderController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> processOrder(@RequestBody Map<String, Object> payload) {
        String requestId = (String) payload.getOrDefault("requestId", "unknown");
        System.out.println("timestamp=" + java.time.Instant.now() + " service=order-service event_type=request_forwarded request_id=" + requestId + " status_code=200 message=Forwarding checkout request to payment-service");

        try {
            // Forward to payment service (mapping amount/userId to whatever it expects)
            payload.put("merchantCode", "M" + payload.get("orderId")); // Map order to merchant code for demo
            ResponseEntity<Map> response = restTemplate.postForEntity(PAYMENT_SERVICE_URL + "/api/payments", payload, Map.class);
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            System.out.println("timestamp=" + java.time.Instant.now() + " service=order-service event_type=downstream_failure request_id=" + requestId + " status_code=500 error_code=DOWNSTREAM_PAYMENT_FAILURE message=payment-service returned HTTP 500");
            throw new RuntimeException("Payment service failed");
        }
    }
}
