package com.codeguardian.orderservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderCheckoutIntegrationTest {

    private ProductRepository productRepository;
    private OrderRepository orderRepository;
    private OrderItemRepository orderItemRepository;
    private RestTemplate restTemplate;
    private StructuredLogWriter logWriter;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        productRepository = Mockito.mock(ProductRepository.class);
        orderRepository = Mockito.mock(OrderRepository.class);
        orderItemRepository = Mockito.mock(OrderItemRepository.class);
        restTemplate = Mockito.mock(RestTemplate.class);
        logWriter = Mockito.mock(StructuredLogWriter.class);

        when(orderRepository.findByOrderNumber(any())).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderCheckoutController controller = new OrderCheckoutController(
                productRepository,
                orderRepository,
                orderItemRepository,
                restTemplate,
                logWriter,
                "http://localhost:8082"
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void checkoutSucceedsWhenPaymentServiceReturnsSuccess() throws Exception {
        String successPayload = "{\"status\":\"SUCCESS\",\"message\":\"Payment processed\",\"errorCode\":null}";
        when(restTemplate.postForEntity(eq("http://localhost:8082/payments/charge"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(successPayload));

        mockMvc.perform(post("/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Request-ID", "req-test-success-5002")
                        .content("""
                                {"userId":101,"orderId":5002,"amount":149.0,"merchantCode":"MCH-5002"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void checkoutFailsWhenPaymentServiceThrows500() throws Exception {
        String errorPayload = """
                {"timestamp":"2026-08-28T12:00:00Z","requestId":"req-test-5001","status":500,"errorCode":"NULL_OBJECT_ACCESS","message":"Payment processing failed","service":"payment-service","path":"/payments/charge","exception":"NullPointerException","source":{"file":"PaymentService.java","line":24}}
                """;

        HttpServerErrorException exception = HttpServerErrorException.create(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                null,
                errorPayload.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );

        when(restTemplate.postForEntity(eq("http://localhost:8082/payments/charge"), any(HttpEntity.class), eq(String.class)))
                .thenThrow(exception);

        mockMvc.perform(post("/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Request-ID", "req-test-5001")
                        .content("""
                                {"userId":101,"orderId":5001,"amount":499.0,"merchantCode":"MCH-UNKNOWN"}
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("NULL_OBJECT_ACCESS"))
                .andExpect(jsonPath("$.source.file").value("PaymentService.java"));
    }
}
