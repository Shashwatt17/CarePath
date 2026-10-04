# Phase 2 authentication implementation

## Token and browser design

The Next.js application proxies `/api/v1/*` to the configured Spring backend. Browser requests
remain same-origin; backend URLs and signing keys never become public environment variables.
Login returns a short-lived RS256 access JWT to JavaScript memory and sets an opaque 256-bit
refresh token in an HttpOnly cookie. No localStorage/sessionStorage token storage is used.
Only the SHA-256 digest of each refresh token is stored. No raw access token is persisted.

Production (`AUTH_COOKIE_SECURE=true`, the backend default) uses `__Host-carepath_refresh`,
Secure, HttpOnly, SameSite=Lax, Path=/ and no Domain. The __Host prefix prevents sibling-domain
cookie injection. Only authentication endpoints consume this cookie. HTTP loopback development
explicitly uses `AUTH_COOKIE_SECURE=false` and a non-prefixed cookie scoped to `/api/v1/auth`.
Non-loopback frontend origins require HTTPS and Secure cookies at startup.

A page reload restores access with POST refresh. All client auth mutations are serialized;
refresh is single-flight per tab. Web Locks coordinate across tabs where supported. BroadcastChannel
communicates only sign-in/sign-out events, never tokens. Browsers without Web Locks may race and
be signed out by replay protection; security is not relaxed to hide that race. No retry automatically
replays a refresh after an ambiguous network failure. Sign in again if a consumed token's response
was lost. Logout retains client state on server failure instead of falsely claiming revocation.

Protected routes render a loading state until restoration resolves. UI guards are not authorization:
Spring independently validates every protected request. `/app` currently contains only the account
shell, not a clinical dashboard. GET `/api/v1/auth/me` proves actual authenticated API access.

## JWT and sessions

RSA keypair is loaded from configured PEM files (PKCS8 private/X509 public), validated for matching
modulus and at least 2048 bits. There are no embedded signing secrets or runtime random-key fallbacks.
Generate local keys using `python3 scripts/generate-auth-keys.py` (OpenSSL required). Deployments
must use securely provisioned keys; key rotation/key-ring distribution is an operational follow-up.

The decoder accepts RS256 only and requires subject/session UUID, exact issuer, expected audience,
issued-at, not-before, expiry and a bounded configured lifetime. Expired tokens are rejected without
an expiry grace period. A database check verifies the session and user are still active on every
bearer request. Database unavailability fails closed. This is stateless HTTP authentication (no
HttpSession); immediate revocation intentionally uses persisted session state.

Every login creates one absolute-lifetime session and refresh-token family. Refresh locks the
session row, re-reads the token, consumes it and inserts one replacement inside the same transaction.
V2 enforces one child per consumed token and owner-consistent session linkage. Replay revokes the
whole family, including the latest token. The denied outcome is returned from the transaction so
revocation and audit actually commit. Logout revokes the family and immediately invalidates its JWTs.
Disabled accounts cannot log in, refresh or use an existing access token. Previous tokens are retained
for replay detection; pruning/retention policy must preserve detection until session expiry.

## Passwords, requests and errors

BCrypt defaults to cost 12 (configurable 10–14); integration tests use cost 10, never plaintext hashing.
Registration requires 12–72 characters and at most 72 UTF-8 bytes, preventing BCrypt truncation.
Login enforces the byte bound too. Emails are normalized with Locale.ROOT and protected by a unique
constraint. Duplicate registration returns a generic 409; this necessary product flow still provides
some account-existence information. Unknown, wrong-password and disabled login share one message.
A dummy BCrypt comparison reduces the obvious nonexistent-user timing shortcut.

DTO Bean Validation and reject-unknown-fields deserialization prevent mass assignment. All auth
POST bodies are bounded to 8 KiB, including chunked requests. Global errors expose safe codes and
request IDs, never rejected values or stack traces. CORS allows one configured origin with credentials.
All auth POSTs require that exact Origin plus X-CarePath-Client: web. This custom-header/Origin CSRF
boundary covers login, registration, refresh and logout. Framework CSRF is disabled because general
APIs accept only explicit bearer tokens and cookie-consuming operations have this enforced boundary.
Do not add a cookie-authenticated endpoint outside it. Cookie-only GET `/me` is denied.

## Rate limiting and audit

Redis Lua atomically increments fixed-window counters with expiry. Auth POSTs have an IP bucket;
login/registration also share normalized-account buckets. Redis keys contain HMACs, not raw email/IP.
A separate environment secret protects those HMACs. Store outages fail auth POSTs closed with 503.
No process-local fallback is enabled in production. The backend intentionally ignores spoofable
forwarding headers. With the current Next proxy, IP buckets may aggregate proxy clients; deploy a
trusted edge limiter/topology before multi-user public traffic. Account limiting remains per account.

AuditService writes typed USER_REGISTERED, LOGIN_SUCCEEDED, LOGIN_FAILED, LOGOUT, TOKEN_REFRESHED
and TOKEN_REUSE_DETECTED events. Fields are action/outcome/reason enum, owner/session/request IDs
and timestamps; no passwords, email, raw tokens, IP or medical content is stored. Unknown-user login
failures have a null owner (V2). Successful events share their service transaction. Login failure and
replay paths commit their failure audits. This phase provides infrastructure, not an activity UI.

## Explicit IDOR foundation

`Ownership.currentOwnerId()` uses only a validated CarePrincipal from SecurityContext.
`Ownership.require(recordId, repository::findByIdAndOwnerId)` returns generic 404 for absent/foreign
records. `OwnerScopedRepository<T>` exposes an owner-scoped lookup and no unscoped findById.
Future MedicalDocument, MedicalObservation, Symptom, Appointment, VisitPack and ShareToken
repositories must use this pattern for reads and equivalent owner predicates for writes/deletes/lists.
Never pass frontend userId as owner. Retain V1's composite-owner foreign keys for nested relationships.
These product APIs are still denied and are NOT implemented or claimed tested end-to-end.

## Database and test boundaries

UserAccount is a JPA entity with optimistic versioning; UserAccounts is Spring Data JPA.
SessionStore and AuditService use parameterized JdbcTemplate within the same Spring/JPA transaction
manager for explicit row locks and content-free inserts. V1 is unchanged. V2 adds auth_session,
refresh/session integrity and uniqueness, nullable audit owner and reason metadata.

Full Spring Boot integration tests use H2 PostgreSQL mode with an explicit test-only auth schema,
real JPA/JDBC/BCrypt/RSA and HTTP/security filters. Only Redis rate storage and Clock are replaced
by deterministic test implementations. Production Redis and native PostgreSQL/Flyway remain distinct
verification gates. PGlite applies actual V1/V2 SQL and constraint checks; it does not establish those gates.

References consulted: Spring Security JWT resource server and CSRF documentation:
https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html
https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html
