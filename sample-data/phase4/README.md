# SYNTHETIC DEMO DATA — NOT A REAL PATIENT

All files are artificial and contain no real patient information. The numbers and reference ranges
are test fixtures, not clinical advice or validated medical reference data. January/April/September
are extracted independently. September has page-2 evidence and a deliberately unreadable TSH value
`l.8?`. Its original string must be preserved and numeric value absent. No cross-report interpretation.
The image and scanned PDF require actual OCR. Prompt-injection text is untrusted source content.
`malformed.pdf` intentionally fails validation; it is not a usable report.

Regenerate: `python3 scripts/generate-extraction-fixtures.py` (reportlab, Pillow, DejaVu Sans Mono).
Ground truth is authored independently of extraction output in evaluation/ground-truth.json.
