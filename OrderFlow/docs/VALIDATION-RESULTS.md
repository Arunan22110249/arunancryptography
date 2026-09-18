# Validation Results

Date: 2026-09-18

| Test | Command | Result | Notes |
|---|---|---|---|
| Backend clean tests | `C:\\tools\\apache-maven-3.9.9\\bin\\mvn.cmd clean test -q` | PASS | Local H2 profile; authentication, tenant product isolation, idempotency replay, schema/entity startup. |
| Frontend production build | `npm run build` in `frontend` | PASS | Vite build completed; only the existing chunk-size warning remains. |
| Compose interpolation/config | `docker compose config` with temporary `JWT_SECRET` | PASS | Production profile, Kafka internal listener, explicit secret requirement. |
| Kubernetes client validation | `kubectl apply --dry-run=client --validate=false ...` | BLOCKED | No reachable Kubernetes API server; kubectl attempted discovery even with client dry-run. |
| Docker image build | `docker compose build` | BLOCKED | Docker Desktop Linux engine was not running (`dockerDesktopLinuxEngine` pipe unavailable). |
| Terraform validation | `terraform init -backend=false -input=false; terraform validate` | BLOCKED | Terraform executable is not installed in the environment. |

No load-test graphs, failure-injection measurements, Kafka recovery measurements, or Redis recovery measurements are reported here because those environments were not actually run during this validation pass.
