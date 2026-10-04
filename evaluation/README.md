# Synthetic extraction evaluation

Run from repository root: `sh evaluation/run.sh` (Java 21, Maven, Tesseract + eng required).
The runner executes ExtractionEvaluationIT against the actual isolated PDFBox/Tesseract engine.
It does not mock OCR, call an LLM or substitute annotated values into extraction.

- `ground-truth.json`: independently authored expected labels/rows/page evidence, generated alongside
  the fixture definitions by `scripts/generate-extraction-fixtures.py`.
- `results/latest.json`: actual measured execution, timestamp, per-document mismatches and numerator/
  denominator metrics. Evaluation code lives in backend test sources, not production.
- Six evaluated documents: January/April/September, scanned PNG, scanned PDF, prompt-injection PDF.
  The corrupted fixture is evaluated as a safe failure in ExtractionRuntimeTest, not as an accuracy row.
- Six category labels and 16 annotated rows. All six documents are lab reports: the classification
  result is not balanced multiclass accuracy. Nine category headings are separately rule-tested.
- Row names match after case/whitespace normalization only, **not** medical concept normalization.
  Value/unit/range/date/provider/flag metrics use exact strings (null stays absent).
  Provenance checks page/method, actual source offsets into extracted text, and expected evidence
  after whitespace/pipe-separator normalization. OCR text is checked against the authored transcript.
- Row precision penalizes extra candidates; recall and field metrics penalize missing candidates.
  These metrics are not clinical validation or generalization estimates. The dataset is small,
  intentionally controlled, and overlaps development fixtures; no held-out performance is claimed.

The recorded run measured classification 6/6, row detection 16/16 (16 predicted), and each name/value/
unit/reference/date/provider/flag/provenance field 16/16. The intentionally illegible value is correctly
preserved as text rather than converted into a guessed number. See the JSON for exact results/time.
The opt-in suite additionally exercises actual JPEG OCR. It fails when runtime OCR is absent rather
than silently skipping or returning fabricated data.

Generate fixtures (development only): Python 3 with reportlab and Pillow, DejaVuSansMono at the path
in the generator. Generation is not needed to run the committed synthetic fixtures. The files are
clearly labelled SYNTHETIC DEMO DATA — NOT A REAL PATIENT; ranges are artificial parser test data.

## Phase 6

Run `./evaluation/run-longitudinal.sh` with JDK21/Maven. Ground truth resides in
`phase6/comparison-ground-truth.json`; actual API fixture assertions use the unchanged Phase 4 reports.
Results are written to `results/phase6-comparison.json` and `results/phase6-history.json`.
They measure authored regression fixtures only, not clinical accuracy. See TESTING.md for exact results.
