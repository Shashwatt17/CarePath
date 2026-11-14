# Laboratory layout reliability
Implemented v3 administrative-row filtering, expanded metadata labels and secondary-column facility detection, ordered multiline reference preservation with validated source spans, and explicit ambiguous-numeric-punctuation warnings. Reference categories remain unverified text; no clinical interpretation or guessed numeric repair is added. Review UI preserves line breaks and identifies old reader versions.

Verification: 78 targeted extraction/runtime/processing/review tests passed. After tightening implicit facility headings to avoid lowercase marketing text, all 33 affected metadata/extraction rule tests passed and the package built. All 59 frontend tests, lint and production build passed. A new synthetic raster fixture produced exactly three measurements, correct date/provider and preserved reference continuations locally. The supplied private report, checked locally only, now yields nine actual measurement candidates (three administrative false positives removed), with report date and provider captured; one numeric punctuation error remains explicitly unverified. No source image or private OCR output is committed.

The production rollout and hosted v3 image check are pending at this checkpoint. Follow docs/RELIABILITY_PLAN.md for remaining work; region-specific OCR retries, calibrated quality checks, broader terminology/reference semantics, multipage integrity and additional document-type extraction are not claimed complete.


### Hosted v3 verification — 14 November 2026

Release 34c5405 passed the hosted synthetic-image check through Netlify, Render and the database: exactly three measurement rows, administrative rows excluded, report date/provider recovered, multiline reference categories preserved, missing-page warning present, OCR one-click confirmation disabled and byte-identical original download. Browser verification confirmed metadata and multiline references render correctly. See docs/verification/hosted-layout-latest.json and layout-live-review.png.

Initial login returned 504/503 around a cold start. Backend health/readiness and direct login subsequently passed, followed by frontend API login and the complete image test. Cold-start handling remains a usability limitation; no always-on or load guarantee is claimed. No real patient report was uploaded during this verification. Release A is verified within this scope; later work in RELIABILITY_PLAN.md remains pending.
