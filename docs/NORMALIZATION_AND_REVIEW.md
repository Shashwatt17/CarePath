# Normalization and human verification — Phase 5

An ExtractionCandidate remains an immutable machine reading. Processing never creates a trusted
MedicalObservation. Even HIGH candidates start PENDING_REVIEW; an authenticated owner must explicitly
confirm or correct them. Verification means the user checked a reading, not clinical validation.

## Terminology

V5 seeds 23 demo concepts: Hemoglobin, Hematocrit, RBC count, WBC count, Platelet count, MCV, MCH,
MCHC, Vitamin D, Vitamin B12, Glucose, HbA1c, Creatinine, Urea, Blood urea nitrogen, Sodium,
Potassium, Total Cholesterol, LDL Cholesterol, HDL Cholesterol, Triglycerides, TSH and Ferritin.
A bounded immutable startup snapshot reads active concepts and reviewed aliases (max 1,000 concepts,
5,000 aliases/rules). Deploy an additive migration and restart to change it. No per-candidate SQL
lookup, runtime terminology administration, LLM, fuzzy mapping or context inference is used.
Case-folding and whitespace normalization affect matching only; originals remain unchanged.

Outputs: EXACT, ALIAS_MATCH, AMBIGUOUS, UNMAPPED. Hb/HGB/Haemoglobin are explicit aliases of
Hemoglobin. Urea and BUN remain distinct; the composite label `urea/bun` is intentionally ambiguous.
Glucose never becomes fasting glucose. Vitamin D never implies a particular D2/D3 or assay method.
All standard identifiers and identifier systems are NULL: a display name is insufficient for LOINC.
An explicit user concept selection is recorded USER_SELECTED, never disguised as a machine match.

Sources for terminology spelling/scope and unit definitions, reviewed for this limited demo:
- https://medlineplus.gov/ency/article/003642.htm (CBC names)
- https://medlineplus.gov/lab-tests/bun-blood-urea-nitrogen/ (BUN terminology)
- https://www.mayocliniclabs.com/order-tests/si-unit-conversion.html (glucose factor)

This is a small authored demo dictionary, not a licensed comprehensive clinical terminology or a
clinically validated mapping service. A clinical terminology review is a deployment prerequisite.
The database `reviewed` flag means explicitly curated configuration; it is not clinical certification.

## Units and precision

V5 has 26 explicit rules: one same-unit rule for each listed concept, g/L to g/dL for Hemoglobin
and MCHC (multiply by 0.1), and glucose mg/dL to mmol/L (multiply by 0.05551). Unit strings are
case-sensitive after trimming. Molecular conversions are concept-specific. No generic mg/dL rule,
Vitamin D mass/molar conversion, urea/BUN conversion, offset conversion or inferred missing unit.
Only the current reviewed rule version, zero offset and the concept's target unit are eligible;
multiple eligible rules abstain. Incompatible, missing, unknown or comparator-only values return
null normalized value/unit plus an explicit status. Inequalities remain threshold readings, not
exact numerical points.

All arithmetic/persistence uses BigDecimal/numeric. Identity and power-of-ten scaling preserve
source precision exactly. Approximate conversion rounds HALF_EVEN to the smaller of source precision
and the four significant digits of 0.05551. Thus 90 becomes 5.0, not a spurious long decimal.
Normalized values travel as JSON strings to avoid browser floating-point conversion. Original
lexical values, units and reference text are never overwritten. Derived comparisons use the
unrounded value against the supplied same-unit numeric bounds; there are no generic medical ranges.
Reference bounds remain in original/effective units (normalized range columns remain unset).
Source HIGH/LOW/ABNORMAL flags remain separate from BELOW/WITHIN/ABOVE/NOT_COMPARABLE comparisons.

## Review policy and transitions

Direct Confirm requires native HIGH extraction, no parser/confidence warnings, known unambiguous
concept, supported exact unit/value, valid numeric format and known date. Missing reference text
from extraction carries the existing parser warning; textual ranges do not create a derived status.
OCR, LOW/MEDIUM, ambiguous/unknown mapping and unsupported units cannot silently pass confirmation.

Correct requires editable fields, explicit sourceReviewed=true and a 10–500 character reason;
validation and normalization run again. Known concepts require a supported unit. Ambiguity must be
resolved. A user may explicitly verify an unknown term as an unmapped reading with no normalized
representation; that does not make it suitable for future trend comparison. Missing dates stay null.
Negative/non-numeric lab values, reversed numeric ranges and arbitrary/inactive concept IDs fail.
The owner can reject any intact candidate; rejection creates no observation.

Only PENDING_REVIEW → CONFIRMED / CORRECTED_AND_CONFIRMED / REJECTED is supported. No reopen or edit
of an already verified observation. Same action and identical canonical request hash replay returns
the original result; changed replay or stale version is 409. Document lock then candidate lock serializes
review with other review/deletion; a unique source_candidate_id prevents duplicate observations.
All changes, source rows, verification history and general audit events commit in one DB transaction.

Documents with candidates become NEEDS_REVIEW even if extraction is HIGH. Once all candidates are
resolved, COMPLETED means review finished, including rejected rows; it does not mean all readings
were accepted. Documents with no candidates retain Phase 4 classification/extraction semantics.
Classification correction is not added in this phase.

## Provenance, authorization and deletion

Review APIs use validated JWT/session identity and queries scoped by ID plus owner. No client owner,
actor, source, confidence, normalized value or trust status is accepted. The 8 KiB JSON bound applies
to review writes; DTO lengths and the existing unknown-field rejection remain active. Unrelated
observation write routes stay denied. Foreign and absent UUIDs both produce generic 404.

Before promotion the service checks exact source offsets/snippet against the owner-scoped stored page
and original field membership in evidence. It copies raw fields unchanged and stores effective fields
separately. ObservationSource retains page/extraction/document/owner/snippet/offsets; the immutable
candidate retains OCR box/method and remains linked by a composite FK. A correction never rewrites
extraction history. Concept name/version snapshots and normalized values are persisted, not recalculated
on reads. All current standard codes remain unset. Future vocabulary upgrades need explicit migrations,
not automatic historical renormalization.

candidate_verification stores the decision, actor, timestamp, references, reason, changed field names
and idempotency hash. General audits contain action/owner/document identifiers, never values/reasons.
The trusted read view requires an accepted candidate, owner verification, source and matching decision.
Legacy AUTO_VERIFIED rows are excluded. Deleting a vault document cascades its extraction, candidate,
observation, source and private verification history; the existing blob-deletion outbox is unchanged.
General security audits retain only their existing minimal metadata.

## UI and limits

/review is an authenticated paginated queue. The document viewer embeds the same review component.
Extracted, normalized preview and verified values are visually separate; React escapes all text.
Correction requires an explicit server preview and source acknowledgement. Changing any field invalidates
the preview. Verified/rejected filters preserve access to original evidence and terminal decisions.
View Source links from the queue to the owner-authorized document/page; no raw local file URLs.

Native PostgreSQL/Flyway/Redis and rendered-browser verification remain pending. H2 exercises real
controllers/security/transactions with a test schema; PGlite checks actual migration SQL separately.
The new source PDF and JSON ground truth are artificial. Measured regression scores are not clinical
accuracy. No timelines, trends, AI, care modules or clinical decision support were implemented.
