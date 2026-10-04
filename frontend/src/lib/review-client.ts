import { authClient } from "./auth-client.ts";
import { checked } from "./vault-client.ts";
import type { Candidate, Source } from "./extraction-client.ts";
export type Concept = { id: string; name: string; unit: string | null; standardIdentifier: string | null; version: string };
export type Fields = { testName: string; value: string; unit: string | null; referenceRange: string | null; date: string | null; conceptId: string | null };
export type Preview = { mapping: { status: string; concept: Concept | null; alternatives: Concept[] }; normalized: { value: string | null; unit: string | null; status: string }; derivedRangeStatus: string; warnings: string[]; canVerify: boolean };
export type ReviewCandidate = { id: string; documentId: string; filename: string; state: string; version: number; extracted: Candidate; decision: { canConfirm: boolean; reasons: string[]; preview: Preview }; observationId: string | null };
export type Observation = { id: string; verificationStatus: string; verifiedAt: string; original: Candidate; verified: Fields; normalization: Preview; source: Source; correctionReason: string | null };
type Queue = { items: ReviewCandidate[]; total: number; page: number; size: number };
export type ActionResult = { candidate: ReviewCandidate; observation: Observation | null };
const path = (id: string) => `/api/v1/review/candidates/${encodeURIComponent(id)}`;
async function request<T>(url: string, init?: RequestInit): Promise<T> { return (await checked(await authClient.api(url, init))).json() as Promise<T>; }
function post<T>(url: string, data: unknown) { return request<T>(url, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(data) }); }
export function originalFields(c: Candidate): Fields { return { testName: c.originalTestName, value: c.originalValue ?? "", unit: c.originalUnit, referenceRange: c.referenceText, date: c.reportDate, conceptId: null }; }
// Explicit allowlist: a response DTO must never be echoed as a correction request.
export function permittedFields(f: Fields): Fields { return { testName: f.testName, value: f.value, unit: f.unit, referenceRange: f.referenceRange, date: f.date, conceptId: f.conceptId }; }
export const reviewClient = {
  list: (documentId: string | undefined, state: string, page: number, signal?: AbortSignal) => { const q = new URLSearchParams({ state, page: String(page), size: "10" }); if (documentId) q.set("documentId", documentId); return request<Queue>(`/api/v1/review/candidates?${q}`, { signal }); },
  concepts: () => request<Concept[]>("/api/v1/terminology/concepts"),
  get: (id: string) => request<ReviewCandidate>(path(id)),
  preview: (id: string, fields: Fields) => post<Preview>(`${path(id)}/preview`, permittedFields(fields)),
  confirm: (id: string, version: number) => post<ActionResult>(`${path(id)}/confirm`, { version }),
  correct: (id: string, version: number, fields: Fields, sourceReviewed: boolean, reason: string) => post<ActionResult>(`${path(id)}/correct`, { version, fields: permittedFields(fields), sourceReviewed, reason }),
  reject: (id: string, version: number, reason: string | null) => post<ActionResult>(`${path(id)}/reject`, { version, reason }),
  observation: (id: string) => request<Observation>(`/api/v1/observations/${encodeURIComponent(id)}`),
};
