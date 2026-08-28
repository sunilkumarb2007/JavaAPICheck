package com.codeguardian.gateway;

import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
public class CheckoutController {

    private final RestTemplate restTemplate;
    private final StructuredLogWriter logWriter;
    private final String orderServiceUrl;

    public CheckoutController(RestTemplate restTemplate,
                              StructuredLogWriter logWriter,
                              @Value("${services.order.base-url}") String orderServiceUrl) {
        this.restTemplate = restTemplate;
        this.logWriter = logWriter;
        this.orderServiceUrl = orderServiceUrl;
    }

    @GetMapping("/products")
    public ResponseEntity<String> getProducts(HttpServletRequest request) {
        String requestId = (String) request.getAttribute(RequestIdFilter.REQUEST_ID_HEADER);
        HttpHeaders headers = createHeaders(requestId);

        try {
            return restTemplate.exchange(
                    orderServiceUrl + "/orders/products",
                    org.springframework.http.HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );
        } catch (HttpStatusCodeException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }

    @GetMapping("/products/search")
    public ResponseEntity<String> searchProducts(@RequestParam(value = "q", defaultValue = "") String query,
                                                 HttpServletRequest request) {
        String requestId = (String) request.getAttribute(RequestIdFilter.REQUEST_ID_HEADER);
        HttpHeaders headers = createHeaders(requestId);

        try {
            return restTemplate.exchange(
                    orderServiceUrl + "/orders/products/search?q=" + query,
                    org.springframework.http.HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );
        } catch (HttpStatusCodeException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }

    @GetMapping("/orders")
    public ResponseEntity<String> getOrders(HttpServletRequest request) {
        String requestId = (String) request.getAttribute(RequestIdFilter.REQUEST_ID_HEADER);
        HttpHeaders headers = createHeaders(requestId);

        try {
            return restTemplate.exchange(
                    orderServiceUrl + "/orders",
                    org.springframework.http.HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );
        } catch (HttpStatusCodeException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<String> getOrderById(@PathVariable Long id, HttpServletRequest request) {
        String requestId = (String) request.getAttribute(RequestIdFilter.REQUEST_ID_HEADER);
        HttpHeaders headers = createHeaders(requestId);

        try {
            return restTemplate.exchange(
                    orderServiceUrl + "/orders/" + id,
                    org.springframework.http.HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );
        } catch (HttpStatusCodeException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }

    @PostMapping("/checkout")
    public ResponseEntity<String> checkout(@RequestBody CheckoutRequest request,
                                           HttpServletRequest httpServletRequest) {
        String requestId = (String) httpServletRequest.getAttribute(RequestIdFilter.REQUEST_ID_HEADER);

        logWriter.log("api-gateway", "request_received", requestId, Map.of(
                "method", "POST",
                "path", "/checkout",
                "order_id", request.orderId() != null ? request.orderId() : 0L
        ));

        HttpHeaders headers = createHeaders(requestId);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    orderServiceUrl + "/orders/checkout",
                    new HttpEntity<>(request, headers),
                    String.class
            );

            logWriter.log("api-gateway", "request_succeeded", requestId, Map.of(
                    "status_code", response.getStatusCode().value(),
                    "message", "Checkout completed"
            ));

            return ResponseEntity.status(response.getStatusCode())
                    .header(RequestIdFilter.REQUEST_ID_HEADER, requestId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response.getBody());
        } catch (HttpStatusCodeException ex) {
            logWriter.log("api-gateway", "request_failed", requestId, Map.of(
                    "status_code", ex.getStatusCode().value(),
                    "error_code", "HTTP_500",
                    "message", "Gateway observed downstream failure"
            ));

            String body = ex.getResponseBodyAsString();
            if (body != null && !body.isBlank()) {
                return ResponseEntity.status(ex.getStatusCode())
                        .header(RequestIdFilter.REQUEST_ID_HEADER, requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body);
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header(RequestIdFilter.REQUEST_ID_HEADER, requestId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"status\":\"FAILED\",\"errorCode\":\"HTTP_500\",\"message\":\"Downstream service failed\"}");
        }
    }

    private HttpHeaders createHeaders(String requestId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (requestId != null) {
            headers.set(RequestIdFilter.REQUEST_ID_HEADER, requestId);
        }
        return headers;
    }
}
