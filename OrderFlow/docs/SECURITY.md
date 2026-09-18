# Security

## Threat model
- Authentication attacks: JWT validation, short expiry, strong secrets.
- Authorization bypass: tenant checks and role-based restrictions.
- Tenant isolation: require organization_id validation on all access.
- SQL injection: parameterized JPA queries and validation.
- API abuse: rate limiting and request validation.
- Replay attacks: idempotency keys and timestamp validation.
- Duplicate requests: idempotency storage with unique key enforcement.
- Kafka message tampering: signed or trusted internal network assumptions and strict event schema validation.
- Secret exposure: keep secrets in environment configuration and never commit .env files.
- Insecure container configuration: run a non-root user, keep base images minimal.

## OWASP alignment
The project applies operational security principles around authentication, authorization, validation, and safe defaults.
