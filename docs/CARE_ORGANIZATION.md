# Phase 8 care organization

This layer organizes user-reported symptoms and scheduling. It makes no diagnosis, treatment or
booking claim. It does not add Visit Packs, sharing or nearby-care integrations.

## Domain and trust

`care/` is a transactional JDBC module following the existing assistant/history architecture.
CareController validates typed DTOs; services derive the owner from the validated security context.
Symptom, Appointment, FollowUp, Reminder and Notification use the existing V1 tables, evolved by V8.
Dates, versions, foreign keys and owner-constrained relationships are enforced on writes. No client
owner, delivered state, audit actor or evidence page is accepted as an editable property.

Symptoms have a 1–10 self-reported severity, ONCE/OCCASIONAL/DAILY/CONSTANT frequency, start instant,
optional resolved instant, notes and optimistic version. Resolution is reversible by clearing the
resolved instant through a versioned edit. Future start/resolution and resolution before start are
rejected. Timeline events label symptoms as USER-REPORTED, separate from verified measurements.
The laboratory histories, comparison engine and assistant retrieval do not read symptoms. Phase 8
intentionally does not add symptom-context dispatch to an external model.

Appointments have SCHEDULED → COMPLETED or CANCELLED transitions. The same terminal action is
idempotent; a conflicting transition or stale scheduled edit returns 409. Completed/cancelled entries
cannot be reopened. Past appointments may be recorded, including as SCHEDULED; the PAST list sorts
newest first. UPCOMING contains only future SCHEDULED entries, sorted by actual instant. Optional
links to documents, symptoms and saved questions must all belong to the authenticated owner.
The optional follow-up date is metadata; it does not automatically create another appointment.
External URLs are user-entered HTTPS links, not verified availability or provider booking integration.

## Follow-up detection and evidence

Detection runs in the extraction publication transaction, or from the document viewer's explicit
Check follow-ups action for an already processed document. It reads at most 200 existing pages and
creates at most 100 instructions per invocation. Exact, bounded English lines are recognized:

- Review after 6 weeks
- Follow up in 3 months
- Return after 14 days
- Next visit in 2 weeks
- Review on 2026-10-13 (or 13 October 2026)

Case-insensitive variants and a trailing period/exclamation are accepted. Compound instructions,
ambiguous durations and medication actions are ignored. Durations use LocalDate calendar arithmetic,
including month-end clipping; the anchor is the document's recorded date, never upload time.
Missing anchor or invalid explicit date remains null/LOW. OCR-derived instructions remain LOW even
when the pattern matches. HIGH describes parsing evidence, not medical certainty.

A pending FollowUp row is the candidate; no separate appointment exists. The row retains document,
page, exact instruction, anchor, duration and machine suggestion. Explicit CONFIRM requires the user
to select date, time, timezone and reminder offsets. A changed/missing suggestion produces EDITED;
the original suggestion is retained. IGNORE persists the decision. SHA-256(page number + instruction)
uniqueness per document prevents identical ignored instructions being rediscovered. Document locks
serialize detection; follow-up locks/version checks serialize decisions. Reconfirmation returns 409.
The current UI does not edit an already confirmed follow-up; that lifecycle is a future enhancement.

## Timezone policy

Appointment/follow-up requests contain an ISO offset timestamp and IANA timezone. The server verifies
the offset against the zone at that local time (rejecting DST gaps and mismatches), then persists a
UTC instant plus zone. Jackson must preserve the submitted offset until validation. Scheduling uses
Instant and injected Clock, never server-local time. The UI uses the device timezone for date/time
entry, explicitly labels it, and renders appointments in the stored zone. Editing in another device
zone preserves the instant until changed; it submits the current device zone. There is no custom
zone picker yet. An ambiguous DST overlap uses the browser-selected valid offset.
Symptom timeline day grouping uses UTC; symptom forms/detail use device-local time.

## Persistent reminders and delivery

Offsets are minutes before the source: 0, 60, 1440, 2880 or 10080. Empty selection disables reminders.
Rows are created in the same transaction as an appointment or explicit follow-up confirmation.
A source/offset unique index prevents duplicates. The poller reads at most 100 due PENDING IDs,
then delivers each in its own transaction, locking the source before the reminder consistently.
It rechecks due time and source state, creates a generic notification, marks DELIVERED and audits.
The notification's unique reminder link and shared locks prevent concurrent duplicate delivery.
Failures roll back and remain eligible for the next poll; there is no external email side effect.

REMINDERS_ENABLED defaults true; REMINDER_POLL_MS defaults 30000. Polling is disabled in integration
test configuration; tests invoke the production poller/service with a controllable Clock. Persisted
rows survive conceptually across restarts, but native PostgreSQL restart verification is still blocked.
Overdue offsets (including a newly recorded past appointment) become due at the next poll. Reminders
are a convenience, not a guaranteed clinical alerting service. No push/browser permission is required.

Changing appointment time/offsets transactionally replaces its reminders and removes their old
notifications through cascade. A metadata-only edit with identical scheduling keeps them, avoiding
redelivery. Cancelling/completing cancels pending reminders and retains delivered notifications.
Deleting an appointment removes its reminders/notifications. Deleting a source document removes
its follow-ups, reminders and notifications; deleting an extraction page also removes linked
follow-ups. Appointment links to deleted documents/symptoms/questions are detached by cascade.
Audit events retain only resource identifiers/actions, not notes. This is a current-state inbox,
not an immutable notification history. There is no native concurrent-load benchmark yet.

Notifications are persistent, generic and private. Listing, unread count, mark-read and mark-all-read
are owner scoped; repeated marking is harmless. The indicator updates on navigation/mount, focus and
read events; it is not a live push channel. EmailNotificationProvider is an extension port only.
No adapter/credential is configured, no email is sent, and the UI says email is not configured.

## Limits and verification

Care mutation bodies are bounded to 8 KiB; notes to 2000 characters, linked resources to 20 per type,
reminder offsets to five and lists to 20 per page. SQL filtering/paging runs server-side. Appointment
list mapping performs bounded per-item link queries; native query-plan/load optimization is pending.
React renders user text without raw HTML. HTTPS links do not trigger backend fetches.

See TESTING.md and PROJECT_STATUS.md for executed test counts, commands and open native/browser gates.
H2 security-stack tests and PGlite SQL checks are explicitly not native PostgreSQL verification.

## Synthetic manual acceptance flow (pending rendered browser)

1. Register/login. Add, edit, resolve and reopen a labelled synthetic symptom; inspect its timeline event.
2. Create an appointment, attach own report/symptom/question, select offsets, edit time, then cancel.
3. Upload/process a synthetic PDF containing a documented follow-up line with document date set.
4. Open Follow-ups, inspect exact source page/suggestion, edit the chosen date/time and confirm.
5. Schedule a synthetic item due now, wait for the server poll, inspect unread notification and source.
6. Mark read/all read, reload, then verify another account cannot access any of these resources.
7. Recheck lab changes and assistant facts; symptoms must not become laboratory evidence.
8. Repeat after real PostgreSQL/backend restart and on a mobile viewport when infrastructure permits.
