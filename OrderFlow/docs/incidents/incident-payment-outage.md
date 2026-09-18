# Incident: Payment service outage

## Timeline
- T0: Payment service starts returning timeouts.
- T1: Order creation still begins, but payment stage stalls.
- T2: Inventory remains reserved until compensation runs.

## Symptoms
- Payment failures increase.
- Orders remain in payment-pending state.

## Root cause
- Deterministic payment simulator is configured to fail for a given window.

## Detection
- Application logs and payment metric spikes.

## Mitigation
- Trigger saga compensation and cancel order.

## Recovery
- Restore the payment simulator and clear the blocked state.

## Prevention
- Add circuit breaker and retry classification.
