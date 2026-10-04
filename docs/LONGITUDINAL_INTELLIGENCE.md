# Phase 6 longitudinal intelligence

All numerical history reads `trusted_medical_observation`: accepted candidate, human verification,
owned source and matching verification history. Pending, rejected and unverified candidates are
excluded. Document timeline events are metadata, not evidence of a verified medical measurement.
Original, effective and normalized representations remain separate. Every point retains the
observation/candidate/document IDs, page, snippet, character offsets, method and verification time.

## Comparability and precision

Compare only the same canonical concept, supported mapping, exact numeric value, supported
normalization into the same unit and normalization version, distinct reports, strictly increasing
dates and the same known laboratory. Known specimen/method must match; known versus unknown fails.
Both-unknown specimen/method permits a limited same-lab numerical comparison, not proof of clinical
assay equivalence. Phase 5 does not extract assay/specimen context. This limitation is returned to
the UI. Threshold values, missing dates/units, same-day ordering and different laboratories abstain.
Unmapped trusted readings remain visible but cannot drive a numeric trend.

BigDecimal computes signed delta (current minus previous). Percentage uses abs(previous) as denominator,
HALF_EVEN to two decimals; previous zero produces null. API decimals are strings; JavaScript numbers
are used only to draw finite chart coordinates. Decisions use unrounded values.

Stability is a configurable numerical display policy, never a clinical threshold:
`abs(current-previous) <= max(abs(current),abs(previous)) * tolerance`.
Default relative tolerance 0.01 groups small numerical differences without inventing concept-specific
clinical meaning; zero versus zero is stable. Configure `HISTORY_RELATIVE_TOLERANCE` and optional
`HISTORY_CONCEPT_TOLERANCES=concept-UUID=0.005;other-UUID=0`. Allowed tolerances 0..0.1; startup rejects
invalid settings. The response includes policy version and effective tolerance for reproducibility.

Reference transitions use each reading's own supplied numeric interval and effective value. Bounds
must be ordered, identical across reports, and in the same effective source unit. Changed bounds,
text-only ranges, missing bounds or different source units abstain from range transitions, even if
normalized numeric direction is available. No old range is applied to the new value. Source abnormal
flags are displayed separately from ENTERED/EXITED/REMAINED_INSIDE/REMAINED_OUTSIDE derived outcomes.
No diagnosis, medical normality, prognosis or clinical significance is inferred.

## Report coverage and trends

Selected report comparisons expose both full point sources. Multiple readings for one concept in
one report produce insufficient evidence rather than arbitrary selection. Common verified concepts
can be compared even if other candidates remain pending. Presence/absence requires both reports
COMPLETED, all candidates reviewed and mapped, consistent dates, LAB_REPORT and the same lab.
NEWLY_OBSERVED additionally requires a matching explicit panel or at least three shared concepts.
PREVIOUSLY_TRACKED_NOT_PRESENT requires matching explicit CBC/COMPLETE BLOOD COUNT or COMPREHENSIVE
METABOLIC PANEL headings. Filenames do not establish panel membership. Wording means absence among
verified records, not that a test was not performed or a condition disappeared. An extractor can miss
rows; this conservative scope is not proof of complete clinical report coverage.

Three or more comparable points produce increasing/decreasing/stable/mixed summaries from adjacent
pairs. Two points can be plotted without a three-report summary. Invalid points and duplicate dates
break chart sequences; no interpolation across known incomparable data, forecast or smoothing.
Same-day and undated readings remain visible in the paginated history. Trend calculation uses the
latest 500 points with explicit truncation; report comparisons reject more than 500 readings/report.
History/event pages have at most 100 entries. Current concept selector loads the first 100 concepts
(the current catalog has 23); API pagination supports larger future catalogs.

## Architecture and security

`longitudinal/HistoryRepository`, `HistoryService`, `ObservationComparison`, `StabilityPolicy`,
`TrendService` and typed `HistoryDtos` separate queries, access control and deterministic calculation.
Owner-scoped SQL derives identity only from the security context. Resource IDs are not authorization;
foreign and nonexistent resources return the same 404. Concept IDs expose only the caller's readings.
The existing default-deny security configuration explicitly permits authenticated history GET routes.
Parameter binding and escaped literal LIKE search prevent SQL injection/wildcard surprises.

Events use a paginated union and batch-load only returned observation IDs. Concept history queries
are bounded; no per-point database calls. V6 adds read indexes only. No derived fact store/cache:
each repeatable-read request recomputes from the current trusted data, so deletion and future allowed
correction cannot leave an irreversible trend record. This is not a distributed concurrency benchmark.
Evidence uses existing authenticated vault preview and audit controls. React renders text as text;
no HTML injection, external evidence URL, medical-value logging or new token persistence is introduced.

## UI and limitations

`/timeline` supplies chronological groups, search, concept/document filters, pagination, original and
normalized details, verification information and evidence links. Recharts uses linear segments,
actual dates, exact-value tooltips, keyboard evidence points and a text/link fallback. `/changes`
compares two explicitly selected reports, displaying insufficiency reasons, both sources, deltas and
separate range outcomes. Document evidence reuses `/records/{id}?candidate={id}` to select the source page.
The chart's concept window is independent of filename/document filters and is labelled accordingly.

Native PostgreSQL/Flyway/Redis and rendered-browser interaction remain unverified in this environment.
HTTP proxy tests are not browser tests. The full suite uses H2 for API/security integration and PGlite
for supplementary PostgreSQL SQL checks. No LLM participates in any calculation. Symptom/appointment
and other future event types, clinical explanations and completed-observation correction UI are absent.
