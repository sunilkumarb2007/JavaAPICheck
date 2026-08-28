# JavaAPICheck — Official CodeGuardian Failure Lab

`JavaAPICheck` is a deterministic, production-grade Java Spring Boot microservice application and the **official live demonstration target** for CodeGuardian:

```
PUBLIC WEBSITE ➔ GATEWAY ➔ ORDER SERVICE ➔ PAYMENT SERVICE ➔ DATABASE ➔ INTENTIONAL FAILURE (HTTP 500)
    │
    ▼
CODEGUARDIAN REPAIR PIPELINE
    │
    ▼
INVESTIGATION ➔ ROOT CAUSE ➔ PATCH ➔ REPLAY ➔ BUILD ➔ TEST ➔ VALIDATION ➔ HUMAN APPROVAL ➔ PR ➔ MERGE ➔ REDEPLOY
    │
    ▼
SAME WEBSITE ➔ SAME USER ACTION ➔ FIXED BEHAVIOR (HTTP 200 / Controlled Response)
```

---

## 1. Architecture Overview

```
                          ┌────────────────────────┐
                          │   Frontend Commerce    │
                          │     (GitHub Pages)     │
                          └───────────┬────────────┘
                                      │ HTTP / REST (X-Request-ID, CORS)
                                      ▼
                          ┌────────────────────────┐
                          │      API Gateway       │  Port 8080
                          └───────────┬────────────┘
                                      │ Internal REST (X-Request-ID propagation)
                                      ▼
                          ┌────────────────────────┐
                          │     Order Service      │  Port 8081
                          └───────────┬────────────┘
                                      │ Internal REST (X-Request-ID propagation)
                                      ▼
                          ┌────────────────────────┐
                          │    Payment Service     │  Port 8082
                          └───────────┬────────────┘
                                      │
                                      ▼
                          ┌────────────────────────┐
                          │   JPA / H2 Database    │  products, orders, merchants, payments
                          └────────────────────────┘
```

### Microservices Breakdown:

| Service | Port | Responsibilities | Key Technologies |
| :--- | :--- | :--- | :--- |
| **`frontend`** | N/A (Static) | Modern SecOps store UI, search, cart, scenario runner, error modal, CodeGuardian deep linking | HTML5, Vanilla CSS, JS |
| **`gateway`** | `8080` | Public ingress, CORS headers, Request ID injection (`X-Request-ID`), reverse proxy routing | Spring Boot Web, Filter |
| **`order-service`** | `8081` | Product catalog, search, order lifecycle, items persistence, downstream payment forwarding | Spring Boot Web, Data JPA, H2 |
| **`payment-service`**| `8082` | Merchant validation, transaction token generation, structured error formatting, **Intentional Defect** | Spring Boot Web, Data JPA, H2 |

---

## 2. The Intentional Defect

The root cause defect is deterministic and located in **`payment-service/src/main/java/com/codeguardian/paymentservice/PaymentService.java`**:

```java
// Lookup merchant record by code
Merchant merchant = merchantRepository.findByMerchantCode(request.merchantCode());

// INTENTIONAL DEFECT: Unvalidated dereference of merchant entity.
// For unseeded/unknown merchants (such as Order 5001 with 'MCH-UNKNOWN'),
// findByMerchantCode returns null, triggering a NullPointerException at runtime.
if (!merchant.isActive()) {
    throw new IllegalStateException("Merchant is not active");
}
```

### Pre-Fix vs Post-Fix Behavior:

- **Baseline (`main` branch)**: `ORDER 5001` throws `NullPointerException` ➔ caught by `@ExceptionHandler` ➔ returns **HTTP 500** with `NULL_OBJECT_ACCESS` and source metadata (`PaymentService.java`).
- **Post-Fix (After CodeGuardian PR)**: Null check guards dereference ➔ returns controlled business response (HTTP 404 / 400 `MERCHANT_NOT_FOUND`), preventing server crash.
- **Healthy Scenarios (`ORDER 5002` / `ORDER 5003`)**: Always succeed with **HTTP 200** before and after the repair.

---

## 3. Structured Error Contract

All services adhere to the standard structured failure schema with correlation tracking:

```json
{
  "timestamp": "2026-08-28T12:48:02.264Z",
  "requestId": "req-test-bug-5001",
  "status": 500,
  "errorCode": "NULL_OBJECT_ACCESS",
  "message": "Payment processing failed because merchant data was unavailable",
  "service": "payment-service",
  "path": "/payments/charge",
  "exception": "NullPointerException",
  "source": {
    "file": "PaymentService.java",
    "line": 24
  }
}
```

---

## 4. API Endpoints

### Gateway (`:8080`)
- `GET /health` — Health status
- `GET /products` — Retrieve all catalog products
- `GET /products/search?q={query}` — Search products by keyword
- `GET /orders` — List stored orders
- `GET /orders/{id}` — Get order details
- `POST /checkout` — Execute checkout flow (proxies to Order Service)

### Order Service (`:8081`)
- `GET /health` — Health check
- `GET /orders/products` — Catalog products
- `GET /orders/products/search?q={query}` — Filtered products
- `GET /orders` — Order history
- `POST /orders/checkout` — Create pending order and forward charge to Payment Service

### Payment Service (`:8082`)
- `GET /health` — Health check
- `GET /merchants` — List active merchants
- `POST /payments/charge` — Process payment charge

---

## 5. Local Setup & Execution

### Prerequisites
- JDK 21+
- Docker & Docker Compose

### Run with Maven Wrapper
```bash
# Run all unit, integration, and regression test suites
./mvnw clean test

# Start individual microservices in separate terminals
./mvnw -pl payment-service spring-boot:run
./mvnw -pl order-service spring-boot:run
./mvnw -pl gateway spring-boot:run
```

### Run with Docker Compose
```bash
docker compose build
docker compose up -d
```

Verify services:
```bash
curl http://localhost:8080/health
curl http://localhost:8080/products
```

---

## 6. Reproducing Failure Scenarios

### Scenario 1: Intentional Failure (ORDER 5001)
```bash
curl -X POST http://localhost:8080/checkout \
  -H "Content-Type: application/json" \
  -H "X-Request-ID: req-demo-5001" \
  -d '{
    "userId": 101,
    "orderId": 5001,
    "amount": 499.0,
    "merchantCode": "MCH-UNKNOWN"
  }'
```
**Expected Response: HTTP 500**
```json
{
  "timestamp": "2026-08-28T12:48:02.264Z",
  "requestId": "req-demo-5001",
  "status": 500,
  "errorCode": "NULL_OBJECT_ACCESS",
  "message": "Payment processing failed because merchant data was unavailable",
  "service": "payment-service",
  "path": "/payments/charge",
  "exception": "NullPointerException",
  "source": {
    "file": "PaymentService.java",
    "line": 24
  }
}
```

### Scenario 2: Healthy Success (ORDER 5002)
```bash
curl -X POST http://localhost:8080/checkout \
  -H "Content-Type: application/json" \
  -H "X-Request-ID: req-demo-5002" \
  -d '{
    "userId": 101,
    "orderId": 5002,
    "amount": 149.0,
    "merchantCode": "MCH-5002"
  }'
```
**Expected Response: HTTP 200 OK**

---

## 7. Frontend Deployment (GitHub Pages)

The static frontend is located in `/frontend` and can be hosted directly on **GitHub Pages**:
1. Open `frontend/index.html` locally in any browser or deploy to GitHub Pages.
2. In the top right, click the **Settings ⚙️** icon to configure the public Gateway URL (e.g. `http://localhost:8080` or your public backend ingress).
3. CORS is enabled on all Gateway endpoints by default.

---

## 8. CodeGuardian Automated Repair Workflow

1. User clicks **"Execute Order 5001"** in the web store UI.
2. Gateway logs HTTP 500 error and displays the Failure Diagnosis Modal.
3. User clicks **"Investigate with CodeGuardian"**.
4. CodeGuardian analyzes logs, correlates `X-Request-ID`, and navigates to `PaymentService.java`.
5. CodeGuardian synthesizes patch adding null safety check.
6. CodeGuardian replays the failure, validates against `PaymentPatchRegressionTest`, compiles sandbox container, and opens a GitHub Pull Request.
7. Human reviewer merges PR and redeploys.
8. Retesting Order 5001 on the same website completes cleanly without server crash.

---

## License
MIT License. Copyright (c) 2026 CodeGuardian.
