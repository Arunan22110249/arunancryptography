# k6 load tests

The repository includes k6 scenarios for normal traffic, contention, and duplicate requests. Real benchmark results are intentionally kept out of the repo until the system is running and the tests are executed.

## Run
```bash
k6 run scenario-contention.js
```

## Results
Generated benchmark reports live under `load-tests/results/` and are only committed after measured execution.
