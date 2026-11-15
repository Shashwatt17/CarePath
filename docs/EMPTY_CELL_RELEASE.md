# Empty laboratory cells — reader v4

The v3 parser used a repeated-whitespace separator for tab-delimited rows. Consecutive tabs representing an empty cell were collapsed, shifting later cells left. A missing unit followed by a numeric reference range could therefore be discarded as an invalid row.

Reader v4 splits explicit tab and pipe delimiters independently, retaining internal empty cells. Repeated spaces remain a fallback only when no explicit delimiter exists. Numeric values, units, reference text and source evidence are never repaired or inferred. Unknown/missing units retain review warnings; a missing value stays null. Administrative exclusions and the minimum structured-column requirement remain in place.

The change is versioned so prior extractions and review decisions remain immutable. Existing results are not retroactively rewritten. Upload a new copy to use v4. The UI only changes its earlier-reader notice; the completed report redesign is unchanged.

## Measured result

The unchanged 12-case synthetic corpus now yields 15/18 exact values, 9/18 exact raw units (including correctly absent units), 3 strict missing matches and 1 extra. The previously omitted unitless case is recovered. The per-case regression comparator passes against v1. All other case metrics remain unchanged. Raw unit equality is not clinical normalization accuracy.

The corpus is a small authored regression set, and its previously frozen cases have now informed development. It must not be represented as independent held-out clinical validation. The Java runtime was restored after workspace maintenance (21.0.13); see the result artifact for environment metadata.

## Remaining scope

This fixes internal empty cells with explicit delimiters. It does not infer a missing unit column from arbitrary prose or indistinguishable spaces. Rotated images, small text, incorrect OCR units and decimal recognition still need separate work. No automatic OCR retry or correction is enabled.

## Verification — 16 November 2026
74 targeted backend tests passed: extraction rules (26), layout (7), empty-cell cases (5), table extraction (10), isolated extraction runtime (12), and review integration (14). All 61 frontend tests passed. Local compilation succeeded under Java 21.0.13 and Maven 3.9.11. Synthetic evaluation and the per-case regression guard passed. Hosted v4 rollout is not yet verified.
