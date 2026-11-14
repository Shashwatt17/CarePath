# CarePath reliability plan

Target: explicit supported document types, source-grounded extraction, and safe review or rejection outside those limits. No universal medical-document accuracy guarantee. Existing hosting, authentication, ownership, private originals, patient summaries, sharing and dated-column separation are retained rather than rebuilt.

## Release A: laboratory layout reliability (deployed and verified 14 November 2026)
- Exclude administrative labels without restricting measurements to a known analyte list.
- Recognize report-generated/sample-collected labels and provider headings in secondary columns.
- Preserve ordered, verbatim reference continuations and their full source span. Do not turn category bands into a single numeric interval.
- Explicitly flag ambiguous numeric punctuation; never infer a missing decimal.
- Display multiline references and a targeted punctuation warning in the review screen.
- Keep older extractions immutable and identify v3 as a new reader version.

Acceptance: synthetic administrative rows excluded; genuine unfamiliar analytes retained; metadata has source evidence; reordered/invented reference fragments rejected; existing extraction/review tests pass; one hosted synthetic image verifies the changed path. The private supplied image must not enter fixtures, commits or external OCR services.

## Release B: measured numeric and terminology reliability
1. Build a versioned synthetic/de-identified evaluation set covering layouts, wrapped cells, multi-date columns, comparators, missing units, decimal punctuation, cropping and rotation. Split development and held-out cases by layout; publish exact denominators, not universal accuracy claims.
2. Add bounded region-specific OCR retries only after benchmarking. Preserve disagreeing alternatives and require review; never select a number because it fits a reference range. Budget retries per document and retain the 512 MB deployment constraint.
3. Extend reviewed concept/unit aliases for the supported panels, including dimensionless ratios and calculated/direct variants. Use additive terminology migrations; never overwrite previous normalization provenance.
4. Model one-sided limits and categorical reference bands separately, retaining inclusive/exclusive operators and original text. No automatic clinical diagnosis or generalized ranges.

## Release C: upload quality and document integrity
5. Separate image quality signals (resolution, orientation, cropping/glare/readability) from OCR confidence. Calibrate against labelled synthetic/consented images. Give precise retake guidance and avoid hard-rejecting usable originals.
6. Extend page-level accounting, repeated-page detection, header carryover and possible mixed-patient warnings. Never silently merge patients or different report dates.
7. Improve per-field source crops and targeted review, including keyboard/mobile use. Only explicitly verified readings enter the trusted timeline or derived summary.

## Release D: additional document types
8. Add separate contracts and evaluation gates for qualitative laboratory results, microbiology/pathology, radiology report text, discharge summaries and prescriptions. Preserve negation, dates, units and medication attributes; retain unsupported/handwritten content for viewing and review.
9. Keep scans/waveforms and clinical image interpretation outside text OCR capability claims. Category recognition alone must not imply structured extraction support.

## Release E: production assurance
10. Evaluate bounded concurrent jobs, worker timeouts, queue recovery, memory ceilings, restore drills and sustained hosted image processing. Preserve existing owner-scoped access and immutable review history. Complete targeted accessibility/mobile checks for changed review controls.

## Cost controls
Use embedded PDF text first. Cache immutable extraction by owner/document hash and pipeline version. Retry only uncertain regions within strict time/memory limits. No external OCR/model transmission or paid services introduced by this plan. Use targeted regression checks during edits, then one appropriate integration check per release; reuse unchanged baseline evidence. Batch deploys after passing checks. Keep status and limitations current so future sessions do not redo completed work.

## Current limits
The current photo reader still misreads some characters/decimals. This release flags ambiguous punctuation but does not implement region-specific retries. Reference categories are preserved, not clinically interpreted. The quality warning is still OCR-confidence based. Broader document-type extraction and the held-out benchmark remain pending.
