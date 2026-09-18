# Data Consistency

## Strong consistency
- Inventory reservation: inventory changes and reservation records happen within the database transaction and are protected by a row lock plus JPA versioning.
- Idempotency uniqueness: PostgreSQL enforces uniqueness on the idempotency key.
- Order state transitions: updates are done in the same transaction as validation.
- Payment state changes: payment and order transitions are committed before their follow-up event is added to the outbox.
- Tenant isolation: all access is filtered by tenant-scoped identifiers.

## Eventual consistency
- Analytics: derived metrics are updated asynchronously from Kafka.
- Notifications: user-facing notification delivery may be delayed.
- Dashboards: read models may lag behind source-of-truth state.

### Why this model?
The system keeps critical business decisions in PostgreSQL because correctness is more important than throughput for order creation and stock reservation. Derived systems can tolerate delay since they are not the source of truth.

### User-visible impact
A customer may see a slightly delayed analytics or notification update, but order and inventory correctness remain intact.

### Inconsistency repair
Replay, outbox publication, and consumer idempotence provide a path to recover from missed or duplicated events.

### Delivery semantics
Kafka delivery is at-least-once. Each consumer stores `(event_id, consumer_name)` transactionally with its business effect, giving effectively-once behavior for the implemented side effects. The outbox is at-least-once until Kafka acknowledgement is recorded.

### Failure behavior
When payment fails, the order saga releases only still-reserved inventory records and transitions the order to `CANCELLED`. Redis is an acceleration layer and catalog reads fall back to PostgreSQL when it is unavailable.
