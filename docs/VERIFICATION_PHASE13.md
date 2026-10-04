# Final Phase 13 verification and release-candidate record

Date: 30 September 2026 (UTC). **RELEASE CANDIDATE: YES**, for the explicitly documented
implementation/test scope. **Not production certified or clinically validated.** No further
development phase is created. Native services, real providers and rendered-browser gates below
remain open. This is a repository review with automated tests, not a professional penetration test.

## Requirements closed

* Nonblocking four-step `/onboarding` guide, registration-success and existing-user entry links;
no profile collection, forced migration or medical questionnaire.
* `/activity` with real authenticated, owner-scoped, category-filtered and paginated audit API.
Only action, outcome, actor kind and time leave the API; internal metadata/IDs are omitted.
* Public `/privacy` explains security, sharing, deletion, external-provider and medical-safety
boundaries. Protected `/settings` shows actual account information and links to existing controls;
it has no fake preferences, account-deletion, MFA, email-delivery or password-reset controls.
* `/search` and POST `/api/v1/search` cover documents/tags, trusted observations, dates, providers,
symptoms and appointments. SQL filtering/pagination, English month tokens and literal AND terms
support CBC, hemoglobin and September prescription. No LLM or semantic-search claim.
* Existing timeline now combines verified observations, documents (including prescriptions),
user-reported symptoms, appointments/previous visits and confirmed/edited follow-ups.
Follow-up page evidence and appointment status/time are retained. Lab calculations are unchanged.
* Selected symptom deep links now fetch the owner-scoped item even when absent from the first page,
and refresh selected state after mutations. Landing/registration stale product claims were removed.
* Both outbound provider clients propagate only a validated opaque UUID request ID.

## Executed verification

No previously completed test was removed or disabled. Comparison against the Phase12 source archive
found **53 baseline migration/test/resource files byte-identical**, including all V1â€“V10 migrations.
New backend tests: ClosureIntegrationTest (10), ClosureJourneyTest (1), RequestCorrelationTest (2).
New frontend tests: workspace-client.test.ts (10). Earlier baseline:307 backend /48 frontend.

|Check|Executed result|Scope|
|-|-|-|
|Complete backend regression|**320 tests; 0 failures, 0 errors, 0 skips**|Spring security/API/JDBC/JPA integration on H2, deterministic rate-store port, units, local provider contracts, OCR/evaluation and Next proxy IT|
|Backend compile/package|**BUILD SUCCESS**|Maven package includes compilation and executable Boot jar|
|Complete frontend regression|**58 tests; 58 passed; 0 failures, cancelled, skipped or TODO**|Node client/contract/source tests; not rendered React/browser E2E|
|TypeScript|**PASS**|tsc --noEmit|
|Lint|**PASS**|eslint .|
|Production frontend build|**PASS**|Next production routes include all closure pages|
|SQL/schema check|**PASS**|PGlite applies V1â€“V10,40 existing invariants and production search/five-type timeline SQL syntax/type checks; NOT native Flyway/JDBC|
|Connected synthetic journey|**PASS**|ClosureJourneyTest, described below|

Exact backend command from workspace root (locally supplied JDK/Maven/cache):

```sh
JAVA\_HOME=/workspace/scratch/toolchains/jdk-21.0.12.1+1 \\
/workspace/scratch/toolchains/apache-maven-3.9.11/bin/mvn -o \\
  -Dmaven.repo.local=/workspace/scratch/toolchains/m2 \\
  -f carepath/backend/pom.xml -B \\
  '-Dtest=\*Test,ExtractionEvaluationIT,NormalizationEvaluationIT,LongitudinalEvaluationIT,FrontendProxyIT' package
npm --prefix carepath/frontend test
npm --prefix carepath/frontend run typecheck
npm --prefix carepath/frontend run lint
npm --prefix carepath/frontend run build
npm --prefix carepath/scripts/schema-check test
```

Commands completed before documentation closure; no source edits after the final complete backend/
frontend runs. Only the SQL-check harness and documentation/package files changed afterward. Focused
Closure\* tests previously passed11/11. Final counts come from Surefire XML and the saved Node report,
not the number of test classes or a started command. Safe summaries are in
`docs/verification/phase13-backend.json` and `phase13-frontend.json`.
The environment emitted an npm unknown `http-proxy` setting warning; all four frontend commands
completed successfully. No dependency or production security setting was weakened to obtain a pass.

## Synthetic connected journey and evaluation

ClosureJourneyTest registers/logs in a synthetic owner; uploads/processes January, April and September
PDFs; confirms/corrects candidate readings; checks Hemoglobin11.3â†’10.4 is DECREASED and September
source page2; checks deterministic assistant history includes12.1 and10.4; creates symptom,
appointment and confirmed document follow-up; delivers a due persistent reminder/notification;
checks five event types; saves a clinician question; previews/generates a Visit Pack and parses its
PDF for10.4 and source page2; creates a30-minute share; reads its snapshot; revokes and confirms
access denial; checks safe Activity output and another user's isolation.

This is an API/service-connected synthetic test, not a rendered UI or native restart. Existing
separate Nearby Care provider-contract tests execute the normalized search/failure path without
paid/live APIs. Existing extraction, normalization and longitudinal evaluation ITs reran; measured
fixture results remain under `evaluation/results/`. Their small synthetic denominators do not
establish clinical accuracy or general medical-document extraction accuracy. PDF visual inspection
from Phase9 is retained; no new Phase13 PDF visual inspection is claimed because layout was unchanged.

## Final architecture, security, privacy and safety assessment

|Boundary|Inspection/executed evidence and outcome|
|-|-|
|Authentication|Existing signed JWT issuer/audience/expiry checks, session revocation, BCrypt, refresh digest/rotation/replay, exact routes/default deny and cookie/Origin/CSRF tests retained. Production startup guard retained.|
|Ownership and relationships|Principal-derived ownership at API/service queries; new search UNION branches and Activity are each owner-scoped. New A/B, forged-field, pending-exclusion and source-deletion tests pass. Existing linked-object/pack/share IDOR suites pass.|
|File boundary|Existing magic/decoder/MIME/path/key/hash/resource controls retained; original/derived deletion and invalid/corrupt upload tests pass. No storage paths added to search/activity/UI.|
|Medical trust|Search/history only query trusted\_medical\_observation. Pending/rejected extraction rows do not enter trusted results. Care event UNION does not feed numeric trends. Source IDs/pages remain traceable. On-demand calculations avoid stale trend stores.|
|AI and safety|Existing bounded verified-lab context, opt-in external provider, citation/fact allowlist, deterministic fallback, prohibited treatment/diagnosis and limited predefined urgent-phrase rules retained and regression tested. No new clinical decisions or reference ranges.|
|Visit Pack/sharing|Existing typed immutable snapshots, safe PDF text generation, digest-only256-bit random capabilities, per-access expiry/revoke locks, scope-restricted public DTO, no original file access and safe headers retained. Tests cover snapshot edits, scope escape and revoke/expiry/concurrency.|
|Nearby Care|Consent/manual location only, transient coordinates, secret backend API key, trusted fixed provider endpoint, bounded radius/request/body, URL validation, plain text and no invented booking fields retained. Provider contracts reran; no live data used.|
|Web and logs|New UI uses React text escaping; no provider HTML, unsafe Markdown or raw HTML rendering. Search text is a bounded JSON body, not a query-string URL. New APIs use existing authenticated safe-error/security-filter path. Audit projection contains no medical notes/tokens/coordinates. Only operational outcome/duration is explicitly logged by assistant code. Deployed proxy/APM logging is still unverified.|
|Correlation|Validated UUID-only X-Request-ID added to Places/LLM clients; malformed/newline MDC content replaced with a random UUID. No user IDs, prompts or coordinates attached.|
|Privacy/deletion|No new external data transmission, location persistence or medical profile. Real demo data absent; supplied fixtures labelled synthetic. Original deletion removes live access/evidence; independently generated snapshots must be deleted separately. Revocation cannot erase recipients' prior copies.|

**No unresolved critical/high security finding was identified in this repository-level review.**
This statement is scoped to inspection and executed tests; it is not assurance that vulnerabilities
cannot exist. No live penetration test, dependency-advisory certification or deployed edge review
was performed. Medium/operational limitations: frontend CSP is not a complete nonce-based deployment
policy; upload/preview parser allocations are not an OS sandbox; encryption at rest, least-privilege
DB roles, secret rotation, retention, backup/restore, quotas and incident response require operator
configuration. These are explicit deployment gates, not silently â€œverifiedâ€ controls.

## Performance and UI assessment

Search is capped at80 characters/eight terms, page20/maxpage10000, JSON body8KiB,60 requests per
account per configured rate window and existing fail-closed Redis port. SQL filters/counts before
pagination; no per-result lookup loop. Timeline uses SQL union/count/page and one batched evidence
lookup. Existing owner/date/job/digest indexes, JDBC statement deadline, worker/page/pixel/time limits,
bounded LLM/PDF/provider contexts and transactional reminder claim/idempotency are retained.
No new index/schema was justified without a native query plan. Large-scale substring search and
operational metrics export remain limitations; no throughput/load benchmark is claimed.

Required routes have implemented targets and existing navigation. New forms have labels, buttons
have meaningful text, workspace links form a named navigation landmark, and loading/error/empty
states are explicit. This is code-level accessibility review and build/client verification.
Keyboard/screen-reader behavior, actual responsive layout, charts, QR scanning and visual contrast
still require rendered desktop/mobile execution. No global redesign was performed.

## Traceability and all remaining limitations

All214 rows were reassessed; Phase12's matrix is retained separately.

|Classification|Count|
|-|-:|
|IMPLEMENTED + VERIFIED (stated automated scope)|159|
|IMPLEMENTED + LIVE VERIFICATION PENDING|44|
|PARTIAL|7|
|INTENTIONALLY DEFERRED|3|
|NOT IMPLEMENTED|0|
|NOT APPLICABLE|1|

Every remaining PARTIAL item:

* **11.01,11.02:** assistant questions/plain-language explanations are bounded to verified structured
laboratory facts and supported templates, not arbitrary narrative prescription/discharge reports.
* **26.01,26.02,26.03,26.04:** named domains/persistence exist, but the original literal all-JPA wording
is only partially satisfied. User uses JPA; most modules intentionally use typed JDBC repositories.
Rewriting working persistence solely for that wording would add risk without product benefit.
* **30.04:** internal metrics exist; a production authenticated collector/exporter is not provisioned.
Public metrics remain closed.

Every intentionally deferred item:

* **05.03:** LOINC assignment requires reliable specimen/method/context; no unsupported mapping added.
* **12.03:** optional symptom context is excluded from the assistant to preserve the verified-lab boundary.
* **20.05:** dedicated direct-booking provider integration deferred; legitimate external actions exist.

**17.06 NOT APPLICABLE:** automatic invalidation of historical generated packs was superseded by the
explicit later immutable snapshot requirement. Live source deletion still prevents original access.
No wholly NOT IMPLEMENTED row remains. Optional S3/email-vendor implementations are absent; real local
storage and explicit unavailable email port exist. Onboarding is nonblocking/no persisted completion;
search is literal English/month/date matching; no separate prescription-treatment domain exists.

## Live-verification gaps retained

|Check|Final status and reason|
|-|-|
|Native PostgreSQL17/Flyway V1â€“V10|BLOCKED: Docker/postgres/psql executables unavailable; PGlite is not a substitute|
|Redis runtime|BLOCKED: redis-server/redis-cli unavailable; rate-store contract tests are not Redis runtime|
|Native restart persistence|BLOCKED by native services; durable schema/transactions tested in available harness only|
|Desktop/mobile rendered browser|BLOCKED: no local browser runtime; previous remote browser could not reach workspace (ERR\_BLOCKED\_BY\_CLIENT). Not retried|
|Live LLM|PENDING / NOT CONFIGURED: no legitimate key; deterministic fallback and local provider contracts pass|
|Live Google Places|PENDING / NOT CONFIGURED: no legitimate key; contract tests pass|
|Production TLS/browser cookie enforcement|PENDING: existing actual loopback HTTP security/proxy tests pass; no HTTPS deployed browser check|
|Email delivery|NOT CONFIGURED: interface/disabled implementation only; in-app notifications work in tests|

Availability was checked once in Phase13; no credentials invented, infrastructure simulated as live,
or repeated installation attempts made. Tesseract and the local Java/Node toolchains are available.

## Repository, documentation and release gate

Documentation updated: PROJECT\_STATUS, README, ARCHITECTURE, SECURITY, API, TESTING, .env.example,
final214-row traceability plus preserved Phase12 matrix, this report and safe machine-readable
verification summaries. No new environment secret or schema migration is needed. V1â€“V10 unchanged;
no V11 introduced. Packaging excludes credentials/private keys, local storage, dependencies, caches,
logs, test work directories and compiled outputs. Synthetic fixtures, evaluation results and fonts
with their existing license information remain legitimate source artifacts.

The final source archive and jar are CRC-checked during closure. The archive contains no .env,
private keys, target/node\_modules/.next/.data or raw verification logs. Baseline test resources and
migrations are byte-identical; regression counts grew by13 backend and10 frontend tests. Phases1â€“12
remain preserved within the executed scope. No critical TODO in a supposedly completed production
path or sensitive logging statement was identified.

**Release gate: YES â€” RELEASE CANDIDATE.** Complete regression/builds pass, implementation gaps
closed or precisely documented, no unresolved critical/high repository finding identified, and
external blockers remain visible. Do not deploy real patient data on the strength of synthetic
tests alone. Complete the open operational/native/browser checks in the target environment before
public production use. This closes Phase13; no Phase14 or additional feature work is planned.



### Closure artifact repair

A retained executable JAR was found truncated during final CRC inspection, despite the preceding
successful package log. With application/test sources unchanged, a packaging-only repair ran:
`mvn -o -Dmaven.repo.local=/workspace/scratch/toolchains/m2 -f carepath/backend/pom.xml -B -DskipTests -Dmaven.jar.forceCreation=true package`
using the same Java21/Maven toolchain. BUILD SUCCESS; rebuilt JAR79,861,933 bytes and full ZIP CRC PASS.
This invocation deliberately reused the already completed320-test regression; it is not a new
regression pass or a removed/disabled test. Final test totals remain320/58 with zero recorded skips.
The source archive excludes build artifacts. A secret-pattern scan matched only a PEM-header string
in AuthTestSupport around freshly generated ephemeral test keys, not an embedded credential.

## Post-closure local verification â€” 4 October 2026



Additional local verification was performed after the original Phase 13

release-candidate closure. This does not create Phase 14 and does not replace

the original Phase 13 regression evidence.



\### Nearby Care data verification



Nearby Care was exercised locally using an OpenStreetMap-derived healthcare

facility dataset generated from the current Geofabrik India central-zone

extract.



The generated dataset contained 9,767 healthcare facilities:



\- 6,635 hospitals

\- 2,014 clinics

\- 952 pharmacies

\- 166 diagnostic centres



For the tested Dehradun location, 89 hospitals were present within a 20 km

straight-line radius. The application successfully returned and ranked nearby

providers by distance, including local hospital results.



The UI successfully displayed provider name, approximate distance, available

address metadata, OpenStreetMap attribution and directions links. Missing

opening-hours/status information remained explicitly unavailable rather than

being fabricated.



This verifies the local OpenStreetMap-backed Nearby Care path. It does not

constitute verification of Google Places or guarantee completeness or current

accuracy of individual OpenStreetMap facility records.



### Local build verification



The frontend Next.js production build completed successfully, including

TypeScript compilation, static-page generation and all expected application

routes.



The backend packaging command:



`mvn -DskipTests package`



completed with BUILD SUCCESS and generated the executable Spring Boot JAR.



A complete Windows Maven regression was also attempted. It reached 311 tests

but encountered environment-dependent failures involving Windows symbolic-link

privileges and OCR runtime behavior. These results do not replace the original

Phase 13 complete regression evidence. Tesseract was subsequently installed

and verified locally, and a Linux/WSL environment was prepared for Unix-specific

runtime verification.



No production certification or clinical validation is claimed by these

additional checks.


