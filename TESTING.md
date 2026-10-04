# Final executed result:320 backend /58 frontend, zero failures/errors/skips

The original testing plan and historical checkpoints below are retained. For the final complete
commands, scope and live gaps, see the FINAL Phase13 section at the end and
[VERIFICATION_PHASE13.md](docs/VERIFICATION_PHASE13.md). Tests/builds do not establish rendered
browser, native PostgreSQL/Flyway/Redis, live providers or production certification.

# Testing — final Phase13 (historical checkpoints below)

Current backend checks run the full Spring Boot app (H2 PostgreSQL mode, real JPA/JDBC/security),
not mocked auth services or fake user repositories. The production Redis store is substituted at
its interface with a deterministic clock-based test store. Actual V1/V2/V3 migrations are independently
applied by the PGlite runner. Do not confuse these with native PostgreSQL/Flyway/Redis tests.

Commands: `cd backend && mvn -B verify`; `cd frontend && npm run typecheck && npm run lint && npm test && npm run build`;
`cd scripts/schema-check && npm ci && npm test`.
Optional full production Next proxy integration: `cd backend && mvn -Dtest=FrontendProxyIT test`
(requires frontend built with BACKEND_URL=http://localhost:8080 and free ports 8080/3000).

AuthenticationIntegrationTest covers registration/duplicate race/hashing, login/generic failures,
protected access, expiry/tamper/issuer/audience/missing expiry/unsigned token rejection, refresh/invalid/
expired/disabled/revoked sessions, serialized concurrent rotation, replay family revocation, logout,
validation/unknown-field injection/UTF-8 limit, CSRF/CORS, body limits, rate windows/outages and audit.
SecureCookieIntegrationTest covers production cookie flags/prefix. Existing foundation assertions remain.
Frontend Node tests cover refresh deduplication, bearer headers, invalid sessions/provider outages,
logout failure semantics and malformed/sensitive responses. These are client tests, not browser UI tests.

Native PostgreSQL gate when available: start Compose and backend; confirm V1/V2/V3 in flyway_schema_history
and readiness. Run the same auth flow, concurrent refresh and Redis outage/rate-limit behavior against
that environment. `python3 scripts/verify_schema.py` checks schema invariants against a migrated local
PostgreSQL (psycopg 3 required). Use disposable synthetic accounts and a dedicated test database.

The following Phase 1 plan is retained for historical context; PROJECT_STATUS.md records current results.

# Original verification and evaluation plan

## Phase 1 executable checks

- `cd backend && mvn -B verify`: compile Java 21, run seven MVC/security foundation tests, package JAR.
- `cd frontend && npm ci && npm run typecheck && npm run lint && npm run build`.
- Start PostgreSQL/Redis with `docker compose up -d --wait`, run backend; Flyway validates/applies V1.
- `pip install 'psycopg[binary]>=3.2,<4'`, then `python3 scripts/verify_schema.py` tests an isolated
  rollback-only transaction against migrated PostgreSQL. Set DATABASE_TEST_URL to a PostgreSQL URI,
  or let the script use root .env. No test account persists after the script exits.
- HTTP health/readiness, denied API, allowed/blocked CORS and server-rendered frontend smoke checks.

Actual results, toolchain versions and blocked checks belong in PROJECT_STATUS.md, not inferred here.

## Schema check without Docker

`cd scripts/schema-check && npm ci && npm test` applies the same V1 migration to PGlite's
PostgreSQL engine, runs 14 rollback-only constraint/view checks and confirms zero test users persist.
This validates SQL syntax and selected invariants; it does not validate Flyway/JDBC, Docker images,
networking, PostgreSQL/Redis server operations or product-level owner authorization.
The Python psycopg runner above uses the same SQL against a genuinely migrated PostgreSQL server.

Mockito uses the subclass mock maker for foundation tests; these tests do not need final/static
mocking. This avoids requiring dynamic agent attachment for the chosen test setup.

## Required later tests

Identity: password verification, generic errors, JWT claims/expiry, refresh rotation/replay,
logout/revocation, sensitive endpoint rate limits and limiter failure handling.
Ownership: two users, every list/get/update/delete/download plus nested resource selection for
vault, observations, appointments, Visit Packs, shares; cross-user UUID never grants access.

Parsing: supported files, type mismatch, malformed/encrypted PDFs, oversized/pixel-bomb files,
OCR fallback, unknown fields, original text retained, pipeline restart/retry/idempotency.
Normalization: alias ambiguity, exact dictionary versions, no unwarranted LOINC mapping;
BigDecimal conversions, dimensions, comparators, missing units, reference bounds, round trips.
Changes: new/increase/decrease/stable with documented tolerance, comparable-panel absence,
reference interval transitions, differing methods/ranges, missing dates/units and insufficient evidence.
Provenance: correct original/page and substring or OCR region; review retains original extraction.

Care: follow-up weeks/months and unknown anchor, leap years/DST/time zones; confirmation before
scheduling; persistent reminders restart/lease recovery/duplicate suppression; appointment edits.
Sharing: create→read→revoke→denied and create→expire→denied using injected Clock; foreign management
rejected; selected revision only; source deletion invalidation; no caching leak.
Visit Pack: selected-only snapshot, source list, correct PDF output, no unsupported diagnosis.
Assistant: insufficient-evidence refusal, owner-scoped retrieval, invented citation rejection,
prompt-injection fixtures, no diagnosis/prescribing. Providers: disabled, timeout, quota and failure.
Frontend: auth guard/session expiry, upload/review, evidence navigation, timeline, pack/share,
empty/loading/error states, keyboard access and mobile layouts.

## Evaluation framework design (implementation deferred)

Use synthetic labelled reports split by template/patient into development and holdout sets.
Maintain gold JSON with document type, exact fields, canonical concept, units/values, expected
change classifications and source page/region. Clearly mark every fixture synthetic.
Measure classification accuracy; extraction field precision/recall/F1 with exact and documented
numeric tolerance; normalization accuracy and abstention coverage; compatible conversion correctness;
change label macro-F1; correct-page/evidence-span provenance rates. Report denominators, unsupported
cases, review coverage and failures; never silently drop difficult documents.
Scripts will emit machine-readable results and reproducible version metadata. No metric is measured
or claimed yet. Evaluation scripts/data belong to the later intelligence phase, not Phase 1.

## Phase 3 tests

`VaultIntegrationTest` boots actual Spring Security/controllers/services/JDBC, real PDFBox/ImageIO
validation and temporary local filesystem. It covers valid formats, path/MIME/magic/malformed/size
rejections, hashing, all owned operations, both-direction two-user IDOR, filters/pagination, integrity
failure audit, deletion recovery, concurrent uploads/update/delete, stale versions, symlink/overwrite,
encrypted/page-limited PDFs and multi-page rendering. H2 schema is a test-compatible subset of V1–V3;
this does not execute Flyway. The independent PGlite runner executes actual SQL and 24 invariants.

The optional FrontendProxyIT also sends a >10 MiB synthetic PNG over real multipart HTTP through
production Next to Spring, lists/previews/downloads byte-for-byte/deletes it, then checks auth refresh,
logout and revoked-session rejection. Client tests cover bearer refresh, token destination restrictions
and upload feedback. None of these is a rendered browser test. Browser-install failure is recorded in
PROJECT_STATUS.md; browser UI verification remains an explicit gate.

## Phase 4 verification

Default `mvn test` includes 98 backend tests: previous 57 plus 20 parser/provenance rules,
11 isolated runtime/resource tests, nine processing API/security integration tests and one actual
scheduled-worker integration test. H2 uses a test subset, not Flyway. The test clock is truncated to
database microsecond precision; access expiry checks are retained, and lease tests reauthenticate
after advancing time. Production security is not weakened.

With Tesseract/eng installed and frontend built, run the complete 101-test set:
`mvn -Dtest='*Test,ExtractionEvaluationIT,FrontendProxyIT' package`.
This adds two real OCR/evaluation tests and the existing production Next HTTP proxy test.
The proxy test now also uploads September, processes it, retrieves five candidates and page-two
evidence/preview, deletes it, then completes refresh/logout. It is **not a rendered-browser test**.

Frontend: `npm run typecheck && npm run lint && npm test && npm run build`: ten configured Node tests.
New tests verify owner-scoped processing routes and safe errors; component interaction/visual tests
remain pending without a browser. The original eight frontend tests remain included.

`node scripts/schema-check/verify.mjs` applies V1–V4 and 30 rollback-only invariants in PGlite.
Native runner `python3 scripts/verify_schema.py` includes the same extraction invariants, but has NOT
been run against native PostgreSQL. Native PostgreSQL/Flyway/Redis/Compose and browser gates remain open.
Do not infer live-service verification from H2, PGlite, compilation or HTTP proxy success.

Evidence: `docs/verification/phase4-backend.json`, `phase4-frontend.txt`, `phase4-schema.txt`,
and `evaluation/results/latest.json`. Tests include page-two exact evidence, ambiguous/missing fields,
explicit flags, injection text, corrupt PDF/image, image/page limits, compressed content, real OCR
timeout, parent deadline, staging symlinks/reaping, job fencing/duplicate prevention, backoff/retry,
integrity failure, cascading deletion and bidirectional process/status/extraction/evidence/retry IDOR.
The packaged Boot PropertiesLauncher worker was separately exercised on January (three candidates).

The optional Maven clean goal initially failed because its plugin was not cached in offline mode.
Generated target output was removed locally and package rerun to force fresh compilation. This is a
tooling-cache issue, not a bypassed compile/test gate. Initial parser/integration failures were fixed
and the complete package run passed; exact commands/results appear in PROJECT_STATUS.md.

## Phase 5 commands and coverage

Full suite (frontend production build required for HTTP proxy test, real Tesseract for OCR):
`cd backend && mvn '-Dtest=*Test,ExtractionEvaluationIT,NormalizationEvaluationIT,FrontendProxyIT' package`.
Frontend: `npm run typecheck`, `npm run lint`, `npm test`, `npm run build` in frontend.
Schema: `node scripts/schema-check/verify.mjs` from root. Native runner includes the same V5 checks,
but PGlite does not establish native PostgreSQL/Flyway/JDBC/runtime verification.

NormalizationTest: 25 cases including aliases, ambiguity/abstention, clinical distinctions, absent
standard identifiers, same-unit/concept-specific conversion, comparator preservation, HALF_EVEN ties,
source precision and supplied-boundary comparisons. ReviewIntegrationTest: 14 cases through real
security/controllers/JDBC/files/extraction, covering all decisions, immutable originals/provenance,
low-confidence correction, idempotency, concurrency/stale version, both-direction IDOR, mass assignment,
invalid fields/concepts, source/value tampering, pagination/request bounds, derivative deletion and the
additional actual alias/unsupported-unit PDF. The review evaluation uses all three prior reports.
FrontendProxyIT adds actual same-origin HTTP review/promotion/read to the previous auth/vault/extraction
flow. This does not test rendered React interaction. Four new typed-client tests bring frontend to 14.

`sh evaluation/run-normalization.sh` executes the authored JSON ground truth and actual review API
suite; measured JSON is written to evaluation/results/phase5-normalization.json and phase5-review.json.
Normalization scores count exact decimal strings including justified scale and expected abstentions.
The review report counts source/original preservation for every decision across three synthetic files.
These authored regression metrics are not held-out or clinical accuracy. Full current counts and exact
executed commands are recorded in PROJECT_STATUS.md and docs/verification/phase5-*.

H2 schema is the compatible test subset plus V5, with Flyway disabled. V1–V5 SQL is applied separately
in PGlite with 36 invariants. Two earlier high-confidence processing state expectations now assert
NEEDS_REVIEW, as explicitly required by Phase 5; no prior test/security checks were removed.

## Phase 6 executed verification — 2026-09-24

From the workspace parent, the complete suite including opt-in evaluation and actual Next HTTP proxy:

```sh
JAVA_HOME=/workspace/scratch/toolchains/jdk-21.0.12.1+1 /workspace/scratch/toolchains/apache-maven-3.9.11/bin/mvn -o -Dmaven.repo.local=/workspace/scratch/toolchains/m2 -f carepath/backend/pom.xml -B '-Dtest=*Test,ExtractionEvaluationIT,NormalizationEvaluationIT,LongitudinalEvaluationIT,FrontendProxyIT' package
npm --prefix carepath/frontend run typecheck
npm --prefix carepath/frontend run lint
npm --prefix carepath/frontend test
npm --prefix carepath/frontend run build
node carepath/scripts/schema-check/verify.mjs
```

The toolchain paths are environment-specific verification commands, not required application paths.
For another machine use JDK21 and `mvn` from PATH; run npm scripts from frontend as usual.
Backend: 172 tests, zero failures/errors/skips; Java compile and executable JAR package PASS.
Frontend: 17 tests, zero failures/skips; TypeScript, lint and production build PASS.
PGlite: V1–V6 SQL accepted, 36 invariants; this does not verify native Flyway/JDBC/PostgreSQL.
V1–V5 bytes match the prior archive. Native PostgreSQL, Redis, Docker and browser executables were absent.
A stale generated JAR initially failed repackage with a ZIP-end error; deleting only generated JARs and
rerunning the complete suite produced BUILD SUCCESS. No security gate was disabled.

Added ComparisonTest (22 cases), HistoryIntegrationTest (8), LongitudinalEvaluationIT (1).
Prior 141 cases remain, including the extended production Next proxy history/evidence checks.
Coverage includes numeric direction/boundaries/zero/negative baseline, supported conversion, incompatible
units/labs/context, reference semantics, grouping/order/undated/same-day data, trusted-only reads,
exact provenance, fresh recomputation, pagination, bidirectional IDOR and conservative presence/absence.
Absence tests explicitly construct synthetic panel context in the test database; they do not relabel
existing PDF ground truth. No database performance benchmark or rendered UI test was executed.

Evaluation: `evaluation/run-longitudinal.sh` reruns the authored Phase 6 checks. Actual result JSON:
`phase6-comparison.json`: direction 12/12, comparability 12/12, reference outcomes 12/12.
`phase6-history.json`: chronological grouping 3/3, change labels 5/5, exact source references 8/8.
These are small authored synthetic regression fixtures, not clinical accuracy or held-out validation.
Manual browser gate: login → upload/process January, April, September → verify candidates → timeline →
Hemoglobin chart → select reports in Changes → inspect both evidence links/source pages. Still pending.

## Phase 7 executed checks — 2026-09-24

```sh
JAVA_HOME=/workspace/scratch/toolchains/jdk-21.0.12.1+1 /workspace/scratch/toolchains/apache-maven-3.9.11/bin/mvn -o -Dmaven.repo.local=/workspace/scratch/toolchains/m2 -f carepath/backend/pom.xml -B '-Dtest=*Test,ExtractionEvaluationIT,NormalizationEvaluationIT,LongitudinalEvaluationIT,FrontendProxyIT' package
npm --prefix carepath/frontend run typecheck
npm --prefix carepath/frontend run lint
npm --prefix carepath/frontend test
npm --prefix carepath/frontend run build
node carepath/scripts/schema-check/verify.mjs
command -v docker postgres redis-server chromium chromium-browser google-chrome tesseract
```

The complete backend run passed **203 tests, zero failures/errors/skips**, including compile and executable
JAR packaging. This retains all 172 Phase 6 cases and adds 14 assistant integration + 17 provider-contract
cases. The actual Next production HTTP proxy now also exercises assistant explanation/source page and
saved-question create/get/delete. It is not a rendered-browser test. Frontend **20 tests** passed with
TypeScript/lint/production build. PGlite applied V1–V7 and **40 invariants**; zero remaining test users.
V1–V6 were byte-compared with the Phase 6 archive and remain unchanged.

AssistantIntegrationTest uses real authentication/API/security/JDBC/extraction/human review and a
controlled provider port: exact three-report changes and September page-two provenance, raw prompt and
malicious document minimization, no pending/rejected/unverified data, insufficient/unsupported context,
clinical boundary and urgent phrases, malformed/forged provider evidence, both-direction IDOR, saved draft
CRUD/version/deletion/audit privacy, body validation, rate limiter failure, consent, year scope and source
change during generation. ProviderContractTest exercises the actual HTTP adapter through a controlled
HttpClient: request JSON/schema/auth configuration, missing key, 401/429/500, total timeout/cancellation,
malformed/schema-invalid/unsafe output, empty/refused/truncated response, exact citations, streaming byte
limits and context limits. The test transport is not live-provider verification. HTTPS-only production
configuration is retained. Three frontend client tests check consent/allowlists, CRUD and safe errors.

Initial verification found an H2 timestamp syntax mismatch and a schema fixture missing extraction_id;
only test fixtures were corrected. Production foreign keys were not weakened. Focused suite passed 31
cases before the complete run. No clinical accuracy numbers are reported. Existing Phase 4–6 synthetic
evaluations ran again unchanged as part of the full regression suite.

BLOCKED: native PostgreSQL/Flyway/Redis/Compose and rendered browser; executable discovery found only
Tesseract among the checked tools. COMPLETE + NOT LIVE-VERIFIED: real provider implementation without
supplied live credentials. Browser gate: login → Assistant → ask Hemoglobin → view facts/evidence/page →
generate/edit/save question → reload → edit/delete; test consent and provider-unavailable states. Native
migration/ownership/deletion/Redis gates remain separate. Clinical safety review remains a deployment gate.


## Checkpoint V1 — 2026-09-24

Available regression verification passed: 203 backend tests, 20 frontend tests, both builds, TypeScript and lint. Actual frontend HTTP returned no-store, nosniff and DENY headers. Native-service, rendered-browser/mobile and live-provider gates remain open. No production security controls changed. See `docs/VERIFICATION_CHECKPOINT_V1.md` for exact commands, execution boundaries and blockers.

## Phase 8 executed checks — 2026-09-28

From the workspace parent of `carepath`, final backend command:
```sh
JAVA_HOME=/workspace/scratch/toolchains/jdk-21.0.12.1+1 /workspace/scratch/toolchains/apache-maven-3.9.11/bin/mvn -o -Dmaven.repo.local=/workspace/scratch/toolchains/m2 -f carepath/backend/pom.xml -B '-Dtest=*Test,ExtractionEvaluationIT,NormalizationEvaluationIT,LongitudinalEvaluationIT,FrontendProxyIT' package
```
Result: **232 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS**, including compile and executable
JAR packaging. All 203 baseline cases retained; 22 CareIntegrationTest + 6 CareRulesTest + one added
ExtractionRuntimeTest case. The original migration bytes were compared to carepath-checkpoint-v1.zip
and V1–V7 were unchanged. No old Java test file was removed. Per-suite totals: docs/verification/phase8-backend.json.

Frontend commands from `carepath/frontend`:
```sh
npm test && npm run typecheck && npm run lint && npm run build
```
Tests **24/24 passed**, TypeScript and ESLint passed. The first build aborted on a corrupt generated
Turbopack cache. The existing `.next/cache` directory was moved to `/tmp/carepath-next-cache-<timestamp>`
using Python Path.rename; `npm run build` then passed. No dependency/security configuration was weakened.
This is a production build, not rendered browser verification. Existing HTTP FrontendProxyIT passed
against real Next and Spring HTTP with the H2/test-limiter profile, not native PostgreSQL/Redis.

Additional commands from the workspace parent:
```sh
node carepath/scripts/schema-check/verify.mjs
command -v docker postgres redis-server chromium chromium-browser google-chrome tesseract
python3 carepath/scripts/package.py --phase phase8
```
PGlite applied V1–V8 and passed the 40 existing invariants with zero remaining test users. This explicitly
does not verify native PostgreSQL, Flyway/JDBC startup or native locking. Only Tesseract was found among
those service/browser executables. Native services, persistence/restart and rendered browser remain BLOCKED.
Live LLM provider remains PENDING; no credentials were added or used. Existing provider contract/fallback
and synthetic evaluation cases passed again in the complete backend suite.

New API tests exercise real registration/bearer security/ownership/validation/JDBC services through
MockMvc. H2 is the database and the test rate limiter is substituted as in earlier phases. Tests cover
symptom lifecycle/search/timeline, lab/assistant exclusion, appointment links/status/timezone/order,
source extraction and follow-up decisions, exact source instruction/page, duplicate decisions, persisted
reminder rows/due arithmetic, notifications/read-all, source deletion, audit privacy and cross-user access.
Concurrency tests execute simultaneous follow-up confirmation and reminder delivery; one result wins.
Reminder tests use the injected TestClock and invoke the production poller, with no wall-clock sleeps.
Real synthetic PDFs are generated and processed; extracted source text is not manufactured in the DB.

Repairs verified during this phase:
- Preserve submitted care OffsetDateTime offsets before IANA/DST validation; a real API test covers
  Asia/Kolkata and rejects a conflicting UTC offset.
- Replace inaccessible parent-PID liveness lookup with supervisor-pipe EOF monitoring in the isolated
  extraction child. The new subprocess case closes the pipe and verifies exit 75. Existing deadline,
  OCR timeout, resource-abuse and temporary cleanup tests still pass; supervisor implementation unchanged.
- Test-only H2 schema syntax corrected while production additive migration strategy retained.

Manual desktop/mobile acceptance and restart checklist: docs/CARE_ORGANIZATION.md. These checks have
not been executed in a rendered browser or native database. Production cookie/CORS/CSRF/TLS checks
remain open as recorded in Checkpoint V1. Synthetic fixture success is not clinical validation.

## Phase 9 executed verification — 2026-09-29

Full regression **250 backend tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS**.
The 232 baseline tests remain; additions are 15 PackIntegrationTest and 3 PackPdfTest tests.
API integration uses real Spring security/controllers/services/JDBC with H2 and the existing test
rate-limit store. It does not establish PostgreSQL/Redis runtime behavior.

Exact commands from the workspace parent (preinstalled offline toolchain):

```bash
JAVA_HOME=/workspace/scratch/toolchains/jdk-21.0.12.1+1 /workspace/scratch/toolchains/apache-maven-3.9.11/bin/mvn -o -Dmaven.repo.local=/workspace/scratch/toolchains/m2 -f carepath/backend/pom.xml -B '-Dtest=*Test,ExtractionEvaluationIT,NormalizationEvaluationIT,LongitudinalEvaluationIT,FrontendProxyIT' package
npm --prefix carepath/frontend run typecheck
npm --prefix carepath/frontend run lint
npm --prefix carepath/frontend test
npm --prefix carepath/frontend run build
npm --prefix carepath/scripts/schema-check test
pdftoppm -scale-to 1100 -png carepath/sample-data/phase9/synthetic-visit-pack.pdf /tmp/carepath-p9-render/final
pdftoppm -scale-to 1100 -png carepath/backend/target/phase9-pdf/multipage.pdf /tmp/carepath-p9-render/stress
python3 carepath/scripts/package.py --phase phase9
```

Frontend **29 tests pass** (24 retained + 5 pack-client contract tests), TypeScript, lint and
production build pass. These test typed requests, allowlists, error handling, binary downloads and
source navigation; they do not render React or replace browser E2E. Full backend package includes
compile and the existing real Next HTTP proxy integration test with the test backend.

Pack tests cover bidirectional pack IDOR, foreign relationship injection, validated selections,
preview fingerprint/source changes, optimistic state, concurrent generation, explicit revisions,
pagination, audits, pending/rejected candidate exclusion and source edits/deletions after generation.
Synthetic January/April/September documents are actually processed and verified through existing
services; generated changes match the existing engine (Hemoglobin 11.3 → 10.4; Vitamin D 24 → 31
for April → September), with exact source pages. No clinical-accuracy metric is inferred.
PDFBox parses actual output to check text, exclusions, safe metadata/actions and pagination; bounds,
long text and Unicode/unsupported-glyph escaping are covered. Three-page synthetic pack and
four-page stress output were rendered and inspected: no visible clipping/overlap. The current
synthetic PDF is retained under sample-data/phase9; build-only stress output is reproducible by tests.

PGlite applied V1–V9 with 40 existing schema invariants. This is SQL compatibility evidence only,
not Flyway/JDBC/native PostgreSQL execution. V1–V8 and all baseline test sources remain byte-identical.
Native PostgreSQL/Flyway, Redis, restart persistence, rendered browser/mobile, live LLM,
production TLS/cookie/CORS/CSRF and email remain unverified. No new infrastructure workaround used.

Failures repaired before the passing full run: concurrent REPEATABLE_READ serialization conflict
originally surfaced as 503; pack-specific advice now maps it to safe 409. A fixture expectation used
January's Vitamin D 19 for an April/September selection; corrected the assertion to April's actual
24 without altering medical fixtures. Frontend source-picker method and new-draft router handling
were repaired during implementation. Vault deletion wording now discloses retained pack snapshots.
Safe aggregate results are in docs/verification/phase9-*; raw medical test logs are not packaged.

## Phase 10 verification — 2026-09-29

The interrupted run's temporary logs were unavailable on resume. Completed code and focused tests
were preserved; the full commands below were executed again with local verification logs retained
outside the source archive. No baseline tests or migrations were changed.

```bash
JAVA_HOME=/workspace/scratch/toolchains/jdk-21.0.12.1+1 /workspace/scratch/toolchains/apache-maven-3.9.11/bin/mvn -o -Dmaven.repo.local=/workspace/scratch/toolchains/m2 -f carepath/backend/pom.xml -B '-Dtest=*Test,ExtractionEvaluationIT,NormalizationEvaluationIT,LongitudinalEvaluationIT,FrontendProxyIT' package
npm --prefix carepath/frontend test
npm --prefix carepath/frontend run typecheck
npm --prefix carepath/frontend run lint
npm --prefix carepath/frontend run build
npm --prefix carepath/scripts/schema-check test
```

SharingIntegrationTest adds 16 real security/controller/JDBC integration cases using H2, not mocked
share services: CSPRNG format/digest persistence, durations, create/access/revoke/denied, exact-clock
expiry, immutable content after source edit/deletion, two-user management IDOR, foreign pack injection,
capability-as-JWT rejection, public ID manipulation/scope escape, citation projection and deletion,
pack deletion cascade, draft/expiry/forged field validation, malformed tokens, security headers,
limiter exhaustion/outage, safe audit actor/redaction, concurrent access/revoke, concurrent revoke,
and paginated metadata without capabilities. No test sleeps to simulate expiration.

Frontend adds 8 tests (37 total, zero failures/skips): request allowlists/expiry choices, owner revoke
and status, fragment-only links, actual local PNG QR generation with exact URL payload, credential-free
fixed-path POST, safe unavailable state, real React static rendering/XSS escaping with no edit controls
or evidence links, and safe error mapping. These do not simulate clicks, clipboard or mobile layout.
TypeScript, lint and production build passed on the resumed run.

PGlite applied actual V1–V10 with 40 existing invariants and zero persisted synthetic users; this is
not native PostgreSQL/Flyway verification. Nine prior migration files and 32 baseline Java/frontend
test source files match carepath-phase9.zip byte-for-byte. Production Next HTTP startup was executed
by spawning `node node_modules/next/dist/bin/next start --hostname 127.0.0.1 --port 3108` from frontend
and issuing a Node fetch to `/share` within the same process namespace: 200, Cache-Control no-store,
Referrer-Policy no-referrer, X-Robots-Tag noindex/nofollow/noarchive. The child was stopped afterward.
An earlier separate-command fetch could not reach another command's loopback namespace; it was not
counted as success. No rendered browser claim follows from HTTP or static React rendering.

Native PostgreSQL/Flyway/Redis, restart persistence, rendered browser/mobile, live LLM,
production TLS/cookie/CORS/CSRF runtime and email remain open. Share generation does not invoke
LLM or email. The public page permits inline Next bootstrap in CSP; nonce hardening remains open.
Review found no critical/high defect remaining in the scoped token/authorization/evidence/expiry/
logging implementation. Full backend outcome and per-suite aggregates are recorded in PROJECT_STATUS.md
and docs/verification/phase10-backend.json once the complete run finishes.

Final backend outcome: **266 tests, 0 failures, 0 errors, 0 skipped**, all 24 reports from this run;
250 retained plus 16 sharing tests. The package stage failed on an already-corrupt generated JAR
with `zip END header not found`. Removed only `backend/target/carepath-backend-0.1.0-SNAPSHOT.jar`
and its `.original`, then ran the same Maven/JDK command with `-DskipTests package`: **BUILD SUCCESS**.
That retry checked packaging without repeating the just-passed full suite; no test source or production
control was disabled. Compile checks passed in both invocations. Safe aggregate: phase10-backend.json.

## Phase 11 execution

Commands executed from the workspace root (logs kept out of source packages):

```sh
JAVA_HOME=/workspace/scratch/toolchains/jdk-21.0.12.1+1 /workspace/scratch/toolchains/apache-maven-3.9.11/bin/mvn -o -Dmaven.repo.local=/workspace/scratch/toolchains/m2 -f carepath/backend/pom.xml -B -DskipTests compile
JAVA_HOME=/workspace/scratch/toolchains/jdk-21.0.12.1+1 /workspace/scratch/toolchains/apache-maven-3.9.11/bin/mvn -o -Dmaven.repo.local=/workspace/scratch/toolchains/m2 -f carepath/backend/pom.xml -B '-Dtest=Nearby*Test' test
JAVA_HOME=/workspace/scratch/toolchains/jdk-21.0.12.1+1 /workspace/scratch/toolchains/apache-maven-3.9.11/bin/mvn -o -Dmaven.repo.local=/workspace/scratch/toolchains/m2 -f carepath/backend/pom.xml -B '-Dtest=*Test,ExtractionEvaluationIT,NormalizationEvaluationIT,LongitudinalEvaluationIT,FrontendProxyIT' package
npm --prefix carepath/frontend test
npm --prefix carepath/frontend run typecheck
npm --prefix carepath/frontend run lint
npm --prefix carepath/frontend run build
```

Focused backend: 30 passed, zero failures/errors/skips. New tests cover four categories, Haversine
boundaries, partial fields, unsafe web/phone values, key absence, fixed endpoint/header contract,
malformed bodies, HTTP401/429/500, timeout cancellation and actual streamed-body bound. Four API
integration tests use real authentication/security with H2 and the test limiter: unauthenticated
access, missing-key configuration/no-store, invalid/forged fields and account rate limiting.
These are not live Google or Redis verification. Frontend additions test explicit consent invocation,
denial/manual fallback, categories, missing fields, no-results, legitimate actions/optional booking,
XSS escaping, intended authenticated route allowlist and safe provider failure/minimized payload.
React static rendering is not a rendered browser/mobile test.

No baseline tests were modified or disabled. V1–V10 were compared byte-for-byte with Phase10.
No V11/schema change; no native database claim. No Google credential was configured, and live Google
requests were not attempted. Existing native/Redis/restart/browser/LLM/TLS-cookie-CORS-CSRF/email
verification gaps remain. Final complete regression results are recorded in PROJECT_STATUS.md.

Phase11 final closure: 296 backend tests and 46 frontend tests passed, zero failures/skips.
Backend compile/package, frontend TypeScript/lint/production build passed. Existing successful
regressions were reused on resume; a corrupt restored target JAR was rebuilt with `-DskipTests
package` after preserving the completed backend suite results. No source tests were disabled.
See PROJECT_STATUS.md and docs/verification/phase11-*.json for evidence and scope.
All native/provider/browser/deployment gaps remain open. Phase12 has not started.
Source checkpoint: `python3 scripts/package.py --phase phase11`.

## Phase 12 complete regression and hardening tests

Exact commands from workspace root:

```sh
JAVA_HOME=/workspace/scratch/toolchains/jdk-21.0.12.1+1 /workspace/scratch/toolchains/apache-maven-3.9.11/bin/mvn -o -Dmaven.repo.local=/workspace/scratch/toolchains/m2 -f carepath/backend/pom.xml -B '-Dtest=*Test,ExtractionEvaluationIT,NormalizationEvaluationIT,LongitudinalEvaluationIT,FrontendProxyIT' package
npm --prefix carepath/frontend test
npm --prefix carepath/frontend run typecheck
npm --prefix carepath/frontend run lint
npm --prefix carepath/frontend run build
```

New ProductionGuardTest (7) checks actual component startup with safe configuration and rejected
profile/origin/cookie/documentation/database credential combinations. New HttpSecurityIntegrationTest
(4) uses actual embedded HTTP transport for cookie rotation/logout/CORS/CSRF/share failure headers
and validates JdbcTemplate timeout configuration. Backend-origin frontend tests (2) cover legitimate
private origins and rejected credential/path/query/scheme configuration without secret disclosure.

The final frontend commands were repeated after the small stale landing-copy repair, not to increase
coverage counts. Complete backend tests retain the real Next proxy IT and extraction/normalization/
longitudinal evaluation ITs. Baseline296/46 sources are preserved. Final results and safe per-suite
reports are recorded in PROJECT_STATUS.md and docs/verification/phase12-*.json.

Native PostgreSQL17/Flyway/Redis/restart tests could not run: executables absent. Workspace production
Next served HTTP200, but remote CUA Chrome returned ERR_BLOCKED_BY_CLIENT for that workspace URL.
No rendered browser/mobile result is claimed. Live LLM/Places credentials and an email adapter are
absent. Secure-cookie headers over loopback HTTP with manually sent cookies are not browser/TLS
verification. No PGlite run is substituted for native verification. See docs/VERIFICATION_PHASE12.md.

Phase12 final result: **307 backend tests, 48 frontend tests; zero failures/errors/skips**.
Backend compile/package, frontend TypeScript/lint/production build all passed. V1–V10 and all
baseline tests/resources preserved; no migration added. Native services/restart, rendered browser/
mobile, live providers/email and production TLS/cookie/proxy gates remain open. Full traceability
and remaining product gaps are explicit audit inputs, not a production-readiness claim.
Package with `python3 scripts/package.py --phase phase12`. Phase13 has not started.


## FINAL Phase13 executed checks

Final complete backend command (frontend production build already present):

```sh
JAVA_HOME=/workspace/scratch/toolchains/jdk-21.0.12.1+1 \
/workspace/scratch/toolchains/apache-maven-3.9.11/bin/mvn -o \
  -Dmaven.repo.local=/workspace/scratch/toolchains/m2 -f carepath/backend/pom.xml -B \
  '-Dtest=*Test,ExtractionEvaluationIT,NormalizationEvaluationIT,LongitudinalEvaluationIT,FrontendProxyIT' package
npm --prefix carepath/frontend test
npm --prefix carepath/frontend run typecheck
npm --prefix carepath/frontend run lint
npm --prefix carepath/frontend run build
npm --prefix carepath/scripts/schema-check test
```

The frontend commands executed in the listed sequence before the full backend command; the schema
check ran afterward. Results:320 backend tests,0 failures/errors/skips;58 frontend tests,0 failures/
cancelled/skips/TODO; compiler, executable Boot package, TypeScript, ESLint and Next production build
all pass. The backend package log and Surefire XML agree. Safe summaries are retained under
`docs/verification/phase13-*.json`. Full raw logs remain ignored local verification artifacts.

New tests:

- ClosureIntegrationTest(10): safe Activity projection/category/page/isolation; cross-module literal,
  tag/month/date/provider search; trusted-only provenance; search limits/rate outage/auth/forged owner;
  five-type timeline order/status/ownership and exact confirmed-follow-up source/date.
- ClosureJourneyTest(1): connected synthetic reports→review→history/change/assistant→care/reminder→
  Visit Pack/PDF→public share/revoke→Activity. H2 and deterministic provider fallback, not browser/live.
- RequestCorrelationTest(2): preserve valid UUID and reject missing/malformed/newline metadata.
- workspace-client.test.ts(10): typed API/body minimization, evidence/care links, error behavior,
  actual auth-client route allowlist, onboarding/privacy/settings route/source semantics and selected
  symptom navigation. Static/client tests are not rendered component or accessibility tests.

All53 baseline migration/test/resource files compared against the Phase12 archive are byte-identical;
no previous test disabled/removed. Existing synthetic evaluation ITs reran and retain measured output
under evaluation/results; no clinical accuracy claim. Phase9 PDF visual inspection remains historical;
no changed PDF layout warranted a new visual claim. Native/service/browser gaps are unchanged.

SQL check: existing40 schema invariants plus two actual production-source query syntax/type checks
for search and five-type timeline pass against PGlite. This is NOT native PostgreSQL, Flyway or JDBC
runtime verification. No migration added; V1–V10 preserved. NoV11 is needed for query/UI additions.


### Closure artifact repair

A retained executable JAR was found truncated during final CRC inspection, despite the preceding
successful package log. With application/test sources unchanged, a packaging-only repair ran:
`mvn -o -Dmaven.repo.local=/workspace/scratch/toolchains/m2 -f carepath/backend/pom.xml -B -DskipTests -Dmaven.jar.forceCreation=true package`
using the same Java21/Maven toolchain. BUILD SUCCESS; rebuilt JAR79,861,933 bytes and full ZIP CRC PASS.
This invocation deliberately reused the already completed320-test regression; it is not a new
regression pass or a removed/disabled test. Final test totals remain320/58 with zero recorded skips.
The source archive excludes build artifacts. A secret-pattern scan matched only a PEM-header string
in AuthTestSupport around freshly generated ephemeral test keys, not an embedded credential.
