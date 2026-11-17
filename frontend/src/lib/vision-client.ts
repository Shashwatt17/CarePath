import { authClient } from "./auth-client.ts";
import { checked } from "./vault-client.ts";
import type { Source } from "./extraction-client.ts";
export type VisionStatus = { available: boolean; provider: string; model: string; consentVersion: string; pages: { page: number; reasons: string[] }[] };
export type VisionReading = { testName: string; value: string | null; unit: string | null; referenceRange: string | null; date: string | null; column: string | null; box: Source["box"] };
export type VisionView = { extractionId: string; page: number; state: string; result: { readings: { reading: VisionReading; warnings: string[] }[]; unreadable: boolean; model: string; policyVersion: string } | null };
const path = (id: string) => `/api/v1/documents/${encodeURIComponent(id)}/vision`;
async function request<T>(url: string, init?: RequestInit): Promise<T> { return (await checked(await authClient.api(url, init))).json() as Promise<T>; }
export const visionClient = {
  status: (id: string, signal?: AbortSignal) => request<VisionStatus>(path(id), { signal }),
  get: (id: string, page: number, signal?: AbortSignal) => request<VisionView>(`${path(id)}/${page}`, { signal }),
  read: (id: string, extractionId: string, page: number, consentVersion: string) => request<VisionView>(path(id), { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ extractionId, page, consent: true, consentVersion }) }),
};
