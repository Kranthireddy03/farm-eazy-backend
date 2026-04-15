# UAT Required Environment Variables (Security + Encryption + Redis)

These values must be configured in UAT runtime environment and must not be hardcoded in application.properties.

## Required Secrets
- JWT_SECRET
- API_ENCRYPTION_SECRET
- API_GATEWAY_SECRET
- REDIS_HOST
- REDIS_PORT

## Recommended Additional Variables
- REDIS_USERNAME
- REDIS_PASSWORD
- REDIS_SSL_ENABLED=true
- API_GATEWAY_ALLOWED_CLIENTS=farmeazy-web,farmeazy-admin
- API_GATEWAY_MAX_SKEW_SECONDS=300
- API_ENCRYPTION_ENABLED=true
- API_ENCRYPTION_REQUIRE_REQUEST=true
- API_GATEWAY_ENABLED=true
- API_GATEWAY_REQUIRED=true

## Notes
- API_ENCRYPTION_SECRET should be at least 32 characters.
- JWT_SECRET should be at least 64 bytes for HS512.
- API gateway client signatures should use HMAC-SHA256 over:
  clientId:timestamp:method:path
- If Redis is unavailable, service will fall back to in-memory cache manager (short-term resilience).
