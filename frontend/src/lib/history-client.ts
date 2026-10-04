import { authClient } from "./auth-client.ts";
import { checked } from "./vault-client.ts";
export type Reading = { text: string | null; number: string | null; unit: string | null; reference: string | null; low: string | null; high: string | null; sourceFlag: string | null };
export type Evidence = { documentId: string; candidateId: string; filename: string; page: number; text: string; start: number; end: number; method: string };
export type Point = { id: string; conceptId: string | null; concept: string; date: string | null; provider: string | null; specimen: string | null; method: string | null; verificationStatus: string; verifiedAt: string; original: Reading; effective: Reading; normalizedValue: string | null; normalizedUnit: string | null; evidence: Evidence };
export type Page<T> = { items: T[]; total: number; page: number; size: number };
export type Concept = { id: string; name: string; count: number };
export type TimelineEvent = { type: string; id: string; date: string | null; title: string; observation: Point | null; documentId: string | null; sourcePage?: number | null; status?: string | null; occursAt?: string | null };
export type Series = { unit: string; points: Point[]; pattern: string; explanation: string };
export type History = { history: Page<Point>; trend: { series: Series[]; limitations: string[]; truncated: boolean; windowLimit: number } };
export type Change = { conceptId: string | null; concept: string; type: string; previous: Point | null; current: Point | null; previousValue: string | null; currentValue: string | null; unit: string | null; absoluteDelta: string | null; percentageDelta: string | null; referenceTransition: string; reason: string; explanation: string; policyVersion: string };
export type Report = { id: string; filename: string; category: string; status: string; date: string | null; provider: string | null; panel: string | null; fullyReviewed: boolean };
export type Comparison = { previousReport: Report; currentReport: Report; changes: Change[]; scopeNote: string };
async function request<T>(path: string, signal?: AbortSignal): Promise<T> { return (await checked(await authClient.api(`/api/v1/history/${path}`, { signal }))).json() as Promise<T>; }
export function evidenceUrl(e: Evidence): string { return `/records/${encodeURIComponent(e.documentId)}?candidate=${encodeURIComponent(e.candidateId)}`; }
export const historyClient = {
  events: (filters: { conceptId?: string; documentId?: string; q?: string; page?: number }, signal?: AbortSignal) => { const q = new URLSearchParams({ page: String(filters.page ?? 0), size: "20" }); for (const key of ["conceptId", "documentId", "q"] as const) if (filters[key]) q.set(key, filters[key]); return request<Page<TimelineEvent>>(`events?${q}`, signal); },
  concepts: (page = 0, signal?: AbortSignal) => request<Page<Concept>>(`concepts?page=${page}&size=100`, signal),
  history: (id: string, page = 0, signal?: AbortSignal) => request<History>(`concepts/${encodeURIComponent(id)}?page=${page}&size=20`, signal),
  evidence: (id: string, signal?: AbortSignal) => request<Point>(`observations/${encodeURIComponent(id)}/evidence`, signal),
  changes: (previous: string, current: string, signal?: AbortSignal) => request<Comparison>(`changes?${new URLSearchParams({ previousReport: previous, currentReport: current })}`, signal),
};
