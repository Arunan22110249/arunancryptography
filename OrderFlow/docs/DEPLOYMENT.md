# Deployment

## Local Docker Compose
```bash
docker compose up -d
```

## K3s
Apply the manifests under infrastructure/kubernetes after building and pushing ARM64-compatible images.

## Oracle Always Free
Use a resource-aware VM with ARM Ampere and a single K3s node. Do not expose PostgreSQL, Kafka, or Redis publicly.

## GitHub Pages
The frontend can be deployed with GitHub Actions and uses demo mode automatically when the backend is unavailable.
