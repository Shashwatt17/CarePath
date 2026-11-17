# Hybrid report reading: controlled second-reading implementation

## What is implemented

The v5 embedded-PDF/Tesseract reader remains the first pass. `VisionRouting` offers an optional second reading for scanned pages, low OCR confidence, uncertain candidates, missing supported rows, or historical-column warnings. The initial route is conservative and has not been calibrated against clinical documents. It does not assert that embedded PDF extraction is always correct.

An owner explicitly selects a page and consents before transmission. `OpenAiVisionProvider` sends that page image to the fixed OpenAI Chat Completions HTTPS endpoint with strict JSON schema, `store:false`, no tools, no redirects, no automatic retry, a 20-second request deadline, a 6 MB image cap, a 128 KB response cap and a 6,000-token output cap. The model is operator-configured; no model is declared best without measurement. The model sees the image, not a proposed OCR answer, reducing anchoring to the original OCR error.

Output contains separate test/value/unit/reference/date/column entries, plus optional model-estimated original-image coordinates. Historical columns must remain distinct. The validator rejects unexpected fields/types, invalid boxes, trailing JSON, oversized output and more than 50 readings. It flags unsupported units, invalid dates, ambiguous numbers, duplicate columns and lack of corroboration within overlapping OCR lines. Neither agreement nor reference-range plausibility verifies a measurement. Abnormal values are never altered to fit reference ranges. No clinical plausibility threshold is invented.

Proposals are stored separately in `vision_page_proposal`, bound to owner, document, extraction revision and page. Original candidates, source transcripts, review decisions and patient history are never overwritten. The UI shows all proposals beside the original page; existing candidates can be corrected through the established source-reviewed correction flow. A model proposal for a previously missing row is **not yet importable as a new verified observation**. Model-estimated boxes are retained as unverified suggestions, not certified evidence crops.

Each page/revision has one attempt, including failures. Reloads use persisted results. Limits: one active call per application instance, three attempts per document, ten per owner per rolling 24 hours and 100 overall per rolling 24 hours. Daily counts use retained audit events so deleting a document does not reset them. Daily limit admission is designed for the current single-instance deployment; distributed atomic budgets are required before multiple replicas. Provider-side spend limits should also be configured. Requests interrupted by server failure display FAILED after 120 seconds and are not automatically resubmitted.

## Enablement — not yet performed

1. Select a vision/strict-schema-compatible OpenAI model using the evaluation runner below. Configure a separately scoped server secret `VISION_API_KEY` and `VISION_MODEL` through the hosting dashboard, never in frontend code or chat.
2. Review the actual provider account's data handling and retention. `store:false` is not a zero-retention guarantee. No-training and zero-retention are different properties. Set `VISION_PRIVACY_REVIEWED=true` only after that review; this flag is an operator assertion, not a legal compliance certification.
3. Apply Flyway V16 and deploy backend/frontend with `VISION_ENABLED=false`; verify owner-scoped endpoints and migrations first.
4. Set `VISION_ENABLED=true` for a controlled synthetic trial, with a provider spend cap. Each user must still consent to the selected page. This feature may incur API charges; the existing deterministic reader remains available without them.
5. Run a hosted synthetic upload → first extraction → second reading → source correction → profile check. Measure latency, memory and API spend. Keep fallback disabled for wider use until results are reviewed.

The adapter follows OpenAI's image-input and strict-schema documentation:
- https://developers.openai.com/api/docs/guides/images-vision?api-mode=chat
- https://developers.openai.com/api/docs/guides/structured-outputs?api-mode=chat
- https://developers.openai.com/api/docs/guides/your-data

## Evaluation

`com.carepath.intelligence.VisionEvaluation` is a test-class CLI using the existing synthetic corpus, with exactly the production provider and validator. Compile test classes, then run with the Maven test dependency classpath:

```
java -Xmx128m -XX:+UseSerialGC -Djava.awt.headless=true -cp <test-classpath> \
  com.carepath.intelligence.VisionEvaluation \
  sample-data/ocr-evaluation/manifest.json backend/target/vision-evaluation
```

Default mode only renders synthetic images and scores any existing `*.response.json` files. It makes **zero** external calls. Add `--send-synthetic` and configure `VISION_API_KEY` / `VISION_MODEL` to opt into up to eight paid synthetic calls. Responses are cached in the output directory; use separate directories per model/run. Do not replace this synthetic manifest with patient data. Model identity must accompany replay artifacts; empty model on a replay means identity was not supplied and must not be used for model comparison.

The report scores exact names/values/units/dates, missing rows and extra rows without numerical proximity matching. It does not score reference ranges, evidence-box correctness or clinical coverage. The known corpus is a regression suite, not an independent holdout. Keep the v5 deterministic baseline unchanged.

Before claiming broad reliability, collect 50–100 consented/de-identified reports covering labs, native/scanned PDFs, phone photos, multiple result dates, unit spellings, small text, blur, glare and rotations. Have expected readings independently checked and freeze a holdout before tuning. Separately label character errors, row/column association errors, missing/extra rows, dates, units, references and evidence location. Compare providers using the same cases and record abstention/review rates, latency and cost. Synthetic variants cannot substitute for this collection.

## Verified in this change

- 66 targeted backend tests passed: 19 new vision validator/integration cases, 14 existing review cases, 26 extraction rules, seven orientation cases.
- 61 existing frontend tests passed; TypeScript, lint and production build passed.
- PGlite applied all 16 migrations and checked vision ownership/extraction/uniqueness constraints. This is not hosted Flyway verification.
- Dry-run evaluation rendered seven synthetic image cases with zero provider calls. No model-accuracy improvement is claimed.

## Remaining limits

No live provider response, hosted rollout, rendered-browser interaction or 512 MB hosted memory check was performed. A first extraction must exist; documents that fail before producing a result (including NO_TEXT) cannot currently use this path. The prompt/schema targets laboratory readings; prescriptions and narrative summaries need separate schemas and evaluations. Blur/cropping detection, model-region calibration, verified import of newly discovered rows, and the larger independent evaluation set remain open. No patient document was transmitted in development.
