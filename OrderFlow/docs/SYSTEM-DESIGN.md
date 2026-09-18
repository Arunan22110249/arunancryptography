# System Design

## 1. Problem
The core business problem is maintaining inventory correctness and reliable order processing when many customers attempt to buy limited stock while payment, inventory, and downstream systems may fail.

## 2. Goals
- Prevent overselling under concurrency.
- Guarantee idempotent order creation.
- Keep order processing observable and recoverable.
- Support local Docker Compose and cloud deployment on ARM64 resource-constrained infrastructure.

## 3. Non-goals
- Real external payment gateways.
- Full multi-region failover in the free tier.
- Dedicated search engine.

## 4. Functional requirements
- Catalog CRUD with validation.
- Inventory reservation with optimistic locking.
- Order state transitions with explicit validation.
- Payment simulation and failure handling.
- Kafka-driven event propagation.
- Audit, analytics, and notifications.

## 5. Non-functional requirements
- Correctness under high concurrency.
- Safe retry and DLQ handling.
- Observability with tracing and metrics.
- Security via JWT and tenant checks.

## 6. Architecture
The system is a modular monolith with domain service boundaries, backed by PostgreSQL, Redis, and Kafka. Components are designed to be deployable independently, but the repository keeps them in a single codebase for local development and zero-cost demonstration.

## 7. Service boundaries
- Catalog: product metadata.
- Inventory: stock and reservation state.
- Order: order lifecycle and idempotency.
- Payment: deterministic payment simulator.
- Notification: consumer-driven notifications.
- Analytics: derived metrics and dashboards.
- Audit: tamper-aware operational logs.

## 8. API design
- Prefix: /api/v1
- Error envelope includes timestamp, status, code, message, traceId.
- JWT authentication and tenant checks protect resources.

## 9. Database
PostgreSQL is the source of truth for orders, inventory, and idempotency. A transactional outbox keeps the database and Kafka consistent.

## 10. Kafka
Kafka handles order, inventory, payment, and notification events. Ordering is partition-local and keys are chosen by aggregate ID where ordering matters.

## 11. Outbox
A database-backed outbox inserts an event in the same transaction as an order mutation and a dedicated publisher pushes pending records to Kafka.

## 12. Saga
Order creation uses a saga pattern: reserve inventory, authorize payment, confirm order, notify downstream consumers. Failures trigger compensation.

## 13. Caching
Redis provides read-through caching for product catalog data and rate limiting. Inventory remains authoritative in PostgreSQL.

## 14. Consistency
The project intentionally mixes strong consistency and eventual consistency. Inventory, order state, and idempotency use strong consistency; analytics, notifications, and dashboards are eventually consistent.

## 15. Reliability
Retries, circuit breakers, DLQ, health checks, and graceful shutdown support operations in failure scenarios.

## 16. Security
JWT-based access control, tenant isolation, and environment-configured secrets allow development without hardcoded credentials.

## 17. Observability
Structured logs, traces, and Prometheus metrics are collected via OpenTelemetry and exposed to Grafana.

## 18. Scaling
The architecture can scale by adding API replicas, Kafka partitions, read replicas, and cache layers. The free deployment remains conservative and intentionally resource-aware.

## 19. Failure modes
Examples include payment outage, consumer lag, duplicate events, inventory contention, and Redis downtime.

## 20. Disaster recovery
Uses PostgreSQL backups, stateless service restart, and documented RPO/RTO limits appropriate to a single free Oracle VM. It is not a multi-region enterprise deployment.

## 21. Trade-offs
This architecture intentionally avoids per-service microservice overhead while still modeling clear domain boundaries.

## 22. Future improvements
- Split services into deployments after product maturity.
- Add stronger event schema governance and registry.
- Introduce multi-zone or multi-region resilience.
