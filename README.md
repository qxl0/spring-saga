# Spring Saga — Event-Driven Microservices

A Spring Boot microservices demo implementing the **Saga pattern** with **CQRS** and **Event Sourcing** using [Axon Framework](https://www.axoniq.io/).

---

## Architecture

```
Client
  │
  └─► OrderService (port 9091)          ← Saga Orchestrator
           │  via Axon Server
           ├─► UserService (port 9092)   ← Query: payment details
           ├─► PaymentService (port 9093) ← Validate / cancel payment
           └─► ShipmentService (port 9094) ← Ship order

ProductService (port 8081)              ← Independent CQRS service
CommonService                           ← Shared library (commands, events, models)
Axon Server                             ← Event store & message broker
```

All inter-service communication flows through **Axon Server** as commands and events — no direct HTTP calls between services.

---

## Services

| Service | Port | Role |
|---------|------|------|
| **CommonService** | — | Shared library: commands, events, domain models |
| **OrderService** | 9091 | Creates orders, orchestrates the saga |
| **ProductService** | 8081 | Product catalog (CQRS: command + query sides) |
| **PaymentService** | 9093 | Validates and processes payments |
| **ShipmentService** | 9094 | Handles order shipment |
| **UserService** | 9092 | Serves user payment details (query model) |

---

## Saga Flow

The `OrderProcessingSaga` in **OrderService** orchestrates the full order lifecycle with compensating transactions on failure.

### Happy Path

```
POST /orders
  └─► CreateOrderCommand
        └─► OrderAggregate → OrderCreatedEvent
              └─► [Saga starts]
                    └─► GetUserPaymentDetailsQuery → UserService
                          └─► ValidatePaymentCommand → PaymentService
                                └─► PaymentAggregate → PaymentProcessedEvent
                                      └─► ShipOrderCommand → ShipmentService
                                            └─► ShipmentAggregate → OrderShippedEvent
                                                  └─► CompleteOrderCommand
                                                        └─► OrderAggregate → OrderCompletedEvent
                                                              └─► [Saga ends] ✓
```

### Compensation (Failure Path)

```
If user query fails:
  └─► CancelOrderCommand → OrderCancelledEvent → [Saga ends] ✗

If shipment fails:
  └─► CancelPaymentCommand → PaymentCancelledEvent
        └─► CancelOrderCommand → OrderCancelledEvent → [Saga ends] ✗
```

---

## REST Endpoints

### OrderService — `http://localhost:9091`

| Method | Path | Body | Description |
|--------|------|------|-------------|
| `POST` | `/orders` | `{ productId, userId, addressId, quantity }` | Create a new order |

### ProductService — `http://localhost:8081`

| Method | Path | Body | Description |
|--------|------|------|-------------|
| `POST` | `/products` | `{ name, price, quantity }` | Create a product |
| `GET` | `/products` | — | List all products |

### UserService — `http://localhost:9092`

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/users/{userId}` | Get user payment details |

> PaymentService and ShipmentService have no REST endpoints — they respond only to Axon commands.

---

## Domain Model

### Commands (CommonService)

| Command | Target | Purpose |
|---------|--------|---------|
| `ValidatePaymentCommand` | PaymentAggregate | Initiate payment processing |
| `ShipOrderCommand` | ShipmentAggregate | Initiate shipment |
| `CompleteOrderCommand` | OrderAggregate | Mark order as APPROVED |
| `CancelOrderCommand` | OrderAggregate | Cancel order (compensation) |
| `CancelPaymentCommand` | PaymentAggregate | Cancel payment (compensation) |

### Events (CommonService)

| Event | Publisher | Meaning |
|-------|-----------|---------|
| `PaymentProcessedEvent` | PaymentAggregate | Payment validated successfully |
| `OrderShippedEvent` | ShipmentAggregate | Shipment completed |
| `OrderCompletedEvent` | OrderAggregate | Order fully complete (APPROVED) |
| `OrderCancelledEvent` | OrderAggregate | Order cancelled |
| `PaymentCancelledEvent` | PaymentAggregate | Payment reversed (compensation) |

---

## Tech Stack

| Technology | Version | Purpose |
|-----------|---------|---------|
| Java | 21 | Language |
| Spring Boot | 3.2.5 | Application framework |
| Axon Framework | 4.9.3 | CQRS, Event Sourcing, Saga |
| Axon Server | 2025.2.6 | Event store & message broker |
| H2 | embedded | Per-service database |
| Lombok | 1.18.36 | Boilerplate reduction |
| Gradle | 8.7 | Build tool |

---

## Infrastructure

### Axon Server (Docker)

```bash
cd Axon-Server
docker compose up -d
```

| Port | Purpose |
|------|---------|
| `8024` | gRPC (command/event bus) |
| `8124` | HTTP dashboard |
| `8224` | Metrics |

Axon Server persists events using named Docker volumes (`axonserver-events`, `axonserver-log`, `axonserver-data`).

### Databases (H2)

Each service uses a file-based H2 database. H2 console is available at `http://localhost:{port}/h2-console`.

| Service | JDBC URL | Credentials |
|---------|----------|-------------|
| OrderService | `jdbc:h2:file:~/data/orderDB` | sa / password |
| ProductService | `jdbc:h2:file:~/data/productDB` | sa / password |
| PaymentService | `jdbc:h2:file:~/data/paymentDB` | sa / password |
| ShipmentService | `jdbc:h2:file:~/data/shipmentDB` | sa / password |

---

## Build & Run

### Prerequisites

- Java 21
- Docker (for Axon Server)

### Start Axon Server

```bash
cd Axon-Server
docker compose up -d
```

### Build All Services

```bash
./gradlew build
```

### Run a Service

```bash
./gradlew :OrderService:bootRun
./gradlew :ProductService:bootRun
./gradlew :PaymentService:bootRun
./gradlew :ShipmentService:bootRun
./gradlew :UserService:bootRun
```

### Useful Gradle Commands

```bash
./gradlew clean build                   # full clean rebuild
./gradlew :OrderService:bootJar         # package a single service
./gradlew dependencies                  # inspect dependency tree
```

---

## Project Structure

```
spring-saga/
├── settings.gradle                     # multi-project root
├── build.gradle                        # shared config (Java 21, BOM, Lombok)
├── gradle/
│   └── libs.versions.toml              # version catalog
├── CommonService/                      # shared library
│   └── src/main/java/com/mtbank/CommonService/
│       ├── commands/                   # Axon command objects
│       ├── events/                     # Axon event objects
│       ├── model/                      # shared domain models
│       └── queries/                    # query objects
├── OrderService/                       # saga orchestrator
├── ProductService/                     # CQRS product service
├── PaymentService/                     # payment processing
├── ShipmentService/                    # shipment processing
├── UserService/                        # user query model
└── Axon-Server/                        # docker-compose for Axon Server
```

---

## Architectural Patterns

- **Saga Pattern** — `OrderProcessingSaga` orchestrates a distributed transaction across 4 services with automatic compensation on failure
- **CQRS** — Command and query responsibilities are separated (e.g. `ProductCommandController` vs `ProductQueryController`)
- **Event Sourcing** — All state changes are persisted as immutable events in Axon Server
- **Aggregate** — Each service owns an Axon aggregate that enforces domain invariants
- **Event Handler** — Services project events to their local H2 read models
