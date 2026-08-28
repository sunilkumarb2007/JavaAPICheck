package com.codeguardian.orderservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
public class OrderCheckoutController {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final RestTemplate restTemplate;
    private final StructuredLogWriter logWriter;
    private final String paymentServiceUrl;

    public OrderCheckoutController(ProductRepository productRepository,
                                   OrderRepository orderRepository,
                                   OrderItemRepository orderItemRepository,
                                   RestTemplate restTemplate,
                                   StructuredLogWriter logWriter,
                                   @Value("${services.payment.base-url}") String paymentServiceUrl) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.restTemplate = restTemplate;
        this.logWriter = logWriter;
        this.paymentServiceUrl = paymentServiceUrl;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "order-service");
    }

    @GetMapping("/orders/products")
    public List<Product> getProducts() {
        return productRepository.findAll();
    }

    @GetMapping("/orders/products/search")
    public List<Product> searchProducts(@RequestParam(value = "q", defaultValue = "") String query) {
        if (query == null || query.trim().isEmpty()) {
            return productRepository.findAll();
        }
        return productRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(query.trim(), query.trim());
    }

    @GetMapping("/orders")
    public List<Order> getOrders() {
        return orderRepository.findAll();
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        return orderRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/orders/checkout")
    public ResponseEntity<?> checkout(@RequestBody CheckoutRequest request,
                                      @RequestHeader(value = "X-Request-ID", required = false) String requestId) {
        String effectiveRequestId = (requestId != null && !requestId.isBlank()) ? requestId : "req-order-gen";

        logWriter.log("order-service", "request_forwarded", effectiveRequestId, Map.of(
                "order_id", request.orderId() != null ? request.orderId() : 0L,
                "merchant_code", request.merchantCode(),
                "status_code", 200,
                "message", "Forwarding checkout request to payment-service"
        ));

        // Create or update order entity in database
        String orderNumber = "ORD-" + (request.orderId() != null ? request.orderId() : System.currentTimeMillis() % 100000);
        Order order = orderRepository.findByOrderNumber(orderNumber).orElseGet(() -> {
            Order newOrder = new Order(
                    request.userId() != null ? request.userId() : 101L,
                    orderNumber,
                    "PENDING",
                    request.amount() != null ? request.amount() : 149.0,
                    request.merchantCode()
            );
            Order saved = orderRepository.save(newOrder);
            return saved != null ? saved : newOrder;
        });

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Request-ID", effectiveRequestId);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    paymentServiceUrl + "/payments/charge",
                    new HttpEntity<>(request, headers),
                    String.class
            );

            order.setStatus("CONFIRMED");
            orderRepository.save(order);

            logWriter.log("order-service", "checkout_completed", effectiveRequestId, Map.of(
                    "order_id", order.getId() != null ? order.getId() : 0L,
                    "status_code", response.getStatusCode().value(),
                    "message", "Payment completed"
            ));

            return ResponseEntity.status(response.getStatusCode())
                    .header("X-Request-ID", effectiveRequestId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response.getBody());
        } catch (HttpStatusCodeException ex) {
            order.setStatus("FAILED");
            orderRepository.save(order);

            String responseBody = ex.getResponseBodyAsString();
            logWriter.log("order-service", "downstream_failure", effectiveRequestId, Map.of(
                    "status_code", ex.getStatusCode().value(),
                    "error_code", "DOWNSTREAM_PAYMENT_FAILURE",
                    "message", "payment-service returned " + ex.getStatusCode().value()
            ));

            if (responseBody != null && !responseBody.isBlank()) {
                return ResponseEntity.status(ex.getStatusCode())
                        .header("X-Request-ID", effectiveRequestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(responseBody);
            }

            ErrorResponse fallbackError = ErrorResponse.of(
                    effectiveRequestId,
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "DOWNSTREAM_PAYMENT_FAILURE",
                    "Payment service failed",
                    "order-service",
                    "/orders/checkout",
                    "HttpStatusCodeException",
                    "OrderCheckoutController.java",
                    90
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Request-ID", effectiveRequestId)
                    .body(fallbackError);
        }
    }
}
