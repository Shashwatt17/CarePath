# Phase 4 document intelligence

## Boundary

A completed extraction is **not a verified medical observation**. No Phase 4 code writes
medical_observation, canonical concepts, timelines or clinical advice. Every result remains a
candidate. User category/date/provider metadata is never silently replaced. No LLM or API key is used.

## Execution

Upload validation and original storage remain the Phase 3 boundary. The owner explicitly starts
processing from the document viewer or POST process. The request stores a durable processing_job
and changes the document to PROCESSING in one transaction, then returns 202. A scheduled worker
claims one due job per application instance with FOR UPDATE SKIP LOCKED, an expiring lease and a
random fencing value. Expensive work happens outside the database transaction through ExtractionEngine.
This port can later be implemented by a queue worker without changing extraction domain components.

The original's SHA-256 is verified before processing. A private staging copy is passed to a separate
Java 21 JVM with a 256 MiB heap and 64 MiB direct-memory limit. The supervisor enforces a wall deadline,
kills descendants, validates the complete result and removes staging. Worker environment variables
are cleared except PATH, fixed locale and an OCR thread limit; credentials are not passed.
The child also watches its parent and deadline. Abandoned job directories older than one hour are
reaped at startup (10-second delay) and hourly without following symlinks. Normal cleanup is immediate.
This is process isolation, **not an OS filesystem/network sandbox or a hard total-RSS/cgroup quota**.

Results, pages, candidates, final job state, document state and success audit commit atomically.
The worker locks the owner-scoped document before checking its job fence: deletion or a stale lease
prevents stale publication. Document deletion cascades extraction pages/candidates/jobs; the existing
durable blob-cleanup mechanism removes the original. A running child may retain a private staging
copy until completion/deadline; deletion is not a promise of instantaneous erasure from RAM/backups.

## Extraction components

- DocumentTextExtractor: PDFBox native text, sorted per page, with UTF-16 character offsets.
- OcrProvider / TesseractOcr: actual local Tesseract TSV subprocess, English language pack, psm 6.
  Images use OCR. A PDF page uses OCR only when native text has fewer than 40 alphanumeric characters
  or 15 letters. Mixed PDFs retain a method per page. OCR renders at up to 150 DPI / 4 million pixels.
- DocumentClassifier: explicit heading rules for the nine supported categories; one matching category
  is HIGH for native text, MEDIUM for OCR. Ambiguous/no headings produce OTHER/LOW. Evidence is retained.
- DocumentInformationExtractor / MedicalDateParser: explicitly labelled dates/lab/provider only.
  ISO dates and unambiguous English month dates are supported. Conflicts/ambiguous dates remain absent
  with warnings. A report date takes precedence over a sample date when both labels are present.
- LabObservationExtractor: bounded pipe/tab/multiple-space rows, plus conservative unstructured
  numeric rows associated with a recognized lexical unit. Numeric values/comparators, original text,
  explicit flags and ranges remain separate. Names are not normalized; units are not converted.
- ReferenceRangeParser: validated numeric lower–upper intervals or original textual range. Missing
  fields stay null; illegible values such as l.8? remain original strings with no invented numeric value.
- ProvenanceValidator: rejects candidates whose names/values/units/ranges are absent from their exact
  source substring; checks page/method/offsets/bounding boxes and document information associations.

Deterministic classification is intentionally narrow, not a general-purpose clinical document model.
The parser supports a bounded subset of English layouts. Handwriting, arbitrary tables, multi-column
association, uncommon units and rotated/noisy scans may be missed or require review.

## Provenance and confidence

V4 adds extraction_candidate and extends the existing extraction/page/job tables. Candidate/page/
extraction/document/owner composite foreign keys bind evidence to the same source and owner. Typed
layout/classification-information records are serialized internally as JSON text; there are no
client-supplied JSON blobs. Candidates are normalized database rows, separate from trusted observations.

Each candidate carries page, exact extracted-line text, UTF-16 start/end offsets, extraction method,
and OCR normalized bounding box where available. Native PDF coordinates are not yet recorded.
Dates/provider labels and classification signals have their own source evidence. OCR evidence is an
OCR transcript, not a claim of exact optical transcription: compare it with the unchanged original.

Confidence is a rule-based evidence band, not a calibrated clinical probability:
- HIGH: native text, structured numeric row, recognized lexical unit, present valid range.
- MEDIUM: weaker row structure, missing/unrecognized unit or missing range; all OCR is at most MEDIUM.
- LOW: unparseable numeric value, invalid range, minimum OCR word confidence below 0.85 or conflicting
  values for an identical original name in the same report.
Reasons are stored. Tesseract's own confidence is rounded to two decimals; it is not invented precision.

Any non-HIGH classification/candidate, OCR use, metadata warning, or empty detected lab report yields
NEEDS_REVIEW. Otherwise extraction is COMPLETED. Both states remain unverified candidates.
Confirm/correct/reject and trusted observation promotion deliberately belong to a later phase.

## Failure and retry

Permanent failures include invalid/corrupt/empty documents, resource limits and integrity mismatch.
Unavailable OCR/worker and timeouts are retryable. Transient failures retry at 10 then 20 seconds,
up to three attempts. A terminal retryable FAILED job offers explicit Retry; at most three jobs per
document lifetime. The API checks an active-job allowance of five per owner (admission is a count,
not a distributed strict tenant quota). A single worker instance runs only one extraction at a time.
Expired RUNNING leases are reclaimable; stale workers cannot publish. Original bytes remain intact.
The UI shows safe generic failure text and exposes Retry only when permitted; no command/path/trace
is returned. Operational logs contain outcome/attempt/correlation IDs, not extracted values or text.

## Limits and configuration

| Setting | Default |
| --- | --- |
| PROCESSING_TEMP_ROOT | ../.data/processing-temp, backend-relative |
| TESSERACT_COMMAND | tesseract, operator-controlled executable/path |
| EXTRACTION_TIMEOUT_SECONDS | 120 (validated 5–300) |
| OCR_TIMEOUT_SECONDS | 30 (validated 1–60) |
| EXTRACTION_MAX_PAGES | 20 (validated 1–200; vault upload remains max 200) |
| EXTRACTION_MAX_CHARACTERS | 250000 (validated 1000–500000) |
| PROCESSING_WORKER_ENABLED | true |

Also: input <=20 MiB, image <=20 million pixels, page text <=50000 characters, <=500 candidates,
<=2 MiB OCR TSV and <=4 MiB worker result. PDFBox retains Phase 3 validation/stream limits.
PDF decoding/rendering during upload/preview still happens in the API process under Phase 3 limits;
the new child isolation applies to **intelligence extraction**, not all vault parser entry points.
Use encrypted local volumes, trusted root parents and restrictive OS ACLs. Windows/other platforms
need their own runtime verification; Linux Java 21 + Tesseract is the exercised configuration.

## UI and telemetry

The document viewer polls real status, renders escaped candidate strings, distinguishes predicted
metadata from saved metadata, and opens the corresponding safe rendered page with its evidence text.
OCR boxes are stored but not drawn as highlights yet. No HTML from documents is rendered.
Unsaved metadata edits are retained during processing refresh; stale edits use existing version checks.

Meters: carepath.extraction.duration (outcome), completed (category), failed (safe code),
ocr_fallback, retries, persistence_unavailable. Meters are registered internally; no public metrics
endpoint was added. Audits: PROCESSING_REQUESTED, PROCESSING_SUCCEEDED, PROCESSING_FAILED,
EXTRACTION_VIEWED; system worker events identify SYSTEM while retaining owner/resource scope.

See evaluation/README.md and PROJECT_STATUS.md for executed evidence and remaining live-service gates.
