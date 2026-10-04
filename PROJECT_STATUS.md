# CarePath project status

**Authoritative implementation ledger — read this first before every subsequent phase.**

Current scope: **FINAL Phase 13 — RELEASE CANDIDATE**, within the recorded automated scope.
Read the Phase13 closure at the end of this ledger and docs/VERIFICATION_PHASE13.md for final results.
docs/REQUIREMENTS_TRACEABILITY.md reconciles all214 original items; the Phase12 matrix is preserved.
Earlier phase ledgers and the original checkboxes are historical, not current verification claims.
Native/live/browser blockers remain open. Code and executed results remain authoritative.

Legend: `[x]` implemented and verified as described; `[ ]` not implemented or not yet verified.
Schema-only work is identified explicitly. Verification findings and unresolved issues follow.

## Continuation protocol

- [x] Architecture chosen: modular monolith, Java 21 backend and Next.js frontend.
- [x] Existing workspace inspected before creating foundation (empty workspace).
- [ ] On every subsequent phase: read this file, inspect existing source and migrations first.
- [ ] Never regenerate working components unnecessarily or assume documentation proves implementation.
- [ ] Mark complete only after implementation; record exact builds/tests and unresolved issues.
- [ ] Preserve this specification and compatibility across all subsequent phases.
- [ ] Eventually deliver complete runnable/testable repository and final carepath.zip.

## Phase 1 acceptance (18 requested tasks)

- [x] P1.01 Analyze complete requirements; requirement matrix below.
- [x] P1.02 Design architecture in ARCHITECTURE.md.
- [x] P1.03 Create repository structure.
- [x] P1.04 Create ARCHITECTURE.md.
- [x] P1.05 Create SECURITY.md initial threat model and limitations.
- [x] P1.06 Create this structured full-requirements checklist.
- [x] P1.07 Design complete initial PostgreSQL schema (27 tables; docs/SCHEMA.md).
- [x] P1.08 Create Flyway V1 foundation with validation/clean safeguards.
- [x] P1.09 Configure PostgreSQL and Redis Docker Compose with durable volumes and healthchecks.
- [x] P1.10 Initialize Java 21 Spring Boot/Maven backend.
- [x] P1.11 Initialize Next.js/React/strict TypeScript frontend.
- [x] P1.12 Establish frontend/backend environment configuration and real metadata connection.
- [x] P1.13 Create root .env.example; no committed secrets.
- [x] P1.14 Create initial README with setup/verification/limitations.
- [x] P1.15 Verify backend compilation (results below).
- [x] P1.16 Verify frontend TypeScript/build (results below).
- [x] P1.17 Fix errors encountered and record any unresolved environment limitations.
- [x] P1.18 Record exact completed work and final verification results.

## Complete product requirements

### 01 Authentication

- [x] Registration with input validation and unique normalized email.
- [x] Login with secure password hashing and generic errors.
- [x] Logout and server-side session/refresh revocation.
- [x] JWT access tokens with signature/issuer/audience/expiry checks.
- [x] Secure rotating refresh tokens and reuse detection.
- [x] Protected frontend routes and session restoration.
- [x] Protected backend APIs using real authenticated identity.
- [ ] Object-level authorization for every owned resource.
- [ ] Rate limits for registration, login, refresh and other sensitive endpoints.
- [x] Authentication/session lifecycle and abuse tests; no frontend-only fake authentication.

### 02 Secure medical document vault

- [x] Upload PDF, JPG, JPEG and PNG with magic/decoder/size validation.
- [x] Document types: lab report, prescription, diagnostic report, discharge summary, vaccination record, doctor note, referral, medical bill, other.
- [x] Private local storage adapter and future S3-compatible storage port.
- [x] Preview owned originals with safe content handling.
- [x] Search and filter documents.
- [x] Download own original with strict owner authorization.
- [ ] Inspect extracted information.
- [ ] User-controlled deletion: originals/metadata implemented and tested; future derived artifacts/share invalidation remain PENDING.
- [x] Persist owner/type/date/upload date/provider/MIME/safe key/hash/status/tags with tested vault service. Confidence remains unset; no extraction. Reserved arbitrary JSON metadata is not exposed.
- [x] Path traversal, filename, MIME mismatch and ownership tests.

### 03 Document intelligence pipeline

- [ ] Upload → validate → store → classify → text extraction → OCR fallback → structured extraction → deterministic validation → normalize → provenance → timeline.
- [ ] PDF text extraction per page.
- [ ] OCR fallback for scanned PDFs and images.
- [ ] Asynchronous processing outside normal upload request.
- [ ] Durable jobs with retry/backoff/leases/recovery/idempotency.
- [ ] UPLOADED, PROCESSING, NEEDS_REVIEW, COMPLETED and FAILED transitions.
- [ ] Explicit processing error/status UI and safe error codes.

### 04 Structured lab extraction

- [ ] Extract original and canonical test names without fabricating missing fields.
- [ ] Extract value, comparator, original unit and supplied reference interval.
- [ ] Retain explicit abnormal indicator only when supported.
- [ ] Extract report/sample date and laboratory where available.
- [ ] Retain page, source evidence and separate extraction confidence.
- [ ] Store original and normalized representations; unknowns remain null.

### 05 Medical concept normalization

- [ ] Actual deterministic alias normalization layer: Hb/HGB/Hemoglobin/Haemoglobin in vetted context.
- [ ] Persist original term, canonical term, mapping confidence and rule version.
- [ ] Use recognized identifiers such as LOINC only with reliable contextual mapping.
- [ ] Review uncertain/ambiguous mappings.
- [ ] Do not use unrestricted LLM string equivalence as normalization.

### 06 Unit normalization

- [ ] Preserve original values and units separately from normalized values.
- [ ] Only validated compatible concept/unit conversion rules.
- [ ] BigDecimal conversions with explicit comparator/precision handling.
- [ ] No silent unit guessing.
- [ ] Meaningful compatible/incompatible/missing-unit and reference-bound conversion tests.

### 07 Uncertainty and human verification

- [ ] Low-confidence observations excluded from trusted timeline.
- [ ] Show exact uncertainty message: CarePath couldn't confidently read this result..
- [ ] Confirm, correct and reject actions.
- [ ] Show source, extracted value, confidence and relevant evidence.
- [ ] Append review history while preserving original extraction.

### 08 Longitudinal health timeline

- [ ] Combine observations, reports, symptoms, prescriptions, doctor visits, appointments and follow-ups.
- [ ] Select one observation concept and view history/chart.
- [ ] January/April/September demonstration: Hemoglobin 12.1 → 11.3 → 10.4.
- [ ] Every point links to original evidence.
- [ ] Handle missing/partial dates and incomparable units without false ordering or trends.

### 09 Health change detector

- [ ] Deterministic increasing and decreasing comparisons.
- [ ] Approximately stable using documented versioned tolerance.
- [ ] Newly observed measurement.
- [ ] Previously tracked measurement absent only from comparable adequately extracted report.
- [ ] Entered/exited supplied reference interval only when deterministically valid.
- [ ] Insufficient-evidence outcome.
- [ ] Numerical rules in tested code, no unrestricted LLM judgment or diagnosis.
- [ ] Demonstrate Hemoglobin 11.3 → 10.4 and Vitamin D 19 → 31 without explaining medical cause.

### 10 Evidence and provenance

- [ ] Every important record-derived statement links to source document/page.
- [ ] Multi-report claim cites all contributing observations.
- [ ] VIEW EVIDENCE interaction to original page/snippet.
- [ ] Distinguish USER DATA, REFERENCE INFORMATION and AI EXPLANATION.
- [ ] Enforce provenance consistency at service layer in addition to schema.

### 11 AI health record assistant

- [ ] Questions over uploaded structured records.
- [ ] Plain-language report explanations.
- [ ] Grounded explanations of longitudinal changes.
- [ ] Owner-scoped structured retrieval → provenance → optional trusted reference context → controlled LLM → evidence response.
- [ ] Provider abstraction and environment-only API keys.
- [ ] Insufficient-evidence refusal and invented-citation rejection.
- [ ] Treat documents as untrusted data; prompt-injection defenses.
- [ ] No primary full-PDF/full-history prompt architecture.
- [ ] No model/database source-of-truth confusion.

### 12 Symptom timeline

- [ ] Create/edit symptom name, start date, severity, frequency, notes and resolved date.
- [ ] Symptoms become timeline events.
- [ ] Assistant may use relevant symptoms without unsupported diagnoses.

### 13 Clinical question builder

- [ ] Generate editable clinician questions grounded in record changes.
- [ ] Edit and save questions.
- [ ] Include selected questions in Visit Pack.
- [ ] No prescribing or unsupported evaluation recommendations.

### 14 Appointments

- [ ] Clinician/provider, specialty, date/time/timezone, location and notes.
- [ ] Link selected owned reports and symptoms.
- [ ] Manage appointment status and follow-up date.
- [ ] Upcoming appointments on dashboard.
- [ ] Ownership and appointment-change audit events.

### 15 Follow-up detection

- [ ] Extract possible follow-up such as Review after 6 weeks.
- [ ] Show original source and suggested date with anchor/uncertainty.
- [ ] Confirm, edit and ignore.
- [ ] Never silently create uncertain follow-ups.
- [ ] Test calendar arithmetic including missing anchor dates.

### 16 Reminders

- [ ] Persistent server-side reminders for appointments and confirmed follow-ups.
- [ ] Configurable reminder offsets.
- [ ] In-app notifications.
- [ ] Email provider abstraction if practical; explicit disabled state.
- [ ] Survive restart; leased scheduling and idempotent delivery.

### 17 Doctor Visit Pack

- [ ] Select reason for visit, symptoms and important timeline events.
- [ ] Select relevant observations/trends/reports/prescriptions/previous visits/questions.
- [ ] Include source evidence for record-derived content.
- [ ] Professional web view.
- [ ] Downloadable PDF generated from same scoped snapshot.
- [ ] No unverified diagnoses; invalidate snapshots after source corrections/deletion.

### 18 Temporary clinician sharing

- [ ] Strong random share token with only digest stored.
- [ ] Scoped selected records through immutable Visit Pack revision.
- [ ] Read-only anonymous access with no clinician account.
- [ ] 15-minute, 30-minute, 1-hour and 24-hour expiry options.
- [ ] Revocation on every subsequent request.
- [ ] QR code for share URL.
- [ ] Audit share creation/access/revocation.
- [ ] Test create → access → revoke → denied.
- [ ] Test create → expire → denied.
- [ ] Owner-only share-management APIs and noncached anonymous views.

### 19 Nearby care

- [ ] Explicit user location permission.
- [ ] Legitimate Maps/Places provider for hospitals, clinics, pharmacies and diagnostic centres.
- [ ] Show source-provided name/address/distance/phone/hours/open-closed/website.
- [ ] Directions and legitimate external booking links where available.
- [ ] No LLM-invented facilities/contact details/hours/availability.
- [ ] Attribution, graceful missing fields and provider timeout/quota behavior.

### 20 Appointment access

- [ ] Call action.
- [ ] Directions action.
- [ ] Official website action.
- [ ] External booking page only when legitimately available.
- [ ] Future direct appointment-provider abstraction.
- [ ] Never fabricate appointment slots.

### 21 Search

- [ ] Search/filter documents, tests, dates, providers, symptoms and appointments.
- [ ] Queries such as CBC, hemoglobin and September prescription.
- [ ] Owner-scoped paginated search and controlled query/sort inputs.

### 22 Security

- [ ] Secure authentication, password hashing and refresh-token security.
- [ ] Object-level authorization and input validation.
- [ ] Secure upload handling and safe storage keys/filenames.
- [ ] Rate limiting.
- [ ] Secure headers and CORS (foundation configured; full feature policy pending).
- [ ] SQL injection prevention and XSS protections across implemented product flows.
- [ ] Path traversal prevention.
- [ ] Access auditing.
- [ ] Temporary share expiry/revocation.
- [ ] User-controlled deletion across originals/derivatives.
- [ ] Environment-based secrets (foundation configured; future adapters pending).
- [ ] No sensitive medical content in application or proxy logs.
- [ ] Maintain SECURITY.md assets/trust boundaries/attack surfaces/controls/limitations.
- [ ] No HIPAA/GDPR/ABDM or other unestablished compliance claims.

### 23 Audit log

- [x] Login events.
- [x] Document upload/view/delete events.
- [ ] Share creation/access/revocation events.
- [ ] Appointment change events.
- [ ] User-facing activity/security history.
- [ ] No passwords, tokens or raw medical documents in audit logs.

### 24 Medical safety

- [ ] No definitive diagnosis.
- [ ] No medication prescription or dosage recommendation.
- [ ] No instructions to stop/change prescribed medications.
- [ ] No fabricated medical information or clinician replacement claims.
- [ ] Conservative predefined urgent-pattern escalation with vetted/versioned rules and sources.
- [ ] Clearly surface uncertainty; adversarial safety tests.

### 25 UI and UX

- [ ] Landing page (foundation page exists; final product landing pending).
- [x] Login and registration.
- [ ] Onboarding.
- [ ] Dashboard: upcoming appointment → health changes → reminders → recent documents → quick actions.
- [ ] Records and document viewer: COMPLETE + NOT LIVE-VERIFIED (implemented/build checked; rendered browser verification pending).
- [ ] Timeline and What Changed?.
- [ ] AI explanation.
- [ ] Symptoms and appointments.
- [ ] Visit Packs and temporary share.
- [ ] Nearby Care.
- [ ] Notifications and Activity.
- [ ] Privacy/Security and Settings.
- [ ] Responsive accessible components, keyboard support, mobile layouts.
- [ ] Excellent typography/spacing, restrained product visual style, no excessive gradients/clutter/huge cards.
- [ ] Skeleton loading, useful empty/error states and confirmation dialogs.
- [ ] Polished Recharts charts where useful and subtle reduced-motion-aware animation.
- [ ] Next.js/React strict TypeScript/Tailwind/shadcn foundation; extend consistently.

### 26 Data model

- [ ] JPA entities and services for User, RefreshToken/Session, MedicalDocument, DocumentExtraction.
- [ ] JPA entities and services for MedicalObservation, CanonicalMedicalConcept, ObservationSource.
- [ ] JPA entities and services for Symptom, Appointment, FollowUp, Reminder, SavedQuestion.
- [ ] JPA entities and services for VisitPack, VisitPackItem, ShareToken, Notification, AuditEvent.
- [ ] Enforce foreign keys, constraints, indexes, timestamps and optimistic locking in services (DDL exists).
- [ ] Append Flyway migrations without regenerating existing deployed schema.

### 27 Testing

- [ ] Authentication and authorization tests.
- [x] Document ownership tests.
- [ ] Concept normalization and unit conversion tests.
- [ ] Change detection and provenance tests.
- [ ] Follow-up date calculation tests.
- [ ] Reminder persistence/restart tests.
- [ ] Share expiration and revocation tests.
- [x] Security: User A never accesses User B documents.
- [ ] Security: User A never accesses User B observations.
- [ ] Security: User A never accesses User B appointments.
- [ ] Security: User A never accesses User B Visit Packs.
- [ ] Security: User A never accesses User B share-management APIs.
- [ ] Frontend critical flow/component tests where practical.

### 28 Evaluation

- [ ] Reproducible evaluation framework and scripts.
- [ ] Document classification accuracy.
- [ ] Lab extraction accuracy.
- [ ] Concept normalization accuracy.
- [ ] Unit normalization accuracy.
- [ ] Change detection accuracy.
- [ ] Provenance correctness.
- [ ] Labelled synthetic evaluation dataset and measured metrics with denominators.
- [ ] Never fabricate evaluation numbers.

### 29 Synthetic demo data

- [ ] No real patient data; clear SYNTHETIC DEMO DATA — NOT A REAL PATIENT labels.
- [ ] At least January, April and September reports.
- [ ] Demonstrate decrease, increase, stable and new measurement.
- [ ] Uncertain extraction requiring verification.
- [ ] Useful follow-up instruction and gold source labels.

### 30 Observability and engineering

- [ ] Structured logs (foundation ECS configured; feature-log privacy validation pending).
- [ ] Correlation/request IDs (foundation implemented; propagate through future jobs/providers).
- [ ] Health endpoint (foundation configured; deployment verification recorded separately).
- [ ] Appropriate metrics and protected operational access.
- [ ] Pagination and bounded result sizes.
- [ ] Indexes (foundation DDL exists; measure query plans with later data).
- [ ] Asynchronous durable processing.
- [ ] Sensible outbound API timeouts.
- [ ] Retry/backoff where safe and appropriate.
- [ ] Graceful external-service failure and explicit missing-credentials states.

## Cross-cutting requirements and engineering constraints

- [x] Repository has frontend/, backend/, infrastructure/, docs/, sample-data/, scripts/,
  docker-compose.yml, .env.example, README.md, ARCHITECTURE.md, SECURITY.md, API.md,
  TESTING.md and PROJECT_STATUS.md.
- [x] Foundation stack: Next.js, React, strict TypeScript, Tailwind and shadcn-compatible button.
- [x] Foundation backend dependencies: Java 21, Spring Boot, Maven, Spring Security,
  Spring Data JPA, Bean Validation, Flyway, OpenAPI/Swagger and PostgreSQL/Redis clients.
- [ ] Add Recharts where the implemented product needs numerical visualization.
- [ ] Storage abstraction: private local development filesystem and future S3-compatible adapter.
- [ ] AI/document stack: provider port, environment-only keys, PDF text, OCR, structured extraction,
  deterministic validation, database as sole record source of truth.
- [ ] Finished local workflow: unzip → VS Code → .env → Compose → backend → frontend → use product.
- [ ] Modern complete working product; no tutorial/prototype/UI-only or mocked-feature substitution.
- [ ] No important TODO implementations, hardcoded secrets, fabricated APIs/medical facts or unnecessary microservices.
- [ ] No giant service/components; use typed DTOs, validation, domain boundaries and reusable components.
- [ ] Deterministic code for calculations; no entire-history LLM prompts or document instruction overrides.
- [ ] Missing external credentials produce explicit graceful unavailable behavior, not fake responses.

## Final demo acceptance sequence (not yet implemented)

- [x] Register/login.
- [ ] Upload January, April and September reports.
- [ ] Process and extract observations.
- [ ] Review uncertain extraction.
- [ ] Open longitudinal timeline and trend charts.
- [ ] Open What Changed? and inspect original evidence for a result.
- [ ] Ask for grounded explanation.
- [ ] Record symptom and create appointment.
- [ ] Detect and confirm follow-up; receive reminder.
- [ ] Generate Doctor Visit Pack and download PDF.
- [ ] Create temporary 30-minute share and show QR.
- [ ] Open anonymous read-only view, revoke and verify access denied.
- [ ] Find nearby care with legitimate phone/hours/directions.
- [ ] View audit history.

## Phase 1 implementation inventory

- SQL: 27 domain/foundation tables, owner-composite FKs, constraints and indexes, updated-at triggers,
  durable jobs/reminders, original/normalized observation fields and trusted observation view.
- Backend: application entry, configuration, read-only stage metadata, deny-by-default security,
  origin policy, headers, generated request ID/MDC, health/readiness and seven foundation MVC tests.
- Frontend: honest Phase 1 shell, responsive styling, shadcn-compatible Button, backend metadata
  lookup with timeout/unavailable state, loading/error/not-found pages, headers and strict TypeScript.
- Docs: architecture, threat model, schema, API, testing/evaluation design and local run instructions.
- No registration, login, uploads, OCR, normalization services, clinical functionality, real provider
  adapters, reminders scheduler, PDF or share APIs were implemented in this run.

## Verification results

Verified on 2026-09-18. Toolchains: Temurin JDK 21.0.12.1, Maven 3.9.11,
Node 24.19.0, npm 11.9.0. Backend Spring Boot 3.5.16; frontend Next 16.3.5,
React 19.2.4, TypeScript 5.9.3. All versions pinned by POM/lockfiles.

| Check | Actual result | Evidence / limitation |
| --- | --- | --- |
| Java 21 `mvn -B verify` (local Maven/JDK paths and runtime proxy settings) | PASS; executable JAR built | docs/backend-verification.txt |
| Backend foundation MVC/security tests | PASS: 7 tests, 0 failures/errors/skips | Default-deny, safe metadata/headers, CORS, request IDs, local docs isolation |
| Frontend dependency install | PASS; lockfile committed to source | npm install --no-audit --no-fund |
| `npm run typecheck` | PASS | docs/frontend-verification.txt |
| `npm run lint` | PASS; no remaining lint findings | docs/frontend-verification.txt |
| `npm run build` | PASS | docs/frontend-build.txt |
| Production frontend HTTP smoke | PASS: HTTP 200, unavailable backend state and security headers | docs/frontend-verification.txt |
| V1 SQL migration in PGlite 0.5.8 PostgreSQL engine | PASS; 27 tables | docs/schema-verification.txt |
| Schema invariants | PASS: 14 checks, 0 persisted synthetic users | scripts/schema-invariants.sql; same source run by portable npm harness |
| Portable schema harness `npm ci && npm test` | PASS after fixing relative path | scripts/schema-check/; no Docker required |
| Compose YAML structural parse | PASS | Two dependency services with healthchecks; not Compose CLI validation |
| Real Docker Compose startup | NOT RUN | Docker executable/daemon unavailable |
| Live PostgreSQL/Redis + Flyway/JDBC migration/startup/readiness | NOT RUN | Requires Docker-enabled host; PGlite does not replace this gate |
| Browser visual QA / live frontend-backend connected state | NOT RUN | Frontend build and HTTP unavailable-state smoke only |
| Product auth/ownership/share/medical tests and evaluation | NOT IMPLEMENTED in Phase 1 | Foundation schema checks are not product authorization or medical accuracy tests |

Corrections made during verification:

- Initial Java 17/no Maven environment: downloaded isolated Java 21 and Maven toolchains.
- System package installation could not run in this environment; no privilege escalation used.
- Oracle JDK download returned unavailable content; Temurin download succeeded.
- Maven proxy certificate/path issue: trusted the runtime-provided CA in the temporary JDK and
  regenerated temporary proxy settings for the active invocation; no project trust bypass.
- Mockito dynamic attachment failed initially; selected subclass mock maker, then all tests passed.
- Replaced homepage anchor with Next Link and named PostCSS config export; lint then passed.
- Scoped local documentation CSP separately, maintaining the strict product API policy.
- Fixed the portable schema runner's relative migration path and reran it successfully.

No fabricated success, benchmark result or medical metric is included. Build outputs/dependencies
are excluded from the source archive; logs and reproducible commands are included.

## Unresolved issues / next phase

- Clinical product functionality remains deferred; Phase 2 results and next action are below.
- Docker is not installed in this execution environment; Compose runtime verification may require
  a Docker-enabled environment. A successful build does not prove container startup.
- No real medical data, vetted safety content, normalization mapping dataset or provider keys supplied.
- Frontend nonce CSP, production encryption/least-privilege roles and deployment hardening remain open.
- OpenAPI JSON and Swagger UI are enabled only in local profile, with documentation-scoped CSP.
- Phase 1 is not production-ready and carries no regulatory compliance claim.

## Checkpoint and next action

Phase 1 source is packaged as carepath-phase1.zip; the working source remains in carepath/.
Restore that archive if a future workspace has no source tree. Do not reinitialize over existing work.
Historical Phase 1 next-step recommendation: authentication/session lifecycle and ownership.
This is now implemented in Phase 2; native Docker/Flyway remains an open gate. Npm reported ESLint 9.39.4 as deprecated; lint/build pass, but dependency
maintenance and security review remain required before production use.


## Phase 2 — authentication, authorization and security foundation

Implementation complete for the requested scope. Verification boundaries are explicitly recorded below.
Native PostgreSQL/Flyway/Redis are still not verified; H2 and PGlite are not substitutes for that claim.
No vault, OCR, medical extraction/normalization, timeline, AI, symptom, appointment, Visit Pack or nearby-care
features were implemented. The original landing and database V1 were preserved, with accurate account status updates.

### Requirement-by-requirement status

| Phase 2 requirement | Status | Evidence / scope |
| --- | --- | --- |
| Read full status/architecture/security and inspect code/config/schema/tests | COMPLETE + VERIFIED | Existing repository read before edits; no regeneration |
| Preserve verified Phase 1 functionality | COMPLETE + VERIFIED | Existing seven assertions retained as full application tests; V1 SHA-256 unchanged |
| Registration | COMPLETE + VERIFIED | Real JPA insert, validation, audit and HTTP integration |
| Duplicate-email handling, including race | COMPLETE + VERIFIED | Normalization, unique constraint, sequential/concurrent duplicate tests |
| BCrypt hashing | COMPLETE + VERIFIED | Cost 12 production default; configurable 10–14; tests use 10 and verify hashes |
| Login and invalid credentials | COMPLETE + VERIFIED | Generic known/unknown/disabled errors; real password checks |
| Logout/session revocation | COMPLETE + VERIFIED | Refresh family and existing access rejected immediately after logout |
| JWT access tokens and stateless HTTP authentication | COMPLETE + VERIFIED | RS256; claims validated; no HttpSession; persisted revocation check |
| Access expiry, signature/issuer/audience/required-claim rejection | COMPLETE + VERIFIED | Expired/tampered/wrong issuer/wrong audience/missing expiry/unsigned tests |
| Refresh lifecycle and secure persistence | COMPLETE + VERIFIED | 256-bit opaque values; only SHA-256 digests in SQL |
| Refresh expiry/revocation/disabled sessions | COMPLETE + VERIFIED | Full application integration tests |
| Rotation and reuse protection | COMPLETE + VERIFIED | Row lock + reread, one child constraint, concurrent rotation/replay tests |
| Protected API endpoints and Spring Security | COMPLETE + VERIFIED | /auth/me requires bearer + active session; future APIs deny all |
| Bean Validation and malformed requests | COMPLETE + VERIFIED | Required fields, lengths, UTF-8 password limit, JSON errors, body cap |
| Consistent errors/global handling | COMPLETE + VERIFIED | Safe messages/codes/request IDs; no input echoes or stack traces |
| CORS and CSRF | COMPLETE + VERIFIED | Exact Origin + custom header for auth POSTs; foreign origin and cookie-only API tests |
| Security headers and cookies | COMPLETE + VERIFIED | API CSP/nosniff/frame/referrer; production __Host/Secure/HttpOnly/SameSite cookie tests |
| Auth rate-limit policy/window/outage behavior | COMPLETE + VERIFIED | IP/account HMAC keys; deterministic store-port integration tests |
| Production Redis Lua adapter | COMPLETE + NOT LIVE-VERIFIED | Real implementation; no Redis server available for runtime test |
| Audit infrastructure and required five actions | COMPLETE + VERIFIED | Transactional typed events, unknown-user failure audit, password/token absence checks |
| Reusable ownership/IDOR foundation | COMPLETE + VERIFIED | SecurityContext-only owner, scoped repository contract, generic 404 helper and caller-userId rejection |
| Later six resource modules' actual IDOR enforcement | PENDING | Modules intentionally not implemented; mandatory A/B tests when introduced |
| Frontend register/login/logout | COMPLETE + VERIFIED | Real API client, forms/build; HTTP auth flow through production Next proxy |
| Authenticated shell/protected routes/loading | COMPLETE + VERIFIED | Account-only /app shell, restoration/loading/error guards; build and client tests |
| Refresh/restoration and safe token handling | COMPLETE + VERIFIED | Memory-only JWT, HttpOnly refresh, single-flight, serialized mutations/Web Locks |
| Frontend validation/error/accessibility/responsive implementation | COMPLETE + NOT LIVE-VERIFIED | Labels/autocomplete/alerts/loading/focus/responsive styles implemented; no full browser UI/a11y run |
| Typed API client | COMPLETE + VERIFIED | Runtime response guards and safe error mapping; four Node tests |
| Token expiry/configuration through environment | COMPLETE + VERIFIED | Validated properties, no embedded secrets, generator and .env.example |
| Additive schema constraints/indexes/timestamps/integrity | COMPLETE + VERIFIED | V2 applied in PGlite; 19 schema invariants including prior checks |
| Native PostgreSQL Flyway startup/JDBC behavior | BLOCKED | No Docker/PostgreSQL executable; remains pending, never marked live verified |
| Phase 2 security review/fix critical-high findings | COMPLETE + VERIFIED | Source inspection + targeted adversarial tests; SECURITY.md and docs/AUTHENTICATION.md |
| Backend compile/all default tests/package | COMPLETE + VERIFIED | Final mvn verify evidence below |
| Frontend TypeScript/lint/configured tests/production build | COMPLETE + VERIFIED | Final frontend evidence below |
| Real HTTP register/login/me/refresh/logout/denied | COMPLETE + VERIFIED | Boot HTTP + H2, and separately through actual production Next proxy |
| Preserve scope; update status/files/limitations | COMPLETE + VERIFIED | This ledger and docs; clinical checkboxes left pending |

### Files/modules created or changed

- identity/: AuthProperties, IdentityConfiguration, UserAccount/UserAccounts, CarePrincipal,
  JwtService, SessionAuthenticationConverter, SessionStore, Tokens, AuthDtos, AuthService, AuthController.
- security/: AuthBoundaryFilter, BoundedAuthRequest, RedisRateLimitStore/RateLimitStore,
  AuthRateLimiter, Ownership, OwnerScopedRepository.
- audit/: AuditService with typed action/reason events; nullable owner for unknown login failure.
- foundation/: safe ApiFailure/ApiErrors, updated SecurityConfiguration and actual stage metadata;
  existing RequestIdFilter remains unchanged.
- Flyway: **V2__authentication_security.sql** only. V1 unmodified; now 28 tables.
- Config/POM: JWT resource-server dependency, required RSA/HMAC settings, fail-unknown JSON,
  suppressed SQL value/constraint logs; test-only H2 dependency/schema/profile.
- Frontend: typed auth client and four tests; AuthProvider/AuthForm/ProtectedShell;
  /login, /register, /app; same-origin API rewrite; minimal landing status/navigation edits.
- Tests: full-app AuthTestSupport, AuthenticationIntegrationTest, SecureCookieIntegrationTest,
  opt-in FrontendProxyIT; original FoundationSecurityTest and LocalDocumentationSecurityTest preserved/expanded.
- Scripts: key generation, current-phase packaging excluding secret files, V2 schema checks,
  5 new authentication SQL invariants and native SQL verification runner update.
- Docs: docs/AUTHENTICATION.md; README/API/ARCHITECTURE/SECURITY/TESTING/SCHEMA/status revisions.

### Verification commands and results

Toolchains: Java 21.0.12.1, Maven 3.9.11, Node 24.19.0, npm 11.9.0.
Runtime path prefix used for Maven:
`JAVA_HOME=/workspace/scratch/toolchains/jdk-21.0.12.1+1 /workspace/scratch/toolchains/apache-maven-3.9.11/bin/mvn`
with `-o -Dmaven.repo.local=/workspace/scratch/toolchains/m2` after dependencies were fetched using
temporary runtime-proxy settings (not shipped in the repository). Standard user commands are below.

| Exact project command / check | Result | Evidence |
| --- | --- | --- |
| `mvn -B verify` from backend (runtime invocation additionally `-f carepath/backend/pom.xml`) | PASS: 26 default tests, zero failures/errors/skips; executable JAR | docs/phase2-backend-verification.txt |
| `mvn -Dtest=FrontendProxyIT -B test` | PASS: one additional integration test; actual production Next proxy → Spring Boot → H2 | docs/phase2-proxy-verification.txt |
| `npm run typecheck` | PASS | docs/phase2-frontend-verification.txt |
| `npm run lint` | PASS | Same file |
| `npm test` from frontend | PASS: 4 client tests | Same file |
| `npm run build` | PASS: landing, login/register and /app compiled | docs/phase2-frontend-build.txt |
| `npm test --prefix carepath/scripts/schema-check` | PASS: actual V1 + V2 SQL; 19 invariants; zero persisted fixture users | docs/phase2-schema-verification.txt |
| `sha256sum -c /workspace/scratch/carepath-v1.sha256` | PASS: V1 unchanged | Hash checkpoint made before Phase 2 edits |
| Docker/PostgreSQL/Redis executable checks | Unavailable | No native service run or Flyway startup claimed |
| Browser-rendered UI/mobile/a11y end-to-end suite | NOT RUN | Implemented semantics reviewed; HTTP proxy tests are not browser tests |

Test coverage includes all 16 requested testing areas. The default total includes the seven retained
foundation checks, eighteen authentication integration tests and one secure-cookie integration test.
The separate proxy check verifies register → login → protected endpoint → rotation → logout → denied
refresh and denied old access. Tests use synthetic identities only. No evaluation metrics invented.

### Security findings fixed and verification corrections

- Enforced signature algorithm, required time/issuer/audience/identity claims and session state.
- Committed replay revocation before returning a denied response; row-locked family rotations and
  unique child constraint prevent concurrent token forks.
- No plaintext credential persistence; BCrypt UTF-8 maximum prevents silent truncation.
- Protected auth POSTs against login/refresh/logout CSRF, including mandatory custom header.
- Production host-prefixed cookies resist sibling-domain cookie injection; insecure mode restricted
  to loopback origins. Cookie authentication cannot authorize general APIs.
- Rejected unknown fields/mass assignment; source of ownership is validated principal only.
- Bounded actual request bodies including chunked input; no unsafe URL/header ownership trust.
- Disabled implicit Spring form/basic/logout mechanisms; only explicit auth surfaces remain.
- Suppressed SQL constraint-value logging and used typed content-free audit metadata/safe errors.
- Handled concurrent duplicate registration with a generic 409 and no duplicate audit/account.
- Corrected ambiguous Java RSA imports, H2 test schema default syntax and frontend Web Locks typing.
- Maven dependencies were re-fetched into a persistent scratch cache after the runtime reset; no
  production trust checks were bypassed. Proxy smoke used HTTP/1.1 for Next compatibility.

### Remaining verification and operational limits

- **BLOCKED:** Docker Compose/native PostgreSQL/Flyway and Redis runtime verification. Run these
  first on a Docker-enabled host; H2 integration and PGlite SQL do not establish PostgreSQL lock or Redis behavior.
- Full browser form/route/mobile/a11y automation remains unexecuted; production proxy HTTP checks pass.
- Current Next proxy means backend IP buckets may aggregate clients; production trusted-edge limiting
  needs deployment-specific configuration. Never simply trust arbitrary X-Forwarded-For.
- Browsers without Web Locks may be signed out by safe replay detection during a cross-tab refresh race.
- Frontend nonce CSP, email ownership verification/recovery/MFA, key rotation, session/audit retention,
  TLS/encrypted volumes, least-privilege DB roles and dependency security review remain deployment gates.
- ESLint 9.39.4 remains deprecated as noted in Phase 1; checks pass, but no vulnerability-free claim is made.
- No real clinical data or medical product features are introduced. No regulatory compliance claim.

### Recommended next phase

Verify the native dependency/Flyway/auth flow first, then Phase 3: secure document vault and durable
processing/provenance foundation. Reuse auth, ownership and audit; add real A/B document access tests.
Continue from carepath/ or restore carepath-phase2.zip if the workspace is absent. Do not regenerate.


## Phase 3 — secure medical document vault

Only Phase 3 was implemented. No OCR, PDF text extraction, classification, medical observations,
normalization, intelligence, clinical workflows, sharing or external-care provider was added.
Historical checkbox sections remain for continuity; the explicit statuses below govern this checkpoint.
The full Phase 1/2 status, architecture/security documents and actual source/config/tests were inspected
before edits. Working authentication was preserved. V1/V2 byte comparison against carepath-phase2.zip
passed; V3 is additive. Existing product-route denial assertion now targets still-unimplemented
observations rather than the newly enabled documents route.

### Requirement-by-requirement status

| Phase 3 requirement | Status | Actual evidence / limitation |
| --- | --- | --- |
| 01 Domain model | COMPLETE + VERIFIED | MedicalDocument internal record, typed DTOs, nine categories/five states, owner/hash/metadata/timestamps/version; real JDBC API tests; new files stay UPLOADED |
| 02 Storage abstraction and local implementation | COMPLETE + VERIFIED | DocumentStorage + LocalDocumentStorage; actual temp filesystem tests for store/retrieve/delete/exists; S3 implementation PENDING, outside scope |
| 03 Secure upload | COMPLETE + VERIFIED | Byte limit, empty/name/extension/MIME/signature/strict parser/decoder validation; PDF/JPEG/PNG API tests; no content-header trust |
| 04 Integrity | COMPLETE + VERIFIED | SHA-256 original-byte assertion and tamper rejection with committed security audit |
| 05 Ownership/IDOR | COMPLETE + VERIFIED | Authenticated owner predicates and generic 404; actual Spring Security API A/B tests in both directions |
| 06 Document REST APIs | COMPLETE + VERIFIED | Upload/list/get/preview/download/update/delete/filter through real services and security; DTOs hide storage/owner internals |
| 07 Search/filter/pagination | COMPLETE + VERIFIED | Server-side JDBC combined filters, escaped wildcard input, capped pages and stable ordering tested; native plans unverified |
| 08 Download/preview security | COMPLETE + VERIFIED | Original bytes/headers and safe image previews tested; PDF multi-page PNG rendering, no executable PDF delivered inline |
| 09 Deletion | COMPLETE + VERIFIED | DB deletion + durable cleanup queue; injected filesystem failure returns 202, retry purges file; repeated and concurrent deletes tested; actual restart pending |
| 10 Audit | COMPLETE + VERIFIED | Six document actions implemented, success/failure event assertions; no filename/raw content/token payload |
| 11 Records UI | COMPLETE + NOT LIVE-VERIFIED | Real /records upload/list/filter/pagination/detail actions, skeleton/empty/error states; strict TS/lint/build and client tests; no rendered browser execution |
| 12 Document viewer | COMPLETE + NOT LIVE-VERIFIED | Authenticated blob image viewer with PDF page controls, metadata editor and original download; backend rendering tested, browser display pending |
| 13 Upload UX | COMPLETE + NOT LIVE-VERIFIED | Drag/drop/file chooser, XHR progress, server-configured limit, validation and Uploaded wording; browser interactions pending |
| 14 Privacy/security UX | COMPLETE + NOT LIVE-VERIFIED | Account-private/no-auto-sharing copy and explicit immediate/queued deletion explanation; browser review pending |
| 15 Configuration | COMPLETE + VERIFIED | STORAGE_ROOT/VAULT_MAX_BYTES, fixed safe allowlist, existing .data Git exclusion; real configured temp storage and size tests; no machine-specific path in app config |
| 16 Flyway/schema | COMPLETE + NOT LIVE-VERIFIED | Immutable V1/V2; additive V3 applied by PGlite with all checks. Native PostgreSQL/Flyway execution BLOCKED |
| 17 Critical backend tests | COMPLETE + VERIFIED | Real Spring/JDBC/filesystem tests cover the 20 requested upload/CRUD/integrity/search cases; H2, not native PostgreSQL |
| 18 Two-user API security tests | COMPLETE + VERIFIED | Both directions: metadata/preview/download/update/delete denied; search/list excludes other owner |
| 19 Malicious file/path tests | COMPLETE + VERIFIED | All named traversal examples, encoded/null names, long/unicode/duplicate names, mismatch/corruption/script/active/encrypted PDF, storage symlink/overwrite tests |
| 20 Concurrency/consistency | COMPLETE + VERIFIED | Concurrent same-name upload, update winner/conflict, delete winner/not-found, download/delete race, abandoned intent cleanup and live-key protection |
| 21 Native services | BLOCKED | docker, postgres and redis-server not installed; no Compose/native Flyway/Redis runtime or backend restart persistence claim |
| 22 Frontend verification | PARTIAL | TypeScript/lint/8 client tests/production build pass; production Next HTTP proxy flow passes; browser binary download failed (timeouts/502), rendered UI BLOCKED |
| 23 Backend verification | COMPLETE + VERIFIED | Full previous + new regression tests, compile and packaged Spring Boot JAR; exact final results below |
| 24 Security review | COMPLETE + VERIFIED | Actual source/control review + adversarial tests; fixes and residual deployment/parser risks in SECURITY.md; no external pentest claim |
| 25 Documentation | COMPLETE + VERIFIED | README, API, ARCHITECTURE, SECURITY, TESTING, .env.example, this ledger and new docs/DOCUMENT_VAULT.md updated and source-checked |
| 26 Status accuracy / phase boundary | COMPLETE + VERIFIED | Explicit verification scope preserved; native/browser gates remain open and future functionality unmarked |

### Significant implementation files

- backend/src/main/java/com/carepath/vault/: domain/DTOs, owner-scoped JDBC repository, controller/service,
  FileValidation, storage port/local adapter, configuration and durable BlobCleanup worker.
- backend/src/main/resources/db/migration/V3__secure_document_vault.sql: page_count, normalized tag
  table, cleanup outbox and indexes. No V1/V2 edits. Reserved extraction fields remain unset.
- Existing AuditService: typed document actions and resource identity. Existing SecurityConfiguration:
  explicit vault routes and PUT/DELETE CORS, safe internal ERROR dispatch handling; direct /error stays denied.
  ApiErrors: multipart/size/type binding errors. Existing auth token/session/security controls retained.
- frontend/src/app/records/{layout,page}.tsx and [id]/page.tsx; components/vault/{records-vault,
  upload-panel,document-viewer,metadata-fields}.tsx; lib/vault-client.ts; existing memory-only auth client
  extended for authenticated JSON/binary requests and upload progress. No mock document UI.
- frontend/next.config.ts: 21 MiB proxy envelope and 120-second timeout, matching upload transport needs.
- VaultIntegrationTest: 31 new cases including parameterized filenames/formats. Existing FrontendProxyIT
  now exercises auth plus >10 MiB multipart and vault lifecycle through the production Next proxy.
- 8 frontend client tests total (4 existing + 4 new); scripts/vault-schema-invariants.sql adds 5 checks;
  schema runners apply V3; source packager supports carepath-phase3.zip.

### Verification commands and results

Commands were executed from the repository parent unless a working directory is explicitly shown.
The environment uses a downloaded JDK 21/Maven toolchain and persistent dependency cache; these
absolute paths are verification-environment details, not application configuration.

```sh
JAVA_HOME=/workspace/scratch/toolchains/jdk-21.0.12.1+1 /workspace/scratch/toolchains/apache-maven-3.9.11/bin/mvn -o -Dmaven.repo.local=/workspace/scratch/toolchains/m2 -f carepath/backend/pom.xml -B verify
JAVA_HOME=/workspace/scratch/toolchains/jdk-21.0.12.1+1 /workspace/scratch/toolchains/apache-maven-3.9.11/bin/mvn -o -Dmaven.repo.local=/workspace/scratch/toolchains/m2 -f carepath/backend/pom.xml -B -Dtest=FrontendProxyIT test
JAVA_HOME=/workspace/scratch/toolchains/jdk-21.0.12.1+1 /workspace/scratch/toolchains/apache-maven-3.9.11/bin/mvn -o -Dmaven.repo.local=/workspace/scratch/toolchains/m2 -f carepath/backend/pom.xml -B '-Dtest=*Test,FrontendProxyIT' verify
npm --prefix carepath/frontend run typecheck
npm --prefix carepath/frontend run lint
npm --prefix carepath/frontend run test
npm --prefix carepath/frontend run build
node carepath/scripts/schema-check/verify.mjs
command -v docker postgres redis-server node npm
node /opt/codex/runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright-core/cli.js install chromium
python3 carepath/scripts/package.py --phase phase3
```

Final combined run: **58 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS**.
Breakdown: 26 preserved foundation/auth tests + 31 vault cases + 1 production Next HTTP proxy flow.
Compilation, test compilation, package and executable JAR repackaging passed. The earlier standalone
verify passed 56 tests before the final compressed-file case; the combined run also includes the
opt-in proxy test, giving 58. The combined run is authoritative.
Frontend: **TypeScript PASS, lint PASS, 8 client tests PASS, production build PASS**.
PGlite: **V1–V3 applied, 24 schema invariants PASS, zero persisted synthetic users**.
The proxy test passed a >10 MiB original byte-for-byte lifecycle and revoked-session checks.
See docs/phase3-verification.txt for the retained command/result summary.
No request-body, token or medical-file dumps are included in the evidence artifact.

### Errors found and fixed

- PDFBox strict mode uses parse(false), not the protected COSParser setter; compilation corrected.
- H2 requires DEFAULT before PRIMARY KEY; only the test schema was adjusted, not Flyway migrations.
- Corrected the missing PDStream import in the compressed-file test.
- Corrected initial JSX syntax and prevented metadata editing from echoing the full server DTO.
- Avoided recursive PDF object traversal; added cumulative decoded-stream/embedded-image budgets, a bounded PDFBox stream cache and a compressed-stream abuse test.
- Normalized duplicate tags before database insert; serialized deletion with an owner-scoped row lock.
- Real >10 MiB multipart transport initially failed with the default 10 MiB Next buffer; explicitly
  configured 21 MiB. Internal servlet errors no longer become misleading security-denied responses;
  direct error endpoint requests remain denied. Rerun of the large-file proxy lifecycle passed.
- Excluded redundant commons-logging dependency; Spring's existing spring-jcl supplies the API.

### Still unverified / limitations

- Native PostgreSQL/Flyway V1–V3, Redis rate-limit runtime, Compose health/readiness and restart
  persistence remain BLOCKED in this environment. H2/PGlite do not substitute for these checks.
- Browser UI interactions, responsive visual rendering, keyboard/modal/image/download behavior and
  browser session restoration remain COMPLETE + NOT LIVE-VERIFIED. Chromium installation was attempted
  and failed with network timeouts/HTTP 502. Production HTTP proxy testing does not establish UI behavior.
- No S3 provider, antivirus, parser process sandbox, enforced CPU timeout, tenant storage quota,
  encrypted-volume setup, backup purge or clinical sharing. Local root parents must be trusted.
  Cleanup is durable and retries every minute but backlog alerts/backoff are future operational work.
- Source deletion handles current vault metadata/originals. Future dependent packs/shares need explicit
  revocation/derivative cleanup before those modules can ship; restrictive FKs presently fail safely.
- Historical Phase 1/2 deployment/security limitations remain visible above and in SECURITY.md.

### Recommended next phase (not started)

First close native PostgreSQL/Flyway/Redis and rendered-browser gates. Then Phase 4: durable document
processing, bounded PDF text extraction/OCR, evidence/provenance and uncertainty review. Normalization
and longitudinal intelligence should follow their own tested scope. Do not silently classify existing
UPLOADED records or invent extracted medical results.

Packaging: COMPLETE + VERIFIED — `python3 carepath/scripts/package.py --phase phase3`; ZIP CRC/readability
checked, 125 source files, V3 present, credentials/keys/storage/dependencies/build directories excluded.

## Phase 4 — document intelligence checkpoint (2026-09-23)

### Requirement ledger

Statuses apply only to the evidence stated. H2/PGlite/native OCR/HTTP proxy verification are distinct
from native PostgreSQL/Flyway/Redis and rendered-browser verification.

| # | Requirement | Status | Implemented evidence / boundary |
| --- | --- | --- | --- |
| 01 | Asynchronous processing architecture | COMPLETE + VERIFIED | Durable job API; real scheduled execution; lease reclaim/fencing, duplicate API requests, retry/backoff; upload remains short and explicit process returns 202 |
| 02 | Native PDF text extraction | COMPLETE + VERIFIED | PDFBox page text, boundaries and UTF-16 offsets; actual synthetic PDFs including page 2 |
| 03 | OCR fallback | COMPLETE + VERIFIED | Real Tesseract 5.3.4 eng runtime on PNG, JPEG and scanned PDF; native PDFs skip OCR; missing engine/timeouts fail explicitly |
| 04 | Classification | COMPLETE + VERIFIED | Deterministic heading evidence for all nine categories, ambiguity LOW; saved user category unchanged |
| 05 | Structured lab candidates | COMPLETE + VERIFIED | Original name/value/unit/range/explicit flag/date/provider, source and confidence; independent reports only |
| 06 | Missing fields never fabricated | COMPLETE + VERIFIED | Null unit/range/date/provider, illegible original value retained; deterministic parser tests |
| 07 | Modular extraction approach | COMPLETE + VERIFIED | Text/OCR/classifier/date/range/row/confidence/provenance modules; actual combined pipeline tests |
| 08 | LLM boundary | COMPLETE + VERIFIED | No LLM/API-key dependency or prompt execution; injection fixture remains untrusted text |
| 09 | Exact source provenance | COMPLETE + VERIFIED | Document/page ownership FK, exact snippets/offsets/method, OCR box; page-two actual evidence tests |
| 10 | Confidence | COMPLETE + VERIFIED | Documented HIGH/MEDIUM/LOW evidence bands; OCR/missing/uncertain/conflicting candidates trigger review |
| 11 | Raw/candidate data model | COMPLETE + VERIFIED | Additive V4, normalized candidate rows, composite owner/source/job keys; no trusted observations; H2 + PGlite only |
| 11a | Native V4 Flyway deployment | BLOCKED | Docker/native PostgreSQL not installed; no native Flyway execution claimed |
| 12 | Authenticated processing APIs | COMPLETE + VERIFIED | Process/status/extraction/evidence/retry; two-user denial through actual Spring security stack |
| 13 | Viewer integration | COMPLETE + NOT LIVE-VERIFIED | Real API status polling, candidate table, metadata distinction, source/page navigation; TS/lint/build/client tests passed, rendered interactions pending |
| 14 | Failed processing UX | COMPLETE + NOT LIVE-VERIFIED | Generic safe failure + permitted retry; browser execution pending |
| 15 | Resource/security controls | COMPLETE + VERIFIED | Child JVM/deadlines, OCR argument arrays, page/image/text/output limits, integrity, staging cleanup; tests cover practical abuse; OS sandbox remains a limitation |
| 16 | Synthetic documents | COMPLETE + VERIFIED | January, April, September (2 pages), scanned PNG/PDF, malformed PDF, prompt injection; generated/visually inspected fixture pages |
| 17 | Extraction testing | COMPLETE + VERIFIED | Twenty rule tests, eleven runtime tests, actual native/OCR/evaluation tests plus API tests; complete suite passed |
| 18 | Provenance testing | COMPLETE + VERIFIED | Values matched to actual page substrings and ground truth, not merely non-null fields |
| 19 | IDOR testing | COMPLETE + VERIFIED | Both directions process/status/extraction/evidence/retry return 404; wrong-document candidate also denied |
| 20 | Prompt-injection fixture | COMPLETE + VERIFIED | Text retained as document content; no instruction consumer/LLM exists in Phase 4 |
| 21 | Resource abuse/failure | COMPLETE + VERIFIED | Corrupt inputs, oversized dimensions/pages, compressed PDF, OCR subprocess/total timeout, temp symlink/cleanup, original retained |
| 22 | Evaluation framework | COMPLETE + VERIFIED | Authored ground truth, opt-in real-engine runner, measured results JSON; small all-lab synthetic corpus, not clinical accuracy |
| 23 | Observability | COMPLETE + VERIFIED | Safe worker logs/correlation IDs observed; audits tested; meters registered in worker code (not an external monitoring deployment) |
| 24a | Backend verification | COMPLETE + VERIFIED | Fresh compile/package and 101 tests, zero failures/errors/skips; packaged JAR worker smoke |
| 24b | Frontend verification | COMPLETE + VERIFIED | Ten configured tests, TypeScript, lint, production build; HTTP proxy checks run |
| 24c | Native PostgreSQL/Flyway/Redis | BLOCKED | Executables/Compose unavailable; historical Phase 1–3 gates preserved |
| 24d | Browser verification | BLOCKED | No local browser executable; rendered login/upload/process/source interaction was not run |
| 25 | Documentation | COMPLETE + VERIFIED | README, ARCHITECTURE, SECURITY, API, TESTING, this ledger, env example and detailed pipeline/evaluation docs updated |
| 26 | Status accuracy / phase boundary | COMPLETE + VERIFIED | Candidates only; future normalization/confirmation/longitudinal/care features remain PENDING |

### Significant files / migrations

- New backend `com.carepath.intelligence` module: processing controller/service/jobs/worker,
  isolated ExtractionEngine/worker entry point, text/OCR interfaces, parsers, provenance validator,
  candidate DTOs/repository and validated properties.
- Additive `V4__extraction_candidates.sql`; V1–V3 unchanged. Extended H2 test subset and
  `scripts/extraction-schema-invariants.sql`; both schema runners include V4 checks.
- Extended security route allowlist, typed audit actions/SYSTEM worker events and environment config.
  Existing JWT/session/CSRF/CORS/ownership/file handling controls remain covered by prior tests.
- Frontend `extraction-client.ts`, `extraction-panel.tsx`, document viewer source navigation and
  Records copy; status refresh retains unsaved metadata, version conflict protection remains.
- Tests: `ExtractionRulesTest`, `ExtractionRuntimeTest`, `ProcessingIntegrationTest`,
  `ScheduledProcessingIntegrationTest`, `ExtractionEvaluationIT`, extended `FrontendProxyIT`,
  `extraction-client.test.ts`.
- `sample-data/phase4/`, fixture generator, `evaluation/ground-truth.json`,
  `evaluation/run.sh`, `evaluation/results/latest.json`, `docs/DOCUMENT_INTELLIGENCE.md`.

### Exact verification and results

Executed from `/workspace/scratch/a24310fa97ab`, 2026-09-23:

```sh
JAVA_HOME=/workspace/scratch/toolchains/jdk-21.0.12.1+1 /workspace/scratch/toolchains/apache-maven-3.9.11/bin/mvn -o -Dmaven.repo.local=/workspace/scratch/toolchains/m2 -f carepath/backend/pom.xml -B '-Dtest=*Test,ExtractionEvaluationIT,FrontendProxyIT' package
```

COMPLETE + VERIFIED: fresh source/test compilation, all 101 tests passed, zero failures/errors/skips,
executable Spring Boot JAR packaged. Breakdown: 57 prior default tests + 20 extraction rules +
11 isolated runtime/resource tests + 9 processing API tests + 1 actual scheduler test +
2 real OCR/evaluation tests + 1 production Next HTTP proxy test. The last test preserves previous
large-file/auth checks and adds the Phase 4 upload→process→status→candidates→evidence→preview→delete path.
The extended proxy test was rerun with the same command replacing the test selection with
`-Dtest=FrontendProxyIT`: 1/1 passed.

```sh
cd carepath/frontend
npm run typecheck
npm run lint
npm test
npm run build
```

COMPLETE + VERIFIED: TypeScript/lint/production build passed; ten tests passed, zero failures/skips.
Build routes include /records and /records/[id]. No rendered browser session was run.

```sh
node carepath/scripts/schema-check/verify.mjs
command -v docker postgres redis-server chromium chromium-browser google-chrome tesseract
tesseract --version
tesseract --list-langs
```

COMPLETE + VERIFIED: PGlite applied V1–V4 and passed 30 schema invariants; rollback left no test users.
Real Tesseract 5.3.4 with eng was executed. BLOCKED: native PostgreSQL/Flyway/Redis/Compose and browser;
those executables were absent. PGlite is not a substitute for native verification.

Packaged worker smoke: Java 21 with `-Xmx256m -Djava.awt.headless=true
-Dloader.main=com.carepath.intelligence.ExtractionWorkerMain -cp
carepath/backend/target/carepath-backend-0.1.0-SNAPSHOT.jar
org.springframework.boot.loader.launch.PropertiesLauncher`, January fixture/input MIME, private
temporary result path, `tesseract 30 20 250000`: exit 0, three actual candidates, no worker error.
This checks the production JAR worker entry point; it does not start native backing services.

Evaluation in the complete suite: six documents, 16 expected/predicted/matched rows, classification
6/6 and name/value/unit/reference/date/provider/flag/provenance 16/16 each; row precision/recall 1.0.
Every number comes from the committed measured JSON. All six are synthetic lab documents;
there is no held-out/multiclass/real-patient accuracy claim. JPEG OCR was also exercised separately.
Evidence summaries are retained in `docs/verification/phase4-*`.

### Failures found and corrected

- Footer/prose lines produced false candidates; now short delimited prose is excluded and loose
  numeric rows require lexical unit evidence. Evaluation counts extras rather than hiding them.
- Fixed integration-test clock microsecond rounding and reauthenticated after intentional token expiry;
  production expiration enforcement was retained.
- Worker results are provenance-validated before persistence; candidate source offsets/metadata
  associations are checked, and job/document/owner identity is constrained.
- Added child parent/deadline watchdog, stale staging reaping and symlink-safe cleanup tests.
- Cancelled/stale results no longer count as completed; worker audits now identify SYSTEM.
- Preserved unsaved metadata during status refresh; extraction messages do not claim verified results.
- Two corrupt local Maven cache JARs were re-fetched without changing dependency versions.
- Offline `clean` could not resolve an uncached clean plugin; generated target output was removed
  and package rerun to force compilation. No test/build gate was waived.

### Remaining limitations / next phase

- BLOCKED: native PostgreSQL/Flyway/Redis, Compose readiness, restart persistence and rendered browser.
  H2 integration and real Next HTTP proxy are the strongest executed application environment.
- PARTIAL: parser coverage is deliberately bounded to supported English headings/table layouts.
  OCR, handwriting, unfamiliar units/layouts, noisy/rotated scans and multi-column reports can be missed.
- PARTIAL: process isolation bounds extraction, not all Phase 3 upload/preview parsers. No OS sandbox,
  cgroup total-RSS/CPU/disk quotas, antivirus or strict distributed tenant quota. Private staging after a
  crash may remain up to an hour; production encrypted volumes/ACLs and operational review remain needed.
- PENDING: native PDF coordinate highlights (page/text offsets implemented), OCR box overlay in UI,
  human confirm/correct/reject, concept normalization/LOINC/unit conversion and trusted promotion.
- PENDING: all later longitudinal, What Changed, AI, symptoms, appointments/reminders, Visit Packs,
  clinician sharing and nearby care. None were started.
- Recommended Phase 5: deterministic medical concept/unit normalization, keeping originals/provenance,
  then an explicitly scoped human verification/trusted-promotion workflow. Close native/browser gates
  before claiming deployment readiness.

Packaging: COMPLETE + VERIFIED — `python3 carepath/scripts/package.py --phase phase4`; ZIP CRC/readability and expected V4/source entries checked, 176 source/fixture/evaluation files; no credentials, keys, runtime storage, dependency or build directories. V1–V3 match the Phase 3 archive byte-for-byte.

## Phase 5 — medical normalization and human verification (2026-09-23)

This section supersedes historical statements that normalization/human review is unavailable.
It does not supersede any native-service/browser or production-security limitations above.
Implementation status and verification scope are stated separately; no clinical accuracy is claimed.

| # | Requirement | Status | Evidence / boundary |
| --- | --- | --- | --- |
| 01 | Canonical concept model | COMPLETE + VERIFIED | Relational existing model extended with active flag; version/source/nullable standard identifiers; real catalog startup tested |
| 02 | Deterministic dictionary | COMPLETE + VERIFIED | 23 seeded demo concepts and explicit reviewed aliases; limited scope, clinical review still required for deployment |
| 03 | Standard identifiers | COMPLETE + VERIFIED | Nullable identifier support; every demo identifier intentionally null, tested; no guessed LOINC |
| 04 | Concept normalization | COMPLETE + VERIFIED | EXACT/ALIAS_MATCH/AMBIGUOUS/UNMAPPED, original term preserved; deterministic tests and authored ground truth |
| 05 | Normalization safety | COMPLETE + VERIFIED | No fuzzy equivalence/context inference; fasting/assay variants abstain; Urea/BUN distinguished |
| 06 | Unit model | COMPLETE + VERIFIED | Original/effective/normalized fields stored separately; BigDecimal DTO/domain values |
| 07 | Safe conversions | COMPLETE + VERIFIED | 23 same-unit rules + 3 conversions; unsupported/missing cases return explicit null normalization |
| 08 | Concept-dependent conversion | COMPLETE + VERIFIED | Glucose-only molecular rule; unrelated analyte and incompatible unit tests |
| 09 | Numeric precision | COMPLETE + VERIFIED | Exact power-of-ten scaling, source/factor significance, HALF_EVEN boundary tests; normalized JSON strings |
| 10 | Reference ranges | COMPLETE + VERIFIED | Original text/flags preserved, separate numeric bounds/derived status; no imported generic ranges; comparison before rounding |
| 11 | Trust decision | COMPLETE + VERIFIED | Every candidate starts pending; native HIGH with no warnings only eligible for direct human confirmation; OCR/unknown/unsupported needs explicit review |
| 12 | Confirm / Correct / Reject | COMPLETE + VERIFIED | Actual API/security/JDBC/extraction integration, all three actions, not mocked promotion |
| 13 | Correction safety | COMPLETE + VERIFIED | Editable DTO allowlist, explicit source acknowledgement/reason, revalidation, active concept IDs, no client trust/source/actor fields |
| 14 | Trusted observation | COMPLETE + VERIFIED | Persisted original/effective/normalized readings, human metadata, source candidate and owner; trusted view requires history/source |
| 15 | Immutable provenance | COMPLETE + VERIFIED | Original candidates unchanged; exact document/page/snippet/offset retained; machine/corrected values separate, tampering tests |
| 16 | Verification audit | COMPLETE + VERIFIED | Typed general events + relational private decision history, changed field names, actor/timestamp/idempotency hash |
| 17 | Idempotency / state | COMPLETE + VERIFIED | Terminal transitions, identical replay, changed replay conflict, one observation per candidate |
| 18 | Document state integration | COMPLETE + VERIFIED | Candidate-bearing extraction → NEEDS_REVIEW; all resolved → COMPLETED, including rejected rows; never automatic trust |
| 19 | Review APIs | COMPLETE + VERIFIED | Owner-scoped queue/detail/preview/actions/observation; server pagination; actual Next HTTP proxy confirmation/read |
| 20 | Review UI | COMPLETE + NOT LIVE-VERIFIED | /review + document panel, extracted/normalized/verified distinction, confidence/evidence/actions; TypeScript/lint/build/client tests, no rendered browser |
| 21 | Correction UI | COMPLETE + NOT LIVE-VERIFIED | Prefilled permitted fields, concept selection, server preview, attestation, explicit verification; preview invalidated after edits; rendered interaction pending |
| 22 | Verified UI | COMPLETE + NOT LIVE-VERIFIED | Separate verified filters and stored observation display, original/source preserved; no timeline |
| 23 | Security review | COMPLETE + VERIFIED | Actual route/auth/query/body/DTO/SQL/text-rendering inspection + adversarial integration tests; not an external pentest |
| 24 | Concept tests | COMPLETE + VERIFIED | Alias/case/space/unknown/ambiguous/fuzzy/context/no-code cases in NormalizationTest |
| 25 | Unit tests | COMPLETE + VERIFIED | Same-unit, scale/molecular rules, unsupported/missing/comparators, exact scale and rounding ties |
| 26 | Review tests | COMPLETE + VERIFIED | Actions, revalidation, original/evidence/audit retention, idempotency, deletion and supplied-source tests |
| 27 | Two-user IDOR | COMPLETE + VERIFIED | Both directions list/detail/preview/confirm/correct/reject/observation denied; actual security stack |
| 28 | Concurrency | COMPLETE + VERIFIED | Simultaneous confirmation → one trusted observation; stale version/replay checks in H2; native PostgreSQL locks NOT LIVE-VERIFIED |
| 29 | Synthetic data | COMPLETE + VERIFIED | Original January/April/September unchanged; new rendered/inspected normalization PDF; actual pipeline tests + JSON fixtures |
| 30 | Evaluation | COMPLETE + VERIFIED | Executed authored normalization and three-report verification/provenance metrics, stored separately from production code |
| 31 | Additive database | COMPLETE + VERIFIED | V5 migration, composite owner/source FKs, unique promotion, indexes/version/history; PGlite SQL and H2 checks only |
| 31a | Native V1–V5 Flyway | BLOCKED | No Docker/native PostgreSQL executable; no live Flyway claim |
| 32 | Bounded terminology cache | COMPLETE + VERIFIED | Immutable bounded startup snapshot, no per-row SQL lookup, explicit migration/restart updates |
| 33 | Documentation | COMPLETE + VERIFIED | README/API/ARCHITECTURE/SECURITY/TESTING/status + detailed normalization/review guide and demo instructions source-checked |
| 34a | Backend compile/test/package | COMPLETE + VERIFIED | Final full run: 141 tests, zero failures/errors/skips; Java compile, test compile, executable JAR package passed |
| 34b | Frontend checks | COMPLETE + VERIFIED | TypeScript, lint, 14 client tests, production build including /review passed |
| 34c | Native PostgreSQL/Flyway/Redis | BLOCKED | Executables absent; no native service/restart/rate-limit claim; historical gaps retained |
| 34d | Rendered browser | BLOCKED | No browser executable; actual React flow, keyboard/modal/page navigation not executed |
| 35 | Status / phase boundary | COMPLETE + VERIFIED | Explicit evidence scope; no Phase 6 longitudinal/trends/AI/care work begun |

### Significant files and compatibility

- Additive `backend/src/main/resources/db/migration/V5__normalization_and_verification.sql`.
  V1–V4 were compared byte-for-byte with carepath-phase4.zip and are unchanged.
- New `terminology/{Terminology,UnitNormalizer}` and `review/{ReviewController,ReviewService,
  ReviewRepository,ReviewPolicy,ReviewDtos}` modules. Existing MedicalDocument/storage/auth flows remain.
- Existing ProcessingWorker changes only the candidate-bearing completion state. Existing security
  allowlist gains explicit review/terminology/observation read routes; review POST body is bounded.
  AuditService gains three typed review events; foundation stage now identifies Phase 5.
- Frontend `lib/review-client.ts`, `components/review/` (review panel, correction dialog, normalization
  preview), `/review`; existing document viewer/extraction panel and shell integrate these components.
- Tests `NormalizationTest`, `ReviewIntegrationTest`, `NormalizationEvaluationIT`, extended
  `FrontendProxyIT`, `review-client.test.ts`; H2 compatible schema plus V5; new review schema invariants.
- `evaluation/phase5/ground-truth.json`, `evaluation/run-normalization.sh`, measured Phase 5 results,
  `sample-data/phase5/normalization-lab.pdf`, reproducible generator and guide.
- No Phase 1–4 tests removed. Two extraction-success assertions now require NEEDS_REVIEW for HIGH
  candidate-bearing reports, as mandated by the new human-trust boundary; security checks preserved.

### Verification commands

Executed from `/workspace/scratch/a24310fa97ab` with the existing Java 21/offline Maven toolchain:

```sh
JAVA_HOME=/workspace/scratch/toolchains/jdk-21.0.12.1+1 /workspace/scratch/toolchains/apache-maven-3.9.11/bin/mvn -o -Dmaven.repo.local=/workspace/scratch/toolchains/m2 -f carepath/backend/pom.xml -B '-Dtest=*Test,ExtractionEvaluationIT,NormalizationEvaluationIT,FrontendProxyIT' package
npm --prefix carepath/frontend run typecheck
npm --prefix carepath/frontend run lint
npm --prefix carepath/frontend test
npm --prefix carepath/frontend run build
node carepath/scripts/schema-check/verify.mjs
command -v docker postgres redis-server chromium chromium-browser google-chrome tesseract
python3 carepath/scripts/generate-normalization-fixture.py
pdftoppm -scale-to 1000 -singlefile -png carepath/sample-data/phase5/normalization-lab.pdf /tmp/phase5-fixture
```

Earlier focused runs: FoundationSecurityTest 6/6; normalization/review/evaluation 38/38; full regression
140/140 before adding the final synthetic PDF test. The final authoritative run is recorded below.
Frontend: 14/14 configured tests, no failures/skips; TypeScript/lint/production build passed.
PGlite: actual V1–V5 SQL applied, 36 rollback-only invariants passed, zero persisted test users.
Native PostgreSQL/Flyway/Redis/Compose and browser remain BLOCKED; only Tesseract was found.
Native OCR runs remain part of the complete Phase 4 regression/evaluation suite.
The new synthetic PDF was rendered and visually inspected; that is not browser UI verification.

### Measured synthetic evaluation

`phase5-normalization.json`: 18 authored cases; concept outcomes 18/18, unit outcomes 18/18, including
8 intentional normalization abstentions. Exact decimal strings test retained justified precision.
`phase5-review.json`: three actual reports, 11 candidates; 9 confirmed, 1 corrected, 1 rejected;
source/original preservation 11/11. The correction retains raw TSH `l.8?` and effective `1.8` separately.
These are small authored regression fixtures, not held-out generalization or clinical accuracy.
The older extraction evaluation is rerun unchanged as part of the full suite.

### Findings and fixes

- Fixed an overprecise test expectation: 0.05551 has four significant digits, so the bounded result
  is 5.000 rather than 5.0000. Added actual HALF_EVEN tie assertions; production precision was retained.
- New g/L PDF test initially expected direct confirmation, but Phase 4 extraction correctly marks
  that unit uncertain. The test now requires explicit correction/source acknowledgement while checking
  deterministic normalization. No confidence rule was weakened to make the test pass.
- Preserved persisted concept-name snapshots rather than re-reading a changed catalog name.
- Added bounded review requests, exact source/original-field checks, forged-field rejection,
  owner-scoped trusted reads and serialized/idempotent promotion. Excluded unsupported offset rules;
  conflicting eligible rules abstain. Separate raw/effective data prevents rewriting extraction history.
- Corrected safe frontend conflict messages; correction previews invalidate on edits and terminal
  failures remain visible. No raw error/body/medical logs were added.

### Remaining limitations and Phase 6 recommendation

- BLOCKED: native PostgreSQL/Flyway V1–V5, Redis runtime/rate limiter, Compose readiness/restart persistence
  and rendered-browser review flow. H2/PGlite/HTTP proxy are not substitutes for these gates.
- PARTIAL: terminology is intentionally limited to 23 demo concepts; no LOINC mapping, fuzzy inference,
  broad unit coverage or clinical terminology certification. No native distributed-concurrency claim.
- PENDING: correction of completed observations/reopening rejected candidates, terminology administration,
  classification review, historical re-normalization and clinician certification. All current decisions
  are terminal and reasons are private owner data. Unknown explicitly reviewed readings remain unmapped;
  future comparability logic must exclude unsupported normalization/thresholds as appropriate.
- PENDING: all later timeline/trend/What Changed/AI/symptom/appointment/reminder/Visit Pack/share/nearby care
  product features. Existing schema placeholders are not implemented functionality.
- Recommended Phase 6: after closing native/browser gates, build owner-scoped longitudinal observation
  history using verified provenance and strict concept/unit/date comparability. Do not infer diagnoses.


### Final authoritative result

COMPLETE + VERIFIED: the final full command above passed **141 tests, 0 failures, 0 errors,
0 skipped; BUILD SUCCESS**. Java source/test compilation and executable JAR packaging succeeded.
Breakdown: 101 prior full-suite cases + 25 normalization cases + 14 review integration cases +
1 normalization evaluation. The existing Next HTTP integration now also confirms and retrieves an
observation through the production same-origin proxy. Frontend remains **14/14 tests**, TypeScript,
lint and production build PASS. Evidence summaries: `docs/verification/phase5-backend.json`,
`phase5-frontend.txt`, `phase5-schema.txt`; measured JSON under `evaluation/results/`.
Native PostgreSQL/Flyway/Redis and rendered-browser gates remain BLOCKED, not verified.

Packaging command: `python3 carepath/scripts/package.py --phase phase5` from the workspace parent.
The archive is source-only; credentials, generated keys, uploaded files, dependencies, target and
.next are excluded. This checkpoint does not claim production deployment readiness.

Packaging: COMPLETE + VERIFIED — `carepath-phase5.zip`, 206 source/fixture/evaluation files;
ZIP CRC, required V5/PDF entries and exclusion of credentials, runtime storage, dependencies and build
outputs checked. The archive preserves the original Phase 1–4 migration bytes.


## Phase 6 authoritative checkpoint — 2026-09-24

Prior ledgers remain historical evidence, including their open native/browser gates. Phase 6 supersedes
older statements that timeline/changes are unimplemented; future AI and care-navigation features remain PENDING.

| Requirement | Status | Evidence / limitation |
| --- | --- | --- |
| P6.01 Longitudinal observation history | COMPLETE + VERIFIED | Owner-scoped chronological trusted history, original/effective/normalized data and sources; real three-report integration. |
| P6.02 Comparability engine | COMPLETE + VERIFIED | Same concept/unit/normalization version, exact values, strict dates, same lab and compatible known context; explicit abstention. |
| P6.03 Change detection | COMPLETE + VERIFIED | Deterministic direction, stability, new/absent, insufficiency and separate reference-transition outcomes. |
| P6.04 Stability policy | COMPLETE + VERIFIED | Configurable generic relative tolerance and concept overrides; boundary tests; not a clinical threshold. |
| P6.05 Delta calculation | COMPLETE + VERIFIED | BigDecimal signed deltas, rounded percentages, zero and negative baseline cases. |
| P6.06 Reference intervals | COMPLETE + VERIFIED | Own supplied ranges; abstains on changed bounds/units; source flags separate. |
| P6.07 Newly observed | COMPLETE + VERIFIED | Fully reviewed selected comparable records; at least three shared concepts or matching explicit panel. |
| P6.08 Previously tracked absent | COMPLETE + VERIFIED | Matching explicit panel required; synthetic API coverage tests; not proof a clinical test disappeared. |
| P6.09 Trend summary | COMPLETE + VERIFIED | Three-point monotonic/stable/mixed summaries; incompatible/same-day points break sequences. |
| P6.10 Service boundaries | COMPLETE + VERIFIED | HistoryService, repository, comparison, stability, trend and DTO modules exercised by tests. |
| P6.11 What Changed engine | COMPLETE + VERIFIED | Explicit report snapshot comparison; duplicates abstain; both sources retained. |
| P6.12 Safe templates | COMPLETE + VERIFIED | Structured deterministic numeric explanations; no LLM. |
| P6.13 Provenance | COMPLETE + VERIFIED | Exact January/September document IDs/pages/snippets asserted for both sides. |
| P6.14 History APIs | COMPLETE + VERIFIED | Authenticated events/concepts/history/compare/changes/evidence routes; pagination and HTTP proxy coverage. |
| P6.15 Timeline frontend | COMPLETE + NOT LIVE-VERIFIED | Real API-backed /timeline built; rendered-browser gate pending. |
| P6.16 Timeline UX | COMPLETE + NOT LIVE-VERIFIED | Grouping/search/concept/document filters, pagination, loading/errors/details; browser pending. |
| P6.17 Trend charts | COMPLETE + NOT LIVE-VERIFIED | Recharts linear observed points, unit, tooltip, keyboard links and text fallback; rendered behavior pending. |
| P6.18 Evidence interaction | COMPLETE + NOT LIVE-VERIFIED | Internal source/candidate links reuse exact page viewer; API source verified, browser interaction pending. |
| P6.19 What Changed UI | COMPLETE + NOT LIVE-VERIFIED | Real two-report selector, structured rows and both sources; browser pending. |
| P6.20 Evidence sufficiency UX | COMPLETE + NOT LIVE-VERIFIED | Incomparable readings/reasons remain visible; browser pending. |
| P6.21 No AI calculation | COMPLETE + VERIFIED | All calculations execute deterministic Java code; no model/provider introduced. |
| P6.22 Database | COMPLETE + NOT LIVE-VERIFIED | Additive V6 read indexes; V1–V5 unchanged. PGlite 36 invariants pass; native Flyway pending. |
| P6.23 Correction/invalidation | COMPLETE + VERIFIED | Fresh repeatable-read calculations; source mutation/deletion tests; no irreversible trend store. |
| P6.24 Authorization | COMPLETE + VERIFIED | Security-context identity, owner-scoped joins, generic 404, default-deny route allowlist. |
| P6.25 History tests | COMPLETE + VERIFIED | Exact chronology, out-of-order ingestion, same-day/undated, pending/rejected/unverified exclusion. |
| P6.26 Change tests | COMPLETE + VERIFIED | Increase/decrease/stability/new/absent/insufficient, units, zero, precision, context and concept mismatch. |
| P6.27 Reference tests | COMPLETE + VERIFIED | Enter/exit/remain, missing/textual/different bounds/source units and laboratory mismatch. |
| P6.28 Provenance tests | COMPLETE + VERIFIED | Exact source observation/document/page and source snippets, not only non-null fields. |
| P6.29 IDOR tests | COMPLETE + VERIFIED | Two users, both directions: histories, changes, evidence and protected source documents. |
| P6.30 Synthetic demo | COMPLETE + VERIFIED | Unchanged January/April/September reports; decrease/increase/stable/new through API; incomparable cases tested separately. |
| P6.31 Evaluation | COMPLETE + VERIFIED | Actual measured grouping 3/3, labels 5/5, sources 8/8; 12/12 direction/comparability/reference fixtures. |
| P6.32 Performance | PARTIAL | Bounded SQL pagination, batched evidence and V6 indexes implemented; no native execution-plan/load benchmark. |
| P6.33 Security review | COMPLETE + VERIFIED | Owner predicates, escaped SQL search, decimal arithmetic, React text rendering, no sensitive logging/cache; targeted regression tests pass. |
| P6.34 Frontend quality | COMPLETE + NOT LIVE-VERIFIED | Responsive layouts/accessibility affordances implemented and statically checked; rendered/mobile audit pending. |
| P6.35 Verification | PARTIAL | 172 backend and 17 frontend tests pass; builds pass. Native services and browser blocked. |
| P6.36 Documentation | COMPLETE + VERIFIED | Required docs updated and checkpoint boundaries recorded; does not verify runtime by itself. |
| P6.37 Project status | COMPLETE + VERIFIED | This 37-item ledger distinguishes executed checks from live verification gaps. |

### Exact verification and delivered files

Commands are recorded verbatim in TESTING.md (Phase 6). Full offline Maven `package` with
`*Test,ExtractionEvaluationIT,NormalizationEvaluationIT,LongitudinalEvaluationIT,FrontendProxyIT`
passed **172 tests, 0 failures, 0 errors, 0 skipped**, including Java compile and executable JAR.
Frontend `typecheck`, `lint`, `test` and `build` passed; **17 tests, no failures/skips**.
PGlite `node carepath/scripts/schema-check/verify.mjs` applied V1–V6 and passed 36 invariants.
The initial stale generated JAR ZIP-end failure was resolved by removing generated JARs only, then
rerunning the complete suite. Final frontend rerun also passed after isolating independent load errors.

Significant files: backend `longitudinal/` module; `V6__longitudinal_read_indexes.sql`; three new
longitudinal test classes; extended FrontendProxyIT; frontend `components/history/`, `lib/history-client*`,
`app/timeline/`, `app/changes/`; Recharts dependency/lock; security route allowlist and tolerance config;
`evaluation/phase6/`, `evaluation/run-longitudinal.sh`, measured result JSON and
`docs/LONGITUDINAL_INTELLIGENCE.md`. All required top-level documentation updated.
Verification summaries reside in `docs/verification/phase6-*`.

### Open gates and limitations

- BLOCKED: native PostgreSQL/Flyway V1–V6, Redis runtime/rate limiter, Docker Compose/restart persistence.
  Executable discovery found none of Docker/PostgreSQL/Redis. H2 and PGlite are explicitly not substitutes.
- BLOCKED: rendered-browser/mobile workflow; browser executables unavailable. Real Next HTTP proxy tests
  verify API integration, not charts, keyboard interaction, layout or visual accessibility.
- PARTIAL: unknown assay/specimen context allows only qualified same-lab numerical comparison, never
  clinical equivalence. Panel coverage is conservative and cannot prove the extractor missed no rows.
- PARTIAL: bounded 500-point trend window, 500 readings/report, first 100 UI concept choices; no native
  performance benchmark. Full history/event API pagination remains available. Current catalog has 23 concepts.
- PENDING: reopening/correcting completed observations through UI, broader terminology, AI explanations,
  symptoms, appointments, reminders, Visit Packs, sharing, nearby care and every other later-phase feature.

Recommended Phase 7: close native/browser gates, then build controlled evidence-grounded explanations
and clinician questions over already-computed verified structured facts. Keep numerical calculations
and medical safety rules deterministic. No Phase 7 work is included in this checkpoint.

Packaging command: `python3 carepath/scripts/package.py --phase phase6`. Source-only archive excludes
credentials, uploads, dependencies and build outputs. V1–V5 bytes were compared with Phase 5 and unchanged.

Packaging: COMPLETE + VERIFIED — 235 source/fixture/evaluation files; archive CRC and exclusions checked.
The archive includes V6 and preserves all V1–V5 migration bytes. This is a development checkpoint,
not a claim of production deployment readiness.


## Phase 7 authoritative checkpoint — 2026-09-24

Earlier ledgers are historical. Phase 7 adds only record explanations and clinician questions, with
no later care/workflow/navigation feature enabled. The model is explicitly constrained to selecting
approved grounded wording; this is not unrestricted medical chat.

| Requirement | Status | Evidence / limitation |
| --- | --- | --- |
| P7.01 Verified data only | COMPLETE + VERIFIED | Trusted view, owner-scoped history; pending/rejected/unverified API exclusion tests. |
| P7.02 Provider abstraction | COMPLETE + VERIFIED | HealthExplanationProvider separates provider from retrieval/domain; deterministic fallback. |
| P7.03 Real provider implementation | COMPLETE + NOT LIVE-VERIFIED | OpenAI Chat Completions HTTPS adapter; contract tested, no live credential used. |
| P7.04 Grounded retrieval | COMPLETE + VERIFIED | Exact/alias concept scope, selected observation/reports, latest/month/year selection; bounded points. |
| P7.05 Question scope | PARTIAL | Supported record/value/trend/change/source intents; limited English deterministic matching, not unrestricted language understanding. |
| P7.06 Structured AI input | COMPLETE + VERIFIED | Intent + computed Fact objects + request-local citations/approved phrasings; contract/minimization assertions. |
| P7.07 Prompt-injection defense | COMPLETE + VERIFIED | Raw question and source snippets excluded; existing malicious PDF tested; strict output selections cannot become instructions. |
| P7.08 Output contract | COMPLETE + VERIFIED | Strict JSON and server phrase/citation validation, unknown/free-text fields rejected. |
| P7.09 Evidence citations | COMPLETE + VERIFIED | Server source map, exact observation/document/page; model has ephemeral references only. |
| P7.10 Fact consistency | COMPLETE + VERIFIED | Model cannot author values or direction; only complete approved phrasings accepted. |
| P7.11 Deterministic fallback | COMPLETE + VERIFIED | Useful labelled rule-generated responses for missing key, failure, timeout, invalid output and rate limits. |
| P7.12 Medical safety boundary | COMPLETE + VERIFIED | No generated diagnosis/dose/treatment actions; requested unsafe prompts tested. Not clinical certification. |
| P7.13 Urgent-symptom safety | COMPLETE + VERIFIED | Limited predefined English escalation phrases, cited reference URLs and conditional local-emergency wording; clinical review pending. |
| P7.14 Uncertainty | COMPLETE + VERIFIED | Missing facts, singleton trends, incompatible units/context and ambiguous scope explicitly surfaced. |
| P7.15 Reference separation | COMPLETE + VERIFIED | Only predefined safety sources; no external medical ranges. UI separates data/wording/reference layers. |
| P7.16 Assistant API | COMPLETE + VERIFIED | Authenticated transient POST answer + config; evidence via existing owner-scoped APIs, no conversation persistence. |
| P7.17 Privacy/minimization | COMPLETE + VERIFIED | Explicit per-request AI consent; no raw question/text/files/account metadata/UUIDs in provider context; assertions inspect actual context. |
| P7.18 Logging | COMPLETE + VERIFIED | Safe outcome/duration/correlation only; audit privacy assertions; no prompt/response logging. |
| P7.19 Clinical question builder | COMPLETE + VERIFIED | Deterministic suggestions from selected verified facts with source IDs; save/edit/delete API. |
| P7.20 Question generation | COMPLETE + VERIFIED | Server templates refer to actual readings/change outcome; no model-invented abnormalities. |
| P7.21 SavedQuestion | COMPLETE + VERIFIED | Owner-scoped JDBC model, sourceType USER, timestamps/version and normalized evidence links; API tests. |
| P7.22 Assistant frontend | COMPLETE + NOT LIVE-VERIFIED | Real /assistant API integration, input/scope/suggestions/loading/error/unavailable states; build/client tests, browser pending. |
| P7.23 Response evidence UI | COMPLETE + NOT LIVE-VERIFIED | Fact → evidence details → original page via existing viewer; exact API sources tested, browser pending. |
| P7.24 Visual distinction | COMPLETE + NOT LIVE-VERIFIED | Verified record facts, AI-approved wording/deterministic explanation, uncertainty and safety references labelled; browser pending. |
| P7.25 Clinical questions UI | COMPLETE + NOT LIVE-VERIFIED | Generate/edit-before-save/manual/create/edit/delete confirmation/pagination with linked sources; browser pending. |
| P7.26 IDOR security | COMPLETE + VERIFIED | Both-direction API tests for selected scope, report pair, evidence and saved-question reads/writes/deletes/lists. |
| P7.27 Prompt-injection tests | COMPLETE + VERIFIED | Existing PDF and injected user question remain data; no raw instructions sent to model. |
| P7.28 Output injection | COMPLETE + VERIFIED | No arbitrary model prose/URLs accepted; plain React text, no Markdown/raw HTML renderer. |
| P7.29 Evidence forgery | COMPLETE + VERIFIED | Nonexistent/malformed/foreign UUID/unsupplied references reject to fallback; actual other-user observation tested. |
| P7.30 Safety tests | COMPLETE + VERIFIED | Five specified diagnosis/medication/dose prompts plus urgent phrase cases; provider never called. |
| P7.31 Grounding tests | COMPLETE + VERIFIED | Hemoglobin chronology/direction, April–September changes equal Phase 6, exact September source page 2. |
| P7.32 Insufficient-evidence tests | COMPLETE + VERIFIED | Unknown concept/no verified points/one point/unit mismatch/outside record scope/absent year. |
| P7.33 Provider failure tests | COMPLETE + VERIFIED | Controlled transport: missing key, timeout, HTTP, rate limit, empty/malformed/refused/schema-invalid/unsupported prose; not live model. |
| P7.34 Minimization test | COMPLETE + VERIFIED | Hemoglobin request excludes Vitamin D/Sodium, raw injection text, filenames/source UUIDs and identity metadata. |
| P7.35 Audit | COMPLETE + VERIFIED | Five new typed actions; no clinical text in general audit; CRUD/audit assertions. |
| P7.36 Database | COMPLETE + NOT LIVE-VERIFIED | Additive V7 applied in PGlite, four new invariants; composite ownership/source FKs. Native Flyway pending. |
| P7.37 Performance/cost | COMPLETE + VERIFIED | 8KiB body/1000 chars/12 readings/16KB context; 1 call, no retries, 15s timeout, streamed byte bound, rate10/min and concurrency2; no load benchmark. |
| P7.38 Frontend quality | COMPLETE + NOT LIVE-VERIFIED | Integrated shell, plain responsive forms/evidence/drafts with accessible labels; rendered mobile/a11y checks pending. |

### Verification and files

COMPLETE + VERIFIED: full Java compile/test/package **203 tests, 0 failures, 0 errors, 0 skipped;
BUILD SUCCESS**. All prior 172 cases retained; new 14 assistant API cases and 17 provider-contract cases.
Actual Next production HTTP proxy test extended to assistant source and saved-question lifecycle.
Frontend **20 tests**, TypeScript, lint and production build PASS including /assistant.
PGlite V1–V7 and **40 schema invariants PASS**; this is not native PostgreSQL/Flyway verification.
Exact executed commands: TESTING.md Phase 7; summaries: docs/verification/phase7-*.

Created backend assistant/{AssistantDtos,RecordSafety,StructuredRetrieval,HealthExplanationProvider,
OpenAiExplanationProvider,ExplanationValidator,AssistantService,SavedQuestionService,AssistantController};
V7__saved_question_evidence.sql; AssistantIntegrationTest/ProviderContractTest; frontend assistant-client,
three client tests, components/assistant and /assistant route. Extended audit, exact security routes/body
bounds, config/env, shell, HTTP proxy test and schema harnesses. Required architecture/security/API/testing/
README/status docs updated; detailed decisions in docs/GROUNDED_ASSISTANT.md. V1–V6 byte-identical to Phase 6.

Findings fixed: test-only H2 timestamptz compatibility, missing extraction FK in schema fixture,
strict HTTPS retained without a production test bypass, explicit-year retrieval and duplicate env entries.
No test/security assertion was weakened. Source freshness is rechecked after external generation.
Provider free prose and forged citations cannot be displayed; source text never becomes instructions.

### Remaining limitations

- COMPLETE + NOT LIVE-VERIFIED: real provider adapter; no live model/credential call. Controlled provider
  tests prove contract/error behavior only, not actual provider availability/quality.
- BLOCKED: native PostgreSQL/Flyway V1–V7, Redis rate limiter/runtime, Compose and restart persistence.
  Current executable discovery found none; no substitution of H2/PGlite for native verification.
- BLOCKED: rendered browser/mobile/a11y workflow. Actual HTTP proxy is not browser interaction.
- PARTIAL: limited English request classification/urgent phrases, bounded latest12 concept window and
  latest100 report selection; no clinical safety certification, multilingual triage or general medical
  reference explanation. The model only selects approved wording. Saved text is a user draft and may be
  stale after correction; deleted evidence is detached/flagged. Future Visit Pack must re-resolve evidence.
- PENDING: symptoms, appointments, follow-ups, reminders, Visit Packs, clinician/QR sharing, nearby care,
  broader reference knowledge, native performance benchmarks and earlier production-hardening gates.

Recommended next phase: close native/browser and live-provider checks, then implement the agreed care
organization scope (symptoms/appointments/follow-ups) in a separate phase. No such work was started here.

Packaging command: `python3 carepath/scripts/package.py --phase phase7`. Source archive excludes keys,
credentials, transcripts, uploaded files, dependencies and build outputs. No production-readiness claim.

Packaging: COMPLETE + VERIFIED — carepath-phase7.zip contains 258 source/fixture/evaluation files;
ZIP CRC and exclusions checked. The archive includes V7 and preserves the V1–V6 migration bytes.


## Checkpoint V1 — 2026-09-24

- COMPLETE + VERIFIED: backend compile/package and all 203 tests, zero failures/errors/skips.
- COMPLETE + VERIFIED: frontend 20 tests, TypeScript, lint, production build and production HTTP landing startup.
- COMPLETE + VERIFIED: existing real HTTP proxy regression, explicitly H2/TestLimits-backed; no native-runtime claim.
- BLOCKED: native PostgreSQL 17, Flyway V1–V7 and persistence/restart; no service/tool/configuration available.
- BLOCKED: Redis runtime/restart and normal backend startup with native services.
- BLOCKED: rendered browser desktop/mobile E2E; remote Chrome refused workspace loopback with ERR_BLOCKED_BY_CLIENT.
- PENDING: live LLM provider and live prompt-injection check; no configured credential.
- COMPLETE + VERIFIED: baseline test counts preserved; application, tests and V1–V7 migration bytes unchanged. Documentation/comment repairs only.

Exact commands, environment, HTTP security findings, limitations and remaining acceptance gates: `docs/VERIFICATION_CHECKPOINT_V1.md`. Existing native/browser/provider gaps are not cleared. No Phase 8 modules implemented.

## Phase 8 authoritative checkpoint — 2026-09-28

This ledger supersedes earlier statements that care organization is unimplemented. Earlier test
counts and verification gates remain historical evidence, not current production-readiness claims.
Only Phase 8 was added. Visit Packs, clinician/QR sharing and nearby care remain PENDING.

| Requirement | Status | Evidence / limitation |
| --- | --- | --- |
| P8.01 Inspect and preserve existing code | COMPLETE + VERIFIED | Existing source/schema/security inspected; V1–V7 bytes unchanged against Checkpoint V1; all prior test files retained. |
| P8.02 Symptom domain | COMPLETE + VERIFIED | Owner, name, start/resolved instants, severity/frequency, notes, status, timestamps/version; API lifecycle tests. |
| P8.03 Symptom operations | COMPLETE + VERIFIED | Create/read/list/edit/resolve/reopen/delete; owner search/status/pagination and stale edit tests. |
| P8.04 Symptom timeline | COMPLETE + VERIFIED | Distinct SYMPTOM events; concept/document-filtered histories remain laboratory-only. |
| P8.05 Assistant boundary | COMPLETE + VERIFIED | Symptoms remain user-reported and excluded from current assistant context; API regression asserts no symptom-to-lab promotion. |
| P8.06 Appointment domain | COMPLETE + VERIFIED | Provider/specialty/time/zone/location/contact/URL/notes/status/follow-up date/timestamps/version persisted. |
| P8.07 Appointment operations | COMPLETE + VERIFIED | Create/read/edit/cancel/complete/delete, actual-instant upcoming/past ordering; idempotent terminal action. |
| P8.08 Appointment links | COMPLETE + VERIFIED | Owned documents/symptoms/saved questions, bounded lists; cross-owner injection/rollback tests. |
| P8.09 Follow-up detection | COMPLETE + VERIFIED | Bounded whole-line deterministic scheduling parser from actual extracted page text; publication hook and owner backfill API. |
| P8.10 Follow-up dates | COMPLETE + VERIFIED | Days/weeks/months/explicit dates, leap/month/year boundaries; missing anchor remains null. |
| P8.11 Explicit confirmation | COMPLETE + VERIFIED | Pending candidate → confirmed/edited/ignored; no automatic appointment; concurrent/repeated decisions tested. |
| P8.12 Follow-up provenance | COMPLETE + VERIFIED | Original PDF instruction/page/document asserted; anchor/suggestion retained separately from user-confirmed instant/date. |
| P8.13 Scheduling-only safety | COMPLETE + VERIFIED | Ambiguous/compound/treatment lines rejected; no medication actions or model invocation. |
| P8.14 Reminder domain | COMPLETE + VERIFIED | Persistent source-specific reminders, offsets/status/delivery metadata and idempotency constraints. |
| P8.15 Controlled offsets | COMPLETE + VERIFIED | 0/60/1440/2880/10080 minutes; exact trigger calculations, duplicate/invalid offsets rejected. |
| P8.16 Restart persistence | COMPLETE + NOT LIVE-VERIFIED | Database-backed rows; no native PostgreSQL restart performed. No in-memory timer is the source of truth. |
| P8.17 Scheduler | COMPLETE + VERIFIED | Production poller/service exercised against H2 with controllable clock, atomic delivery and concurrent duplicate suppression; native locking remains open. |
| P8.18 In-app notifications | COMPLETE + VERIFIED | Persistent list/count/read/read-all, generic content and source links; API tests. |
| P8.19 Email abstraction | PARTIAL | Extension interface exists; no adapter, credential or send operation configured. In-app delivery works independently. |
| P8.20 Dashboard | COMPLETE + NOT LIVE-VERIFIED | Real upcoming appointments, deterministic changes, pending reminders/follow-ups, unread count and documents; rendered browser pending. |
| P8.21 Appointments UI | COMPLETE + NOT LIVE-VERIFIED | Actual CRUD/status/link pickers/reminder settings, past/upcoming, date controls; static/client verification only. |
| P8.22 Symptoms UI | COMPLETE + NOT LIVE-VERIFIED | Real active/resolved CRUD, severity/frequency/dates/notes; browser pending. |
| P8.23 Follow-up UI | COMPLETE + NOT LIVE-VERIFIED | Candidate/source/suggestion/confidence, explicit date/time confirmation, ignore and source page navigation; browser pending. |
| P8.24 Notifications UI | COMPLETE + NOT LIVE-VERIFIED | Indicator/inbox/read controls/internal source links; no email or push delivery claim. |
| P8.25 Timezones | COMPLETE + VERIFIED | Offset preserved through JSON, validated against IANA zone/DST rules, UTC scheduling; conversion/mismatch tests. Device-zone UI rendering not live-verified. |
| P8.26 IDOR | COMPLETE + VERIFIED | Actual Spring Security/API tests for both user directions, linked resources, follow-up decisions, reminders and notifications. |
| P8.27 Mass assignment | COMPLETE + VERIFIED | Strict input DTOs/JSON, no writable owner/delivery/provenance; forged-field and bounded-body tests. |
| P8.28 Concurrency | COMPLETE + VERIFIED | Simultaneous confirmation/delivery, repeated status/read, stale appointment versions; H2 scope only. |
| P8.29 Audit | COMPLETE + VERIFIED | Typed symptom/appointment/follow-up/reminder events; no clinical notes/content in audit assertions. |
| P8.30 Symptom tests | COMPLETE + VERIFIED | Lifecycle, invalid input/dates, ownership, versions and timeline separation. |
| P8.31 Appointment tests | COMPLETE + VERIFIED | Lifecycle/order/past/timezones/links/URL validation/stale edits. |
| P8.32 Follow-up extraction tests | COMPLETE + VERIFIED | Four required relative phrases, explicit dates, missing anchor, empty/ambiguous/treatment lines; real synthetic PDF extraction. |
| P8.33 Follow-up workflow tests | COMPLETE + VERIFIED | Detect/confirm/edit/ignore, duplicate/concurrent confirmation, exact source preservation and ignored deduplication. |
| P8.34 Reminder tests | COMPLETE + VERIFIED | All offsets, future/due, clock advance, cancel/reschedule, metadata edit without redelivery, follow-up and repeated/concurrent polling. |
| P8.35 Notification tests | COMPLETE + VERIFIED | Unread/list/read/read-all/owner/source and single delivery; source-deletion cascade. |
| P8.36 Timeline regression | COMPLETE + VERIFIED | Existing numerical suites retained; symptom event integration does not enter verified concept histories. |
| P8.37 Assistant regression | COMPLETE + VERIFIED | Existing grounding/safety/minimization tests retained; explicit user-reported symptom exclusion test. |
| P8.38 Database | COMPLETE + NOT LIVE-VERIFIED | Additive V8; PGlite V1–V8 applies and 40 existing invariants pass. Native PostgreSQL/Flyway remains blocked. |
| P8.39 Security review | COMPLETE + VERIFIED | Owner predicates, relationship FKs, write allowlists, locks, controlled URLs, plain-text rendering and safe logs reviewed/tested; native/browser deployment gates remain. |
| P8.40 Frontend quality | COMPLETE + NOT LIVE-VERIFIED | Existing shell/design retained; labelled forms, responsive layout, loading/error/empty states and confirmations; rendered/mobile accessibility pending. |
| P8.41 Complete regression | COMPLETE + VERIFIED | 232 backend / 24 frontend tests pass, zero failures/errors/skips; Java package, TypeScript, lint and production build pass. Native/browser scope remains separately blocked. |
| P8.42 Live gaps | BLOCKED | No Docker/PostgreSQL/Redis/local browser executables. Remote browser previously refused loopback; no new live provider credential. |
| P8.43 Documentation | COMPLETE + VERIFIED | Six required docs updated plus care architecture/consistency manual; verification records distinguish test and live scopes. |
| P8.44 Status accuracy | COMPLETE + VERIFIED | This ledger covers all 44 items and keeps Checkpoint V1 gates visible. |

### Significant modules and repairs

New backend `care/` services/DTOs/controller/parser/scheduler/email port, `V8__care_organization.sql`,
CareRulesTest and CareIntegrationTest. Existing processing publication, audit, security route/body
allowlists and timeline event query integrate the module. Frontend `components/care/`, care-client,
four protected care routes, dashboard/shell and document viewer use real APIs. Test schema and additive
SQL harness updated. Environment adds REMINDERS_ENABLED and REMINDER_POLL_MS. Source packager supports phase8.

Two runtime defects were fixed without weakening controls: Jackson converting request offsets before
IANA validation, and extraction child parent-PID visibility in this restricted namespace. Offset fields
now preserve input; the child uses supervisor-pipe EOF plus independent deadline and existing limits.
A new real subprocess test covers loss of supervision. A corrupt generated Turbopack cache caused an
initial build abort; moving only that cache aside allowed a successful build without source changes.

### Remaining limitations and deployment gates

- BLOCKED: native PostgreSQL 17/Flyway V1–V8, Redis runtime, Compose/backend normal startup and native
  persistence/restart. H2/PGlite results do not clear these gates.
- BLOCKED: rendered desktop/mobile browser workflows, chart/dialog/accessibility review. HTTP proxy
  execution is not a browser test. Production TLS/cookie/CORS/CSRF runtime remains open.
- PENDING: live LLM provider and live injection check; contract/fallback tests are not live verification.
- PARTIAL: email port only, no outbound email; no guaranteed clinical alert delivery or push channel.
- PARTIAL: conservative English whole-line scheduling recognition; document date anchors only; no
  confirmed follow-up reschedule UI. OCR candidates stay LOW and all candidates require a user decision.
- PARTIAL: device-timezone entry only; bounded per-appointment link queries, no native load benchmark.
  Rescheduling/deletion removes related current-inbox notifications as documented, not immutable history.
- PENDING: Visit Packs/PDFs, sharing/QR, nearby care and all later-phase work. No Phase 9 implemented.

Recommended next phase: close native/runtime and rendered-browser gates, then Phase 9 Doctor Visit Pack
using fresh owner-scoped verified facts, symptoms, appointments, saved questions and source evidence.

### Final executed verification

COMPLETE + VERIFIED: full Maven compile/test/package **232 tests, 0 failures, 0 errors, 0 skipped;
BUILD SUCCESS** on 2026-09-28. Baseline 203 retained plus 22 care integration, 6 care rule and 1 extraction
supervision regression. Frontend **24 tests, all pass**, retaining the prior 20 plus 4 care client tests.
TypeScript, lint and production build pass. The existing real Next HTTP proxy test passed with the
H2/test-limiter backend; it is not native-service or rendered-browser verification. Existing synthetic
extraction/normalization/longitudinal evaluations reran in the full suite; no new clinical-accuracy claim.

PGlite V1–V8 applies with 40 existing invariants; V1–V7 bytes match Checkpoint V1. No old test file was
removed. Exact commands, failures repaired and verification limits are in TESTING.md; safe aggregate
results are in docs/verification/phase8-*. Full logs containing test fixtures are not packaged.

Packaging: COMPLETE + VERIFIED — `python3 carepath/scripts/package.py --phase phase8` creates
carepath-phase8.zip with 293 source/fixture/documentation files. ZIP CRC/exclusion checks pass;
V1–V7 bytes and all 26 baseline Java/frontend test files are retained. Credentials, uploads,
dependencies and build outputs are excluded. This remains a development checkpoint, not production certification.

## Phase 9 — Doctor Visit Pack, 2026-09-29

This ledger supersedes the historical Phase 8 statement that Visit Packs are pending. No sharing,
QR, maps, nearby care or unrelated redesign was implemented. Existing sources were continued,
not regenerated. Native/runtime gaps remain open. Detailed design: docs/VISIT_PACKS.md.

| Requirement | Status | Implemented/verified scope |
| --- | --- | --- |
| P9.01 Existing-code inspection/preservation | COMPLETE + VERIFIED | Existing checkpoint inspected; V1–V8 and 29 baseline test source files unchanged |
| P9.02 Preparation purpose | COMPLETE + VERIFIED | Selected source composition, no new medical facts/LLM |
| P9.03 User selection | COMPLETE + NOT LIVE-VERIFIED | Explicit source pickers, optional appointment, opt-in notes; browser pending |
| P9.04 VisitPack domain | COMPLETE + VERIFIED | Draft/generated, revision, version, dates and owner persistence in H2 tests |
| P9.05 Typed items | COMPLETE + VERIFIED | Seven source types plus manual question, owner-scoped resolution |
| P9.06 Snapshot semantics | COMPLETE + VERIFIED | Immutable generation, edits/deletions of live sources retain snapshot |
| P9.07 Trusted source boundary | COMPLETE + VERIFIED | Verified observations only, user-reported symptom labels, existing change engine |
| P9.08 Evidence | COMPLETE + VERIFIED | Exact document/page for observations and both comparison sides |
| P9.09 Pack structure | COMPLETE + VERIFIED | Canonical sections in web representation and rendered PDF |
| P9.10 Record summary | COMPLETE + VERIFIED | Deliberate selected values, source-supplied ranges, no internet ranges |
| P9.11 Change summary | COMPLETE + VERIFIED | Existing deterministic pair comparisons, no diagnostic conclusions |
| P9.12 Symptom summary | COMPLETE + VERIFIED | User-reported dates/severity/frequency/status, opt-in notes |
| P9.13 Clinician questions | COMPLETE + VERIFIED | Selected, reordered, overridden or manual pack-only questions; source unchanged |
| P9.14 Appointment context | COMPLETE + VERIFIED | Optional owned appointment with actual stored details/timezone |
| P9.15 Document references | COMPLETE + VERIFIED | Safe display metadata, no embedded original/internal paths in PDF |
| P9.16 APIs | COMPLETE + VERIFIED | Authenticated create/get/edit/preview/generate/revise/list/PDF/delete |
| P9.17 Preview | COMPLETE + VERIFIED | Same assembly as generation; fingerprint detects source changes |
| P9.18 PDF generation | COMPLETE + VERIFIED | Real local PDFBox PDF from immutable snapshot |
| P9.19 PDF quality | COMPLETE + VERIFIED | A4, wrapping, pagination, headers/footer; seven fixture pages visually inspected |
| P9.20 PDF text safety | COMPLETE + VERIFIED | Plain text, bounded long content, Unicode escape fallback, resource tests |
| P9.21 PDF metadata | COMPLETE + VERIFIED | Minimal metadata; tests exclude secrets/paths/IDs |
| P9.22 Download security | COMPLETE + VERIFIED | API-stack owner tests, safe attachment/no-store/nosniff |
| P9.23 Storage | COMPLETE + VERIFIED | On-demand rendering, no stored PDF orphan lifecycle |
| P9.24 Reproducibility | COMPLETE + VERIFIED | Snapshot-based content independent of mutable source; not byte-identical promise |
| P9.25 Versioning | PARTIAL | Explicit immutable historical revision chain; unavailable old sources block revision, fresh draft works |
| P9.26 Packs page | COMPLETE + NOT LIVE-VERIFIED | List/create/open/download/delete implemented; browser pending |
| P9.27 Builder UX | COMPLETE + NOT LIVE-VERIFIED | Details, typed selection, ordering, preview/generate; browser pending |
| P9.28 Selection context | COMPLETE + NOT LIVE-VERIFIED | Real labels/values/dates/evidence, no raw IDs displayed |
| P9.29 Web snapshot view | COMPLETE + NOT LIVE-VERIFIED | Same canonical snapshot plus protected evidence links |
| P9.30 Dashboard integration | COMPLETE + NOT LIVE-VERIFIED | Prepare Visit Pack action; upcoming appointment context |
| P9.31 Audit | COMPLETE + VERIFIED | Five safe pack events; no medical-content audit payload |
| P9.32 Pack IDOR | COMPLETE + VERIFIED | Bidirectional API/security-stack tests including download and mutation |
| P9.33 Relationship injection | COMPLETE + VERIFIED | Foreign source entities rejected, no cross-owner copy |
| P9.34 State/concurrency | COMPLETE + VERIFIED | Stale edits, generated mutation, duplicate generation race and safe conflict |
| P9.35 Snapshot test | COMPLETE + VERIFIED | Live symptom/question/observation changes and deletion preserve generated content |
| P9.36 PDF tests | COMPLETE + VERIFIED | Real parse/signature/text, exclusions, long text, multiple pages, Unicode/action checks |
| P9.37 PDF visual inspection | COMPLETE + VERIFIED | Current three-page fixture and four-page stress fixture rendered, inspected |
| P9.38 Content correctness | COMPLETE + VERIFIED | Existing Jan/Apr/Sep sources processed/verified, exact Hb/Vitamin D pair values/pages |
| P9.39 Unverified exclusion | COMPLETE + VERIFIED | Pending/rejected/unverified candidates cannot enter trusted pack summary |
| P9.40 Assistant boundary | COMPLETE + VERIFIED | No LLM call or credential needed for pack generation |
| P9.41 Privacy | COMPLETE + VERIFIED | Local rendering, safe audits; retained-copy deletion disclosed |
| P9.42 Performance/limits | PARTIAL | Bounded items/text/pages/concurrency/time; batched snapshot reads; no native load benchmark |
| P9.43 Database | COMPLETE + NOT LIVE-VERIFIED | Additive V9; H2 API + PGlite SQL checks; native Flyway pending |
| P9.44 Security review | COMPLETE + VERIFIED | Code/API/PDF review, ownership/replay/snapshot/XSS boundaries tested; not penetration certification |
| P9.45 Frontend quality | COMPLETE + NOT LIVE-VERIFIED | Existing design, accessible controls/error/loading/dialog states; rendered/mobile pending |

### Files and repairs

New backend `visitpack/`: PackDtos, PackAssembler, PackRepository, PackService, PackController,
PackErrors, PackPdf; bundled DejaVu fonts/license. Additive V9__visit_pack_snapshots.sql.
Existing security route/body allowlists and audit enum extended. H2 schema and SQL runner updated.
New PackIntegrationTest (15) and PackPdfTest (3); no baseline test edits/removals.
Frontend: pack-client and five tests, components/visit-packs, protected /visit-packs routes,
shell navigation and dashboard action. Vault deletion confirmation discloses retained snapshots.
Sample-data/phase9 contains an actual generated synthetic PDF. README, architecture, API, security,
testing and design documentation updated; source packager supports phase9.

Generation race initially surfaced as generic 503; pack-specific concurrency advice now returns
409 without weakening transaction isolation. A test used January Vitamin D 19 when selecting April
24 → September 31; fixed the assertion, not the ground truth. Picker client method/router handling
were corrected before final checks. No outstanding critical/high issue from this scoped review.

### Exact verification and outcomes

COMPLETE + VERIFIED: full compile/test/package **250 tests, 0 failures, 0 errors, 0 skipped;
BUILD SUCCESS**. Prior 232 retained plus 18 new. Exact command (workspace parent):

```bash
JAVA_HOME=/workspace/scratch/toolchains/jdk-21.0.12.1+1 /workspace/scratch/toolchains/apache-maven-3.9.11/bin/mvn -o -Dmaven.repo.local=/workspace/scratch/toolchains/m2 -f carepath/backend/pom.xml -B '-Dtest=*Test,ExtractionEvaluationIT,NormalizationEvaluationIT,LongitudinalEvaluationIT,FrontendProxyIT' package
npm --prefix carepath/frontend run typecheck
npm --prefix carepath/frontend run lint
npm --prefix carepath/frontend test
npm --prefix carepath/frontend run build
npm --prefix carepath/scripts/schema-check test
```

COMPLETE + VERIFIED: frontend **29 tests, all pass**, prior 24 retained plus 5. TypeScript, lint,
production build pass, including final deletion-copy change. Existing extraction/normalization/
longitudinal evaluation fixtures and HTTP proxy integration reran successfully in the complete
backend suite; no synthetic result is a clinical-accuracy claim. PGlite applies V1–V9 with 40 existing
invariants, not native PostgreSQL/Flyway validation. PDF parse tests plus actual Poppler rendering
and image inspection cover the synthetic three-page pack and four-page stress fixture.

### Open gates and limitations

- BLOCKED: native PostgreSQL 17/Flyway V1–V9, Redis, normal native backend/Compose startup and
  native restart persistence. No substitutes declared native verification.
- BLOCKED: rendered desktop/mobile end-to-end, visual interaction/accessibility, production
  TLS/cookie/CORS/CSRF runtime verification. Client/build/HTTP tests do not close these gates.
- PENDING: live LLM and live prompt-injection checks; no provider call required/added for packs.
- PENDING: actual email delivery (existing abstraction only).
- PARTIAL: revisions with unavailable live sources fail safely; create a fresh draft. Historical
  snapshots retain deleted source content until their own deletion. Downloaded copies cannot be erased.
- PARTIAL: cooperative PDF deadline, bounded live per-selection lookups, no native load benchmark;
  unsupported font glyphs escape visibly; PDF not tagged-accessibility certified.
- PENDING: clinician sharing/QR and nearby care; not started.

Next phase recommendation: temporary clinician sharing of explicitly scoped generated packs,
with expiry/revocation and audit. Close native/browser deployment gates before deployment; do not
interpret Phase 9 fixture/build success as production certification.

Packaging: COMPLETE + VERIFIED — `python3 carepath/scripts/package.py --phase phase9` creates
carepath-phase9.zip with 322 source/fixture/documentation files. ZIP CRC and exclusions checked;
8 old migrations and 29 baseline Java/frontend test source files retained byte-identical.
Credentials, private uploads, dependencies, raw logs and build artifacts are excluded.

## Phase 10 — temporary clinician sharing, 2026-09-29

Continued the existing Phase 9 tree and interrupted Phase 10 implementation. No regeneration,
nearby-care/maps/direct-booking, global redesign or final release audit. The original 250 backend
and 29 frontend tests were retained; previous native/runtime limitations remain open.

| Requirement | Status | Evidence / scope |
| --- | --- | --- |
| Existing implementation inspection/preservation | COMPLETE + VERIFIED | Security/snapshot/schema/routing/audit inspected; 9 migrations and 32 baseline test sources byte-identical |
| Generated pack sharing only | COMPLETE + VERIFIED | Owned GENERATED pack required; draft/foreign creation rejected through API |
| 256-bit capability generation | COMPLETE + VERIFIED | SecureRandom 32 bytes, canonical Base64URL; byte length/uniqueness assertions |
| Digest-only storage | COMPLETE + VERIFIED | SHA-256 equality checked in SQL; raw token returned once, never list/revoke |
| Token/log/audit minimization | COMPLETE + VERIFIED | Fragment link/fixed API path, no module body/URL logs, audit assertions, SQL logging suppressed |
| Server expiry/revoke/target checks | COMPLETE + VERIFIED | Per-access validation, actual Clock recheck after assembly; exact boundary denied |
| Immutable snapshot scope | COMPLETE + VERIFIED | Public DTO from generated snapshot; source edit/deletion preserves content |
| Original/evidence isolation | COMPLETE + VERIFIED | Citation text only; no source IDs/navigation/downloads; scope escape tested |
| Owner create/list/status/revoke | COMPLETE + VERIFIED | Security-context identity, owner predicates, 20/page, safe status, repeat revoke |
| Four expiry choices | COMPLETE + VERIFIED | 15/30/60/1440-minute allowlist and exact duration assertions |
| ACTIVE/EXPIRED/REVOKED | COMPLETE + VERIFIED | Server-derived; REVOKED precedence; reauthenticated owner sees expired status |
| Create → access → revoke → denied | COMPLETE + VERIFIED | Actual API/security/JDBC test |
| Create → advance Clock → denied | COMPLETE + VERIFIED | 899s access, 900s denied for 15-minute share; no sleep |
| Underlying-source snapshot test | COMPLETE + VERIFIED | Live symptom edit/delete does not rewrite public frozen content |
| IDOR and relationship validation | COMPLETE + VERIFIED | Both owners; foreign create/revoke/list and capability misuse tests |
| Malformed/random/modified token rejection | COMPLETE + VERIFIED | Grammar, truncation/modification/random value, safe errors and body bounds |
| Concurrency | COMPLETE + VERIFIED | Pack-first locks, access/revoke race and repeat-revoke race tested with H2; native locking open |
| Abuse/rate limits | COMPLETE + VERIFIED | Existing store port, share namespace, 60/configured window, 429 and fail-closed 503 tested; native Redis open |
| Public cache/referrer/index/security headers | COMPLETE + VERIFIED | API response tests and actual Next HTTP share-shell headers; production TLS/browser CSP execution open |
| Stored XSS | COMPLETE + VERIFIED | Plain React static output escapes script/image markup; no raw HTML/unsafe links |
| Local QR | COMPLETE + VERIFIED | Pinned qrcode package, real PNG generation and exact URL segment payload; device scan not executed |
| No AI/external data dispatch | COMPLETE + VERIFIED | No LLM, remote QR or external PDF service used by sharing |
| Owner sharing and management UI | COMPLETE + NOT LIVE-VERIFIED | Real API create/expiry/copy/QR/revoke/status pages; client tests/build pass; rendered interactions pending |
| Anonymous read-only page | COMPLETE + NOT LIVE-VERIFIED | No owner shell/edit controls; static React tests + HTTP 200; browser/mobile pending |
| Safe unavailable UX | COMPLETE + NOT LIVE-VERIFIED | Generic invalid/expired/revoked state; client/static tests pass; browser pending |
| Deletion semantics | COMPLETE + VERIFIED | Pack cascade closes capabilities; original deletion does not expose files; retained historical citations labelled |
| Audit events | COMPLETE + VERIFIED | SHARE_CREATED/ACCESSED/REVOKED; public actor SHARE; no token/URL/medical audit payload |
| Additive V10 | COMPLETE + NOT LIVE-VERIFIED | Revision/owner FK and index, PGlite actual SQL applies; native Flyway remains blocked |
| Security review | COMPLETE + VERIFIED | Token/entropy/IDOR/scope/evidence/expiry/cache/XSS/races/audit inspected and tested; not a pentest |
| Documentation | COMPLETE + VERIFIED | Status, architecture, security, API, testing, README and TEMPORARY_SHARING design updated |

### Implementation inventory

Backend sharing/{ShareTokens,ShareDtos,ShareService,ShareController}; existing security route/body
allowlists, rate-limit namespace and audit infrastructure extended. V10__scoped_pack_sharing.sql
reuses the V1 share_token table and pins the pack revision. H2 compatible schema and PGlite runner
extended; SharingIntegrationTest adds 16 cases without modifying any old tests.
Frontend share-client/share-view and 8 tests; components/sharing; anonymous /share and authenticated
/shares; generated pack panel and shell navigation. Local qrcode 1.5.4 and types pinned in lockfile.
No raw capabilities persist in frontend storage. Link is returned once and fragment removed on load.
The source packager supports phase10 and excludes local verification logs.

### Security decisions and limitations

- COMPLETE + VERIFIED: anonymous snapshot includes only selected historical fields and citation text,
  no owner/source IDs or live record links. Holding a capability cannot authenticate owner APIs.
- COMPLETE + VERIFIED: committed revocation blocks subsequent authorization immediately. An access
  serialized before revocation may finish; already-delivered/copied content cannot be recalled.
- PARTIAL: recipient UI revalidates every 15 seconds, clears on hiding/expiry/failure and uses a
  10-second request deadline. Browser timers may be throttled; this is not server authorization.
- PARTIAL: share shell CSP permits Next inline bootstrap; nonce CSP remains a deployment gate.
- PARTIAL: proxy-observed IP limits may aggregate recipients. Trusted-edge limits, retention/pruning,
  tenant quotas and proxy/APM body-capture policy require deployment work; no spoofed forwarded IP trust.
- COMPLETE + VERIFIED: original source deletion keeps historical pack copies consistently with Phase 9.
  Deleting the pack removes shares. No share path can resurrect original medical files.
- BLOCKED: native PostgreSQL 17/Flyway V1–V10, Redis runtime and native restart persistence.
- BLOCKED: rendered desktop/mobile/clipboard/QR scanning and production cookie/CORS/CSRF/TLS runtime.
- PENDING: live LLM/provider injection and email delivery; neither is required or invoked for sharing.

### Verification record

Focused sharing API tests passed 16/16. On resume, missing temporary logs were not assumed to prove
full success; complete backend/frontend commands were executed again. Commands are in TESTING.md.
Frontend: COMPLETE + VERIFIED — 37 tests, 0 failures, 0 skipped; TypeScript/lint/production build pass.
Schema: COMPLETE + VERIFIED for PGlite SQL only — V1–V10 applies, 40 existing invariants pass. This does
not verify native Flyway/JDBC. Actual production Next HTTP /share returned 200, no-store/no-referrer/
noindex headers. Static React rendering tested XSS and absence of edit/source navigation controls;
no rendered-browser verification claimed. Full backend outcome is appended below after completion.

Recommended Phase 11: Nearby Care using a legitimate Places provider and explicit location consent,
with official contact/directions links. Native deployment/browser gates remain prerequisites for
production use. Phase 11 was not started.

### Final executed outcome

COMPLETE + VERIFIED: **266 backend tests, 0 failures, 0 errors, 0 skipped**, all 24 Surefire
suite reports freshly produced by the resumed complete regression. This retains 250 baseline tests
and adds 16 sharing tests. Compilation checks passed. The package stage encountered a corrupt retained
generated JAR (`zip END header not found`); removing only target JAR/.original artifacts and rerunning
`mvn -o -Dmaven.repo.local=/workspace/scratch/toolchains/m2 -f carepath/backend/pom.xml -B -DskipTests package`
with the same Java/Maven paths produced **BUILD SUCCESS**. Tests were not rerun in this packaging-only
retry because the complete 266-test suite had just passed; no source test was disabled or removed.
No application/security change was needed for this generated-artifact repair.

COMPLETE + VERIFIED: **37 frontend tests**, TypeScript, lint and production build passed. Safe per-suite
results and HTTP/schema scope are in docs/verification/phase10-*. The existing real Next HTTP proxy
regression also passed with the H2/test-limiter backend; it does not close native or browser gates.

Implementation and available Phase 10 verification are complete. All live-verification gaps above
remain open. No Phase 11 product functionality was implemented.

Packaging: COMPLETE + VERIFIED — `python3 carepath/scripts/package.py --phase phase10` creates
carepath-phase10.zip with 343 source/fixture/documentation files. ZIP CRC and secret/build/runtime
exclusion checks pass. V1–V9 and all 32 baseline Java/frontend test sources remain byte-identical.

## Phase 11 — Nearby Care (2026-09-29)

Scope: real provider-backed healthcare discovery and contact access only. No direct booking,
medical interpretation, continuous tracking, unrelated redesign or final release audit.

| Requirement | Status | Evidence / boundary |
| --- | --- | --- |
| Provider abstraction and real Google Places adapter | COMPLETE + NOT LIVE-VERIFIED | Contract, normalization and failure tests; no configured credential |
| Four categories; bounded radius and coordinate validation | COMPLETE + VERIFIED | Focused unit and actual Spring security/API tests |
| Explicit one-shot consent; manual coordinates | COMPLETE + VERIFIED | Consent helper tests; no automatic location calls |
| Rendered location permission/mobile flow | BLOCKED | Browser/mobile runtime remains unverified |
| Haversine distance | COMPLETE + VERIFIED | Identical/polar, antipodal, equatorial and dateline fixtures |
| Provider contact/hours/website and safe links | COMPLETE + VERIFIED | Missing-field, phone, unsafe URL and static React tests |
| Directions | COMPLETE + NOT LIVE-VERIFIED | Provider-destination Google Maps links built; external navigation not executed |
| External booking boundary | COMPLETE + VERIFIED | Optional normalized URL only; Google adapter always null; no slots |
| Graceful absent key/quota/HTTP/timeout/malformed response | COMPLETE + VERIFIED | Mock HTTP contract plus real security/API missing-key path |
| Rate limiting and authentication | COMPLETE + VERIFIED | Existing limiter reused, API unauthenticated/limit tests |
| Privacy, SSRF, XSS and cost/resource bounds | COMPLETE + VERIFIED | Scoped code review, unsafe links/escaped text/body budget tests |
| Cache strategy | COMPLETE + VERIFIED | No persistence/cache; no-store successful HTTP API responses tested |
| Database | COMPLETE + VERIFIED | No schema needed; V1–V10 byte-identical to Phase 10 archive; no V11 |
| Complete regression / package | COMPLETE + VERIFIED | 296 backend / 46 frontend; compile/package, TypeScript/lint/build passed; closure record below |
| Live Places provider | PENDING | No key in environment or local .env files; no live calls attempted |

Added backend nearby provider/controller/safety/bounded-body modules and NearbyTest/
NearbyIntegrationTest. Added typed frontend client, consent helper, result renderer, protected
/nearby-care page, navigation and nearby.test.ts. `.env.example` documents backend-only credentials.
Existing auth client allowlist omitted care/visit-packs/shares; added those intended routes with
regression coverage alongside nearby-care. No backend authorization rule was broadened for them.
No baseline migration or test source changed.

Security review: fixed endpoint HTTPS, no redirects, no client URLs or userIds accepted, escaped
provider text, validated web/phone schemes, bounded body/time/concurrency, account rate limits and
safe errors. No precise-coordinate logging/auditing/storage or medical context sent externally.
No unresolved critical/high issue identified in this scoped code/test review; not a penetration test.
Operator proxy/APM body capture must remain off. See docs/NEARBY_CARE.md for limitations/references.

Existing open gaps remain: native PostgreSQL/Flyway, Redis, restart persistence, rendered browser/
mobile, live LLM, production cookie/CORS/CSRF and email. Native infrastructure was not retried.

### Phase 11 closure — 30 September 2026 (Asia/Kolkata)

COMPLETE + VERIFIED: **296 backend tests, 0 failures, 0 errors, 0 skipped** in 26 retained
Surefire reports. All reports identify the complete selector documented in TESTING.md, including
FrontendProxyIT and all three evaluation IT suites. These reports establish completion after the
prior progress message stopped; full tests were not unnecessarily rerun. Safe per-suite counts and
report digests: docs/verification/phase11-backend.json. Baseline 266 retained, 30 added.

COMPLETE + VERIFIED: **46 frontend tests, 0 failures, 0 skipped**, TypeScript, lint and production
build completed in the earlier part of this Work run, including the final loading-state repair.
Tool outputs recorded the test totals and build exit 0. Those successful checks were reused rather
than repeated; temporary logs no longer exist. Summary: docs/verification/phase11-frontend.json.
Baseline 37 retained, 9 added. No frontend source changed during closure.

The restored generated backend JAR was corrupt (BadZipFile). Removed only target JAR/.original and
executed the existing Java21/Maven command with `-DskipTests package` on closure: **BUILD SUCCESS**,
including compile and Spring Boot repackage. The repaired JAR's ZIP CRC passes and contains the
Google Places adapter. This packaging-only command intentionally reused the completed 296-test
regression; no test was removed or disabled. Local log: .verification/p11-package.log.

Compared Phase10 source archive: all 10 historical migrations and all 37 baseline test/resource
files are byte-identical. Phases 1–10 functionality/security are preserved; the previously noted
frontend route allowlist repair is regression-covered. No V11 was needed. No implementation was
rebuilt on closure, no infrastructure was retried, and no Phase12 code was started.

Final scoped security/privacy review found no unresolved critical/high defect. Google key absent;
LIVE GOOGLE PLACES VERIFICATION = PENDING / NOT CONFIGURED. All previously listed live-service,
restart, browser/mobile, LLM, production cookie/CORS/CSRF and email gaps remain open.

Phase11 implementation and available verification are COMPLETE. Appropriate to proceed to Phase12
development, not to claim production readiness. Recommended Phase12: integration/usability and
deployment verification using actual services/browser, followed by the separately scoped release
review. Manual fallback uses coordinates, coverage is provider-dependent, at most 20 results,
straight-line distance only, no direct booking or appointment availability.

## Phase 12 — production integration, hardening and live verification

Closure: 30 September 2026 (Asia/Kolkata). Only Phase12 executed; no major product module,
historical migration change, global redesign or Phase13 implementation.

| Area | Status | Actual evidence / limit |
| --- | --- | --- |
| Native PostgreSQL17 / Flyway V1–V10 / upgrade | BLOCKED | No Docker/postgres/pg_ctl/psql executables or native installation; one discovery check |
| Redis runtime / TTL / reconnect | BLOCKED | No redis-server/redis-cli; existing Lua implementation and test-store failure paths inspected |
| Native restart persistence | BLOCKED | Native DB unavailable; no H2 restart substituted |
| Auth/cookie/CORS/CSRF HTTP | COMPLETE + VERIFIED | New real loopback HTTP tests, host-cookie flags, rotation/logout, preflight/Origin/custom-header checks; H2/test limiter |
| Production TLS/proxy/browser cookie behavior | COMPLETE + NOT LIVE-VERIFIED | No TLS deployment/browser execution; manual cookie sending is narrower evidence |
| LLM / Google Places live | PENDING | No configured credentials; no fabricated live responses |
| Desktop/mobile browser journey | BLOCKED | Workspace Next HTTP200; CUA Chrome ERR_BLOCKED_BY_CLIENT; no retries |
| Email | PENDING | Interface exists; adapter/delivery not configured |
| Configuration hardening | COMPLETE + VERIFIED | ProductionGuard(7), backend URL tests(2), actual JdbcTemplate timeout binding |
| Cross-module / security / failure regression | COMPLETE + VERIFIED | Complete retained synthetic suites; no scoped critical/high finding, no pentest claim |
| Performance sanity | PARTIAL | Query/worker/body/PDF/provider/scheduler bounds and indexes inspected; JDBC deadline added; native load/EXPLAIN not executed |
| Requirements traceability | COMPLETE + VERIFIED | 214 original checkbox requirements mapped to current code/evidence/limitations |
| Backend compile/test/package | COMPLETE + VERIFIED | 307 tests, 0 failures/errors/skips; BUILD SUCCESS; JAR CRC passes |
| Frontend tests/TypeScript/lint/build | COMPLETE + VERIFIED | 48 tests, 0 failures/skips; all commands passed after final copy repair |

### Changes and findings

Added explicit production-profile startup guard, 20-second configurable JDBC statement deadline,
validated Next backend origin; removed obsolete Places/email variables and corrected stale landing
copy. Added ProductionGuardTest, HttpSecurityIntegrationTest and backend-origin.test.ts. Existing
working auth, extraction/trust/provenance, care, snapshots/shares and nearby providers are preserved.
No V11/V12 migration needed. All50 baseline migration/test/resource files from Phase11 archive are
byte-identical. Original296 backend/46 frontend tests retained; 11 backend and2 frontend tests added.

Exact commands are in TESTING.md. Safe results in docs/verification/phase12-backend.json and
phase12-frontend.json; runtime checks/review detail in docs/VERIFICATION_PHASE12.md. No backend
packaging repair was needed this phase. New actual HTTP tests do not close native/TLS/browser gates.
Production Next HTTP proxy, >10MiB upload, processing/review/evidence and existing cross-module
fixtures reran in the full suite. Synthetic evaluation is not clinical accuracy.

### Original requirements, honestly scoped

REQUIREMENTS_TRACEABILITY.md classifies all214 original product items: 151 IMPLEMENTED + VERIFIED
(with cited automated scope), 37 IMPLEMENTED + LIVE VERIFICATION PENDING, 19 PARTIAL,
4 NOT APPLICABLE / intentionally deferred and3 NOT IMPLEMENTED. Missing UI/API requirements include
Activity/security history, onboarding and Privacy/Settings; search and timeline aggregation are
narrower than the original wishlist. No complete structured prescription/clinical-visit module,
external email adapter, S3 adapter or direct booking provider is claimed. Later immutable snapshot
requirements supersede the original automatic-invalidation proposal.

All live gaps remain visible. Production quotas/retention/backups/restore, edge TLS/HSTS/logging,
nonce CSP, native load/concurrency and rendered accessibility/QR remain deployment/audit work.
Phase12 is COMPLETE within available execution capabilities. The repository is ready for the
FINAL Phase13 audit with these explicit inputs, not certified for production deployment. STOP: no
Phase13 work started.


## FINAL Phase 13 closure — 30 September 2026

**COMPLETE + VERIFIED** for implemented closure APIs, automated tests, source review and builds.
**RELEASE CANDIDATE: YES.** Not production certified or clinically validated. No Phase14.

Completed missing original requirements: nonblocking onboarding; projected owner-scoped Activity API
and page; Privacy/Security and real account Settings; deterministic cross-module search; appointment/
confirmed-follow-up timeline events with evidence; prescription document categories and previous
visits represented by existing entities. Fixed selected-symptom deep links and stale landing/auth
copy. Added validated UUID request correlation to both outbound providers. No new product subsystem.

### Verification actually executed

- COMPLETE + VERIFIED: backend **320 tests,0 failures/errors/skips**, compile and executable package.
- COMPLETE + VERIFIED: frontend **58 tests,0 failures/cancelled/skips/TODO**, TypeScript, lint, production build.
- COMPLETE + VERIFIED: connected synthetic API/service journey from three reports/review through
  evidence, assistant fallback, care/reminder/notification, PDF, public share/revoke and Activity.
- COMPLETE + VERIFIED: PGlite V1–V10/40 invariants plus production search/timeline SQL checks.
  This does not establish native PostgreSQL/Flyway behavior.
- COMPLETE + VERIFIED: all53 baseline migration/test/resource files compared with Phase12 archive
  are byte-identical;10 migrations retained.13 backend and10 frontend tests added; none removed.
- COMPLETE + NOT LIVE-VERIFIED: new UI routes/navigation are implemented and build/client-tested;
  rendered desktop/mobile behavior remains blocked.

Exact commands, test scope, audit findings and known limits: docs/VERIFICATION_PHASE13.md;
safe machine summaries: docs/verification/phase13-backend.json and phase13-frontend.json.
New tests: ClosureIntegrationTest(10), ClosureJourneyTest(1), RequestCorrelationTest(2),
workspace-client.test.ts(10). No V11 migration or new secret is required.

### Final214-item assessment

159 IMPLEMENTED + VERIFIED;44 IMPLEMENTED + LIVE VERIFICATION PENDING;7 PARTIAL;
3 INTENTIONALLY DEFERRED;0 NOT IMPLEMENTED;1 NOT APPLICABLE.
Remaining partials:11.01/11.02 bounded assistant rather than arbitrary narrative reports;
26.01–26.04 mixed JPA/JDBC instead of literal all-JPA entities;30.04 internal metrics without a
production authenticated collector/exporter. Deferred:05.03 reliable LOINC mapping,12.03 optional
symptom assistant context,20.05 dedicated direct-booking provider.17.06 automatic historical pack
invalidation is superseded by explicit immutable snapshot semantics. Every reason is in the matrix.

### Final audit and unresolved limits

No unresolved critical/high finding identified within repository inspection and executed regression.
This is not a professional penetration test. Existing trust, ownership, immutable snapshot, share,
medical-safety, upload and provider boundaries remain preserved. New queries are principal-scoped,
bounded and parameterized; Activity excludes sensitive metadata; new UI renders text safely.
No fake controls, fabricated medical/provider data or new external medical-data transmission added.

BLOCKED: native PostgreSQL/Flyway, Redis, native restart persistence, rendered desktop/mobile.
PENDING / NOT CONFIGURED: live LLM, live Places, production TLS/browser cookie enforcement, email.
Existing loopback HTTP/proxy tests pass but do not close those gates. Provider credentials and native
executables remain absent; unavailable infrastructure was not repeatedly retried.
Deployment still requires operator TLS/CSP, secrets/keys, least-privilege DB roles, encrypted storage,
quotas, backups/restore/retention and clinical safety/content review. No clinical accuracy or regulatory
compliance claimed. Original-file deletion does not erase independently generated pack snapshots or
recipients' copies; delete historical packs separately.

Final documentation: README, ARCHITECTURE, SECURITY, API, TESTING, .env.example, requirements matrix,
preserved Phase12 matrix and Phase13 verification report. Source packaging excludes private data,
keys, dependencies/build output/logs; legitimate fixtures/fonts/evaluation records remain.
Phases1–12 preserved; development stops at this final Phase13 checkpoint.


### Closure artifact repair

A retained executable JAR was found truncated during final CRC inspection, despite the preceding
successful package log. With application/test sources unchanged, a packaging-only repair ran:
`mvn -o -Dmaven.repo.local=/workspace/scratch/toolchains/m2 -f carepath/backend/pom.xml -B -DskipTests -Dmaven.jar.forceCreation=true package`
using the same Java21/Maven toolchain. BUILD SUCCESS; rebuilt JAR79,861,933 bytes and full ZIP CRC PASS.
This invocation deliberately reused the already completed320-test regression; it is not a new
regression pass or a removed/disabled test. Final test totals remain320/58 with zero recorded skips.
The source archive excludes build artifacts. A secret-pattern scan matched only a PEM-header string
in AuthTestSupport around freshly generated ephemeral test keys, not an embedded credential.
