# Engineering Blog

## 1. Why CRUD was insufficient
A CRUD application can create orders and mutate prices, but it cannot enforce stock correctness when many requests compete for the same inventory. The real story is not about tables; it is about transaction boundaries and compensation.

## 2. Why inventory requires concurrency control
Inventory reservation is a classic race condition. If two purchases both read `available_quantity = 1`, both may succeed even though only one should be allowed. The correct response is not a database workaround; it is a well-defined inventory transaction with version checks and invariants.

## 3. The dual-write problem
When a database update and a Kafka publish are not coupled, the system can commit to the database while failing to publish the event. The outbox solves this by recording the event in the same transaction that updates order state.

## 4. Why transactional outbox
Outbox ensures that the commit to PostgreSQL and the publish to Kafka become a single durable outcome from an operational perspective. It is the practical fix for the dual-write dilemma.

## 5. Why Saga
The business flow crosses multiple systems: inventory, payment, notification, and audit. Each may fail independently. The saga pattern records the state and triggers compensation rather than pretending all operations are atomic.

## 6. Why eventual consistency
Analytics, dashboards, and notifications do not need to be strongly consistent to provide user value. Their eventual consistency is cheaper and more resilient than making them critical path dependencies for order creation.

## 7. Kafka ordering
Kafka ordering is only guaranteed per partition, not globally across the cluster. This matters when ordering by aggregate is required: choose the same key for events from the same aggregate to preserve local sequencing.

## 8. Idempotency
Customers may retry the same order request. Without a unique idempotency key, the system cannot distinguish a repeat request from a legitimate new order. PostgreSQL-backed idempotency is the canonical protection point.

## 9. Redis trade-offs
Redis is excellent for cache-aside and rate limiting, but misleading to treat as the source of truth for inventory. A cache can be stale or evicted, which is fine for product reads but not for stock correctness.

## 10. Failure handling
Failures are not edge cases; they are part of the operating environment. A healthy design uses retries, DLQ handling, and circuit breakers to avoid cascading outages.

## 11. Kubernetes deployment
Kubernetes introduces clear operational boundaries and health probes. Even a small free-tier deployment benefits from readiness and liveness checks, environment configuration, and controlled upgrades.

## 12. Zero-cost cloud architecture
A single free VM cannot match the reliability of a more expensive production topology, and our design should be honest about those limits. The purpose is to demonstrate correctness and engineering judgment, not pretend that a free tier replaces a production-grade distributed system.
