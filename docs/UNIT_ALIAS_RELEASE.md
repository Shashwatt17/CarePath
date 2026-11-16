# Explicit litre-unit aliases

This change distinguishes valid unit spelling from OCR repair. Migration V15 adds 16 concept-specific rules for the lowercase litre symbol in existing supported `mg/dL`, `g/dL`, `mmol/L`, `ng/mL` and `pg/mL` conversions. Prefix case is preserved; no general case folding or fuzzy matching is introduced.

Each additive rule has its own ID and `carepath-units-v2` provenance. Existing v1 rules, source readings and persisted normalization snapshots are unchanged. The terminology snapshot loads both supported versions and still rejects ambiguous rule matches. The existing conversion factors and target units are reused, not recalculated.

This does not repair OCR strings such as `mag/dL`, `mgid)` or `m9/dl`, and does not treat `Mg/dL` as `mg/dL`. Missing units remain missing. OCR source-review protections remain in place. Extraction benchmark metrics are unchanged because this change affects normalization, not the raw extracted text.

Verification and hosted rollout status are recorded below. Production requires migration V15 and a backend restart/deployment; no existing normalization snapshots are rewritten.

## Verified checkpoint — 16 November 2026
45 targeted tests passed: 31 normalization tests (including all 16 new alias rules and five damaged/prefix-case rejection cases) and 14 review integration tests. H2 seed parity was updated because these tests disable Flyway. Production Flyway/PostgreSQL rollout is not verified. Original text, rule provenance and OCR review safeguards are retained.
