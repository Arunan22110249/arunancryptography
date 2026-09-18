# Runbook

## API unavailable
**Detection**: health check fails or requests time out.
**Diagnosis**: inspect logs, service readiness, and ingress.
**Mitigation**: restart the application container or service pod.
**Recovery**: confirm health checks pass after restart.
**Verification**: call /actuator/health and verify traffic resumes.
**Rollback**: revert to last-known-good image.

## PostgreSQL unavailable
**Detection**: DB connection error and failed actuator health.
**Diagnosis**: check container status and database logs.
**Mitigation**: restart PostgreSQL and restore from backup if needed.
**Recovery**: re-run Flyway migrations and validate app health.
**Verification**: run a smoke order-create request.
**Rollback**: use the latest backup snapshot.

## Kafka unavailable
**Detection**: publish failures and consumer lag.
**Diagnosis**: check Kafka broker status and network access.
**Mitigation**: restart Kafka broker and heal consumers.
**Recovery**: drain outbox backlog and replay pending events.
**Verification**: inspect Kafka topic creation and consumer groups.
**Rollback**: recover to previous configuration.

## Redis unavailable
**Detection**: rate limiting and cache misses increase.
**Diagnosis**: check Redis health and application logs.
**Mitigation**: fall back to read-through without cache, disable rate limit if necessary in demo mode.
**Recovery**: restore Redis and rebuild cache.
**Verification**: confirm cache hit ratio recovers.
**Rollback**: revert to last good config.

## Consumer lag
**Detection**: increased lag metric and delayed Kafka processing.
**Diagnosis**: inspect consumer group and partition throughput.
**Mitigation**: scale consumers and inspect DLQ backlog.
**Recovery**: rebalance partitions and restart consumers.
**Verification**: lag gauge returns near baseline.
**Rollback**: reduce consumer concurrency to previous safe configuration.

## Outbox backlog
**Detection**: `outbox_pending_events` metric grows.
**Diagnosis**: determine if Kafka or publisher is down.
**Mitigation**: restart publisher and replay backlog.
**Recovery**: ensure the outbox is drained.
**Verification**: pending events return to zero.
**Rollback**: revert to last successful publisher version.

## DLQ growth
**Detection**: repeated poison messages in DLQ.
**Diagnosis**: inspect headers, consumer, and exception stack traces.
**Mitigation**: fix bad payloads and replay with explicit approval.
**Recovery**: confirm consumer resumes from good events.
**Verification**: DLQ count stabilizes.
**Rollback**: disable replay once a trusted fix is in place.

## Inventory inconsistency
**Detection**: negative available quantity or unexpected reserved count.
**Diagnosis**: inspect database version numbers and logs.
**Mitigation**: stop order creation and reconcile inventory from the authoritative source.
**Recovery**: restore from the last consistent inventory snapshot.
**Verification**: rerun concurrency tests.
**Rollback**: revert faulty deployment or migration.

## Payment failures
**Detection**: repeated payment failure metrics.
**Diagnosis**: simulate a transient or permanent payment issue and inspect the saga.
**Mitigation**: compensate inventory and cancel order.
**Recovery**: restore payment simulator health and clear failed payments.
**Verification**: confirm order creation succeeds when payment is restored.
**Rollback**: revert payment simulator configuration.

## High latency
**Detection**: p95 and p99 latency metrics increase.
**Diagnosis**: inspect CPU, network, DB latency, and Kafka lag.
**Mitigation**: scale API pods and optimize DB queries.
**Recovery**: restore healthy latency trends.
**Verification**: compare latency against baseline.
**Rollback**: reduce concurrency if needed.

## High error rate
**Detection**: HTTP 5xx or 429 spikes.
**Diagnosis**: inspect application logs and API metrics.
**Mitigation**: rate-limit abusive clients and investigate service errors.
**Recovery**: confirm health and resilience policies recover.
**Verification**: error rate falls below threshold.
**Rollback**: revert unstable deployment.

## Certificate/HTTPS issue
**Detection**: browser shows invalid certificate or ingress failures.
**Diagnosis**: inspect ingress, certificate, and DNS settings.
**Mitigation**: renew or reconfigure certificates.
**Recovery**: validate HTTPS endpoint success.
**Verification**: use curl with certificate check disabled for diagnostics.
**Rollback**: revert ingress certificate config.

## Deployment rollback
**Detection**: deployment introduces instability or failed health checks.
**Diagnosis**: compare last good deployment manifest.
**Mitigation**: redeploy previous image or revert IaC.
**Recovery**: restore baseline config.
**Verification**: app health and smoke tests return green.
**Rollback**: use the previous stable image tag.

## Database migration failure
**Detection**: Flyway migration error or startup failure.
**Diagnosis**: inspect migration logs and database transaction state.
**Mitigation**: revert the migration or restore the database snapshot.
**Recovery**: rerun migration in a controlled environment.
**Verification**: app starts and schema is valid.
**Rollback**: rollback the migration and restore last known data.
