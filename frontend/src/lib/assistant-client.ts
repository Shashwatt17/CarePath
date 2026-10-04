import { authClient } from "./auth-client.ts";
import { checked } from "./vault-client.ts";
import type { Point, Page } from "./history-client.ts";
export type Ask = { question: string; conceptId?: string; observationId?: string; previousReport?: string; currentReport?: string; useAi: boolean };
export type Suggestion = { text: string; observationIds: string[] };
export type Fact = { id: string; kind: string; text: string; evidenceIds: string[] };
export type Answer = { mode: string; providerStatus: string; message: string; uncertainty: string; facts: Fact[]; evidence: { id: string; observation: Point }[]; explanation: string[]; suggestedQuestions: Suggestion[]; referenceUrls: string[] };
export type Saved = { id: string; text: string; sourceType: string; createdAt: string; updatedAt: string; version: number; evidenceMissing: boolean; evidence: Point[] };
export type Config = { providerAvailable: boolean; maxQuestionLength: number; maxObservations: number; privacy: string };
async function request<T>(path: string, init?: RequestInit): Promise<T> { const r = await checked(await authClient.api(`/api/v1/assistant/${path}`, init)); return r.status === 204 ? undefined as T : r.json() as Promise<T>; }
const body = (method: string, value: unknown): RequestInit => ({ method, headers: { "Content-Type": "application/json" }, body: JSON.stringify(value) });
export const assistantClient = {
  config: () => request<Config>("config"),
  ask: (value: Ask) => request<Answer>("ask", body("POST", { question: value.question, conceptId: value.conceptId, observationId: value.observationId, previousReport: value.previousReport, currentReport: value.currentReport, useAi: value.useAi })),
  list: (page = 0) => request<Page<Saved>>(`questions?page=${page}`),
  save: (value: Suggestion) => request<Saved>("questions", body("POST", { text: value.text, observationIds: value.observationIds })),
  edit: (id: string, text: string, version: number) => request<Saved>(`questions/${encodeURIComponent(id)}`, body("PUT", { text, version })),
  delete: (id: string) => request<void>(`questions/${encodeURIComponent(id)}`, { method: "DELETE" }),
};
