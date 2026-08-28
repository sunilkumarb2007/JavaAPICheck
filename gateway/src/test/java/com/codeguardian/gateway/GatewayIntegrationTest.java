package com.codeguardian.gateway;

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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GatewayIntegrationTest {

    private RestTemplate restTemplate;
    private StructuredLogWriter logWriter;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        restTemplate = Mockito.mock(RestTemplate.class);
        logWriter = Mockito.mock(StructuredLogWriter.class);
        CheckoutController controller = new CheckoutController(restTemplate, logWriter, "http://localhost:8081");

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addFilters(new RequestIdFilter())
                .build();
    }

    @Test
    void checkoutProxiesSuccessfulResponse() throws Exception {
        String orderSuccessResponse = "{\"status\":\"SUCCESS\",\"message\":\"Payment completed\"}";
        when(restTemplate.postForEntity(eq("http://localhost:8081/orders/checkout"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(orderSuccessResponse));

        mockMvc.perform(post("/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Request-ID", "req-gw-success-5002")
                        .content("""
                                {"userId":101,"orderId":5002,"amount":149.0,"merchantCode":"MCH-5002"}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-ID", "req-gw-success-5002"))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void checkoutProxies500DownstreamFailureWithDiagnostics() throws Exception {
        String errorPayload = """
                {"timestamp":"2026-08-28T12:00:00Z","requestId":"req-gw-test-5001","status":500,"errorCode":"NULL_OBJECT_ACCESS","message":"Payment processing failed","service":"payment-service","path":"/payments/charge","exception":"NullPointerException","source":{"file":"PaymentService.java","line":24}}
                """;

        HttpServerErrorException exception = HttpServerErrorException.create(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                null,
                errorPayload.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );

        when(restTemplate.postForEntity(eq("http://localhost:8081/orders/checkout"), any(HttpEntity.class), eq(String.class)))
                .thenThrow(exception);

        mockMvc.perform(post("/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Request-ID", "req-gw-test-5001")
                        .content("""
                                {"userId":101,"orderId":5001,"amount":499.0,"merchantCode":"MCH-UNKNOWN"}
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(header().string("X-Request-ID", "req-gw-test-5001"))
                .andExpect(jsonPath("$.errorCode").value("NULL_OBJECT_ACCESS"))
                .andExpect(jsonPath("$.source.file").value("PaymentService.java"));
    }
}
