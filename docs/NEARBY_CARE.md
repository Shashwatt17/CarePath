# Nearby Care architecture and limits

## Phase 11 — Nearby Care

The `nearby` module provides authenticated, transient Google Places API (New) searches behind
`NearbyCareProvider`. Category mapping: HOSPITAL → hospital, CLINIC → medical_clinic, PHARMACY
→ pharmacy, DIAGNOSTIC_CENTER → medical_lab. The explicit field mask requests names, addresses,
location, phone, current hours, website and attribution only. Google booking URLs/slots are not
provided by this integration; bookingUrl remains null. The normalized optional field is the
boundary for a future legitimate appointment-access provider. No LLM or medical history is sent.

The page requests browser location only on Use my location; manual decimal coordinates are also
supported. Search explicitly consents to sending coordinates/category/radius to Google via the
backend. No watchPosition, coordinate persistence, cache, analytics, audit payload or request/body
logging is introduced. Coordinates remain in page memory until navigation. Deployment proxies/APM
must not capture these bodies. Google has its own privacy/retention policies linked in the UI.

Search radius is user-selected, bounded to 100–20,000 metres; maximum 20 provider-ranked results.
Haversine with mean Earth radius 6,371,008.8m produces approximate straight-line distance, never
driving distance. Directions use Google Maps destination links and provider place IDs, without
including the user's origin. Missing location means no distance/directions; missing hours, phone
and website stay absent. External HTTP(S) URLs reject credentials and local/numeric hosts; phone
links allow only normalized digits and a leading plus. No server fetch of facility URLs occurs.
Google Maps and supplied third-party attributions appear as escaped text with safe links.

Provider endpoint is fixed HTTPS; redirects disabled. Timeout defaults to 8 seconds (1–15),
response body capped at 256 KiB while streaming, four concurrent provider calls maximum, no retries.
Ten searches per authenticated account per existing configured rate window (normally 60 seconds)
use the existing HMAC-keyed rate limiter and fail closed on limiter outage. Requests have the
existing 8 KiB body bound. Output is React text, no provider HTML. Responses are no-store.
No tables or V11 migration are needed; V1–V10 remain unchanged.

Configuration: backend-only GOOGLE_PLACES_API_KEY (blank disables provider),
PLACES_TIMEOUT_SECONDS=8. Enable Places API (New), billing and operator quota/key restrictions.
Phone/hours/website field selection can incur Enterprise Places charges. Load environment into
the backend process using the existing launch instructions; never use NEXT_PUBLIC for the key.
Absent/invalid key, timeout, malformed response, quota or HTTP failure returns a safe 503; rate
limit returns 429; invalid input returns 400. Other CarePath modules remain independent.

Limitations: coverage and opening information depend on Google, results are not exhaustive, manual
fallback is coordinates rather than geocoded addresses, no map/route calculation/direct booking,
no cached offline results. Provider terms/attribution and deployment privacy notices require operator
review before public deployment. Live Places verification is PENDING: no credential configured.
Native PostgreSQL/Flyway, Redis, restart persistence, rendered-browser/mobile, live LLM, production
cookie/CORS/CSRF and email verification remain open.

Official API references checked during implementation:
- https://developers.google.com/maps/documentation/places/web-service/nearby-search
- https://developers.google.com/maps/documentation/places/web-service/place-types
- https://developers.google.com/maps/documentation/places/web-service/policies
