# OCR reading-axis correction (v5)

Tesseract can recognize sideways words while returning their original vertical coordinates. CarePath previously grouped these as horizontal rows, mixing labels and values. The reader now infers reading axes from word order in at least three OCR lines, requiring 80% agreement, groups words in upright coordinates, and maps evidence boxes back onto the original uploaded page. No extra OCR call, raster copy, external service, or increased memory limit is introduced.

This is geometric reconstruction, not correction of recognized numbers. Sparse or mixed-direction pages do not trigger reorientation. OCR review requirements remain intact. Existing extraction snapshots are unchanged; explicit reprocessing after deployment creates v5 results. The frontend outdated-extraction notice now recognizes v5.

## Verification

- 81 targeted backend tests passed: extraction rules, table layout, empty cells, runtime limits, review integration, and seven new orientation cases.
- Synthetic TSV tests cover 90/180/270-degree reading order, numeric extraction, original-page source boxes, provenance validation, sparse/mixed-direction abstention, and invalid coordinates.
- The unchanged 12-case synthetic corpus ran through local Tesseract 5.5.0 and the production extraction path with a 128 MB Java heap. Its rotated-image case now matches the expected value, raw unit, and date.
- Against v4: exact values 15/18 → 16/18; exact raw units 9/18 → 10/18 (includes correctly missing units); missing strict row matches 3 → 2; extra rows remain 1. No per-case measured regression. Full results: `docs/verification/ocr-evaluation-v5.json`.

## Limits

This small, already inspected synthetic corpus is a regression suite, not independent clinical validation. Actual image OCR was tested at the corpus's 90-degree rotation; 180/270-degree geometry was tested with synthetic TSV, not a claim about Tesseract recognizing every upside-down image. Small text and punctuation remain known failure cases, and unit recognition is not perfect. No automatic clinical interpretation is added. Live deployment and authenticated upload/reprocessing have not been verified for this release.
