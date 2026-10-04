# Temporary clinician sharing — Phase 10

## Capability and scope

Only an owned GENERATED Visit Pack can be shared. Creation uses Java SecureRandom to generate
32 random bytes (256 bits), encoded as 43 canonical Base64URL characters. The database stores
only the SHA-256 digest. Random high-entropy capabilities do not need password-style hashing.
The raw value is returned once; list/revoke responses cannot recover it. The application does not
persist the raw token in browser storage, cookies, SQL, audit, or generated server files.

The owner chooses 15, 30, 60 or 1,440 minutes. The capability is bound to one owner, pack ID and
revision. Anonymous reads project only that frozen snapshot's title, visit context, selected item
fields and historical citation text. No internal owner/document/observation IDs, source links,
original downloads, account navigation, PDF endpoint or write operation are provided publicly.
Holding a share token never authenticates an owner API. No LLM or external QR/PDF service is used.

## Transport, UI and QR

Links are constructed as `<frontend-origin>/share#<capability>`. URL fragments are not sent in
HTTP requests or referrers. The anonymous page removes the fragment from the address bar after
reading it and retains the token in memory. It POSTs JSON to a fixed `/api/v1/public/share/access`
path using no credentials, no-store and no-referrer. Reloading the cleaned URL requires reopening
the original link. Do not put tokens in path/query parameters. There is no sitemap entry or analytics.

The owner UI shows the link once and renders a QR PNG locally using the pinned `qrcode` package.
The QR payload is exactly the temporary URL. Copy/link/QR availability does not change server
expiry. `/shares` lists active/recent shares with server-derived status and immediate revoke controls.
The recipient view is plain escaped React text, with no edit controls or outgoing evidence links.

API responses use no-store, no-referrer, noindex/nofollow/noarchive, nosniff, DENY and the existing
restrictive API CSP. The Next share shell also has no-store/noindex/no-referrer and a dedicated CSP
restricting external connections, objects, forms and framing. Its script/style policy permits inline
Next bootstrap code; nonce-based frontend CSP remains a production-hardening gap.

## Expiry, revocation and concurrency

Status is derived server-side: REVOKED takes precedence, otherwise EXPIRED at `now >= expiresAt`,
otherwise ACTIVE. Every access validates the digest, token grammar, expiry, revocation, pack status
and revision. The injected Clock is rechecked after snapshot assembly. There is no authorization cache.

Create/access/revoke lock the pack before the share row, matching deletion's parent lock order.
Revoke is idempotent and emits one event. Access and revocation serialize: an already-authorized
request can finish before revocation; requests authorized after revocation commits are denied.
The service uses the existing transactional JDBC boundary. Native PostgreSQL locking remains an
unverified deployment gate; concurrency assertions ran against the real services with H2.

Already disclosed information cannot be recalled. The recipient page polls every 15 seconds,
clears data on failed revalidation/expiry and while hidden, and revalidates on returning to view.
Requests have a 10-second client deadline. Timer throttling/network delays can affect clearing of
already-delivered content, but never authorize another server read. Screenshots, copied text,
clipboard history and browser extensions are outside revocation control.

## Snapshot and deletion semantics

Live source edits/deletions do not alter an existing generated pack, consistent with Phase 9.
Historical filenames/pages/snippets remain snapshot content, clearly labelled as such. There is
no share-scoped original-evidence endpoint, so deleted or unrelated source files cannot be
resurrected through a share. To remove the retained copy, delete the pack. Pack deletion cascades
its shares/snapshot and subsequent capability access fails. Other pack revisions remain separate.

## Abuse controls, audit and logging

Public lookup is limited to 60 requests per configured AUTH_RATE_WINDOW_SECONDS (default 60)
per server-observed IP using the existing Redis/HMAC rate-limit port with a separate `share`
namespace. Redis errors fail closed. The Next proxy may aggregate client IPs; arbitrary forwarded
headers are deliberately not trusted. Configure trusted-edge limits before public deployment.
Request bodies retain the existing 8 KiB actual-body bound; DTOs reject forged fields. Snapshot
size is bounded by the Phase 9 assembly limits and management lists paginate by 20.

SHARE_CREATED, SHARE_ACCESSED and SHARE_REVOKED contain only safe entity/correlation metadata.
Public access uses audit actor SHARE, never impersonates the owner. Access count/last access and
version are persisted. No token, full URL, medical content or raw request/response is logged by
this module. Existing SQL value logging is suppressed. Operators must keep proxy/APM/body/response
capture disabled or redacted; the fragment design cannot protect a separately enabled body logger.
There is no expired-share/audit retention job or global tenant quota in this phase.

## Schema and verification boundary

V10 adds the pack/owner/revision unique key, composite share revision FK with cascade deletion,
and pack-owner lookup index. Existing V1 share_token digest/expiry/owner constraints remain.
V1–V9 are immutable. H2 tests use a compatible schema; PGlite executes actual V1–V10 SQL only.
Neither establishes native PostgreSQL/Flyway operation. Full counts/commands: TESTING.md and
PROJECT_STATUS.md. Native PostgreSQL/Flyway, Redis, restart persistence, rendered browser/mobile,
live LLM, production cookie/CORS/CSRF/TLS and email gates remain open.
