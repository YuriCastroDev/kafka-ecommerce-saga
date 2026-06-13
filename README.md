# 🛒 Kafka E-commerce Saga

Event-driven e-commerce system built with **Java 21**, **Spring Boot 3** and **Apache Kafka** implementing the **Saga Choreography Pattern**.  
Each service reacts to events autonomously — no central orchestrator. Distributed consistency is achieved through compensating transactions when failures occur.

---

## 🏗️ Architecture

```
[POST /orders] → Order Service
                     │ saves Order + OutboxEvent (same transaction)
                     │ OutboxPublisher publishes to Kafka every 1s
                     ▼
              [Topic: order-events]
                     │
                     ▼
              Stock Service
                     │ reserves stock (idempotent)
                     ├── OK  → publishes StockReserved
                     └── FAIL → publishes StockFailed → saga CANCELLED
                     ▼
              [Topic: stock-events]
                     │
                     ▼
              Payment Service
                     │ processes payment (simulates 20% failure)
                     ├── OK  → publishes PaymentConfirmed
                     └── FAIL → publishes PaymentFailed
                     ▼
              [Topic: payment-events]
                     │
                     ├──► Stock Service (compensation)
                     │         └── PaymentFailed → releases reserved stock
                     │
                     ├──► Notification Service
                     │         └── notifies customer + persists log
                     │
                     └──► Order Service (saga tracker)
                               └── updates SagaTracker state
```

---

## 🛠️ Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 21 | Language |
| Spring Boot | 3.5 | Framework |
| Spring Kafka | 3.3 | Kafka integration |
| Spring Data JPA | - | Persistence |
| PostgreSQL | 16 | One database per service |
| Docker Compose | - | Local infrastructure |
| Kafdrop | latest | Kafka UI |
| JUnit 5 + Mockito | - | Unit tests |

---

## ▶️ Running Locally

### Prerequisites
- Docker Desktop
- Java 21 (Temurin recommended)
- Maven

### Steps

**1. Start infrastructure**
```bash
docker-compose up -d
```

This starts Kafka, Zookeeper, Kafdrop and **4 separate PostgreSQL databases** (one per service).

**2. Start each service in a separate terminal**
```bash
# Terminal 1
cd order-service; ..\mvnw spring-boot:run

# Terminal 2
cd stock-service; ..\mvnw spring-boot:run

# Terminal 3
cd payment-service; ..\mvnw spring-boot:run

# Terminal 4
cd notification-service; ..\mvnw spring-boot:run
```

**3. Create an order**
```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{"customerName":"John","productName":"Teclado","quantity":1,"price":150.00}'
```

**4. Check saga status**
```bash
curl http://localhost:8080/orders/{orderId}/saga-status
```

**5. Watch Kafka topics at http://localhost:9000**

---

## 🔄 Saga Flow

### Happy path (80% of requests)
```
PENDING → STOCK_RESERVED → PAYMENT_CONFIRMED
```

### Payment failure (20% simulated)
```
PENDING → STOCK_RESERVED → PAYMENT_FAILED → CANCELLED
(stock is automatically released via compensating transaction)
```

### Stock unavailable
```
PENDING → STOCK_FAILED → CANCELLED
```

---

## 💡 Key Concepts Demonstrated

| Concept | Where |
|---|---|
| Saga Choreography | Each service reacts to events independently |
| Outbox Pattern | `OutboxEvent` saved with order in same transaction; `OutboxPublisher` retries on failure |
| Compensating transaction | `StockService.releaseStock()` triggered by `PaymentFailed` |
| Idempotency | All consumers check if event was already processed before acting |
| Saga state tracking | `SagaTracker` in order-service updated by `SagaConsumer` |
| Separate databases | Each service has its own PostgreSQL — true microservice isolation |

---

## 🗂️ Project Structure

```
kafka-ecommerce-saga/
├── docker-compose.yml              # Kafka + 4 PostgreSQL databases
├── pom.xml                         # Parent Maven POM
├── order-service/                  # Port 8080
│   ├── entity/Order.java
│   ├── entity/OutboxEvent.java     # Outbox pattern
│   ├── outbox/OutboxPublisher.java # Polls outbox every 1s and publishes
│   ├── saga/SagaTracker.java       # Tracks saga state
│   ├── consumer/SagaConsumer.java  # Updates state from stock/payment events
│   └── controller/OrderController  # POST /orders, GET /orders/{id}/saga-status
├── stock-service/                  # Port 8081
│   ├── entity/StockItem.java
│   ├── entity/StockReservation.java
│   ├── service/StockService.java   # Reserve + release stock
│   └── consumer/StockConsumer.java # Reacts to order-events + payment-events
├── payment-service/                # Port 8082
│   ├── entity/Payment.java
│   ├── service/PaymentService.java # 20% failure simulation
│   └── consumer/PaymentConsumer.java
└── notification-service/           # Port 8083
    ├── entity/NotificationLog.java
    └── consumer/NotificationConsumer.java
```

---

## 🧪 Tests

| Test | Service | What it covers |
|---|---|---|
| `OrderServiceTest` | order-service | Creates order, saves outbox, initializes saga tracker |
| `SagaConsumerTest` | order-service | Stock/payment events update saga state correctly |
| `StockServiceTest` | stock-service | Reserve stock, stock failure, idempotency, quantity decrease |
| `PaymentServiceTest` | payment-service | Idempotency, persists payment, publishes CONFIRMED or FAILED |

---

## 📝 Interview talking points

- **Why Saga and not 2PC?** — 2PC locks resources and doesn't scale. Saga is async and resilient to partial failures.
- **What if Stock Service crashes during compensation?** — The `PaymentFailed` event stays in Kafka. When it restarts, it reprocesses. That's why idempotency is mandatory.
- **How do you guarantee the event is never lost?** — Outbox Pattern: the event and the order are saved in the same database transaction. If the service crashes after commit, the `OutboxPublisher` retries.
- **How do you know the current state of an order?** — `GET /orders/{id}/saga-status` returns the `SagaTracker` which is updated as events flow through the system.
