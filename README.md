# CarePath

**Evidence-Grounded Longitudinal Health Record Intelligence & Care Navigation Platform**

**Final Phase13: RELEASE CANDIDATE — test-verified, not production certified.**
320 backend tests and58 frontend tests pass, with zero failures/errors/skips. Backend package,
TypeScript, lint and frontend production build pass. Native PostgreSQL/Flyway/Redis, restart,
rendered desktop/mobile, live providers and production TLS/browser checks remain open.
See [final verification](docs/VERIFICATION_PHASE13.md) and [all214 requirements](docs/REQUIREMENTS_TRACEABILITY.md).

CarePath organizes user-provided records. It is not an AI doctor, does not diagnose or prescribe,
and has no claim of regulatory compliance or clinical validation. Use synthetic data until the
operational/privacy/runtime gates are completed in your deployment.

## What is implemented

Private PDF/JPEG/PNG vault → bounded PDF/OCR processing → extraction candidates → explicit
Confirm/Correct/Reject → deterministic concept/unit normalization → trusted observations →
evidence-linked history/charts/What Changed? → bounded explanations and clinician questions.

Symptoms, appointments, confirmed document follow-ups, persistent in-app reminders and notifications
support care organization. Visit Packs preserve selected immutable snapshots, web views, PDFs and
revocable/expiring read-only sharing with locally generated QR codes. Nearby Care uses a real Google
Places adapter when configured, with explicit/transient location input and legitimate external actions.

New-user onboarding, cross-module search, Activity history, Privacy/Security and Settings complete
the workspace. Search covers records/tags, trusted tests, dates/providers, symptoms and appointments.
The unified timeline includes documents/prescriptions, trusted readings, symptoms, appointments/
previous visits and confirmed follow-ups. User-reported events never drive laboratory calculations.

## Local setup

Prerequisites: JDK21, Maven3.9+, Node22.18+ (Node24 tested), npm, Python3, OpenSSL and Docker Composev2.
Windows users may use WSL2. Compose runs PostgreSQL17.6 and Redis7.4.5; it does not start the apps.

From the extracted `carepath/` directory:

1. `cp .env.example .env`. Set different random POSTGRES_PASSWORD, REDIS_PASSWORD and AUTH_RATE_KEY
   values. Generate each with `python3 -c "import secrets; print(secrets.token_hex(32))"`.
   Never commit .env. Leave optional provider keys blank initially.
2. `python3 scripts/generate-auth-keys.py` creates local RSA keys without overwriting existing ones.
   The configured paths and storage roots are relative to `backend/`.
3. `docker compose up -d --wait`. Existing volume credentials do not change automatically when .env
   changes. Do not delete valuable volumes to resolve a credential mismatch.
4. Backend terminal:
   ```sh
   cd backend
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```
5. Frontend terminal:
   ```sh
   cd frontend
   npm ci
   npm run dev
   ```
6. Open http://localhost:3000, register, read the optional onboarding guide and sign in.

Use **localhost consistently**. `127.0.0.1` is a different Origin. FRONTEND_ORIGIN must match the
browser address; BACKEND_URL must match the backend port and be set before building Next. The Next
server proxies `/api/v1/*`; secrets never use NEXT_PUBLIC_ variables. Root .env is read by Compose,
Spring and Next configuration. DATABASE_URL must match POSTGRES_PORT. Stable JWT keys are required
across restarts. Redis authentication mutation limits fail closed with503 when unavailable.

AUTH_COOKIE_SECURE=false in .env.example is for HTTP loopback development only. Deployed backend
must use `SPRING_PROFILES_ACTIVE=production`, secure cookies, exact HTTPS frontend origin and valid
non-development credentials/configuration. The production guard rejects unsafe settings. Run the
frontend behind TLS on its configured origin. Production TLS, encrypted storage, least-privilege
DB roles, backup/restore, retention, edge logs and key rotation are operator responsibilities.
Do not expose local Swagger or weaken settings to make a deployment start.

Health: http://localhost:8080/actuator/health (details hidden). Local-profile API docs:
http://localhost:8080/swagger-ui/index.html. `/api/v1/system/info` is a historical compatibility
endpoint, not a live deployment readiness or current feature inventory.

## Documents, OCR and demonstration

Local originals live under STORAGE_ROOT (default `../.data/documents` from backend/), outside web
roots. Upload ceiling is20MiB. Supported types are PDF/JPEG/PNG; PDFs require valid content, no active
content/embedded attachments, and at most200 upload pages. Processing has a separate default20-page
limit. Files remain UPLOADED until processing is requested; candidates never become trusted silently.

Install Tesseract5 and English language data for scanned/image documents, e.g. on Debian/Ubuntu
`sudo apt-get install tesseract-ocr tesseract-ocr-eng`. Verify `tesseract --version` and `tesseract
--list-langs`; configure TESSERACT_COMMAND if needed. Native text PDFs need no OCR executable.
Unavailable OCR produces an explicit failure/retry state, never synthetic output.

Use **SYNTHETIC DEMO DATA — NOT A REAL PATIENT** files in `sample-data/phase4/`. Upload January,
April and September → Process → View source → Confirm/Correct/Reject → Timeline → What Changed?
→ Assistant → symptom/appointment/follow-up → Visit Pack/PDF → temporary share → revoke → Activity.
The scanned PDF/PNG and uncertain September page exercise OCR/review. No real patient data is included.

Deleting an original removes live record/evidence access and queues durable cleanup if needed.
Previously generated Visit Pack snapshots are independent historical copies: delete those packs
separately. Revocation stops future public access but cannot erase a recipient's downloaded copy.

## Optional providers and limits

- **LLM:** disabled by default. Configure LLM_ENABLED/API_KEY/MODEL/ENDPOINT/timeouts only if desired;
  users also opt in per request. Only bounded relevant verified facts leave the app. Deterministic
  explanations work without a key. Arbitrary narrative interpretation/diagnosis is not supported.
- **Nearby Care:** GOOGLE_PLACES_API_KEY (Places API New, billing/quota restrictions) remains backend
  only. Search requires explicit browser location action or manual coordinates. No continuous
  tracking or coordinate persistence. Approximate straight-line distances are not driving routes.
  Missing fields stay unavailable; no invented booking slots or URLs. No key means unavailable UI.
- **Email:** interface/disabled implementation only; no external adapter or delivery. Database-backed
  in-app notifications remain available. Configure offsets on actual appointments/follow-ups.
- **Storage:** real local adapter; S3-compatible port exists but no S3 implementation is claimed.

Full limits/contracts: [vault](docs/DOCUMENT_VAULT.md), [processing](docs/DOCUMENT_INTELLIGENCE.md),
[review](docs/NORMALIZATION_AND_REVIEW.md), [history](docs/LONGITUDINAL_INTELLIGENCE.md),
[assistant](docs/GROUNDED_ASSISTANT.md), [care](docs/CARE_ORGANIZATION.md),
[packs](docs/VISIT_PACKS.md), [sharing](docs/TEMPORARY_SHARING.md), [nearby](docs/NEARBY_CARE.md).

## Full verification

Build the frontend before the complete backend command because FrontendProxyIT runs the actual
production Next server on loopback ports3000/8080. Tests use synthetic records, H2 and controlled
rate-store/provider ports; native services are a separate gate. Tesseract is required for OCR ITs.

```sh
cd frontend
npm ci
npm test
npm run typecheck
npm run lint
npm run build
cd ../backend
mvn -B '-Dtest=*Test,ExtractionEvaluationIT,NormalizationEvaluationIT,LongitudinalEvaluationIT,FrontendProxyIT' package
cd ../scripts/schema-check
npm ci
npm test
```

The schema harness is **PGlite only**, not native PostgreSQL/Flyway verification. `mvn test` alone
omits explicitly selected evaluation/proxy ITs. Exact executed commands and safe result summaries
are in [TESTING.md](TESTING.md) and `docs/verification/phase13-*.json`. Synthetic accuracy is not
clinical accuracy. No browser/TLS/real-provider check is implied by passing builds or HTTP tests.

## Repository and final status

`backend/` modular Spring application; `frontend/` Next app; `infrastructure/` operational support;
`sample-data/` synthetic fixtures; `evaluation/` ground truth/measured synthetic results; `docs/`
contracts/audits; `scripts/` key generation, schema checks and source packaging.

All V1–V10 migrations are retained unchanged. No V11 was needed. User uses JPA; most feature
repositories use explicit typed JDBC. Original all-JPA wording, broad narrative assistant support
and operational metrics export are documented partial items. LOINC assignment, optional symptom
assistant context and direct booking integration are deliberately deferred. All214 rows are mapped:
159 verified within stated scope,44 live pending,7 partial,3 deferred,0 not implemented,1 not applicable.

`python3 scripts/package.py --phase phase13` produces `../carepath-phase13.zip`, excluding credentials,
keys, private data, dependencies and build outputs. `docker compose down` preserves volumes;
`docker compose down -v` destroys them. Follow PROJECT_STATUS.md and inspect code before maintenance.
The final development phase is closed; no Phase14 is planned.
