# Final original-requirements traceability — Phase 13

All214 original checklist items are retained below. The Phase12 matrix is preserved in
REQUIREMENTS_TRACEABILITY_PHASE12.md. Classifications follow code plus executed scope, not mere
documentation. VERIFIED means the stated automated/synthetic scope; it is not clinical accuracy,
rendered-browser verification, native-service verification or production certification.

| ID | Original requirement | Classification | Evidence / limitation |
| --- | --- | --- | --- |
| 01.01 | Registration with input validation and unique normalized email. | IMPLEMENTED + VERIFIED | identity/security; AuthenticationIntegrationTest, HttpSecurityIntegrationTest |
| 01.02 | Login with secure password hashing and generic errors. | IMPLEMENTED + VERIFIED | identity/security; AuthenticationIntegrationTest, HttpSecurityIntegrationTest |
| 01.03 | Logout and server-side session/refresh revocation. | IMPLEMENTED + VERIFIED | identity/security; AuthenticationIntegrationTest, HttpSecurityIntegrationTest |
| 01.04 | JWT access tokens with signature/issuer/audience/expiry checks. | IMPLEMENTED + VERIFIED | identity/security; AuthenticationIntegrationTest, HttpSecurityIntegrationTest |
| 01.05 | Secure rotating refresh tokens and reuse detection. | IMPLEMENTED + VERIFIED | identity/security; AuthenticationIntegrationTest, HttpSecurityIntegrationTest |
| 01.06 | Protected frontend routes and session restoration. | IMPLEMENTED + LIVE VERIFICATION PENDING | identity/security; AuthenticationIntegrationTest, HttpSecurityIntegrationTest; implementation inspected, rendered interaction pending |
| 01.07 | Protected backend APIs using real authenticated identity. | IMPLEMENTED + VERIFIED | identity/security; AuthenticationIntegrationTest, HttpSecurityIntegrationTest |
| 01.08 | Object-level authorization for every owned resource. | IMPLEMENTED + VERIFIED | identity/security; AuthenticationIntegrationTest, HttpSecurityIntegrationTest |
| 01.09 | Rate limits for registration, login, refresh and other sensitive endpoints. | IMPLEMENTED + VERIFIED | identity/security; AuthenticationIntegrationTest, HttpSecurityIntegrationTest |
| 01.10 | Authentication/session lifecycle and abuse tests; no frontend-only fake authentication. | IMPLEMENTED + VERIFIED | identity/security; AuthenticationIntegrationTest, HttpSecurityIntegrationTest |
| 02.01 | Upload PDF, JPG, JPEG and PNG with magic/decoder/size validation. | IMPLEMENTED + VERIFIED | vault; VaultIntegrationTest |
| 02.02 | Document types: lab report, prescription, diagnostic report, discharge summary, vaccination record, doctor note, referral, medical bill, other. | IMPLEMENTED + VERIFIED | vault; VaultIntegrationTest |
| 02.03 | Private local storage adapter and future S3-compatible storage port. | IMPLEMENTED + VERIFIED | vault; VaultIntegrationTest |
| 02.04 | Preview owned originals with safe content handling. | IMPLEMENTED + VERIFIED | vault; VaultIntegrationTest |
| 02.05 | Search and filter documents. | IMPLEMENTED + VERIFIED | vault; VaultIntegrationTest |
| 02.06 | Download own original with strict owner authorization. | IMPLEMENTED + VERIFIED | vault; VaultIntegrationTest |
| 02.07 | Inspect extracted information. | IMPLEMENTED + VERIFIED | vault; VaultIntegrationTest |
| 02.08 | User-controlled deletion: originals/metadata implemented and tested; future derived artifacts/share invalidation remain PENDING. | IMPLEMENTED + VERIFIED | VaultIntegrationTest and PackIntegrationTest: originals and live derived evidence become inaccessible; durable blob cleanup. Phase9 explicitly requires historical generated snapshots to remain until separately deleted; sharing never exposes deleted originals. |
| 02.09 | Persist owner/type/date/upload date/provider/MIME/safe key/hash/status/tags with tested vault service. Confidence remains unset; no extraction. Reserved arbitrary JSON metadata is not exposed. | IMPLEMENTED + VERIFIED | vault; VaultIntegrationTest |
| 02.10 | Path traversal, filename, MIME mismatch and ownership tests. | IMPLEMENTED + VERIFIED | vault; VaultIntegrationTest |
| 03.01 | Upload → validate → store → classify → text extraction → OCR fallback → structured extraction → deterministic validation → normalize → provenance → timeline. | IMPLEMENTED + VERIFIED | intelligence/review; ProcessingIntegrationTest, ExtractionRuntimeTest |
| 03.02 | PDF text extraction per page. | IMPLEMENTED + VERIFIED | intelligence/review; ProcessingIntegrationTest, ExtractionRuntimeTest |
| 03.03 | OCR fallback for scanned PDFs and images. | IMPLEMENTED + VERIFIED | intelligence/review; ProcessingIntegrationTest, ExtractionRuntimeTest |
| 03.04 | Asynchronous processing outside normal upload request. | IMPLEMENTED + VERIFIED | intelligence/review; ProcessingIntegrationTest, ExtractionRuntimeTest |
| 03.05 | Durable jobs with retry/backoff/leases/recovery/idempotency. | IMPLEMENTED + VERIFIED | intelligence/review; ProcessingIntegrationTest, ExtractionRuntimeTest |
| 03.06 | UPLOADED, PROCESSING, NEEDS_REVIEW, COMPLETED and FAILED transitions. | IMPLEMENTED + VERIFIED | intelligence/review; ProcessingIntegrationTest, ExtractionRuntimeTest |
| 03.07 | Explicit processing error/status UI and safe error codes. | IMPLEMENTED + VERIFIED | intelligence/review; ProcessingIntegrationTest, ExtractionRuntimeTest |
| 04.01 | Extract original and canonical test names without fabricating missing fields. | IMPLEMENTED + VERIFIED | intelligence; ExtractionRulesTest, ExtractionEvaluationIT |
| 04.02 | Extract value, comparator, original unit and supplied reference interval. | IMPLEMENTED + VERIFIED | intelligence; ExtractionRulesTest, ExtractionEvaluationIT |
| 04.03 | Retain explicit abnormal indicator only when supported. | IMPLEMENTED + VERIFIED | intelligence; ExtractionRulesTest, ExtractionEvaluationIT |
| 04.04 | Extract report/sample date and laboratory where available. | IMPLEMENTED + VERIFIED | intelligence; ExtractionRulesTest, ExtractionEvaluationIT |
| 04.05 | Retain page, source evidence and separate extraction confidence. | IMPLEMENTED + VERIFIED | intelligence; ExtractionRulesTest, ExtractionEvaluationIT |
| 04.06 | Store original and normalized representations; unknowns remain null. | IMPLEMENTED + VERIFIED | intelligence; ExtractionRulesTest, ExtractionEvaluationIT |
| 05.01 | Actual deterministic alias normalization layer: Hb/HGB/Hemoglobin/Haemoglobin in vetted context. | IMPLEMENTED + VERIFIED | terminology/review; NormalizationTest, ReviewIntegrationTest |
| 05.02 | Persist original term, canonical term, mapping confidence and rule version. | IMPLEMENTED + VERIFIED | terminology/review; NormalizationTest, ReviewIntegrationTest |
| 05.03 | Use recognized identifiers such as LOINC only with reliable contextual mapping. | INTENTIONALLY DEFERRED | Reliable specimen/method/context-specific LOINC mapping is not established. Curated canonical concepts/aliases work; assigning unsupported identifiers would misrepresent evidence. |
| 05.04 | Review uncertain/ambiguous mappings. | IMPLEMENTED + VERIFIED | terminology/review; NormalizationTest, ReviewIntegrationTest |
| 05.05 | Do not use unrestricted LLM string equivalence as normalization. | IMPLEMENTED + VERIFIED | terminology/review; NormalizationTest, ReviewIntegrationTest |
| 06.01 | Preserve original values and units separately from normalized values. | IMPLEMENTED + VERIFIED | terminology/UnitNormalizer; NormalizationTest |
| 06.02 | Only validated compatible concept/unit conversion rules. | IMPLEMENTED + VERIFIED | terminology/UnitNormalizer; NormalizationTest |
| 06.03 | BigDecimal conversions with explicit comparator/precision handling. | IMPLEMENTED + VERIFIED | terminology/UnitNormalizer; NormalizationTest |
| 06.04 | No silent unit guessing. | IMPLEMENTED + VERIFIED | terminology/UnitNormalizer; NormalizationTest |
| 06.05 | Meaningful compatible/incompatible/missing-unit and reference-bound conversion tests. | IMPLEMENTED + VERIFIED | terminology/UnitNormalizer; NormalizationTest |
| 07.01 | Low-confidence observations excluded from trusted timeline. | IMPLEMENTED + VERIFIED | review; ReviewIntegrationTest and review UI |
| 07.02 | Show exact uncertainty message: CarePath couldn't confidently read this result.. | IMPLEMENTED + LIVE VERIFICATION PENDING | review; ReviewIntegrationTest and review UI; implementation inspected, rendered interaction pending |
| 07.03 | Confirm, correct and reject actions. | IMPLEMENTED + VERIFIED | review; ReviewIntegrationTest and review UI |
| 07.04 | Show source, extracted value, confidence and relevant evidence. | IMPLEMENTED + VERIFIED | review; ReviewIntegrationTest and review UI |
| 07.05 | Append review history while preserving original extraction. | IMPLEMENTED + VERIFIED | review; ReviewIntegrationTest and review UI |
| 08.01 | Combine observations, reports, symptoms, prescriptions, doctor visits, appointments and follow-ups. | IMPLEMENTED + VERIFIED | ClosureIntegrationTest and ClosureJourneyTest: owner-scoped five-type paginated timeline includes verified observations, documents (including prescriptions), symptoms, appointments/previous visits and confirmed/edited follow-ups with source pages. No new prescription facts invented. |
| 08.02 | Select one observation concept and view history/chart. | IMPLEMENTED + LIVE VERIFICATION PENDING | longitudinal; HistoryIntegrationTest, timeline UI; implementation inspected, rendered interaction pending |
| 08.03 | January/April/September demonstration: Hemoglobin 12.1 → 11.3 → 10.4. | IMPLEMENTED + VERIFIED | longitudinal; HistoryIntegrationTest, timeline UI |
| 08.04 | Every point links to original evidence. | IMPLEMENTED + VERIFIED | longitudinal; HistoryIntegrationTest, timeline UI |
| 08.05 | Handle missing/partial dates and incomparable units without false ordering or trends. | IMPLEMENTED + VERIFIED | longitudinal; HistoryIntegrationTest, timeline UI |
| 09.01 | Deterministic increasing and decreasing comparisons. | IMPLEMENTED + VERIFIED | longitudinal; ComparisonTest, LongitudinalEvaluationIT |
| 09.02 | Approximately stable using documented versioned tolerance. | IMPLEMENTED + VERIFIED | longitudinal; ComparisonTest, LongitudinalEvaluationIT |
| 09.03 | Newly observed measurement. | IMPLEMENTED + VERIFIED | longitudinal; ComparisonTest, LongitudinalEvaluationIT |
| 09.04 | Previously tracked measurement absent only from comparable adequately extracted report. | IMPLEMENTED + VERIFIED | longitudinal; ComparisonTest, LongitudinalEvaluationIT |
| 09.05 | Entered/exited supplied reference interval only when deterministically valid. | IMPLEMENTED + VERIFIED | longitudinal; ComparisonTest, LongitudinalEvaluationIT |
| 09.06 | Insufficient-evidence outcome. | IMPLEMENTED + VERIFIED | longitudinal; ComparisonTest, LongitudinalEvaluationIT |
| 09.07 | Numerical rules in tested code, no unrestricted LLM judgment or diagnosis. | IMPLEMENTED + VERIFIED | longitudinal; ComparisonTest, LongitudinalEvaluationIT |
| 09.08 | Demonstrate Hemoglobin 11.3 → 10.4 and Vitamin D 19 → 31 without explaining medical cause. | IMPLEMENTED + VERIFIED | longitudinal; ComparisonTest, LongitudinalEvaluationIT |
| 10.01 | Every important record-derived statement links to source document/page. | IMPLEMENTED + VERIFIED | review/longitudinal; provenance assertions in integration tests |
| 10.02 | Multi-report claim cites all contributing observations. | IMPLEMENTED + VERIFIED | review/longitudinal; provenance assertions in integration tests |
| 10.03 | VIEW EVIDENCE interaction to original page/snippet. | IMPLEMENTED + LIVE VERIFICATION PENDING | review/longitudinal; provenance assertions in integration tests; implementation inspected, rendered interaction pending |
| 10.04 | Distinguish USER DATA, REFERENCE INFORMATION and AI EXPLANATION. | IMPLEMENTED + VERIFIED | review/longitudinal; provenance assertions in integration tests |
| 10.05 | Enforce provenance consistency at service layer in addition to schema. | IMPLEMENTED + VERIFIED | review/longitudinal; provenance assertions in integration tests |
| 11.01 | Questions over uploaded structured records. | PARTIAL | assistant; AssistantIntegrationTest, ProviderContractTest; bounded verified lab scopes and approved explanation templates, not arbitrary report interpretation |
| 11.02 | Plain-language report explanations. | PARTIAL | assistant; AssistantIntegrationTest, ProviderContractTest; bounded verified lab scopes and approved explanation templates, not arbitrary report interpretation |
| 11.03 | Grounded explanations of longitudinal changes. | IMPLEMENTED + VERIFIED | assistant; AssistantIntegrationTest, ProviderContractTest |
| 11.04 | Owner-scoped structured retrieval → provenance → optional trusted reference context → controlled LLM → evidence response. | IMPLEMENTED + VERIFIED | assistant; AssistantIntegrationTest, ProviderContractTest |
| 11.05 | Provider abstraction and environment-only API keys. | IMPLEMENTED + VERIFIED | assistant; AssistantIntegrationTest, ProviderContractTest |
| 11.06 | Insufficient-evidence refusal and invented-citation rejection. | IMPLEMENTED + VERIFIED | assistant; AssistantIntegrationTest, ProviderContractTest |
| 11.07 | Treat documents as untrusted data; prompt-injection defenses. | IMPLEMENTED + VERIFIED | assistant; AssistantIntegrationTest, ProviderContractTest |
| 11.08 | No primary full-PDF/full-history prompt architecture. | IMPLEMENTED + VERIFIED | assistant; AssistantIntegrationTest, ProviderContractTest |
| 11.09 | No model/database source-of-truth confusion. | IMPLEMENTED + VERIFIED | assistant; AssistantIntegrationTest, ProviderContractTest |
| 12.01 | Create/edit symptom name, start date, severity, frequency, notes and resolved date. | IMPLEMENTED + VERIFIED | care/SymptomService; CareIntegrationTest |
| 12.02 | Symptoms become timeline events. | IMPLEMENTED + VERIFIED | care/SymptomService; CareIntegrationTest |
| 12.03 | Assistant may use relevant symptoms without unsupported diagnoses. | INTENTIONALLY DEFERRED | Original optional assistant symptom context is intentionally excluded: user-reported symptoms remain separate from verified laboratory facts; no symptom diagnosis. |
| 13.01 | Generate editable clinician questions grounded in record changes. | IMPLEMENTED + VERIFIED | assistant/SavedQuestionService; AssistantIntegrationTest, PackIntegrationTest |
| 13.02 | Edit and save questions. | IMPLEMENTED + VERIFIED | assistant/SavedQuestionService; AssistantIntegrationTest, PackIntegrationTest |
| 13.03 | Include selected questions in Visit Pack. | IMPLEMENTED + VERIFIED | assistant/SavedQuestionService; AssistantIntegrationTest, PackIntegrationTest |
| 13.04 | No prescribing or unsupported evaluation recommendations. | IMPLEMENTED + VERIFIED | assistant/SavedQuestionService; AssistantIntegrationTest, PackIntegrationTest |
| 14.01 | Clinician/provider, specialty, date/time/timezone, location and notes. | IMPLEMENTED + VERIFIED | care/AppointmentService; CareIntegrationTest |
| 14.02 | Link selected owned reports and symptoms. | IMPLEMENTED + VERIFIED | care/AppointmentService; CareIntegrationTest |
| 14.03 | Manage appointment status and follow-up date. | IMPLEMENTED + VERIFIED | care/AppointmentService; CareIntegrationTest |
| 14.04 | Upcoming appointments on dashboard. | IMPLEMENTED + LIVE VERIFICATION PENDING | care/AppointmentService; CareIntegrationTest; implementation inspected, rendered interaction pending |
| 14.05 | Ownership and appointment-change audit events. | IMPLEMENTED + VERIFIED | care/AppointmentService; CareIntegrationTest |
| 15.01 | Extract possible follow-up such as Review after 6 weeks. | IMPLEMENTED + VERIFIED | care/FollowUpParser, FollowUpService; CareRulesTest, CareIntegrationTest |
| 15.02 | Show original source and suggested date with anchor/uncertainty. | IMPLEMENTED + VERIFIED | care/FollowUpParser, FollowUpService; CareRulesTest, CareIntegrationTest |
| 15.03 | Confirm, edit and ignore. | IMPLEMENTED + VERIFIED | care/FollowUpParser, FollowUpService; CareRulesTest, CareIntegrationTest |
| 15.04 | Never silently create uncertain follow-ups. | IMPLEMENTED + VERIFIED | care/FollowUpParser, FollowUpService; CareRulesTest, CareIntegrationTest |
| 15.05 | Test calendar arithmetic including missing anchor dates. | IMPLEMENTED + VERIFIED | care/FollowUpParser, FollowUpService; CareRulesTest, CareIntegrationTest |
| 16.01 | Persistent server-side reminders for appointments and confirmed follow-ups. | IMPLEMENTED + VERIFIED | care/ReminderService; CareIntegrationTest |
| 16.02 | Configurable reminder offsets. | IMPLEMENTED + VERIFIED | care/ReminderService; CareIntegrationTest |
| 16.03 | In-app notifications. | IMPLEMENTED + VERIFIED | care/ReminderService; CareIntegrationTest |
| 16.04 | Email provider abstraction if practical; explicit disabled state. | IMPLEMENTED + VERIFIED | Optional email port and explicit disabled configuration/status exist; CareIntegrationTest verifies persistent in-app delivery. No external email adapter or delivery is claimed. |
| 16.05 | Survive restart; leased scheduling and idempotent delivery. | IMPLEMENTED + LIVE VERIFICATION PENDING | care/ReminderService; CareIntegrationTest; database-backed atomic notifications tested with H2; native restart not executed |
| 17.01 | Select reason for visit, symptoms and important timeline events. | IMPLEMENTED + VERIFIED | visitpack; PackIntegrationTest, PackPdfTest |
| 17.02 | Select relevant observations/trends/reports/prescriptions/previous visits/questions. | IMPLEMENTED + VERIFIED | visitpack; PackIntegrationTest, PackPdfTest |
| 17.03 | Include source evidence for record-derived content. | IMPLEMENTED + VERIFIED | visitpack; PackIntegrationTest, PackPdfTest |
| 17.04 | Professional web view. | IMPLEMENTED + LIVE VERIFICATION PENDING | visitpack; PackIntegrationTest, PackPdfTest; implementation inspected, rendered interaction pending |
| 17.05 | Downloadable PDF generated from same scoped snapshot. | IMPLEMENTED + VERIFIED | visitpack; PackIntegrationTest, PackPdfTest |
| 17.06 | No unverified diagnoses; invalidate snapshots after source corrections/deletion. | NOT APPLICABLE | Automatic historical snapshot invalidation was explicitly superseded by Phase9 immutable snapshot/revision semantics. PackIntegrationTest checks retained snapshots; originals remain protected/deleted; packs are deleted separately. |
| 18.01 | Strong random share token with only digest stored. | IMPLEMENTED + VERIFIED | sharing; SharingIntegrationTest, share-client tests |
| 18.02 | Scoped selected records through immutable Visit Pack revision. | IMPLEMENTED + VERIFIED | sharing; SharingIntegrationTest, share-client tests |
| 18.03 | Read-only anonymous access with no clinician account. | IMPLEMENTED + VERIFIED | sharing; SharingIntegrationTest, share-client tests |
| 18.04 | 15-minute, 30-minute, 1-hour and 24-hour expiry options. | IMPLEMENTED + VERIFIED | sharing; SharingIntegrationTest, share-client tests |
| 18.05 | Revocation on every subsequent request. | IMPLEMENTED + VERIFIED | sharing; SharingIntegrationTest, share-client tests |
| 18.06 | QR code for share URL. | IMPLEMENTED + LIVE VERIFICATION PENDING | sharing; SharingIntegrationTest, share-client tests; implementation inspected, rendered interaction pending |
| 18.07 | Audit share creation/access/revocation. | IMPLEMENTED + VERIFIED | sharing; SharingIntegrationTest, share-client tests |
| 18.08 | Test create → access → revoke → denied. | IMPLEMENTED + VERIFIED | sharing; SharingIntegrationTest, share-client tests |
| 18.09 | Test create → expire → denied. | IMPLEMENTED + VERIFIED | sharing; SharingIntegrationTest, share-client tests |
| 18.10 | Owner-only share-management APIs and noncached anonymous views. | IMPLEMENTED + VERIFIED | sharing; SharingIntegrationTest, share-client tests |
| 19.01 | Explicit user location permission. | IMPLEMENTED + LIVE VERIFICATION PENDING | nearby; NearbyTest, NearbyIntegrationTest, nearby.test.ts; real provider is not configured; deterministic/contract tests pass |
| 19.02 | Legitimate Maps/Places provider for hospitals, clinics, pharmacies and diagnostic centres. | IMPLEMENTED + LIVE VERIFICATION PENDING | nearby; NearbyTest, NearbyIntegrationTest, nearby.test.ts; real provider is not configured; deterministic/contract tests pass |
| 19.03 | Show source-provided name/address/distance/phone/hours/open-closed/website. | IMPLEMENTED + LIVE VERIFICATION PENDING | nearby; NearbyTest, NearbyIntegrationTest, nearby.test.ts; real provider is not configured; deterministic/contract tests pass |
| 19.04 | Directions and legitimate external booking links where available. | IMPLEMENTED + LIVE VERIFICATION PENDING | nearby; NearbyTest, NearbyIntegrationTest, nearby.test.ts; real provider is not configured; deterministic/contract tests pass |
| 19.05 | No LLM-invented facilities/contact details/hours/availability. | IMPLEMENTED + LIVE VERIFICATION PENDING | nearby; NearbyTest, NearbyIntegrationTest, nearby.test.ts; real provider is not configured; deterministic/contract tests pass |
| 19.06 | Attribution, graceful missing fields and provider timeout/quota behavior. | IMPLEMENTED + LIVE VERIFICATION PENDING | nearby; NearbyTest, NearbyIntegrationTest, nearby.test.ts; real provider is not configured; deterministic/contract tests pass |
| 20.01 | Call action. | IMPLEMENTED + LIVE VERIFICATION PENDING | nearby; normalized actions and optional booking URL; real provider is not configured; deterministic/contract tests pass |
| 20.02 | Directions action. | IMPLEMENTED + LIVE VERIFICATION PENDING | nearby; normalized actions and optional booking URL; real provider is not configured; deterministic/contract tests pass |
| 20.03 | Official website action. | IMPLEMENTED + LIVE VERIFICATION PENDING | nearby; normalized actions and optional booking URL; real provider is not configured; deterministic/contract tests pass |
| 20.04 | External booking page only when legitimately available. | IMPLEMENTED + LIVE VERIFICATION PENDING | nearby; normalized actions and optional booking URL; real provider is not configured; deterministic/contract tests pass |
| 20.05 | Future direct appointment-provider abstraction. | INTENTIONALLY DEFERRED | NearbyCareProvider normalizes legitimate external actions; dedicated direct appointment-provider booking remains intentionally deferred. No invented slots or booking URL. |
| 20.06 | Never fabricate appointment slots. | IMPLEMENTED + LIVE VERIFICATION PENDING | nearby; normalized actions and optional booking URL; real provider is not configured; deterministic/contract tests pass |
| 21.01 | Search/filter documents, tests, dates, providers, symptoms and appointments. | IMPLEMENTED + VERIFIED | SearchService/ClosureIntegrationTest: owner-scoped SQL search across documents/tags, trusted observations, dates/providers, symptoms and appointments; date/type/provider filters, page20, bounded terms; no LLM. |
| 21.02 | Queries such as CBC, hemoglobin and September prescription. | IMPLEMENTED + VERIFIED | ClosureIntegrationTest checks CBC tags, Hemoglobin trusted values and September prescription month/category terms. English month tokens and literal AND matching; not unrestricted natural-language search. |
| 21.03 | Owner-scoped paginated search and controlled query/sort inputs. | IMPLEMENTED + VERIFIED | ClosureIntegrationTest checks pagination, A/B isolation, pending exclusion, deleted-source freshness, forged fields, escaped wildcard input, rate limit/outage and request validation. |
| 22.01 | Secure authentication, password hashing and refresh-token security. | IMPLEMENTED + VERIFIED | security/vault/review/care/visitpack/sharing boundaries and regression suites |
| 22.02 | Object-level authorization and input validation. | IMPLEMENTED + VERIFIED | security/vault/review/care/visitpack/sharing boundaries and regression suites |
| 22.03 | Secure upload handling and safe storage keys/filenames. | IMPLEMENTED + VERIFIED | security/vault/review/care/visitpack/sharing boundaries and regression suites |
| 22.04 | Rate limiting. | IMPLEMENTED + VERIFIED | security/vault/review/care/visitpack/sharing boundaries and regression suites |
| 22.05 | Secure headers and CORS (foundation configured; full feature policy pending). | IMPLEMENTED + LIVE VERIFICATION PENDING | security/vault/review/care/visitpack/sharing boundaries and regression suites; runtime loopback checks and code review only, production proxy/TLS/browser pending |
| 22.06 | SQL injection prevention and XSS protections across implemented product flows. | IMPLEMENTED + VERIFIED | security/vault/review/care/visitpack/sharing boundaries and regression suites |
| 22.07 | Path traversal prevention. | IMPLEMENTED + VERIFIED | security/vault/review/care/visitpack/sharing boundaries and regression suites |
| 22.08 | Access auditing. | IMPLEMENTED + VERIFIED | security/vault/review/care/visitpack/sharing boundaries and regression suites |
| 22.09 | Temporary share expiry/revocation. | IMPLEMENTED + VERIFIED | security/vault/review/care/visitpack/sharing boundaries and regression suites |
| 22.10 | User-controlled deletion across originals/derivatives. | IMPLEMENTED + VERIFIED | security/vault/review/care/visitpack/sharing boundaries and regression suites |
| 22.11 | Environment-based secrets (foundation configured; future adapters pending). | IMPLEMENTED + VERIFIED | security/vault/review/care/visitpack/sharing boundaries and regression suites |
| 22.12 | No sensitive medical content in application or proxy logs. | IMPLEMENTED + LIVE VERIFICATION PENDING | security/vault/review/care/visitpack/sharing boundaries and regression suites; runtime loopback checks and code review only, production proxy/TLS/browser pending |
| 22.13 | Maintain SECURITY.md assets/trust boundaries/attack surfaces/controls/limitations. | IMPLEMENTED + VERIFIED | security/vault/review/care/visitpack/sharing boundaries and regression suites |
| 22.14 | No HIPAA/GDPR/ABDM or other unestablished compliance claims. | IMPLEMENTED + VERIFIED | security/vault/review/care/visitpack/sharing boundaries and regression suites |
| 23.01 | Login events. | IMPLEMENTED + VERIFIED | audit/AuditService; audit assertions in integration suites |
| 23.02 | Document upload/view/delete events. | IMPLEMENTED + VERIFIED | audit/AuditService; audit assertions in integration suites |
| 23.03 | Share creation/access/revocation events. | IMPLEMENTED + VERIFIED | audit/AuditService; audit assertions in integration suites |
| 23.04 | Appointment change events. | IMPLEMENTED + VERIFIED | audit/AuditService; audit assertions in integration suites |
| 23.05 | User-facing activity/security history. | IMPLEMENTED + VERIFIED | ActivityController and ClosureIntegrationTest: authenticated owner-scoped projection with category/page filters; no tokens, medical text, resource/session/request IDs or internal reason metadata. UI implemented; rendered behavior pending under25.11. |
| 23.06 | No passwords, tokens or raw medical documents in audit logs. | IMPLEMENTED + VERIFIED | audit/AuditService; audit assertions in integration suites |
| 24.01 | No definitive diagnosis. | IMPLEMENTED + VERIFIED | assistant/RecordSafety, ExplanationValidator; safety/forgery tests |
| 24.02 | No medication prescription or dosage recommendation. | IMPLEMENTED + VERIFIED | assistant/RecordSafety, ExplanationValidator; safety/forgery tests |
| 24.03 | No instructions to stop/change prescribed medications. | IMPLEMENTED + VERIFIED | assistant/RecordSafety, ExplanationValidator; safety/forgery tests |
| 24.04 | No fabricated medical information or clinician replacement claims. | IMPLEMENTED + VERIFIED | assistant/RecordSafety, ExplanationValidator; safety/forgery tests |
| 24.05 | Conservative predefined urgent-pattern escalation with vetted/versioned rules and sources. | IMPLEMENTED + VERIFIED | assistant/RecordSafety, ExplanationValidator; safety/forgery tests |
| 24.06 | Clearly surface uncertainty; adversarial safety tests. | IMPLEMENTED + VERIFIED | assistant/RecordSafety, ExplanationValidator; safety/forgery tests |
| 25.01 | Landing page (foundation page exists; final product landing pending). | IMPLEMENTED + LIVE VERIFICATION PENDING | Current landing copy/navigation describes implemented capabilities and links privacy/workspace; production build passes. Rendered desktop/mobile polish/accessibility remains unverified. |
| 25.02 | Login and registration. | IMPLEMENTED + LIVE VERIFICATION PENDING | frontend/src/app and component/client tests; browser blocked |
| 25.03 | Onboarding. | IMPLEMENTED + LIVE VERIFICATION PENDING | Four-step nonblocking onboarding guide, linked after registration and from existing-user Dashboard/Settings; no sensitive profile data or completion tracking. Client/source tests/build pass; rendered flow pending. |
| 25.04 | Dashboard: upcoming appointment → health changes → reminders → recent documents → quick actions. | IMPLEMENTED + LIVE VERIFICATION PENDING | Real care dashboard and quick actions; visual order/accessibility not browser-verified |
| 25.05 | Records and document viewer: COMPLETE + NOT LIVE-VERIFIED (implemented/build checked; rendered browser verification pending). | IMPLEMENTED + LIVE VERIFICATION PENDING | frontend/src/app and component/client tests; browser blocked |
| 25.06 | Timeline and What Changed?. | IMPLEMENTED + LIVE VERIFICATION PENDING | frontend/src/app and component/client tests; browser blocked |
| 25.07 | AI explanation. | IMPLEMENTED + LIVE VERIFICATION PENDING | frontend/src/app and component/client tests; browser blocked |
| 25.08 | Symptoms and appointments. | IMPLEMENTED + LIVE VERIFICATION PENDING | frontend/src/app and component/client tests; browser blocked |
| 25.09 | Visit Packs and temporary share. | IMPLEMENTED + LIVE VERIFICATION PENDING | frontend/src/app and component/client tests; browser blocked |
| 25.10 | Nearby Care. | IMPLEMENTED + LIVE VERIFICATION PENDING | frontend/src/app and component/client tests; browser blocked |
| 25.11 | Notifications and Activity. | IMPLEMENTED + LIVE VERIFICATION PENDING | Notifications plus new category-filtered paginated Activity page use real APIs. API isolation and client tests pass; rendered interaction pending. |
| 25.12 | Privacy/Security and Settings. | IMPLEMENTED + LIVE VERIFICATION PENDING | Privacy/Security and authenticated Settings pages expose real account information, privacy/snapshot/external-provider behavior and existing controls; no fake toggles. Source/client/build verified; rendered interaction pending. |
| 25.13 | Responsive accessible components, keyboard support, mobile layouts. | IMPLEMENTED + LIVE VERIFICATION PENDING | frontend/src/app and component/client tests; browser blocked |
| 25.14 | Excellent typography/spacing, restrained product visual style, no excessive gradients/clutter/huge cards. | IMPLEMENTED + LIVE VERIFICATION PENDING | frontend/src/app and component/client tests; browser blocked |
| 25.15 | Skeleton loading, useful empty/error states and confirmation dialogs. | IMPLEMENTED + LIVE VERIFICATION PENDING | frontend/src/app and component/client tests; browser blocked |
| 25.16 | Polished Recharts charts where useful and subtle reduced-motion-aware animation. | IMPLEMENTED + LIVE VERIFICATION PENDING | frontend/src/app and component/client tests; browser blocked |
| 25.17 | Next.js/React strict TypeScript/Tailwind/shadcn foundation; extend consistently. | IMPLEMENTED + LIVE VERIFICATION PENDING | frontend/src/app and component/client tests; browser blocked |
| 26.01 | JPA entities and services for User, RefreshToken/Session, MedicalDocument, DocumentExtraction. | PARTIAL | All named domains have schema, typed records/services and owner-scoped persistence. User uses JPA; most feature repositories deliberately use JDBC rather than JPA entities. This literal original JPA-specific requirement remains PARTIAL; no risky ORM rewrite for a checklist. Native FK/locking verification remains separately blocked. |
| 26.02 | JPA entities and services for MedicalObservation, CanonicalMedicalConcept, ObservationSource. | PARTIAL | All named domains have schema, typed records/services and owner-scoped persistence. User uses JPA; most feature repositories deliberately use JDBC rather than JPA entities. This literal original JPA-specific requirement remains PARTIAL; no risky ORM rewrite for a checklist. Native FK/locking verification remains separately blocked. |
| 26.03 | JPA entities and services for Symptom, Appointment, FollowUp, Reminder, SavedQuestion. | PARTIAL | All named domains have schema, typed records/services and owner-scoped persistence. User uses JPA; most feature repositories deliberately use JDBC rather than JPA entities. This literal original JPA-specific requirement remains PARTIAL; no risky ORM rewrite for a checklist. Native FK/locking verification remains separately blocked. |
| 26.04 | JPA entities and services for VisitPack, VisitPackItem, ShareToken, Notification, AuditEvent. | PARTIAL | All named domains have schema, typed records/services and owner-scoped persistence. User uses JPA; most feature repositories deliberately use JDBC rather than JPA entities. This literal original JPA-specific requirement remains PARTIAL; no risky ORM rewrite for a checklist. Native FK/locking verification remains separately blocked. |
| 26.05 | Enforce foreign keys, constraints, indexes, timestamps and optimistic locking in services (DDL exists). | IMPLEMENTED + LIVE VERIFICATION PENDING | db/migration/V1–V10 and JDBC/JPA repositories; native FK/locking/Flyway startup remains blocked |
| 26.06 | Append Flyway migrations without regenerating existing deployed schema. | IMPLEMENTED + LIVE VERIFICATION PENDING | db/migration/V1–V10 and JDBC/JPA repositories; native FK/locking/Flyway startup remains blocked |
| 27.01 | Authentication and authorization tests. | IMPLEMENTED + VERIFIED | backend/src/test, frontend/src/lib/*.test.ts |
| 27.02 | Document ownership tests. | IMPLEMENTED + VERIFIED | backend/src/test, frontend/src/lib/*.test.ts |
| 27.03 | Concept normalization and unit conversion tests. | IMPLEMENTED + VERIFIED | backend/src/test, frontend/src/lib/*.test.ts |
| 27.04 | Change detection and provenance tests. | IMPLEMENTED + VERIFIED | backend/src/test, frontend/src/lib/*.test.ts |
| 27.05 | Follow-up date calculation tests. | IMPLEMENTED + VERIFIED | backend/src/test, frontend/src/lib/*.test.ts |
| 27.06 | Reminder persistence/restart tests. | IMPLEMENTED + LIVE VERIFICATION PENDING | backend/src/test, frontend/src/lib/*.test.ts; reminder persistence/idempotency tested in H2, no native restart |
| 27.07 | Share expiration and revocation tests. | IMPLEMENTED + VERIFIED | backend/src/test, frontend/src/lib/*.test.ts |
| 27.08 | Security: User A never accesses User B documents. | IMPLEMENTED + VERIFIED | backend/src/test, frontend/src/lib/*.test.ts |
| 27.09 | Security: User A never accesses User B observations. | IMPLEMENTED + VERIFIED | backend/src/test, frontend/src/lib/*.test.ts |
| 27.10 | Security: User A never accesses User B appointments. | IMPLEMENTED + VERIFIED | backend/src/test, frontend/src/lib/*.test.ts |
| 27.11 | Security: User A never accesses User B Visit Packs. | IMPLEMENTED + VERIFIED | backend/src/test, frontend/src/lib/*.test.ts |
| 27.12 | Security: User A never accesses User B share-management APIs. | IMPLEMENTED + VERIFIED | backend/src/test, frontend/src/lib/*.test.ts |
| 27.13 | Frontend critical flow/component tests where practical. | IMPLEMENTED + VERIFIED | backend/src/test, frontend/src/lib/*.test.ts |
| 28.01 | Reproducible evaluation framework and scripts. | IMPLEMENTED + VERIFIED | evaluation; Extraction/Normalization/LongitudinalEvaluationIT |
| 28.02 | Document classification accuracy. | IMPLEMENTED + VERIFIED | evaluation; Extraction/Normalization/LongitudinalEvaluationIT |
| 28.03 | Lab extraction accuracy. | IMPLEMENTED + VERIFIED | evaluation; Extraction/Normalization/LongitudinalEvaluationIT |
| 28.04 | Concept normalization accuracy. | IMPLEMENTED + VERIFIED | evaluation; Extraction/Normalization/LongitudinalEvaluationIT |
| 28.05 | Unit normalization accuracy. | IMPLEMENTED + VERIFIED | evaluation; Extraction/Normalization/LongitudinalEvaluationIT |
| 28.06 | Change detection accuracy. | IMPLEMENTED + VERIFIED | evaluation; Extraction/Normalization/LongitudinalEvaluationIT |
| 28.07 | Provenance correctness. | IMPLEMENTED + VERIFIED | evaluation; Extraction/Normalization/LongitudinalEvaluationIT |
| 28.08 | Labelled synthetic evaluation dataset and measured metrics with denominators. | IMPLEMENTED + VERIFIED | evaluation; Extraction/Normalization/LongitudinalEvaluationIT |
| 28.09 | Never fabricate evaluation numbers. | IMPLEMENTED + VERIFIED | evaluation; Extraction/Normalization/LongitudinalEvaluationIT |
| 29.01 | No real patient data; clear SYNTHETIC DEMO DATA — NOT A REAL PATIENT labels. | IMPLEMENTED + VERIFIED | sample-data/phase4, phase5, phase9; evaluation fixtures |
| 29.02 | At least January, April and September reports. | IMPLEMENTED + VERIFIED | sample-data/phase4, phase5, phase9; evaluation fixtures |
| 29.03 | Demonstrate decrease, increase, stable and new measurement. | IMPLEMENTED + VERIFIED | sample-data/phase4, phase5, phase9; evaluation fixtures |
| 29.04 | Uncertain extraction requiring verification. | IMPLEMENTED + VERIFIED | sample-data/phase4, phase5, phase9; evaluation fixtures |
| 29.05 | Useful follow-up instruction and gold source labels. | IMPLEMENTED + VERIFIED | sample-data/phase4, phase5, phase9; evaluation fixtures |
| 30.01 | Structured logs (foundation ECS configured; feature-log privacy validation pending). | IMPLEMENTED + LIVE VERIFICATION PENDING | ECS structured logging configured; inspected application logs omit bodies/records/tokens/coordinates. Real proxy/APM/retention enforcement remains a deployment check. |
| 30.02 | Correlation/request IDs (foundation implemented; propagate through future jobs/providers). | IMPLEMENTED + VERIFIED | RequestCorrelationTest validates opaque UUID-only outbound correlation; Google Places and LLM HTTP clients send X-Request-ID, processing already propagates request IDs. No medical/security metadata added. |
| 30.03 | Health endpoint (foundation configured; deployment verification recorded separately). | IMPLEMENTED + LIVE VERIFICATION PENDING | Health endpoint and hidden details implemented/tested; native PostgreSQL/Redis-backed healthy startup and deployment readiness remain blocked. |
| 30.04 | Appropriate metrics and protected operational access. | PARTIAL | Internal Micrometer processing/provider metrics are implemented. Public metrics remain closed; a production authenticated operational collector/exporter is not provisioned. No operational monitoring service is claimed. |
| 30.05 | Pagination and bounded result sizes. | IMPLEMENTED + VERIFIED | foundation, bounded workers/providers, repository limits and metrics |
| 30.06 | Indexes (foundation DDL exists; measure query plans with later data). | IMPLEMENTED + LIVE VERIFICATION PENDING | Existing owner/date/job/digest indexes and additive V1–V10 SQL pass PGlite schema checks; native PostgreSQL query plans/real workload performance remain unverified. |
| 30.07 | Asynchronous durable processing. | IMPLEMENTED + VERIFIED | foundation, bounded workers/providers, repository limits and metrics |
| 30.08 | Sensible outbound API timeouts. | IMPLEMENTED + VERIFIED | foundation, bounded workers/providers, repository limits and metrics |
| 30.09 | Retry/backoff where safe and appropriate. | IMPLEMENTED + VERIFIED | foundation, bounded workers/providers, repository limits and metrics |
| 30.10 | Graceful external-service failure and explicit missing-credentials states. | IMPLEMENTED + VERIFIED | foundation, bounded workers/providers, repository limits and metrics |

## Final counts

- IMPLEMENTED + VERIFIED: 159
- IMPLEMENTED + LIVE VERIFICATION PENDING: 44
- PARTIAL: 7
- INTENTIONALLY DEFERRED: 3
- NOT IMPLEMENTED: 0
- NOT APPLICABLE: 1

## Reconciliation of all previously incomplete requirements

The37 Phase12 LIVE VERIFICATION PENDING rows remain in that class: environment/rendered/provider
verification gaps (B), not missing implementation. Their individual evidence/limitations above are
retained. The26 other formerly incomplete rows were individually inspected:

| Former item(s) | Gap type and final decision |
| --- | --- |
|02.08|D: deletion implemented; independent immutable snapshots are required by the later specification, not a missing cascade.|
|05.03|C: defer unsupported LOINC mapping; preserve original terms and bounded canonical dictionary.|
|08.01|A: closed with existing appointment/confirmed-follow-up events and prescription document labels.|
|11.01,11.02|A: remain partial; arbitrary narrative report interpretation is beyond the current bounded verified-lab retrieval/parser. No fabricated explanation added.|
|12.03|C: optional symptom assistant context deferred to preserve the verified-fact boundary.|
|16.04|C: optional port/disabled behavior delivered; external email adapter/delivery remains not configured, not simulated.|
|17.06|D: superseded automatic snapshot invalidation is not applicable; retained historical snapshot and deletion semantics documented.|
|20.05|C: direct booking integration remains deferred; legitimate external actions are implemented.|
|21.01–21.03|A: closed with deterministic multi-domain search, safe filters and owner/page tests.|
|23.05|A: closed with safe projected Activity API/UI and A/B tests.|
|25.01|A/B: stale landing claims repaired; rendered verification remains open.|
|25.03,25.11,25.12|A/B: onboarding, Activity, Privacy/Security and Settings implemented; rendered interaction pending.|
|26.01–26.04|D: mixed JPA/JDBC architecture intentionally retained; literal all-JPA requirement remains partial rather than relabelled complete.|
|30.01,30.03,30.06|B: code/schema exists; deployed logs/health/native index plans need runtime verification.|
|30.02|A: closed opaque request-ID propagation to both external HTTP providers; regression tested.|
|30.04|A/C: operational metrics export/collector remains partial; closed endpoint is safer than exposing operational data.|

There are no remaining wholly NOT IMPLEMENTED original rows. Seven PARTIAL rows remain explicitly
limited: two assistant breadth items, four literal JPA-model items and operational metrics export.
No separate structured prescription/treatment subsystem is claimed: prescription originals are
vault documents/timeline events; previous visits are completed appointments. Search is bounded
literal English text/month/date filtering, not semantic search. Onboarding is a voluntary guide,
not a medical profile. S3 implementation is optional; the real local storage port is implemented.

The connected synthetic ClosureJourneyTest crosses API/service boundaries using H2, local OCR,
synthetic PDFs and deterministic provider fallback. Places contract tests use a local fake HTTP
upstream with explicitly synthetic responses. These are not live provider, browser or native
PostgreSQL tests. Remaining deployment gates are listed in VERIFICATION_PHASE13.md.
