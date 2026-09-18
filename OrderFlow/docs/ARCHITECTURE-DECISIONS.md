# Architecture Decision Records

## ADR 1: Java vs Go
**Context**: The platform requires strong typing, Spring ecosystem, and broad enterprise operational tooling.

**Decision**: Use Java 17 and Spring Boot 3.

**Alternatives**: Go, Node.js.

**Why**: Java offers mature transaction APIs, JPA, Kafka, security, and observability integration relevant to the project.

**Trade-offs**: Higher runtime footprint than Go, but better fit for enterprise distributed systems prototype.

## ADR 2: PostgreSQL
**Context**: Strong consistency and transaction correctness are required for order and inventory state.

**Decision**: Use PostgreSQL as the source of truth.

**Alternatives**: MySQL, NoSQL.

**Why**: PostgreSQL has support for transactions, row locking, JSONB, and robust operations needed by an inventory correctness system.

## ADR 3: Kafka
**Context**: Event-driven processing and downstream decoupling are required.

**Decision**: Use Kafka for durable event transport.

**Alternatives**: RabbitMQ, direct REST integration.

**Why**: Kafka excels in event replay, partitioning, and consumer scale-out.

## ADR 4: Redis
**Context**: We need read-heavy cache and rate limiting without making Redis authoritative for inventory.

**Decision**: Use Redis only as a cache and rate-limiter backend.

**Trade-offs**: Redis is fast but not the truth source for inventory.

## ADR 5: Optimistic locking
**Context**: High concurrency on inventory reservation can cause oversell if not carefully handled.

**Decision**: Use optimistic locking with version numbers on inventory records.

**Why**: It reduces lock contention and suits the high-read, moderate-write workload.

## ADR 6: Transactional outbox
**Context**: Database commit and Kafka publish must be consistent.

**Decision**: Use a database outbox to publish events after commit.

**Why**: Prevents the dangerous dual-write problem.

## ADR 7: Saga
**Context**: Order processing spans several systems and may fail independently.

**Decision**: Use a saga pattern with compensating actions.

## ADR 8: K3s
**Context**: A lightweight Kubernetes distribution is needed for Oracle Always Free.

**Decision**: Use K3s.

## ADR 9: Docker Compose
**Context**: Local development must be easy and reproducible.

**Decision**: Keep Docker Compose as the primary local environment.

## ADR 10: GitHub Pages
**Context**: The frontend must be publicly accessible without a paid backend.

**Decision**: Host the Vite frontend via GitHub Pages with demo mode fallback.

## ADR 11: Oracle Always Free
**Context**: Zero-cost deployment target.

**Decision**: Design around Oracle Always Free VM plus K3s, ARM64-compatible images, and minimal resource usage.

## ADR 12: Modular architecture
**Context**: Local development should be simpler than full microservice overhead.

**Decision**: Use a modular monolith with clear domain boundaries.

## ADR 13: Cache-aside
**Context**: Product catalog read workloads are high but not always required.

**Decision**: Use cache-aside with invalidation on updates.

## ADR 14: JWT
**Context**: Need lightweight stateless security and role-based access.

**Decision**: Use JWT with role claims and tenant IDs.

## ADR 15: Multi-tenancy
**Context**: The system must support tenant isolation without leaking data.

**Decision**: Every major entity includes organization_id or tenant_id and access checks enforce correct scopes.
