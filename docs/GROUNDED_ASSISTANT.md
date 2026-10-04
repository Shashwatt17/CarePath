# Phase 7 evidence-grounded record explanations

CarePath explains verified structured records; it does not acquire medical knowledge from model output.
`StructuredRetrieval` → `AssistantService` → optional `HealthExplanationProvider` → `ExplanationValidator`
returns transient typed answers. `SavedQuestionService` persists only explicit user-saved drafts.

## Retrieval and trust

Every point comes from Phase 5's `trusted_medical_observation` view through owner-scoped Phase 6 queries.
Accepted candidate, human verification, source and matching decision history remain mandatory. Pending,
rejected, ambiguous unverified and foreign-owner candidates never enter retrieval. The assistant does not
query raw extraction pages. Approved evidence stays in the response for the authenticated owner only.

A request can select one observation, one canonical concept or two reports. Without selection, bounded
English token n-grams use the existing exact/alias dictionary to identify one concept. Multiple or unknown
concepts ask for a narrower scope. Report comparison questions use two selected reports, or uniquely dated
latest reports; English month names and four-digit years restrict selection. Ambiguous dates/reports abstain.
This is a limited deterministic intent/retrieval layer, not unrestricted natural-language understanding.
Concept history considers at most the latest 12 points, ordered chronologically for explanations; any
truncation is disclosed. Date filters are applied within that bounded window; older matches can therefore
be unavailable. Report pair selection scans at most 100 dated report summaries. Comparisons exceeding 12
distinct observations ask the user to narrow scope/use Changes instead of silently dropping facts.

Numerical changes, comparability and supplied-range outcomes come directly from Phase 6. No model decides
values, units, concepts, verification, deltas, ranges, dates or evidence. Same-day/unknown context and
unsupported units retain insufficiency. No external medical ranges or definitions are introduced.
Responses carry complete source points, page/snippet and both comparison sides. After an optional provider
call, all source points are revalidated; changed/deleted/untrusted sources discard the transient answer.
There is no persistent conversation store or answer-ID endpoint. Existing history/vault evidence APIs
recheck ownership on every request. A previously displayed answer is a snapshot; refresh after record edits.

## Optional model contract

The OpenAI Chat Completions adapter is real HTTP integration, disabled unless `LLM_ENABLED=true` and
`LLM_API_KEY` is present. `LLM_MODEL` and operator-only HTTPS `LLM_ENDPOINT` configure the provider.
No live credential was supplied or used for this checkpoint. Contract tests use a controlled HttpClient,
not a live model. Frontend requires explicit per-request opt-in before sending selected facts externally.

The model may **choose/order approved complete phrasings**, rather than generate unchecked medical prose.
Input is JSON `{intent,facts}`. Each fact has a local `fN` ID, kind, deterministic text, local `eN` evidence
references and two approved phrasings. Output is strict JSON:

```json
{"selections":[{"factId":"f1","phrasing":0,"evidenceIds":["e1","e2"]}]}
```

Every fact must appear exactly once; phrase index must be in range; evidence array must match that fact
exactly. Unknown/foreign/malformed citations, free-text answer fields, missing/extra facts and contradictory
prose fail validation. Only the server-held approved wording is rendered. Schema conformance alone is not
trusted. This deliberately limits AI fluency: the model is a constrained presentation layer, not a general
medical interpreter. The UI labels `AI_CONSTRAINED` as approved wording selected by AI; rule-based responses
are clearly `DETERMINISTIC`, never disguised as model output.

The system instruction is fixed. User question, raw documents/snippets, filenames, providers, account/name/
email, storage keys and database UUIDs are not sent. Selected concept/value/unit/date statements and
computed change/range statements may leave the local environment. Human-corrected fields are still
untrusted data. They cannot become instructions or tool calls, and cannot introduce arbitrary model prose.
`store:false` requests no API response storage; it does not promise zero provider retention. Operators must
review provider policies, lawful use and consent before using real health data. Changing the endpoint can
change the data recipient. Only trusted operators may configure it; HTTPS and redirects-disabled apply.

## Failure, safety and cost

Missing configuration, HTTP error, refusal, malformed/empty output, quota, response-size breach, timeout,
invalid citations or schema/phrase violation produce useful deterministic fallback. No fabricated provider
response is returned. No recursive calls or retries; at most one call per opted-in request. Redis-backed
10 requests/owner/minute applies to external calls; limiter outage denies external dispatch while local
explanations still work. Two concurrent external requests per process are permitted. Deployment replicas
must have additional global cost limits; this is not a distributed concurrency semaphore.

Limits: request body 8 KiB, question 1000 characters, observations 12, serialized provider context 16000
UTF-8 bytes, timeout default 15 seconds (1–20), output tokens default 1500 (128–3000), streamed response
bytes default 32768 (1024–65536). The deadline includes body completion; cancellation stops a hung future.
Actual streamed bytes are bounded, not just Content-Length. No redirects or model tools are enabled.

Deterministic `record-safety-en-v1` checks the question before retrieval/provider dispatch. Diagnosis,
medication/dose/treatment-change requests receive a record-explanation boundary and clinician redirect.
A limited set of urgent English phrases (severe breathing/chest/bleeding, unconsciousness and stroke-like
phrases) returns conditional emergency escalation immediately, without model reasoning or a diagnosis.
It says to contact local emergency services rather than guessing the user's emergency telephone number.
Negations/historical quotations can conservatively trigger the same conditional notice. This is not a
complete triage system, multilingual safety classifier or clinically validated rule set. It never returns
an all-clear when no phrase matches. Clinical review is a deployment gate.

Reference content is limited to this labelled predefined safety message. Engineering sources consulted
2026-09-24 (paraphrased, not copied as clinical advice):
- https://www.nhs.uk/conditions/heart-attack/
- https://www.nhs.uk/conditions/stroke/symptoms/
- https://www.londonambulance.nhs.uk/calling-us/calling-999/

Provider contract reference:
https://developers.openai.com/api/docs/guides/structured-outputs

## Clinician questions, persistence and security

Deterministic suggestions attach the exact contributing observation IDs. Users can edit before saving,
manually create, edit with optimistic version, list/page or delete questions. Text is a user-controlled
draft, not a verified statement or provider-certified recommendation. Server sourceType `USER` describes
saved wording, including edited suggestions; clients cannot set origin/trust/owner fields. All linked IDs
are resolved through the trusted owner view before insertion. Unknown/foreign IDs use generic 404.

V7 adds normalized `saved_question_evidence` with composite owner foreign keys and an expected-evidence
count. It reuses V1 `saved_question`; no old migration changes. Source deletion cascades evidence links,
retains the user's draft and displays evidenceMissing when links are lost. Text may be stale after source
correction; questions remain user notes rather than derived medical facts. Future Visit Pack must freshly
resolve evidence and ask the user to review draft wording. Existing unused legacy observation_id is never
populated by Phase 7. Future pack foreign keys can prevent deletion until that later module handles scope.

JWT/session, CSRF/CORS/default-deny routing and ownership remain unchanged. Assistant POST/PUT bodies use
the existing bounded wrapper. React renders plain text, with no Markdown/HTML or provider URLs. Evidence
links resolve via authenticated source navigation. Audit events contain only action/outcome/owner/resource/
request ID, never prompts/answers/questions. Operational logs contain safe provider outcome/duration only.
No transcript database retention is introduced; saved questions persist until user deletion/account deletion,
subject to existing backup/retention deployment limitations.

## Verification boundary

See TESTING.md and PROJECT_STATUS.md for exact commands/counts. H2 API/security integration, provider
contract tests, client tests, PGlite SQL and actual production Next HTTP proxy were used where reported.
No paid/live model, native PostgreSQL/Flyway/Redis, rendered browser, clinical safety validation or external
penetration test is claimed. Future symptoms, appointments, reminders, Visit Packs, sharing and nearby care
are not implemented by this phase.
