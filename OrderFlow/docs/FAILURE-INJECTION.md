# Failure Injection

Failure flags are available only when `FAILURE_INJECTION_ENABLED=true` and through the authenticated admin/operator API. This document does not fabricate runtime measurements.

| Scenario | Expected Behavior | Observed Behavior | Recovery Mechanism | Data Consistency Impact | User Impact | Metrics | Lessons Learned |
|---|---|---|---|---|---|---|---|
| Payment failure | Compensate inventory and cancel order | Not executed in this pass | Saga compensation | No overselling | User sees order cancellation | `payment.failed`, `order.cancelled` | Pending |
| Inventory unavailable | Reject order and preserve stock integrity | Pending | Validation + retry | None | User sees order rejected | Pending | Pending |
| Kafka delay | Consumer lag is visible | Pending | Retry + DLQ investigation | Eventual consistency | UI may lag | Pending | Pending |
| Duplicate event | No duplicate side effects | Not executed in this pass | Per-consumer processed event table | No duplicate processing | No change | Pending | Pending |

Supported API:

- `POST /api/v1/admin/failures` with `{ "type": "payment_failure", "active": true }`
- `GET /api/v1/admin/failures`
- `DELETE /api/v1/admin/failures/{type}`

Supported payment flags are `payment_failure` and `payment_timeout`. The API never executes arbitrary commands.
