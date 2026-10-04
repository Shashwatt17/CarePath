# PostgreSQL schema

The executable definition is `backend/src/main/resources/db/migration/V1__foundation.sql`.
This is a complete initial domain schema, not an implementation of its product workflows.
No patient, vocabulary, mapping, conversion or medical-reference rows are invented or seeded.

## Conventions

- UUID primary keys, UTC `timestamptz` audit timestamps, explicit clinical `date` values.
- Mutable rows have `version` for future JPA `@Version`; database triggers update `updated_at`.
- Every patient-owned table has `owner_id`, `(id, owner_id)` uniqueness and an owner index.
- Composite foreign keys enforce same-owner links. This is not row-level access authorization;
  API repositories must still scope every read/write to the authenticated principal.
- Observations keep exact original text and values, independent normalized numbers/units,
  mapping/version, confidence, date basis, specimen/method/panel context and review state.
- Source joins enforce observation, extraction, document and page consistency.
- A trusted observation view excludes pending/rejected records and records without provenance.
  It deliberately does not infer a trustworthy date or comparable unit.
- `numeric` avoids binary floating-point conversion drift. Unknown data is SQL NULL, never zero.
- Date precision prevents turning a partial report date into a fabricated exact day.
- Full-text document search plus owner/date/concept indexes support paginated retrieval.
- Jobs and reminders use durable PostgreSQL rows; Redis is never their only storage.
- Public shares reference a selected immutable Visit Pack revision. Only token digests persist.
- Typed Visit Pack references avoid unvalidated polymorphic UUID references; exactly one target
  per item. `RESTRICT` protects provenance from accidental deletion.

## Tables

- `app_user`
- `refresh_token`
- `medical_document`
- `document_extraction`
- `document_page`
- `processing_job`
- `canonical_medical_concept`
- `concept_alias`
- `unit_conversion_rule`
- `medical_observation`
- `observation_source`
- `observation_review`
- `symptom`
- `appointment`
- `appointment_document`
- `appointment_symptom`
- `clinical_visit`
- `prescription`
- `prescription_item`
- `follow_up`
- `saved_question`
- `reminder`
- `notification`
- `visit_pack`
- `visit_pack_item`
- `share_token`
- `audit_event`

## Relationships and deletion contract

User → documents → extraction revisions/pages → observations → evidence/reviews.
User → appointments ↔ selected documents/symptoms → visits and follow-ups → reminders → notifications.
User → Visit Packs → typed selected items; shares → one pack revision.
Global concepts → aliases and versioned conversion rules → normalized observations.

A future deletion service must first revoke/invalidate dependent shares and pack snapshots,
remove pack item and question references, remove dependent reminders/notifications, and only
then remove source rows/blobs. RESTRICT intentionally forces this orchestration. Account deletion
must use this same ordered service, not assume one cascading DELETE succeeds. Blob deletion
needs a durable deletion/outbox record independent of the document row; Phase 1's processing_job
DELETE_BLOB type is usable while the document remains present, before final DB deletion.
Originals and generated PDFs require coordinated deletion and backup retention documentation.
Audit resource IDs intentionally have no resource FK so deletion can retain a content-free event.

## Concurrency and future migration policy

Worker claims: transaction + `FOR UPDATE SKIP LOCKED`, lease expiry and bounded retries.
Reminder idempotency keys and notification uniqueness prevent duplicate in-app deliveries.
Refresh-family revocation and rotation must be atomic. Sharing checks expiry/revocation on
EVERY request; caching cannot bypass a check. JPA optimistic version checks prevent lost edits.
Append V2+ migrations after V1 has been applied; never silently edit a deployed migration.
Hibernate must remain `ddl-auto=validate`. Flyway baseline/clean are disabled.

## Remaining service invariants

Schema constraints do not implement authorization, evidence validation, extraction confidence
thresholds, allowed state transitions, audit immutability, idempotent uploads, matching conversion
rule to concept/unit, or medical safety. These require later services and adversarial tests.
Follow-up source pages and prescription item pages require service validation against their document.
Do not treat schema presence as feature completion.

## Phase 2 additive authentication schema

V2__authentication_security.sql adds auth_session (28 total tables), links each refresh-token
family to a same-owner session, enforces one child per parent refresh token, and permits null
audit owners for unknown-account login failures. Audit reason codes contain no arbitrary input.
Session rows are locked before rotation/logout; access-token validation checks active session/owner.
V1 is preserved byte-for-byte. Actual JDBC/Flyway execution on native PostgreSQL remains pending;
V1 + V2 SQL and 19 invariants have been exercised with PGlite.
