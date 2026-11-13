# OCR table repair — 13 November 2026

The v2 extraction pipeline preserves OCR column geometry, separates explicitly dated numeric cells, and requires source review for uncertain readings. Raster photos receive bounded grayscale, contrast and scaling preparation; stored originals remain unchanged. Names, decimals, units and ambiguous years are never repaired by guessing.

The review screen shows per-reading dates, image readability guidance, possible missing-page warnings, and multiple-date-column guidance. Technical reasons are collapsed. Earlier extractions remain immutable; upload a new copy to run v2 without overwriting prior review decisions.

Verification: a complete backend run passed 336 tests before the final row-grouping refinement. After that refinement, all 70 targeted extraction/runtime/processing/review tests passed, including nine table regression tests, and the executable package built. All 59 frontend tests, lint and production build passed. The final native workflow passed all 13 checks with PostgreSQL, Redis, the production frontend proxy, text/scanned PDFs, registration/login, review/history, patient summary/PDF, selected-original sharing/revocation and restart persistence.

Only independently authored synthetic fixtures are included in source/tests. The supplied private photo was evaluated locally and remains outside this repository. Extraction improves but is not fully accurate on that photo: some names, units and a historical numeric reading remain wrong. Readability warnings are OCR confidence signals, not a definitive blur or wrong-document detector. No clinical accuracy, complete document identification, or automated verification is claimed.

Hosted rollout and synthetic image verification are pending at this commit.


### Free-instance memory correction

The first hosted synthetic image check on cdbc151 exhausted Render's 512 MB allowance. That live check did not pass. Raster decode now subsamples to at most 4 MP before preparation, preparation uses single-channel grayscale at no more than 6 MP / 2x, and the isolated worker uses a 128 MB heap with Serial GC. The container uses a 192 MB heap with Serial GC to leave room for native OCR; the native verification launcher uses those same flags.

The memory revision passed 71 targeted processing/review/extraction tests; after the final 6 MP adjustment, all 22 table/runtime tests passed and packaging succeeded. All 13 native PostgreSQL/Redis/frontend workflow checks passed again with the smaller server heap. The private photo still produces incorrect names/units and some missing decimal points, so the earlier partial transcription improvement must not be presented as a complete fix. Hosted verification of this correction is pending.
