import { authClient, ApiError } from "./auth-client.ts";
import type { Page } from "./history-client.ts";
export type PackType = "SYMPTOM" | "OBSERVATION" | "CHANGE" | "DOCUMENT" | "APPOINTMENT" | "FOLLOW_UP" | "SAVED_QUESTION" | "MANUAL_QUESTION";
export type Selection = { type: PackType; sourceId: string | null; otherId?: string | null; includeNotes?: boolean; questionText?: string | null };
export type PackInput = { title: string; reasonForVisit: string; appointmentId: string | null; packDate: string | null; items: Selection[]; version: number };
export type PackEvidence = { documentId: string; observationId: string | null; filename: string; page: number; snippet: string };
export type PackItem = { type: PackType; heading: string; fields: { label: string; value: string }[]; evidence: PackEvidence[] };
export type PackContent = { title: string; reasonForVisit: string; packDate: string | null; revision: number; items: PackItem[] };
export type PackPreview = { content: PackContent; hash: string };
export type PackSummary = { id: string; title: string; status: string; revision: number; version: number; createdAt: string; generatedAt: string | null };
export type Pack = PackSummary & { reasonForVisit: string; appointmentId: string | null; packDate: string | null; updatedAt: string; previousPackId: string | null; selections: Selection[]; snapshot: PackContent | null };
async function response(path: string, method = "GET", body?: unknown) {
 const r = await authClient.api(`/api/v1/visit-packs${path}`, { method, ...(body === undefined ? {} : { headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) }) });
 if (!r.ok) throw new ApiError(r.status, "PACK_REQUEST_FAILED", r.status === 409 ? "The draft or selected records changed. Reload and preview again." : r.status === 404 ? "This pack or selected source is no longer available." : r.status === 422 ? "Review missing sources or reduce long notes and selections." : r.status === 413 ? "The draft is too large. Reduce selected notes or questions." : "CarePath could not complete this request. Please retry.");
 return r;
}
async function request<T>(path: string, method = "GET", body?: unknown): Promise<T> { const r = await response(path, method, body); return r.status === 204 ? undefined as T : r.json() as Promise<T>; }
export function draftBody(x: PackInput): PackInput { return { title: x.title, reasonForVisit: x.reasonForVisit, appointmentId: x.appointmentId, packDate: x.packDate, version: x.version, items: x.items.map((s) => ({ type: s.type, sourceId: s.sourceId, otherId: s.otherId ?? null, includeNotes: s.includeNotes ?? false, questionText: s.questionText ?? null })) }; }
const id = encodeURIComponent;
export const packs = {
 list: (page = 0) => request<Page<PackSummary>>(`?page=${page}`),
 get: (key: string) => request<Pack>(`/${id(key)}`),
 save: (key: string | null, x: PackInput) => request<Pack>(key ? `/${id(key)}` : "", key ? "PUT" : "POST", draftBody(x)),
 preview: (key: string) => request<PackPreview>(`/${id(key)}/preview`),
 generate: (key: string, version: number, previewHash: string) => request<Pack>(`/${id(key)}/generate`, "POST", { version, previewHash }),
 revise: (key: string) => request<Pack>(`/${id(key)}/revise`, "POST"),
 remove: (key: string) => request<void>(`/${id(key)}`, "DELETE"),
 pdf: async (key: string) => (await response(`/${id(key)}/pdf`)).blob(),
};
export const packEvidenceUrl = (e: PackEvidence) => `/records/${id(e.documentId)}?page=${e.page}`;
export const packSections: Record<PackType, string> = { SYMPTOM: "User-reported symptoms", OBSERVATION: "Verified record summary", CHANGE: "Observed changes", DOCUMENT: "Selected documents", APPOINTMENT: "Appointment context", FOLLOW_UP: "Confirmed follow-ups", SAVED_QUESTION: "Questions for clinician", MANUAL_QUESTION: "Questions for clinician" };
