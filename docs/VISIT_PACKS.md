# Doctor Visit Packs — Phase 9

A private, user-selected preparation document, not a new source of medical facts. No LLM,
external PDF service, sharing token or public download is involved.

## Assembly and lifecycle

`PackAssembler` resolves only explicitly selected owner-scoped sources. `PackService` manages
DRAFT → GENERATED; `PackRepository` stores typed selections and relational snapshots.
`PackPdf` renders the same `Content` representation used by the web preview. The frontend
provides visit details, source pickers, ordered selections, preview and generation.

Supported selections: user-reported symptoms, verified observations, two-observation deterministic
changes, documents, appointments, confirmed/edited follow-ups, saved questions and pack-only
manual questions. Optional appointment context is selected deliberately. Symptom/appointment
notes are opt-in. A saved question can be overridden for the pack without editing its source.
Document selection is a reference, not an assertion that every document statement is verified.
Original files are not embedded. Pending/rejected candidates cannot be selected as observations.

The existing Phase 6 comparison service supplies changes, range transitions, units, deltas and
insufficient-evidence explanations. Phase 9 does not invent new calculations, diagnoses or treatment.
Each observation/change includes the exact source document, page and evidence; both sides of a
change are retained. Sources deleted after generation may no longer be navigable, but the historical
snapshot remains readable. Filenames and evidence are plain text, never HTML or external links.

Draft edits use an optimistic version. Preview returns a SHA-256 fingerprint of canonical content;
generation requires that fingerprint and version, locks the pack, reassembles in a REPEATABLE_READ
transaction and rejects changed sources. Renderability is checked before the snapshot commits.
Concurrent generation yields one success and a safe conflict; it is not automatically replayed.
GENERATED is immutable through application APIs. Explicit revision creates a new draft and leaves
the older pack intact; repeating the same revision action returns its existing child.

Generated content is independent of mutable source entities. Source deletion clears selection
references, not snapshot content. Deleting a pack cascades its own selections and snapshots only.
**Delete generated packs separately to remove their retained copies of medical information.**
Downloaded copies remain under the user's control. Database operators are outside API immutability.

## Persistence and PDF

Additive V9 evolves the existing V1 pack tables. Typed item, field and evidence snapshot tables
replace any need for an uncontrolled JSON content blob. Composite owner foreign keys protect
live source relationships. Historical snapshot evidence IDs deliberately have no live source FK;
otherwise source deletion would rewrite the prepared record. Snapshot retrieval batches item,
field and evidence queries. Draft assembly performs bounded per-selected-source lookups.

PDFs are generated locally on demand from the immutable snapshot; no stored binary or orphan
cleanup is needed. Content is reproducible, not necessarily byte-identical PDF serialization.
The renderer version is recorded. A4 pages use embedded DejaVu Sans fonts (license in resources),
plain text, wrapped paragraphs, section hierarchy, and page numbers. Minimal PDF metadata contains
no patient name, source identifiers, token or path. Unsupported glyphs/control characters render
as explicit `[U+xxxx]` escapes rather than silently disappearing; this is not full-script support.
PDFs are not tagged-accessibility certified.

## Bounds and authorization

Every API derives identity from the validated security context. Foreign pack/source IDs return
safe failures; generated evidence links still go through protected source APIs. Download uses a
constant attachment filename, application/pdf, Content-Length, no-store and nosniff.

Limits: 40 selected items, existing 8 KiB JSON request bound, title 160, reason 2,000, question
1,000 characters; assembled content 80,000 characters and individual fields 4,000. Rendering has
2 concurrent slots, a cooperative 8-second deadline, 40 pages and 5 MiB output maximum. This is
not a hard OS-isolated PDF worker timeout. No arbitrary paths, markup, remote fonts or executable
PDF actions are accepted. Audits contain event/entity metadata, not pack contents.

## Known limits and verification boundaries

- Native PostgreSQL/Flyway V1–V9 and restart persistence remain blocked. H2 API tests and PGlite
  SQL checks do not prove native operation. H2's source-deletion FKs are single-column because
  it does not implement PostgreSQL's composite `SET NULL (column)` syntax; production uses owner FKs.
- Rendered desktop/mobile interaction remains unverified. Client tests and Next builds passed.
- Revision creation fails safely if a selected live source was deleted or became unavailable;
  create a fresh draft and select surviving sources. The generated historical pack remains usable.
- No date-range bulk selector, drag reorder, whole-history auto-selection or sharing. Picker paging
  and move-up/down controls support deliberate selection. Follow-up paging can have empty filtered
  pages. Concept chooser is bounded by the existing concept API.
- No native load benchmark; live draft reads are bounded, not a fully batched history export.
- Three-page synthetic output and four-page stress output were rendered and visually inspected;
  no visible clipping/overlap. This is fixture coverage, not a claim about every possible input.
