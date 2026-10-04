# SYNTHETIC DEMO DATA — NOT A REAL PATIENT

`normalization-lab.pdf` adds a deterministic alias/conversion demo without changing Phase 4 reports.
Upload as LAB_REPORT, process, then open Review:

- Hb 104 g/L maps to Hemoglobin and normalizes to 10.4 g/dL. The extraction unit dictionary still marks g/L uncertain: use Correct / verify, retain 104 g/L, preview and explicitly acknowledge the source.
- Glucose 90 mg/dL normalizes to 5.0 mmol/L using the listed concept-specific rule.
- Sodium deliberately has an unsupported unit. No normalized value is created; direct confirmation
  is blocked. Reject it, or correct only if the original supplies evidence. Do not guess a unit.

These numbers and supplied reference ranges are invented fixtures, never medical guidance.
For uncertain correction, use the existing Phase 4 September TSH `l.8?`; demonstrate an explicit
synthetic correction to `1.8` with source acknowledgement, retaining `l.8?` as the original.
For rejection, reject September Ferritin. Review decisions are terminal in this version.
The January/April/September files are unchanged; no cross-report interpretation is implemented.

Regenerate: `python3 scripts/generate-normalization-fixture.py` (reportlab).
