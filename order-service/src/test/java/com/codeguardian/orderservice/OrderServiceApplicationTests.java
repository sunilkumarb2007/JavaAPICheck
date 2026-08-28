package com.codeguardian.orderservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderServiceApplicationTests {

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
    void healthEndpointReturnsUp() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("order-service"));
    }

    @Test
    void productsEndpointReturnsProductList() throws Exception {
        Product p = new Product("PROD-001", "CodeGuardian Pro Enterprise", "Security tool", 499.0, "Security", "img", 50);
        when(productRepository.findAll()).thenReturn(List.of(p));

        mockMvc.perform(get("/orders/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].code").value("PROD-001"));
    }

    @Test
    void searchEndpointFiltersProducts() throws Exception {
        Product p = new Product("PROD-002", "GhostTrace Debugger Suite", "Debugger tool", 149.0, "DevTools", "img", 100);
        when(productRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase("GhostTrace", "GhostTrace"))
                .thenReturn(List.of(p));

        mockMvc.perform(get("/orders/products/search").param("q", "GhostTrace"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("GhostTrace Debugger Suite"));
    }
}
