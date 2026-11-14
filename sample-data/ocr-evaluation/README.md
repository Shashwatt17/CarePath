# Synthetic OCR evaluation v1

This is a small, authored regression corpus, not a clinical validation dataset. It contains no patient reports or identities. Six development and six frozen validation cases exercise 18 expected readings. Do not tune against validation cases and continue describing them as unseen; version the corpus when its contract changes.

## Run locally

Requires Java 21, Maven, Python 3 and local Tesseract with English data. Set JAVA_HOME for Maven as well as Java.

```sh
python scripts/evaluate-ocr.py
python scripts/ocr_regression.py docs/verification/ocr-evaluation-v1.json backend/target/ocr-evaluation/report.json
python -m unittest discover -s scripts -p test_ocr_regression.py
```

The runner supports `--compiled`, `--classpath-file`, `--java`, `--maven`, `--output` and `--tesseract`. Reuse compiled classes/dependencies only when current. Raster inputs are generated locally; temporary images are deleted. No credentials, patient uploads, paid services or external OCR calls are used. Test code is not part of the application runtime.

## Measurement contract

Rows match one-to-one by exact source name, preferring matching date. Numeric proximity and reference-range fit never select matches. Extra/duplicate rows cannot inflate recall. Values, units and dates are counted separately, with all expected rows as the denominator. Exact units are lexical: `mg/dl` differs from `mg/dL`; this is not normalized-unit accuracy. Name spacing differences count as a missed expected row plus an extra row. Numeric abstentions count matched rows only; a preserved colon can be lexically correct while remaining unsafe to interpret as a decimal.

Text fixtures enter the parser directly, not PDF decoding. Raster cases enter the image extractor. Wrapped reference content is exercised, but this first metric version does not score reference-text correctness. Existing dedicated reference/provenance tests remain necessary. It does not cover cropped pages, glare, multilingual/handwritten reports, scans embedded in PDFs, clinical interpretations, all report types, or concurrent hosted memory use.

The baseline records runtime/manifest metadata. Logical Java fonts may resolve differently across machines, and Tesseract versions can change results. Investigate environment differences and review baseline updates explicitly; never overwrite the baseline merely to make checks pass. The comparator checks per-case regressions, not just aggregate totals; an unchanged baseline can still contain known failures.

## Baseline — 14 November 2026

| Split | Expected rows | Name-matched rows | Exact values | Exact raw units | Exact row dates | Missing | Extra |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Development | 9 | 9 | 9 | 6 | 9 | 0 | 0 |
| Frozen validation | 9 | 5 | 5 | 2 | 5 | 4 | 1 |
| Total | 18 | 14 | 14 | 8 | 14 | 4 | 1 |

Document date: 11/12; provider: 12/12. Of the four strict row misses, one is a name-spacing mismatch; the other three are absent readings in missing-unit, small-text and rotated cases. Several units became `mag/dL`; one case differs only in letter case. These are observed limitations, not accepted production accuracy targets.

## Bounded retry experiment

Test-only crop retry: at most two ambiguous rows, 5 seconds per process, 8 seconds document budget, 1 MP crop, one OCR thread, 128 MB Java heap for the evaluation runner. Retry output never changes an original or verifies a reading. The one eligible raster row returned the same colon-containing value (`UNCHANGED_UNVERIFIED`). This does not establish decimal-recovery benefit; runtime retry rollout remains blocked pending representative evidence and hosted resource checks.

## Next work

1. Preserve omitted unitless rows as review candidates without admitting administrative numbers.
2. Separate lexical unit OCR errors from valid unit spelling/case variants; never silently alter source text.
3. Add orientation recovery with bounded work and explicit provenance, then evaluate small text and cropping.
4. Expand independent, consented/de-identified layouts and field-level reference scoring before broader accuracy claims.
