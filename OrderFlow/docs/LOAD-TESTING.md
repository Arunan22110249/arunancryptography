# Load Testing

This repository preserves a load-test harness and result structure without fabricating performance data. Before actual execution, the benchmark remains marked as pending.

## Scenarios
- Normal traffic
- High concurrency order creation
- Inventory contention
- Duplicate requests
- Downstream payment failures
- Kafka delay

## Mandatory benchmark
- 10,000 concurrent purchase attempts
- 100 units inventory
- Correctness assertion: overselling == 0

## Commands
```bash
cd load-tests/k6
k6 run scenario-normal.js
k6 run scenario-contention.js
```

## Reporting
Generated reports are stored under `load-tests/results/` and must not contain invented numbers.
