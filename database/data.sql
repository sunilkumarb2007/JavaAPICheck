-- Seed Data for JavaAPICheck

-- Seed Products
INSERT INTO products (code, name, description, price, category, image_url, stock) VALUES
('PROD-001', 'CodeGuardian Pro Enterprise', 'Autonomous failure detection, GhostTrace causality engine, and deterministic patch synthesizer for modern microservices.', 499.00, 'Security', 'https://images.unsplash.com/photo-1550751827-4bd374c3f58b?w=600&auto=format&fit=crop&q=80', 50),
('PROD-002', 'GhostTrace Debugger Suite', 'Real-time causal graph reconstruction with zero-overhead distributed log capture and execution replay.', 149.00, 'DevTools', 'https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=600&auto=format&fit=crop&q=80', 100),
('PROD-003', 'SecOps Threat Immunizer', 'Continuous memory-driven vulnerability scanner with automated CI/CD gating and patch certification.', 299.00, 'Security', 'https://images.unsplash.com/photo-1563986768609-322da13575f3?w=600&auto=format&fit=crop&q=80', 75),
('PROD-004', 'Cloud Sentinel Watchdog', 'High-fidelity telemetry probe and synthetic incident replay agent with automated rollback capabilities.', 199.00, 'Monitoring', 'https://images.unsplash.com/photo-1518770660439-4636190af475?w=600&auto=format&fit=crop&q=80', 60);

-- Seed Merchants
-- Note: MCH-5002 and MCH-5003 exist and are active.
-- Merchant for Order 5001 (MCH-UNKNOWN) is intentionally omitted from the database to trigger the deterministic lookup failure.
INSERT INTO merchants (merchant_code, name, active, api_key) VALUES
('MCH-5002', 'CodeGuardian Verified Merchant', TRUE, 'key_live_cg_verified_5002'),
('MCH-5003', 'Global SecOps Services Ltd', TRUE, 'key_live_secops_5003');

-- Seed Baseline Orders
INSERT INTO orders (id, user_id, order_number, status, total_amount, merchant_code) VALUES
(5001, 101, 'ORD-5001', 'PENDING', 499.00, 'MCH-UNKNOWN'),
(5002, 101, 'ORD-5002', 'CONFIRMED', 149.00, 'MCH-5002'),
(5003, 102, 'ORD-5003', 'CONFIRMED', 299.00, 'MCH-5003');

-- Seed Order Items
INSERT INTO order_items (order_id, product_id, product_name, unit_price, quantity) VALUES
(5001, 1, 'CodeGuardian Pro Enterprise', 499.00, 1),
(5002, 2, 'GhostTrace Debugger Suite', 149.00, 1),
(5003, 3, 'SecOps Threat Immunizer', 299.00, 1);
