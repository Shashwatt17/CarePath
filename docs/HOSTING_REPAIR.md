# CarePath hosting repair and verification

Status: application changes verified locally; production restoration pending.
Verified on 7 November 2026; continuation recorded 8 November 2026.

## What changed

- Added optional `STORAGE_BACKEND=database`. Original PDFs/images are stored in PostgreSQL and participate in the existing upload transaction. Owner checks and SHA-256 integrity verification remain in DocumentService. Local storage remains the default.
- Added Flyway V11; V1–V10 are unchanged. The adapter rejects overwrites, malformed keys, empty/oversized files and missing reads. Delete is idempotent.
- Added configurable JDBC pool size, Redis TLS and hosting `PORT` fallback.
- Added six storage regression tests and a native-service verification script.

The database adapter is intended for a small demonstration deployment. Original files consume the database quota alongside application rows. It is not a substitute for a scalable object-storage service or backups. Switching an existing local-storage deployment does not migrate its old originals; preserve/export those before changing storage mode.

## Verification actually performed

| Check | Result |
|---|---|
| Full backend selected suite and package | 328 tests; zero failures, errors or skipped tests; BUILD SUCCESS |
| Frontend unit/client tests | 58 passed; zero failed/skipped |
| TypeScript, ESLint and Next production build | Passed |
| Native PostgreSQL | 16.2; Flyway V1–V11 applied, including a fresh isolated `carepath` schema |
| Redis | Real authenticated server; registration/login and readiness passed |
| Production Next HTTP proxy | Registration/login, multipart transport and cookies tested |
| Synthetic PDFs | January, April, September and scanned lab PDF upload/process/preview/download passed |
| Review and explanation | Confirmable candidates reviewed; trusted timeline and evidence-linked deterministic explanation passed |
| LLM absent | Explicit NOT_CONFIGURED response with deterministic fallback passed |
| Restart | Backend, PostgreSQL and Redis restarted; session, identical PDF bytes and timeline preserved |
| Delete | Durable original removed and subsequent download denied |

These results use synthetic records. No clinical accuracy claim is made. The native script leaves uncertain candidates unconfirmed rather than asserting all extracted fields are correct. Its JSON result is in `docs/verification/native-runtime-latest.json`.

## Reproduce

Prerequisites: Java 21, Maven 3.9+, Node >=22.18, Python with `requests`, Tesseract with English data, PostgreSQL binaries and Redis server. Ports 3000, 8080, 55432 and 56379 must be unused. Run as an unprivileged user.

```sh
cd frontend
npm ci
npm test
npm run typecheck
npm run lint
npm run build
cd ../backend
mvn -B '-Dtest=*Test,ExtractionEvaluationIT,NormalizationEvaluationIT,LongitudinalEvaluationIT,FrontendProxyIT' package
cd ..
python scripts/verify-native.py --postgres-bin /path/to/postgres/bin --redis-bin /path/to/redis-server --java /path/to/java21/bin/java
```

The native script creates its own temporary cluster, test credentials and synthetic account; it does not connect to production. It starts and stops all child processes and writes a compact verification result. The temporary directory contains local test credentials and should not be published. Test reports from Maven are under `backend/target/surefire-reports`.

## Observed production blocker

The existing Render PostgreSQL instance was expired/suspended. The backend log showed a failed database connection/UnknownHostException at Flyway startup. The Render dashboard showed a 10-day deletion countdown on 7 November. No paid upgrade, destructive action or attempt to replace the old data has been made. A new database will not recover old users or records.

## Proposed free demonstration deployment

Retain Netlify frontend and Render backend/Redis. Create a separate free Supabase PostgreSQL project for CarePath; do not reuse the unrelated frauddetection project. Disable its Data API and automatic table exposure. Keep the application's tables in a private `carepath` schema and do not add that schema to exposed APIs.

Backend environment:

| Setting | Value/source |
|---|---|
| SPRING_PROFILES_ACTIVE | production |
| DATABASE_URL | JDBC URL from the Supabase session pooler, port 5432; append `sslmode=verify-full&currentSchema=carepath` and configure the provider CA if required |
| POSTGRES_USER / POSTGRES_PASSWORD | Exact pooler username and database credential, stored only in Render secrets |
| SPRING_FLYWAY_SCHEMAS | carepath |
| DATABASE_POOL_SIZE | 3 |
| STORAGE_BACKEND | database |
| VAULT_MAX_BYTES | 4194304 for the initial Netlify deployment |
| FRONTEND_ORIGIN | https://carepath-health.netlify.app |
| AUTH_COOKIE_SECURE | true |
| REDIS_HOST / REDIS_PORT / REDIS_PASSWORD | Existing Render Redis values; preserve them |
| REDIS_SSL_ENABLED | Match the Redis provider's connection requirement |
| JWT_PRIVATE_KEY_PATH / JWT_PUBLIC_KEY_PATH | Stable existing Render secret-file paths; preserve keys across redeploys |
| AUTH_RATE_KEY | Preserve the existing secret |
| LLM_ENABLED | false until a funded provider and credentials are deliberately configured |

Netlify `BACKEND_URL` remains `https://carepath-backend-yyj1.onrender.com` unless the service changes. Rebuild after changing it. The original app supports 20 MiB uploads, but Netlify's buffered function payload limit is smaller; start with a 4 MiB upload limit and verify the actual deployed route before raising it.

After provisioning: verify schema isolation and migrations, database/Redis health, production cookies and CORS, then repeat the synthetic workflow online. Inspect memory while processing a scan; local success is not proof that Render's free memory limit is sufficient. Verify desktop/mobile rendering, restart persistence, and share/revoke on the deployed host.

## Open items

- Supabase organization selection/project provisioning and backend database credentials.
- Publication of this repair and production deployment verification.
- Recovery/export of any valuable old Render data; a fresh database does not restore it.
- Render free-tier memory/load validation and Netlify upload behavior.
- Rendered desktop/mobile UI, external LLM and real-provider verification.
- Production backup/restore, retention and operational security checks.

Do not describe the app as fully hosted or production certified until the relevant deployed checks pass.

## Provider documentation checked

- https://render.com/docs/free
- https://render.com/docs/disks
- https://supabase.com/docs/guides/database/connecting-to-postgres
- https://supabase.com/docs/guides/getting-started/quickstarts/spring-boot
- https://supabase.com/docs/guides/platform/free-project-pausing
- https://docs.netlify.com/build/functions/configuration/
