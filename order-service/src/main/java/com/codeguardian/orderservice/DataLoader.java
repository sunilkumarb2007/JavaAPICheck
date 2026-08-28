package com.codeguardian.orderservice;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public DataLoader(ProductRepository productRepository, OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    public void run(String... args) {
        if (productRepository.count() == 0) {
            Product p1 = productRepository.save(new Product(
                    "PROD-001",
                    "CodeGuardian Pro Enterprise",
                    "Autonomous failure detection, GhostTrace causality engine, and deterministic patch synthesizer for modern microservices.",
                    499.00,
                    "Security",
                    "https://images.unsplash.com/photo-1550751827-4bd374c3f58b?w=600&auto=format&fit=crop&q=80",
                    50
            ));

            Product p2 = productRepository.save(new Product(
                    "PROD-002",
                    "GhostTrace Debugger Suite",
                    "Real-time causal graph reconstruction with zero-overhead distributed log capture and execution replay.",
                    149.00,
                    "DevTools",
                    "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=600&auto=format&fit=crop&q=80",
                    100
            ));

            Product p3 = productRepository.save(new Product(
                    "PROD-003",
                    "SecOps Threat Immunizer",
                    "Continuous memory-driven vulnerability scanner with automated CI/CD gating and patch certification.",
                    299.00,
                    "Security",
                    "https://images.unsplash.com/photo-1563986768609-322da13575f3?w=600&auto=format&fit=crop&q=80",
                    75
            ));

            Product p4 = productRepository.save(new Product(
                    "PROD-004",
                    "Cloud Sentinel Watchdog",
                    "High-fidelity telemetry probe and synthetic incident replay agent with automated rollback capabilities.",
                    199.00,
                    "Monitoring",
                    "https://images.unsplash.com/photo-1518770660439-4636190af475?w=600&auto=format&fit=crop&q=80",
                    60
            ));
        }

        if (orderRepository.count() == 0) {
            Order o1 = new Order(101L, "ORD-5001", "PENDING", 499.00, "MCH-UNKNOWN");
            o1.addItem(new OrderItem(1L, "CodeGuardian Pro Enterprise", 499.00, 1));
            orderRepository.save(o1);

            Order o2 = new Order(101L, "ORD-5002", "CONFIRMED", 149.00, "MCH-5002");
            o2.addItem(new OrderItem(2L, "GhostTrace Debugger Suite", 149.00, 1));
            orderRepository.save(o2);

            Order o3 = new Order(102L, "ORD-5003", "CONFIRMED", 299.00, "MCH-5003");
            o3.addItem(new OrderItem(3L, "SecOps Threat Immunizer", 299.00, 1));
            orderRepository.save(o3);
        }
    }
}
