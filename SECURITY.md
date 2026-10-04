# CarePath threat model and final security review

Final Phase13 assessment is below and in docs/VERIFICATION_PHASE13.md. Earlier phase tables
are historical; production deployment and clinical-data handling gates remain open.
No HIPAA, GDPR, ABDM or other regulatory compliance is claimed.

## Assets

Original medical files, extracted text and observations, symptoms/prescriptions/appointments,
Visit Pack snapshots/PDFs, location, account credentials, refresh/share tokens, provider keys,
audit trail and backups. Metadata and filenames can themselves reveal sensitive health details.

## Actors and trust boundaries

Unauthenticated attackers, malicious authenticated users, public share recipients, uploaded
malicious documents, compromised third-party services and accidental operator misuse.
Boundaries: browser/API; authenticated owner/other owners; parser/untrusted file; worker/storage;
LLM/data instructions; app/external provider; share/owner scope; runtime/operator/backups.

## Historical Phase1 threat/control matrix

| Surface / threat | Implemented in Phase 1 | Required before feature release |
| --- | --- | --- |
| Open API / privilege escalation | Default-deny Spring Security, no generated user, no fake login | JWT validation, BCrypt, rotation/reuse detection, principal-scoped access, A/B tests |
| Cross-user linked data | Composite owner FKs, typed pack items | Scope every query and download; verify all nested references |
| CSRF/CORS | CSRF enabled; exact configurable GET/OPTIONS origin; no credentials | Bearer/cookie flow, refresh/logout CSRF and Origin tests |
| XSS/clickjacking | React escaping, nosniff, frame denial, referrer policy | Strict nonce CSP for frontend, sanitize markdown/links, safe PDF preview origin/sandbox |
| Upload/path traversal | Opaque key DB constraint and multipart size configuration only | Magic/decoder validation, page/pixel limits, parser isolation, safe filenames, no symlink escape, malicious-file tests |
| Token theft | Schema stores digests, no token endpoints | 256-bit randomness, cookie flags, expiry, revoke/reuse, redact URL/token logs |
| Prompt injection / hallucination | No model access in Phase 1 | Delimited untrusted evidence, bounded structured retrieval, no tools, citation validation, refusal tests |
| Data corruption / false precision | Numeric values, separate originals, confidence/state fields, evidence consistency FKs | Vetted aliases, context/unit rules, review gate, correction history, evaluation |
| Job loss / duplicate reminders | Durable job/reminder tables, idempotency uniqueness | Transactional enqueue/claim, leases, retries/backoff, restart/duplicate-delivery tests |
| Share leakage | Scoped pack FK, expiry constraint and token digest schema | Read-only anonymous endpoint, per-request revoke/expiry check, QR scope, no-store/no-referrer, no analytics |
| DoS / brute force | Connection and multipart limits; closed product endpoints | Redis rate limiting, quotas, parser/LLM budgets and failure policy |
| Injection | No SQL/API accepting user input yet; migration constraints | Parameterized queries, Bean Validation DTOs, no shell/user SQL interpolation |
| Secrets/exposure | No embedded credentials; .env ignored; loopback DB/Redis ports, required passwords | Production secret manager, TLS, encrypted storage, least-privilege runtime/migration roles |
| Logs/errors | ECS JSON, generated request IDs, no request-body logging, hidden error details | Security event writes without PHI, restricted retention/access and proxy redaction |
| Deletion/backup leakage | FK lifecycle documented | Revoke/invalidate shares/packs, purge derivatives/blobs, retention + restore procedures |
| Dependencies | Pinned dependency definitions and frontend lockfile | Ongoing SCA/license review, image digests/refresh policy, CI security gates |

## Controls present versus planned

Phase 2 implements registration/JWT/session rotation, auth rate limiting and audit infrastructure.
Document uploads and scoped temporary sharing are implemented (Phase3/Phase10 reviews below). The table above records the Phase1 baseline;
the Phase 2 review below supersedes its authentication rows.
Do not cite schema constraints or dependencies as proof those controls work end-to-end.
Foundation security tests cover public status, deny-by-default, origin policy and response headers.
A/B ownership tests belong to the phase that introduces each domain API.

## Operational assumptions and limitations

- Compose credentials are environment-based; local volumes and traffic are not encrypted by this repository.
- PostgreSQL's Compose user owns the schema and is not a least-privilege production runtime role.
- Malware scanning and upload/preview OS isolation, retention/backups, secrets rotation, deployed
  nonce CSP and incident response remain operational limitations. The extraction worker has a
  bounded separate JVM; provider/location consent is implemented but rendered behavior is unverified.
- Local Swagger is a developer convenience with a separate documentation-only CSP. Do not enable
  the local profile on a public deployment or loosen production API CSP for documentation.
- Limited predefined urgent-phrase escalation and source attribution exist; clinical review remains
  necessary before claiming a validated triage system. CarePath does not provide one.
- A human can copy a shared PDF; revocation prevents future access, not recovery of copies.
- Even perfect evidence retrieval does not prove clinical correctness. CarePath cannot replace clinicians.

## Release security gates

1. Register/login/logout/refresh replay and expiry tests; generic errors; rate-limit outage policy.
2. User A cannot access User B's documents, observations, appointments, packs or share management,
   including nested links, downloads, filters and guessed IDs.
3. File traversal/content mismatch/parser resource exhaustion and prompt-injection fixtures.
4. Share create/access/revoke/denied and create/expire/denied using an injected clock.
5. Original deletion prevents live derivative/evidence access; generated snapshots are independent
   historical copies under the later Phase9 requirement. Delete packs separately. Backup retention
   and restore require deployment verification.
6. Secrets/log inspection, dependency scanning, HTTPS/cookies/CSP and least privilege reviewed.

Report vulnerabilities privately to the repository maintainer; do not include patient records or secrets.
No monitored security email or disclosure SLA has been established yet.

## Phase 2 security review

| Review area | Implemented decision / review finding |
| --- | --- |
| Plaintext passwords | BCrypt cost 12 default; no password persistence or logs; UTF-8 bound prevents silent truncation |
| Plaintext refresh persistence | Only SHA-256 digests; raw value appears only in an HttpOnly Set-Cookie response |
| Secrets | No hardcoded signing key; RSA PEM paths and HMAC rate secret required; .data/.env/PEM/key files excluded from source/archive |
| JWT validation | RS256-only signature, issuer, audience, exp/nbf/iat, UUID subject/session and lifetime validation |
| Revocation/bypass | Every bearer request checks active owner/session; cookie alone cannot access protected APIs |
| CSRF | Exact Origin plus mandatory custom header for all auth mutations, including login/logout/refresh |
| CORS | One validated frontend origin; explicit headers/methods, no wildcard origins |
| Validation/mass assignment | Explicit DTOs, unknown fields rejected, Bean Validation, 8 KiB actual-body bound including chunked input |
| Ownership | SecurityContext principal only; owner-scoped repository contract, generic foreign-resource 404; later APIs remain denied |
| Refresh replay | Session lock, reread after lock, one descendant constraint; reuse revokes family in committed transaction |
| Cookie injection | Production __Host prefix, Secure, HttpOnly, SameSite=Lax, no Domain; HTTP cookies permitted only for loopback setup |
| Logging | Content-free typed audit, safe errors, SQL value logging disabled; no auth body/token loggers |
| Rate limiting | Atomic Redis Lua counters, HMAC identifiers, time-limited keys, fail closed on outage |
| Open endpoints | Only explicit auth routes, system/health and opt-in local docs; no blanket authenticated wildcard enables future modules |

Fixed during review: bounded chunked request bodies, explicit JWT claim requirements, production
host-prefixed cookies, committed replay revocation (no rollback-on-error bug), serialized rotations,
duplicate signup race handling, and suppressed Hibernate constraint logging that could expose values.

Remaining limits: native PostgreSQL/Flyway/Redis live verification; full browser automated UI coverage;
frontend nonce CSP; email verification/recovery/MFA; signing-key rotation/key-ring operations;
retention/pruning for expired sessions and failed-auth audit; production TLS, encrypted volumes and
least-privilege DB roles. Redis IP buckets aggregate clients behind the current Next proxy; trusted
edge limits are needed before public traffic. Proxy/backend body and cookie logs must remain disabled.
No claim of production readiness, external penetration testing or regulatory compliance.

## Phase 3 review and controls

- IDOR: every document query, mutation, lock, preview and download uses authenticated owner. Generic
  404 for absent/foreign IDs. Both directions of real API/security A/B isolation are tested.
- Traversal/arbitrary read/delete: original filenames never determine paths; opaque random keys,
  strict key grammar, no-follow opens, exclusive create, private root, symlink/root checks.
- Upload spoofing: independent service byte limit, extension/MIME/signature consistency, strict PDF
  parse/image decode, active-PDF rejection, page/pixel/decompressed-stream bounds. No executable type.
- Stored XSS: React text rendering, JSON DTOs, no raw HTML injection. PDF preview delivers rendered
  PNG pixels; originals are attachments. Framework Content-Disposition escaping, explicit MIME/length,
  nosniff, no-store and existing CSP/frame/referrer policies remain enabled.
- Mass assignment: explicit metadata DTOs plus version; unknown JSON fields rejected. Server-owned
  identity/hash/path/status/upload fields cannot be set by clients. PUT/DELETE were added to exact-origin
  CORS; no wildcard origin or cookie-based vault authentication was introduced.
- Integrity/audit: size/SHA-256 verified before bytes return. Typed document events contain owner,
  document UUID, action/outcome/request ID, never filenames, raw contents or credentials. PDFBox
  logging is disabled to prevent untrusted parser details reaching application logs.
- Consistency: durable upload intents, transactional metadata/tags/audit writes, independent deletion
  outbox, idempotent retries, row locks and refusal to delete live keys. See docs/DOCUMENT_VAULT.md.

Review fixes: avoided recursive PDF object traversal (stack exhaustion), capped decoded streams and
embedded pixels, normalized duplicate tags before insertion, serialized concurrent deletion, kept
cleanup independent of the deleted parent, and prevented frontend metadata from echoing server-owned
response fields. Regression tests cover these paths where described in the status ledger.

Remaining: native PostgreSQL/Flyway/Redis checks; rendered browser tests; S3 adapter; malware scanning
and parser-process isolation with enforceable CPU/memory timeouts; storage quotas; encrypted volumes,
TLS and filesystem-parent trust/Windows ACL setup; backup retention/purge; cleanup backlog alerts and
production privacy review. Parsing/decoder bounds do not prove immunity to all malicious files.
No third-party penetration test or production-readiness/compliance certification is claimed.

PDFBox stream caches additionally use a 50 MiB bounded in-memory ScratchFile; decoded-size checks
do not imply a hard limit on all parser/renderer allocations. Isolation of those Phase 3 upload/preview entry points remains a deployment gate; the Phase 4 extraction boundary below uses a separate bounded JVM.

## Phase 4 review — document intelligence

Extraction APIs use the same validated JWT/session identity and owner+document scoped queries.
Cross-user process/status/extraction/evidence/retry requests return generic 404, including candidate
IDs paired with another document. Both ownership directions are API integration tested.
Original integrity is verified before work; candidates cannot exist without source page/document/
owner foreign keys. The entire result is validated before atomic persistence; no trusted medical
observation is created.

OCR uses ProcessBuilder argument arrays, never a shell, with random private staging paths and an
operator-only executable setting. User filenames/content never become command arguments. Child JVM
heap/direct memory, total wall deadline, OCR deadlines, page/pixel/text/output/candidate limits and
descendant termination bound exposure. Private staging is cleaned normally and stale directories
are reaped after an hour; symlink traversal is rejected/tested. Original storage remains unchanged.
Leases and random fencing prevent duplicate/stale result publication. Content-free audit and metrics
retain request correlation. Document strings are escaped React text, never injected HTML.
Prompt-injection fixtures remain ordinary untrusted text; no LLM runs in Phase 4.

Security tests exercise IDOR, malformed/oversized/compressed input, unavailable OCR, actual hung OCR,
child timeout, stale leases, integrity failure, staging symlinks and deletion cascades.
The security review fixed false-positive prose parsing, provenance validation before publication,
cross-document job linkage, stale-worker success accounting, and worker SYSTEM actor attribution.

Limitations: the worker is not an OS sandbox, has no cgroup CPU/total-RSS/disk quota or seccomp/network
policy, and inherits the service OS identity. An exploitable parser still requires defense in depth.
Vault upload/preview parsing remains in-process under existing Phase 3 limits; extraction isolation
does not close that older deployment gate. Temporary cleanup may lag a crash by one hour; use encrypted
volumes. No antivirus, clinical validation, public production security audit or regulatory compliance
is claimed. Native PostgreSQL/Flyway/Redis and rendered-browser checks remain pending.

## Phase 5 security review — normalization and human decisions

Sensitive assets now include effective/corrected readings and private verification reasons. Trust
boundaries remain browser → validated JWT/session → owner-scoped service → database/private storage.
Terminology is operator-controlled migration data; clinical equivalence is not inferred from a user
string. Corrected values and manual concept choices are explicit user data, not clinician certification.

Controls: ID+owner queries for candidates/observations; generic foreign/absent 404; closed route allowlist;
unknown-field rejection; 8 KiB bounded review JSON; Bean Validation; precise bounded numeric parsing;
active concept lookup; concept-specific compatible unit rules; null output on unsupported conversion;
React text rendering; no document text or review reason in application/security logs. Existing
password/JWT/session/refresh/CORS/CSRF controls are unchanged.

Concurrency: document then candidate row locks, unique source candidate, atomic observation/source/
verification/audit transaction, expected version and canonical payload hash. Identical replay is
idempotent; stale or changed replay and terminal state changes fail. Review cannot reassign source
or actor. Evidence substring/offset and original field membership are checked before promotion.
Original candidate fields are never writable by review APIs. Trusted reads require the accepted
candidate, matching human history and source; legacy AUTO_VERIFIED is insufficient.

Actual integration tests cover both-direction review IDOR, forbidden observation writes, forged owner/
actor/confidence/source/status fields, arbitrary concept IDs, invalid units/numbers/ranges, duplicate
and concurrent confirmation, stale correction, tampered evidence/value, body bounds and cascade deletion.
Additional fixes during review: preserve concept-name snapshots on read; bound review request bodies;
exclude unsupported offset rules and abstain on conflicting unit definitions; keep original/effective
fields separate; invalidate correction previews after UI edits; display only safe mapped error strings.

Remaining: native PostgreSQL lock/Flyway behavior, real Redis rate limits and rendered-browser checks;
clinical terminology review and broader extraction evaluation; no administrator-resistant database
immutability or tamper-proof audit archive. A compromised database operator can mutate data, and a user
can make an incorrect attested correction. Verification is not a medical correctness guarantee.
No regulatory compliance, clinical accuracy or external penetration-test certification is claimed.
Earlier parser isolation, encryption/backup/OS sandbox and deployment limitations remain open.

## Phase 6 review and boundaries

Timeline, concept history, comparisons and evidence use authenticated owner-scoped trusted-view queries.
Cross-user IDs yield generic 404; public terminology IDs never grant access to another user's history.
Bidirectional two-user API tests cover history, changes, evidence and original document access.
Pending/rejected/unverified records are excluded at the database view; joins repeat owner predicates.
There is no client userId authority, entity mass assignment, new secret or new token storage.
Search uses bound parameters and literal wildcard escaping; text is React-escaped, and evidence links
are internal authenticated routes. No raw medical logging was added. BigDecimal and bounded queries
protect numerical accuracy and query size; chart conversion does not determine comparison outcomes.
No derived cache survives source correction/deletion. Existing vault and auth controls are unchanged.
Same-day ambiguity, incompatible context, changed reference intervals and unsupported units abstain.
Review fixed shared frontend error handling so successful event loading cannot hide concept/history
failures. Remaining risks: unknown assay/specimen context, limited parser/catalog coverage, and untested
native database concurrency/Redis and rendered browser behavior. No compliance or clinical accuracy claim.

## Phase 7 assistant security review

External model input is limited to selected verified fact statements and computed changes with ephemeral
references. The raw question, source snippets/documents, identity, filenames and database IDs stay local.
Per-request opt-in, disabled-by-default provider, HTTPS-only endpoint, disabled redirects, single-call
policy, owner rate limit, concurrency semaphore, total deadline and streaming byte limits bound exposure.
The output schema permits only phrase selections: no arbitrary medical text, HTML, links or provider
citations can cross the server validator. Unknown fields, missing/extra facts, wrong evidence and invalid
phrase indices cause deterministic fallback. This is a constrained AI wording feature, not open-ended
medical reasoning. Document injection fixtures and adversarial provider outputs are exercised in tests.

Saved questions use security-context ownership, trusted evidence validation, composite foreign keys,
optimistic edits and generic foreign-resource 404. Original-file deletion detaches evidence and marks
the remaining user draft as missing evidence. No source text, prompt/response or draft enters audit/logs.
Plain React text prevents HTML/Markdown execution. Source revalidation after provider work avoids serving
an answer from a changed/deleted trust snapshot. Existing authorization and vault controls are retained.

Review fixes: removed a provisional HTTP test-endpoint exception before verification; tests inject a
controlled transport while production always requires HTTPS. Explicit years now constrain retrieval.
Removed duplicate/obsolete env placeholders. Corrected H2 timestamp syntax and supplied the extraction
foreign key in the new SQL fixture without weakening production constraints.

Limits: English keyword/phrase safety is not complete triage or clinically certified. Unknown wording
may receive an evidence-only response, not a definitive safety decision. Clinical review is required.
Provider retention/legal terms and endpoint configuration require operator review before real data use.
No real-provider call, native Redis/PostgreSQL/Flyway or rendered-browser behavior has been verified here.
Earlier deployment, encryption, backup, parser and dependency-review limitations remain open.


## Checkpoint V1 — 2026-09-24

Available regression verification passed: 203 backend tests, 20 frontend tests, both builds, TypeScript and lint. Actual frontend HTTP returned no-store, nosniff and DENY headers. Native-service, rendered-browser/mobile and live-provider gates remain open. No production security controls changed. See `docs/VERIFICATION_CHECKPOINT_V1.md` for exact commands, execution boundaries and blockers.

## Phase 8 review and controls

Care endpoints retain bearer authentication, default-deny route matching, existing refresh-cookie/CSRF
boundaries and generic owner-scoped 404 responses. Mutation DTOs expose no owner, delivery state or
source provenance; the existing strict JSON mapper rejects unknown fields. Bodies are limited to 8 KiB.
Resource IDs are UUIDs and all linked documents/symptoms/questions are checked against the current owner
inside the transaction. Composite foreign keys add defense in depth. SQL values are bound parameters;
dynamic table/column names are internal constants, never request values.

Version checks and row locks prevent stale edits/double confirmation. Scheduler lock order is source
then reminder; unique source-offset and notification-reminder constraints guard duplicate delivery.
Notification text is generic and includes no symptom, clinician or document details. Reminder status
and delivered timestamps are server controlled. Audit events contain typed actions/resource IDs only.
Follow-up source text is untrusted data; the bounded parser recognizes scheduling lines only and never
executes content or sends it to an LLM. Symptoms remain user reported and excluded from medical facts.
HTTPS URLs reject credentials/non-web schemes and are never fetched by the backend. React renders
notes/instructions as text; no unsafe HTML/Markdown path was introduced.

Regression exposed a process-namespace portability defect: the isolated extraction child could not
see its parent PID and exited early. A supervisor-owned stdin EOF guard replaces that lookup; loss of
the pipe exits 75, and the independent total deadline/heap/OCR limits remain. A subprocess regression
verifies termination when the pipe closes. Child diagnostics remain discarded; no raw document logs.
The timezone integration test also caught Jackson normalizing offsets before zone validation; the two
care offset fields explicitly preserve request offsets, retaining mismatch/DST-gap rejection.

Remaining gates: native PostgreSQL/Flyway V1–V8, Redis, true persistence/restart, rendered desktop/mobile
UI, production TLS/cookie/CORS/CSRF runtime and live LLM provider. No email adapter is configured or
claimed. Native locking/query performance and clinical safety review remain deployment gates.
Deletion and rescheduling intentionally cascade current reminder notifications; see CARE_ORGANIZATION.md.
No regulatory compliance or guaranteed emergency notification delivery is claimed.

## Phase 9 security review

Visit Pack APIs use validated identity, explicit DTOs and owner-scoped pack/source queries.
Tests exercise both-user IDOR, foreign relationship injection, unverified candidate exclusion,
immutable snapshot mutation, stale preview/version and concurrent generation. API serialization
conflicts return 409 without exposing database internals. Generated evidence is historical data;
following a source link still requires current document ownership. There is no public PDF route.

PDFBox receives plain text only; no HTML parsing, remote URL fetching or script/action creation.
Constant attachment names and no-store/nosniff protect downloads. Bundled fonts and bounded
items/text/pages/output/concurrency limit resource use; the deadline is cooperative, not process
isolation. Browser output uses React text rendering, not unsafe Markdown/HTML. Audit events omit
medical contents. Generated snapshots intentionally survive source deletion; users must delete
packs separately, and downloaded files cannot be remotely erased. See docs/VISIT_PACKS.md.

No critical/high finding remains from this phase's code/test review. This is not a penetration
test or compliance claim. Native PostgreSQL/Flyway, Redis, restart persistence, rendered browser,
production TLS/cookie/CORS/CSRF and external delivery/provider gates remain open.

## Phase 10 scoped security review

Reviewed actual token generation/persistence, routes, DTO projection, owner predicates, evidence
access, expiry/locking, cache/referrer/indexing, React rendering and log/audit call sites.
256-bit SecureRandom tokens are stored only as SHA-256 digests. Raw tokens/full links are returned
only to the creating owner and kept in UI memory; fragments stay out of HTTP request URLs. No body
or response logging was introduced. Operator-controlled proxy/APM capture must remain disabled.

Share reads validate current server time/revocation/generated revision every time, under the same
pack-first locking order as revocation/deletion. Exact expiry is denied. Public reads disclose only
the frozen snapshot and textual historical citations, not identifiers, original files or live APIs.
A capability is never a JWT. All management uses current authenticated owner; foreign links are
rejected. Anonymous responses are no-store/no-referrer/noindex with existing CSP/nosniff/frame denial.
The page CSP permits Next inline bootstrap; nonce hardening remains open. QR generation is local.

Tests cover bidirectional IDOR, ID manipulation, scope escape, evidence isolation, malformed tokens,
clock expiry, immediate revocation, concurrent access/revoke, repeat revoke, audit redaction,
rate-limit failure and escaped static React output without edit/navigation controls. This is a
scoped code/test review, not external penetration testing or native concurrency certification.
No unresolved critical/high finding identified within this reviewed Phase 10 scope.

Limitations: possession grants access and recipients can forward/copy contents. Revoke cannot erase
prior disclosure. UI rechecks every 15 seconds and clears on hiding/expiry/failure, subject to browser
timing. Historical copies persist until the pack is deleted. Proxy IP buckets may aggregate users;
edge deployment policy, retention/quotas, native services/restart and rendered-browser verification
remain open. See docs/TEMPORARY_SHARING.md.

## Phase 11 — Nearby Care

The `nearby` module provides authenticated, transient Google Places API (New) searches behind
`NearbyCareProvider`. Category mapping: HOSPITAL → hospital, CLINIC → medical_clinic, PHARMACY
→ pharmacy, DIAGNOSTIC_CENTER → medical_lab. The explicit field mask requests names, addresses,
location, phone, current hours, website and attribution only. Google booking URLs/slots are not
provided by this integration; bookingUrl remains null. The normalized optional field is the
boundary for a future legitimate appointment-access provider. No LLM or medical history is sent.

The page requests browser location only on Use my location; manual decimal coordinates are also
supported. Search explicitly consents to sending coordinates/category/radius to Google via the
backend. No watchPosition, coordinate persistence, cache, analytics, audit payload or request/body
logging is introduced. Coordinates remain in page memory until navigation. Deployment proxies/APM
must not capture these bodies. Google has its own privacy/retention policies linked in the UI.

Search radius is user-selected, bounded to 100–20,000 metres; maximum 20 provider-ranked results.
Haversine with mean Earth radius 6,371,008.8m produces approximate straight-line distance, never
driving distance. Directions use Google Maps destination links and provider place IDs, without
including the user's origin. Missing location means no distance/directions; missing hours, phone
and website stay absent. External HTTP(S) URLs reject credentials and local/numeric hosts; phone
links allow only normalized digits and a leading plus. No server fetch of facility URLs occurs.
Google Maps and supplied third-party attributions appear as escaped text with safe links.

Provider endpoint is fixed HTTPS; redirects disabled. Timeout defaults to 8 seconds (1–15),
response body capped at 256 KiB while streaming, four concurrent provider calls maximum, no retries.
Ten searches per authenticated account per existing configured rate window (normally 60 seconds)
use the existing HMAC-keyed rate limiter and fail closed on limiter outage. Requests have the
existing 8 KiB body bound. Output is React text, no provider HTML. Responses are no-store.
No tables or V11 migration are needed; V1–V10 remain unchanged.

Configuration: backend-only GOOGLE_PLACES_API_KEY (blank disables provider),
PLACES_TIMEOUT_SECONDS=8. Enable Places API (New), billing and operator quota/key restrictions.
Phone/hours/website field selection can incur Enterprise Places charges. Load environment into
the backend process using the existing launch instructions; never use NEXT_PUBLIC for the key.
Absent/invalid key, timeout, malformed response, quota or HTTP failure returns a safe 503; rate
limit returns 429; invalid input returns 400. Other CarePath modules remain independent.

Limitations: coverage and opening information depend on Google, results are not exhaustive, manual
fallback is coordinates rather than geocoded addresses, no map/route calculation/direct booking,
no cached offline results. Provider terms/attribution and deployment privacy notices require operator
review before public deployment. Live Places verification is PENDING: no credential configured.
Native PostgreSQL/Flyway, Redis, restart persistence, rendered-browser/mobile, live LLM, production
cookie/CORS/CSRF and email verification remain open.

Official API references checked during implementation:
- https://developers.google.com/maps/documentation/places/web-service/nearby-search
- https://developers.google.com/maps/documentation/places/web-service/place-types
- https://developers.google.com/maps/documentation/places/web-service/policies

## Phase 12 security checkpoint

ProductionGuard rejects development profiles, insecure cookies/origins, mismatched CORS/auth origins,
non-PostgreSQL configuration, absent service passwords and public documentation when production is
selected. JDBC statements default to a20s deadline; Next backend URLs reject embedded credentials and
non-origin suffixes. Existing controls and historical migrations remain intact. Real loopback HTTP
checks cover cookie flags/rotation/logout, strict Origin/custom header, CORS and safe share errors.
Those checks use H2/test limiter and manually sent cookies: no native Redis/PostgreSQL, TLS or browser
security verification is implied. No critical/high issue remains from this scoped code/test review;
not a penetration test. Edge TLS/HSTS, nonce CSP, operator logging/retention, backups/restore, storage
quotas, native load/concurrency and full rendered usability are still release gates.
Detailed evidence: docs/VERIFICATION_PHASE12.md.


## FINAL Phase13 repository review

No unresolved critical/high finding identified within code inspection and executed regression.
This is not a professional penetration test, dependency-advisory certification or deployed security
approval. The full review matrix and limitations are in docs/VERIFICATION_PHASE13.md.

New boundaries reviewed/tested:

- Activity derives owner from security context and projects only action/outcome/actor-kind/time.
  No raw audit metadata, session/resource/request IDs, security reasons, prompts or medical text.
- Search uses principal-scoped UNION branches, trusted observation view, parameterized literal LIKE
  terms, eight-term/80-character limits, fixed20/page,8KiB body limit and60/account rate limit.
  It does not persist terms, expose unreviewed extraction, or place health query text in URLs.
  Upstream proxies/APM must also avoid request-body logging.
- Timeline scheduling events join source owner/page/document. They do not influence laboratory
  comparability/trends. Pending/ignored follow-ups are excluded; IDOR tests cover source filters.
- New UI strings remain React text; no HTML/Markdown interpretation. Protected pages use the
  existing session shell. Onboarding collects no sensitive data; Settings has no pretend toggles.
- Provider request correlation accepts only UUID text; missing/malformed/newline values get a new
  opaque UUID. No medical/user/location metadata is sent in correlation headers.
- Symptom links load their owner-scoped target outside the current list page; no client ownership
  assertion is accepted by the API.

Complete320-test backend regression retains JWT/refresh/CSRF/CORS/header, ownership/relationship,
malicious upload/path, candidate trust, evidence/citation forgery, provider failure, PDF snapshot,
share expiry/revoke/scope/concurrency and reminder idempotency coverage.58 frontend tests and builds
pass. These counts do not establish browser enforcement or native-service concurrency.

Privacy/medical safety: synthetic-only fixtures; no added external medical data flows; no LLM
calculation/diagnosis/prescribing; bounded verified facts remain the source of explanations. Search
and Activity do not alter records. Nearby coordinates remain transient. Sharing remains digest-only
and selected-snapshot-only; revocation cannot recover previously downloaded copies.

Open gates: native PostgreSQL/Flyway/Redis/restart, rendered desktop/mobile, liveLLM/Places,
productionTLS/browser cookies and email. Keep synthetic data until the target deployment's TLS,
CSP, least-privilege roles, encryption, backups/retention, quotas and incident response are reviewed.
No secrets, sensitive production logging or critical completed-feature TODO was identified in the
source/archive review. This limited inspection cannot prove that no unknown vulnerability exists.
