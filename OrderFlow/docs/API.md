# API Documentation

## Authentication
Use JWT tokens with Authorization Bearer <token>.
- POST /api/v1/auth/login
- POST /api/v1/auth/register
- GET /api/v1/auth/me

## Core endpoints
- GET /api/v1/products
- GET /api/v1/products/{id}
- POST /api/v1/products
- PUT /api/v1/products/{id}
- DELETE /api/v1/products/{id}
- GET /api/v1/orders
- POST /api/v1/orders
- GET /api/v1/demo/status
- GET /api/v1/system/health
- GET/POST/DELETE /api/v1/admin/failures

## Error format
```json
{
  "timestamp": "2026-09-17T00:00:00Z",
  "status": 409,
  "code": "IDEMPOTENCY_CONFLICT",
  "message": "The same idempotency key was reused with a different request body.",
  "traceId": "..."
}
```

## Rate limits
Use Redis-backed rate limiting and return HTTP 429 when limits are exceeded.

Product and order APIs require JWT authentication. Tenant scope is derived from the token; request bodies cannot select another organization.
