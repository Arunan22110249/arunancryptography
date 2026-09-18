# Failure Lab

The failure lab provides a controlled, auditable set of development and demo failure scenarios. It does not execute arbitrary shell commands from the UI and is intended for local or demo-only scenarios.

## Available scenarios
- Payment failure
- Inventory unavailable
- Kafka consumer pause
- Kafka processing delay
- Database latency
- Downstream timeout
- Duplicate event
- Malformed event

## Safety notes
- All failures are whitelisted.
- They are reversible and development-controlled.
- They are intended for debugging and demonstration, not arbitrary host execution.
