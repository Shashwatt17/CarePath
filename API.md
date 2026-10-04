# API contract — final Phase 13 release candidate

Browser calls `/api/v1/*` through Next's same-origin proxy. Backend base locally is
http://localhost:8080. All auth responses are noncacheable and have X-Request-ID.

| Method | Path | Authentication | Behavior |
| --- | --- | --- | --- |
| GET | /api/v1/system/info | Public | Legacy stage metadata; not a production-readiness signal |
| GET | /actuator/health, /liveness, /readiness under /actuator/health | Public | Health without details; live dependencies required for readiness |
| POST | /api/v1/auth/register | Origin + custom header | JSON email/password/displayName; 201, generic duplicate 409 |
| POST | /api/v1/auth/login | Origin + custom header | JSON email/password; accessToken/expiresIn/user, HttpOnly refresh cookie |
| POST | /api/v1/auth/refresh | Origin + custom header + refresh cookie | Rotates refresh cookie, returns fresh access/user; 401 on invalid/expired/replayed/revoked |
| POST | /api/v1/auth/logout | Origin + custom header + optional refresh cookie | Idempotent 204; revokes family, expires cookie |
| GET | /api/v1/auth/me | Valid Bearer JWT + active session | User id/email/displayName from authenticated principal |
| GET | /v3/api-docs, /swagger-ui/index.html | Local profile only | Actual OpenAPI/Swagger with documentation-scoped CSP |
| any | Other product APIs except the implemented routes below | Denied | Not implemented in this phase |

Every auth POST needs `Origin: <FRONTEND_ORIGIN>` and `X-CarePath-Client: web`.
For registration/login send Content-Type: application/json. Browser sets Origin automatically.
Do not add a userId, role or status to DTOs; unknown JSON fields are rejected.
Passwords: registration 12–72 characters, maximum 72 UTF-8 bytes. Email <=254; name <=120.
Cookie name is `__Host-carepath_refresh` in secure mode, `carepath_refresh` in HTTP loopback mode.

Error JSON: `{status, code, message, requestId}`. Auth/security filter errors use
application/problem+json; controller advice returns the same safe shape. 400 validation/malformed,
401 invalid authentication/session, 403 origin/forbidden, 409 registration unavailable, 413 body limit,
415 content type, 429 rate limit, 503 authentication/service unavailable. Error messages never echo input.
Do not distinguish refresh expiry/revocation/replay to unauthenticated clients. A reused token revokes
its family even though the response is 401. Empty logout is safe and returns 204.

Care, sharing and navigation APIs are implemented as detailed in the later sections below.

## Phase 3 document API (implemented)

All routes below require a valid Bearer JWT and active server-side session. Cookies alone do not
authorize vault calls. No request accepts ownerId, storage key, hash, status or filesystem path.

| Method | Path | Request / result |
| --- | --- | --- |
| GET | /api/v1/documents/config | maxBytes and fixed MIME allowlist |
| POST | /api/v1/documents | multipart `file` + JSON `metadata`; 201 with document DTO |
| GET | /api/v1/documents | owner-scoped `{items,total,page,size}` |
| GET | /api/v1/documents/{id} | metadata DTO; foreign/absent 404 |
| PUT | /api/v1/documents/{id} | `{version,metadata}`; stale version 409 |
| GET | /api/v1/documents/{id}/download | integrity-verified original attachment |
| GET | /api/v1/documents/{id}/preview?page=1 | safe inline image; PDF page rendered to PNG |
| DELETE | /api/v1/documents/{id} | 204 fully deleted; 202 inaccessible, physical cleanup queued |

Metadata: `{documentType, documentDate?, providerName?, tags}`. Category is one of the nine enums
in `DocumentDtos.Category`; tags is an array (required, can be empty), at most 12 nonblank strings
of at most 40 characters. Provider maximum 255; date ISO YYYY-MM-DD or null. Upload timestamp,
processing status, SHA-256, original MIME/size and version are server-controlled. Response DTO
includes sanitized filename, SHA-256, pageCount and timestamps, never owner or internal storage key.

Search parameters: `type`, `status`, `filename`, `provider`, `from`, `to`, `uploadedFrom`, `uploadedTo`,
`page` (zero-based, default 0), `size` (default 20, maximum 100). Document dates are inclusive;
uploaded date boundaries are UTC and inclusive by calendar day. Stable newest-first ordering.
Literal %/_ in search are escaped, not treated as wildcards. Missing dates remain unknown.

Errors: 400 invalid metadata/filter/page, 401 missing/invalid session, 404 foreign/absent UUID,
409 VERSION_CONFLICT or INTEGRITY_FAILURE, 413 FILE_TOO_LARGE, 422 INVALID_FILE, 429 VAULT_BUSY,
503 FILE_UNAVAILABLE/service unavailable. All content responses are private/noncacheable.
The system-info legacy stage remains PHASE_8_CARE_ORGANIZATION; its full-product availability flag remains false. Neither field is a current verification/readiness signal.

## Phase 4 processing APIs

All require Bearer access tokens plus a valid active session; ownership comes only from security context.
Responses are noncacheable. No client owner, storage path or classifier override is accepted.

| Method | Path under /api/v1/documents/{id} | Result |
| --- | --- | --- |
| POST | /process | 202; enqueue uploaded document, or return existing state idempotently |
| POST | /retry | 202 only for terminal retryable failure within retry allowance; otherwise 409 |
| GET | /processing-status | state, jobState, attempts, safe errorCode, retryAllowed |
| GET | /extraction | latest committed extraction: id, documentId, revision, result |
| GET | /extraction/evidence/{candidateId} | exact source page/text/offsets/method/optional OCR box |

No extraction yet: 404 EXTRACTION_NOT_READY. Absent/other-owner document or mismatched candidate:
generic 404. Invalid UUID: 400. Admission allowance: 429 PROCESSING_LIMIT.
The extraction result contains pages, classification+evidence, labelled information+evidence,
candidate rows, needsReview, ocrUsed and pipelineVersion. Missing fields are null. Candidate fields:
originalTestName/originalValue/numericValue/comparator/originalUnit, referenceLower/referenceUpper/
referenceText, abnormalFlag, reportDate, providerName, confidence/reasons, source.
Source offsets are UTF-16 code units into the stored page text; OCR boxes are normalized [0,1].
Phase 5 supersedes the extraction-only status: documents with candidates stay NEEDS_REVIEW until all candidates are resolved. COMPLETED is not a medical correctness guarantee.

## Phase 5 normalization and review

All routes require a valid Bearer JWT and active session. No owner/user ID is accepted. Foreign or
absent resources return the same 404. Review writes have an 8 KiB JSON bound. Unknown fields fail 400.

| Method | Path under /api/v1 | Request / result |
| --- | --- | --- |
| GET | /terminology/concepts | Active bounded concept catalog, names/units/version; standard identifiers nullable |
| GET | /review/candidates | `documentId?`, `state` (default PENDING_REVIEW, empty for all), page 0–10000, size 1–100; owner-scoped paginated queue |
| GET | /review/candidates/{id} | Original candidate/provenance, state/version, deterministic decision and normalization preview, observationId if accepted |
| POST | /review/candidates/{id}/preview | Editable Fields below; validation/mapping/unit preview, no mutation |
| POST | /review/candidates/{id}/confirm | `{version}`; direct confirmation if policy permits |
| POST | /review/candidates/{id}/correct | `{version, fields, sourceReviewed:true, reason}`; explicit correction and verification |
| POST | /review/candidates/{id}/reject | `{version, reason?}`; terminal rejection, no observation |
| GET | /observations/{id} | Owner-scoped trusted result with original/effective fields, persisted normalization, evidence and verification timestamp |

Fields: `{testName, value, unit?, referenceRange?, date?, conceptId?}`. Limits: name 160, value 80,
unit 50, reference 255; date ISO YYYY-MM-DD. Null means absent. Value remains a string (including
supported inequality prefix). Optional conceptId must identify an active curated concept and records
USER_SELECTED. Reason is 10–500 characters for correction; reject reason maximum 500.
Neither fields nor action DTOs accept owner, actor, confidence, source, normalizedValue or trust status.

Successful actions return `{candidate, observation}` (observation null for rejection), 200, and increment
review version. Identical terminal replay returns the original result without duplicating data/audits.
Errors: 409 REVIEW_STATE_CONFLICT / VERSION_CONFLICT / PROVENANCE_INVALID; 422 CORRECTION_REQUIRED /
REVIEW_VALIDATION; 400 invalid fields; 413 request limit. No PATCH/PUT/DELETE observation mutation API.
Normalized BigDecimal values are JSON strings. Raw candidate representations remain immutable.
See `docs/NORMALIZATION_AND_REVIEW.md` for abstention and document-state semantics.

## Longitudinal history (authenticated GET only)

All routes below have prefix `/api/v1/history`. Ownership comes from the JWT security context.
Foreign/missing resources return 404; invalid pagination returns 400. No entities/storage keys exposed.

| Suffix | Parameters | Response |
| --- | --- | --- |
| `/events` | conceptId?, documentId?, q? (80 chars), page, size | Page of DOCUMENT / OBSERVATION / SYMPTOM / APPOINTMENT / FOLLOW_UP events with provenance |
| `/concepts` | page, size | Owner's observed concepts with counts |
| `/concepts/{id}` | page, size | Paginated chronological history plus bounded trend series/limitations |
| `/observations/{id}/evidence` | — | Owned trusted point and exact source evidence |
| `/compare` | previous, current observation UUIDs | Structured numerical change or insufficient evidence |
| `/changes` | previousReport, currentReport document UUIDs | Both report contexts and structured changes |

Page starts at 0 (maximum 10000); size 1..100, default 20. Decimal fields are strings. Changes retain
previous/current points and sources, signed absoluteDelta, percentageDelta (null for zero baseline),
unit, direction type, separate referenceTransition, reason, deterministic explanation and policyVersion.
Presence/absence is scoped to selected verified records, not clinical onset/resolution. Reports above
500 verified readings return 422 COMPARISON_LIMIT. Trend window 500 exposes truncation. Same-day and
undated points remain visible but are not forced into comparisons. Details: docs/LONGITUDINAL_INTELLIGENCE.md.

## Phase 7 assistant and clinician questions

All require an active Bearer session; no cookie-only authority or client owner IDs. POST/PUT bodies
are bounded to 8 KiB. All record selection and linked evidence is owner-scoped; foreign/absent IDs 404.

| Method | Path under /api/v1/assistant | Request / response |
| --- | --- | --- |
| GET | /config | Provider availability, limits and external-data notice |
| POST | /ask | `{question, conceptId?, observationId?, previousReport?, currentReport?, useAi}` → transient Answer |
| GET | /questions | page 0..10000; size 20 → owned saved-question page |
| GET | /questions/{id} | Owned draft with fresh trusted evidence and missing-evidence indicator |
| POST | /questions | `{text,observationIds:[]}` → 201 Saved |
| PUT | /questions/{id} | `{text,version}` → updated Saved; stale version 409 |
| DELETE | /questions/{id} | 204; absent/foreign 404 |

Question/text length 1..1000; at most 12 linked observation IDs. Only one optional record scope is
allowed; report scope requires both IDs. `useAi` defaults false. No provider configuration, origin or
trust fields can be supplied. Evidence links are checked against verified owned observations.
Answer: mode, providerStatus, message, uncertainty, facts, evidence, explanation, suggestedQuestions,
referenceUrls. Modes: DETERMINISTIC, AI_CONSTRAINED, INSUFFICIENT_EVIDENCE, BOUNDARY, URGENT.
Provider failures are successful deterministic answers with an explicit unavailable reason, not fake AI.
No answer/conversation is persisted; evidence navigation reuses history/vault APIs. Suggested questions
are editable drafts with contributing observation IDs; save is an explicit separate operation.
`sourceType=USER` identifies saved user wording, including edited suggestions.
See docs/GROUNDED_ASSISTANT.md for strict model contract, selection limits and retention behavior.

## Phase 8 care organization

All routes below are under `/api/v1/care`, require the existing bearer session, and derive ownership
from the authenticated context. Cross-owner/missing IDs return generic 404. Unknown JSON fields are
rejected, invalid input returns 400, stale/conflicting transitions 409, oversized bodies 413.
Lists return `{items,total,page,size}` with size 20 and page 0–10000.

| Method | Route | Behavior |
| --- | --- | --- |
| GET / POST | `/symptoms` | List (`status=ALL/ACTIVE/RESOLVED`, `q`, `page`) / create 201 |
| GET / PUT / DELETE | `/symptoms/{id}` | Read / versioned edit / delete 204 |
| GET / POST | `/appointments` | List (`scope=ALL/UPCOMING/PAST`, `page`) / create 201 |
| GET / PUT / DELETE | `/appointments/{id}` | Read / edit scheduled item / delete 204 |
| POST | `/appointments/{id}/status` | `{status:"COMPLETED" or "CANCELLED",version}` |
| POST | `/documents/{id}/detect-follow-ups` | Scan owned persisted extraction; 204 |
| GET | `/follow-ups` | `scope=ALL/UPCOMING`, `page` |
| GET | `/follow-ups/{id}` | Candidate/decision plus exact source evidence |
| POST | `/follow-ups/{id}/decision` | Explicit CONFIRM or IGNORE, never automatic |
| GET | `/reminders` | `status=ALL/PENDING`, `page`; server-managed writes only |
| GET | `/notifications` | Private inbox, `page` |
| GET | `/notifications/unread` | `{count}` |
| POST | `/notifications/{id}/read` | Idempotent mark-read, 204 |
| POST | `/notifications/read-all` | Owner-only mark-all-read, 204 |

Symptom create/edit: `name` (1–160 chars), `startedAt` (ISO instant), nullable `resolvedAt`,
`severity` (1–10), `frequency` (ONCE/OCCASIONAL/DAILY/CONSTANT), nullable `notes` (max 2000), `version`.
Null resolvedAt means ACTIVE; setting/clearing it resolves/reopens the symptom. GET includes timestamps.

Appointment create/edit: `providerName`, optional `specialty`, `location`, `phone`, `externalUrl`
(HTTPS only), `notes`, `followUpDate`; required `startsAt` (ISO offset timestamp), `timeZone` (IANA),
`documentIds`, `symptomIds`, `questionIds` (arrays, max 20 each), `offsets` and `version`.
Example time: `2026-10-13T09:00:00+05:30`, zone `Asia/Kolkata`. The offset must be valid in that zone.
Responses use UTC startsAt plus retained zone. Create with version 0; edits use the returned version.
Client-supplied owner, status, created timestamps or delivered timestamps are not accepted.
Offsets are distinct members of `[0,60,1440,2880,10080]`; `[]` creates no reminders.

Follow-up decision examples:
```json
{"action":"CONFIRM","confirmedAt":"2026-10-13T09:00:00+05:30","timeZone":"Asia/Kolkata","offsets":[60,1440],"version":0}
```
```json
{"action":"IGNORE","offsets":[],"version":0}
```
CONFIRM on a changed/missing suggestion records EDITED and retains both dates. Repeat confirmation
returns 409; repeat IGNORE is harmless. Read DTOs include source document/page/instruction, anchor,
machine suggestion, confidence, confirmed date/time/zone and version. No endpoint accepts page/owner
for a detected candidate. Evidence uses existing authenticated document-page APIs.

`/api/v1/history/events` also returns SYMPTOM events. Their documentId is null; link to `/symptoms`.
They are excluded when a concept/document filter is selected and never enter concept histories/charts.
Appointments/follow-ups are displayed in their own screens; no numerical timeline aggregation is added.

## Visit Packs (Phase 9)

All routes below require bearer authentication and current-owner scope. Prefix `/api/v1/visit-packs`.

| Method | Suffix | Behavior |
| --- | --- | --- |
| GET | `?page=0` | Paginated current-owner summaries, 20 per page |
| POST | (base) | Create validated draft; 201 |
| GET | `/{id}` | Draft selections or generated snapshot |
| PUT | `/{id}` | Replace editable draft with expected version |
| GET | `/{id}/preview` | Canonical content plus fingerprint |
| POST | `/{id}/generate` | `{version, previewHash}`; freeze draft |
| POST | `/{id}/revise` | Explicit child draft; repeat returns same child |
| GET | `/{id}/pdf` | Authenticated generated PDF attachment |
| DELETE | `/{id}` | Delete own pack and snapshot; 204 |

Draft body: `title`, `reasonForVisit`, optional `appointmentId`, `packDate`, ordered `items`,
`version`. Each selection contains `type`, optional `sourceId`, `otherId`, `includeNotes`,
`questionText`. Types: SYMPTOM, OBSERVATION, CHANGE, DOCUMENT, APPOINTMENT, FOLLOW_UP,
SAVED_QUESTION, MANUAL_QUESTION. CHANGE uses previous/current verified observation IDs; manual
questions have no source ID. A question override affects this pack only. Unsupported fields/types
and foreign links cannot assign ownership or snapshot state. Limits and semantics: docs/VISIT_PACKS.md.

409 indicates immutable/stale state, changed preview or concurrent transaction conflict; reload
and preview again. Missing/unowned roots use safe 404 behavior. Unavailable draft sources fail
safely rather than silently disappearing. PDF content includes no internal IDs/storage paths.

## Temporary sharing — Phase 10

| Method | Route | Authentication / contract |
| --- | --- | --- |
| POST | `/api/v1/shares` | Owner bearer; `{packId, expiryMinutes}`; GENERATED own pack only; 201 with share metadata and one-time token |
| GET | `/api/v1/shares?page=0` | Owner bearer; 20 own summaries/page, no token/digest |
| POST | `/api/v1/shares/{id}/revoke` | Owner bearer; idempotent; returns current metadata/status |
| POST | `/api/v1/public/share/access` | No account; `{token}`; only anonymous snapshot projection |

Expiry minutes: 15, 30, 60, 1440. Status ACTIVE/EXPIRED/REVOKED is server-derived. Invalid, missing,
expired or revoked capabilities use generic 404 SHARE_UNAVAILABLE; malformed JSON/forged fields use
safe 400, oversized body 413, rate limit 429, limiter outage 503. Foreign management IDs are not-found.
The anonymous response has no IDs or owner/source navigation URLs. Historical citation text is
included; original-file/evidence downloads and public PDF downloads are not supported. Share tokens
cannot authenticate other endpoints. All medical responses are noncacheable. Transport uses a
fragment link and fixed-path JSON POST; see docs/TEMPORARY_SHARING.md for concurrency/deletion limits.

## Nearby Care — Phase 11

Bearer authentication required. GET `/api/v1/nearby-care/config` returns provider availability
and maximum radius, never the API key. POST `/api/v1/nearby-care/search` accepts only
`{latitude, longitude, radiusMeters, category, locationConsent:true}`. Latitude −90..90, longitude
−180..180, radius 100..20000m; category HOSPITAL/CLINIC/PHARMACY/DIAGNOSTIC_CENTER.
Returns `{provider,results}` with provider IDs and optional normalized contact/hours/directions.
No facility metadata/owner IDs accepted from clients. Missing fields are null/empty, not inferred.
400 validation, 401 unauthenticated, 429 rate limit, 503 provider/limiter unavailable.
Responses no-store. No location persistence or query-string coordinates. See docs/NEARBY_CARE.md.

## Phase 12 HTTP security verification scope

No new product routes. Actual loopback HTTP tests exercised Secure/HttpOnly/SameSite host cookie
headers, refresh/logout, strict Origin/custom header, CORS allow/deny, protected API denial and
anonymous share no-store/no-referrer/noindex errors. H2/test limiter supported this execution;
production TLS, reverse proxy, browser cookie enforcement, native DB and Redis remain unverified.
`system/info` retains its legacy compatibility fields; use the status ledger for current scope.


## Phase13 Activity and cross-module search

Both APIs use validated security-context ownership and existing no-store/security/error handling.
Neither accepts a client userId as authorization.

- `GET /api/v1/activity?category=ALL&page=0`: category ALL/SECURITY/DOCUMENT/CARE/VISIT_PACK/SHARE;
  fixed20/page, page0..10000. Returns `{items,total,page,size}`; each event contains only
  `{action,outcome,actor,occurredAt}`. No resource/session IDs, internal reason or medical content.
- `POST /api/v1/search`: JSON `{q,kind,from?,to?,provider?,page}`. q required (empty allowed for filters),
  max80 characters/eight whitespace terms; kind ALL/DOCUMENT/OBSERVATION/SYMPTOM/APPOINTMENT;
  dates ISO local dates, from<=to; provider max80; page0..10000,20 results/page.
  Search matches literal AND terms against titles/provider/summary/document tags; full/abbreviated
  English months filter event month. Percent/underscore are literal, not SQL wildcard instructions.
  Returns `{items,total,page,size}`; items contain kind,id,title,date,provider,summary,documentId,
  candidateId. Observation IDs come only from trusted observations, with exact source navigation.
  Body8KiB cap; account limit60 per configured rate window; outage fails closed with503.
  Health-related terms belong in the body; do not log request bodies in an edge/APM layer.

Timeline events additionally expose optional sourcePage/status/occursAt. Appointments group by UTC
calendar date while preserving the actual instant; follow-ups use confirmed date and linked source
page. Only CONFIRMED/EDITED follow-ups appear. Concept filters exclude non-observation event types;
document filters retain linked documents/observations/follow-ups. Symptoms and appointments have no
fabricated document ID. These event rows do not feed laboratory comparisons.
