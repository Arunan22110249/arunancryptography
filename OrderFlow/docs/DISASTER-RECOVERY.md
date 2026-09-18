# Disaster Recovery

## PostgreSQL backup strategy
Use daily logical backups or database snapshots, with point-in-time recovery where available. The free deployment should keep backups small and focused on critical tables.

## Restore procedure
1. Stop application traffic.
2. Restore the most recent valid snapshot.
3. Rerun Flyway migrations if needed.
4. Rebuild cache and verify order counts.
5. Resume traffic only after health checks pass.

## Kafka persistence assumptions
Kafka is durable only if brokers remain healthy and storage is retained. The free-tier deployment should assume a single-broker configuration and avoid over-claiming high availability.

## Redis cache rebuild
Redis can be rebuilt from PostgreSQL or product catalog queries. Inventory is never authoritative in Redis.

## Stateless service restart
Backend services are stateless and can be restarted without data-loss if PostgreSQL, Kafka, and Redis remain healthy.

## Kubernetes recovery
A K3s node or pod restart should be handled by redeploying the manifests and validating readiness.

## RPO / RTO
The repository documents recovery goals appropriate for a zero-cost setup rather than claiming enterprise-grade disaster recovery. A realistic free deployment should assume moderate RPO and higher RTO.

## Data-loss scenarios
Potential scenarios include database corruption, Kafka retention loss, or VM failure. The documentation should clearly state the limits of the free deployment and encourage backup discipline.
