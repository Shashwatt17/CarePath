# Local infrastructure

Root docker-compose.yml owns PostgreSQL 17 and Redis 7 services, named durable volumes,
loopback ports, required environment passwords and healthchecks. Applications run on the host.
Use .env.example and README.md. Keep this topology small; do not add microservices without evidence.
Production deployment, TLS, encrypted volumes, backup/restore and least-privilege roles are deferred.
