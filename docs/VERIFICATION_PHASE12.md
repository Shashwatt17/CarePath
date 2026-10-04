# Phase 12 verification and hardening record

Date: 30 September 2026 (Asia/Kolkata). Scope: existing Phases1–11 only; no Phase13 work.

## Runtime availability (one bounded check)

| Dependency | Executed check | Result |
| --- | --- | --- |
| Docker/native PostgreSQL | executable discovery for docker/postgres/pg_ctl/psql and /usr/lib/postgresql | BLOCKED: unavailable; no native Flyway V1–V10, upgrade or restart claim |
| Redis | redis-server/redis-cli executable discovery | BLOCKED: absent; production Lua runtime/TTL/reconnect not executed |
| Rendered browser | CUA Chrome attempted http://127.0.0.1:3108 after workspace HTTP200 | BLOCKED: ERR_BLOCKED_BY_CLIENT; desktop/mobile not verified; no retries |
| Frontend process | node next start on port3108; same-process fetch | COMPLETE + VERIFIED: HTTP200 only; not browser execution |
| LLM / Google Places | environment key-presence check; no local .env configured | PENDING / NOT CONFIGURED; no external medical/provider request made |
| Email | EmailNotificationProvider inspected | Interface only, no implementation/configured delivery; NOT CONFIGURED |
| Restart persistence | Native services unavailable | BLOCKED; database-backed design and H2 tests are separate evidence |

No H2/PGlite substitute is labelled native PostgreSQL. No installation or repeated infrastructure
probing was attempted. The browser tool's single blocked attempt took unusually long; it was not
repeated. No services were exposed publicly to bypass that boundary.

## Repairs and hardening

1. Added `ProductionGuard` under the explicit `production` Spring profile: rejects local/test profile
   coexistence, nonsecure cookies, inconsistent CORS/auth origins, nonexternal HTTPS origins, non-
   PostgreSQL datasource, absent service passwords and public Swagger configuration. Errors omit
   supplied values. Existing identity origin/key validation and route authorization remain intact.
2. Added `spring.jdbc.template.query-timeout` (DATABASE_QUERY_TIMEOUT_SECONDS, default20s) to bound
   JDBC statement/lock waits. This is not a transaction-wide deadline or a native performance test.
3. Next rewrites now validate BACKEND_URL as an HTTP(S) origin without credentials/path/query/fragment;
   private backend HTTP remains legitimate behind a trusted TLS edge. Invalid config fails safely.
4. Removed unused PLACES_PROVIDER/PLACES_API_KEY and EMAIL_* placeholders from .env.example;
   actual GOOGLE_PLACES_API_KEY and the interface-only email status are documented.
5. Corrected landing/page metadata that still described implemented history as future Phase4 work.
   No navigation or global visual redesign. Legacy system-info compatibility fields remain documented.

## Actual HTTP scope

HttpSecurityIntegrationTest starts the real embedded HTTP server with synthetic accounts, H2 and
test rate limiter. It registers/logs in, inspects __Host refresh-cookie flags, rotates, authenticates,
logs out and verifies token/session rejection. Separate tests verify allowed/foreign preflight, missing
CSRF header/foreign Origin, generic invalid share response/cache/referrer/indexing and protected denial.
JDBC timeout binding is asserted against the actual injected JdbcTemplate. Cookies are passed manually
in this HTTP test: production TLS/browser enforcement, secure-edge forwarding and Redis are NOT verified.
Existing FrontendProxyIT additionally uses real Next production HTTP proxy, >10MiB upload, processing,
review and evidence routes. Existing suites cover later modules separately; no full rendered journey claimed.

## Cross-module review

- `trusted_medical_observation` / review provenance gates feed HistoryRepository; pending/rejected
  candidates do not flow into calculations. Deltas/comparability remain deterministic, derived on read.
- StructuredRetrieval selects at most12 relevant readings and bounded computed facts; provider sees
  approved wording choices, not documents/account IDs. Exact validator and freshness checks persist.
- ProcessingWorker commits extraction/follow-up detection with job state under document ownership/locks.
  Follow-up confirmation remains explicit. Reminder source-first locks plus unique notification/offset
  constraints and a single DB transaction guard duplicate delivery. Native concurrent behavior remains open.
- PackAssembler scopes sources, trusts verified observations, reuses deterministic change services.
  Generated typed snapshots/PDFs remain immutable. ShareService pins revision and locks pack before
  share, checks expiry again after assembly, projects citation text without granting original downloads.
- Original deletion does not erase generated historical snapshots; deleting packs revokes access via
  cascade. This intentional Phase9 semantics supersedes initial foundation invalidation proposals.

## Resilience, security and performance review

Full suites retain invalid credentials, expired/replayed refresh, IDOR/relationship injection, corrupt
file/path/signature attacks, OCR timeouts/resource limits, safe provider failures, forged citations,
prompt injection, expired/revoked/scoped shares and reminder concurrency coverage. These are synthetic
and automated tests, not a professional penetration test. No unresolved critical/high finding was
identified in this scoped review. Live provider/network/DB failures remain unverified.

Reviewed bounds: paginated API lists, 501-reading comparison sentinel/500 trend window, batched timeline
provenance, 12-reading AI context, bounded body/provider concurrency, four Places requests at once/20
results, two PDF generators/40pages/cooperative8s deadline, one extraction worker with child heap/time/
page/pixel limits, reminder polling100 rows. Indexes cover ownership/history/source references and due
queues. Pack assembly uses bounded per-item reads (40 items), not an unbounded account dump. No native
EXPLAIN, load benchmark or distributed scheduler certification; owner storage quotas, retention/pruning,
backup/restore, JDBC driver socket timeouts and fleet-level cost limits remain deployment work.

Application logs omit raw documents/prompts/token URLs/coordinates. SQL/bind logs remain disabled.
No Nearby coordinate storage/cache or LLM/QR/PDF third-party disclosure was added. Operator proxy/APM
body capture must remain disabled. Next share CSP still permits inline bootstrap; nonce CSP and
complete rendered accessibility/clipboard/QR behavior remain open.

## Requirements assessment

See REQUIREMENTS_TRACEABILITY.md: all214 original product checklist items have individual
classification and code/test pointers. Activity UI/API, onboarding and Privacy/Settings are absent;
unified search, additional timeline event types and structured prescription/visit models are partial
or intentionally deferred. Email is an optional port without delivery. S3/direct booking not implemented.
These are Phase13 audit inputs, not silently added modules in Phase12. Synthetic metrics are not clinical
accuracy. Current builds do not establish production readiness.

## Regression

Exact commands and final counts are recorded in TESTING.md and PROJECT_STATUS.md after completion.
Baseline296 backend/46 frontend tests are retained; new guard/HTTP tests and backend-URL tests added.
No migration required; V1–V10 unchanged. No existing tests removed, disabled or skipped.

Final executed outcome: 307 backend /48 frontend tests, no failures/errors/skips; compile/package,
TypeScript/lint/production build pass. Safe per-suite results are in docs/verification/phase12-*.json.
