# Verification checkpoint V1 — 2026-09-24

Verification and repair only. No Phase 8 functionality, application code changes, migration changes or test removals.

## Results

| Check | Status | Executed evidence / blocker |
| --- | --- | --- |
| Native PostgreSQL 17 | BLOCKED | No docker/podman/postgres/pg_ctl/psql executables, Docker socket or configured remote database. Loopback 5432 refused. |
| Flyway V1–V7 on clean PostgreSQL | BLOCKED | Real PostgreSQL unavailable. No migrations executed this checkpoint; previous PGlite results are not native verification. |
| Persistence after backend restart | BLOCKED | No real PostgreSQL runtime. |
| Redis runtime/restart/failure | BLOCKED | No redis-server/redis-cli, remote configuration or loopback 6379 listener. Redis 7.4.5 is configured in Compose; not started. |
| Packaged backend with real services | BLOCKED | PostgreSQL/Redis unavailable. Test-context startup is not this acceptance gate. |
| Production frontend startup | COMPLETE + VERIFIED | `npm run start -- --port 3001`; HTTP GET `/` returned 200 and CarePath HTML. Next.js 16.3.5. |
| Frontend/backend HTTP regression | COMPLETE + VERIFIED | Existing FrontendProxyIT passed over actual Next/Spring HTTP with H2 and TestLimits. Includes >10 MiB upload, processing/review, evidence, deterministic assistant, questions, refresh/logout. Does not verify native services or rendered UI. |
| Desktop rendered E2E | BLOCKED | Cloud Chrome connected but navigation to workspace loopback returned `net::ERR_BLOCKED_BY_CLIENT`. No rendered CarePath flow executed. No local browser executable found. |
| Mobile smoke | BLOCKED | Same browser reachability blocker. |
| Live LLM / live prompt injection | PENDING | No LLM_API_KEY/OPENAI_API_KEY or local credential file configured. No provider request made. Contract/security/fallback tests passed, not live-provider verification. |
| Backend regression/package | COMPLETE + VERIFIED | 203 tests; 0 failures, 0 errors, 0 skipped. Maven package includes compile and Spring Boot repackaging. |
| Frontend regression/build | COMPLETE + VERIFIED | 20 tests; 0 failures, 0 skips. TypeScript, ESLint and production build passed. |

## Environment and commands

Java 21 toolchain and Maven 3.9.11 from `/workspace/scratch/toolchains`; Node 24.19.0. Expected services remain Compose PostgreSQL 17.6-alpine and Redis 7.4.5-alpine; neither was started. No credential values were printed.

From repository parent:

```sh
JAVA_HOME=/workspace/scratch/toolchains/jdk-21.0.12.1+1 /workspace/scratch/toolchains/apache-maven-3.9.11/bin/mvn -o -Dmaven.repo.local=/workspace/scratch/toolchains/m2 -f carepath/backend/pom.xml -B '-Dtest=*Test,ExtractionEvaluationIT,NormalizationEvaluationIT,LongitudinalEvaluationIT,FrontendProxyIT' package
```

From `frontend/`:

```sh
npm run test && npm run typecheck && npm run lint && npm run build
npm run start -- --port 3001
```

A Python subprocess harness started the production frontend, requested `http://127.0.0.1:3001/` using urllib, captured status/headers and terminated its launcher. A separate remote Chrome navigation was blocked by its network boundary. No tunnel/public deployment or security weakening was attempted.

Availability checks: `command -v` for Docker/Podman/PostgreSQL/Redis/browser executables; existence of `/var/run/docker.sock`; bounded TCP connection checks on local 5432/6379; presence-only checks of database/Redis/LLM environment settings and local .env files. No services or credentials available to close those gates.

## Security findings and repair scope

Actual frontend HTTP response: `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`, restrictive camera/microphone Permissions-Policy and private/no-store cache policy. No frontend CSP was returned; the previously documented frontend CSP limitation remains. Backend CSP and cookie/CSRF/CORS behavior remain supported by existing regression tests/code inspection, not production TLS/native-service execution here.

Redis is used for rate limiting, not medical storage or session persistence. Authentication store failure fails closed; assistant rate-store failure blocks provider dispatch and permits deterministic fallback. Runtime Redis restart/failure behavior remains unverified.

Existing tests rerun authentication failures, ownership, malformed uploads, provenance, output/citation injection and provider failures. HTTP proxy integration uses test database and rate-limit infrastructure; its successful requests cannot establish PostgreSQL SQL compatibility, persistent restart behavior or real Redis correctness.

No application defect established by available execution. Corrected stale README packaging/schema wording and misleading provider configuration comment. All seven migration files and application/test source remain unchanged from the Phase 7 checkpoint. No tests removed/disabled; baseline counts preserved.

## Remaining acceptance gates

Run configured PostgreSQL 17 and Redis, migrate clean database and restart packaged backend, then execute synthetic persistence and rendered desktop/mobile flows. Configure an authorized provider credential for minimal synthetic live-provider tests. CarePath has passed the available regression gate but has **not passed this checkpoint's complete live-integration gate**. Complete these checks before treating Phase 8 as cleared by this checkpoint.
