import { authClient } from "./auth-client.ts";
import { checked } from "./vault-client.ts";
export type Confidence = "HIGH" | "MEDIUM" | "LOW";
export type ProcessingState = "UPLOADED" | "PROCESSING" | "NEEDS_REVIEW" | "COMPLETED" | "FAILED";
export type Source = { page: number; start: number; end: number; text: string; method: "PDFBOX_TEXT" | "TESSERACT_OCR"; box: { x: number; y: number; width: number; height: number } | null };
export type Candidate = {
  id: string; originalTestName: string; originalValue: string | null; numericValue: number | null;
  originalUnit: string | null; referenceLower: number | null; referenceUpper: number | null;
  referenceText: string | null; abnormalFlag: string | null; reportDate: string | null;
  providerName: string | null; confidence: Confidence; reasons: string[]; source: Source;
};
export type ProcessingStatus = { state: ProcessingState; jobState: string | null; attempts: number; errorCode: string | null; retryAllowed: boolean };
export type Extraction = { id: string; documentId: string; revision: number; result: {
  classification: { category: string; confidence: Confidence; method: string; evidence: Source[] };
  information: { date: { value: string; source: Source } | null; provider: { value: string; source: Source } | null; warnings: string[] };
  candidates: Candidate[]; needsReview: boolean; ocrUsed: boolean; pipelineVersion: string;
} };
const path = (id: string) => `/api/v1/documents/${encodeURIComponent(id)}`;
async function request<T>(url: string, init?: RequestInit): Promise<T> { return (await checked(await authClient.api(url, init))).json() as Promise<T>; }
export const extractionClient = {
  status: (id: string, signal?: AbortSignal) => request<ProcessingStatus>(`${path(id)}/processing-status`, { signal }),
  process: (id: string) => request<ProcessingStatus>(`${path(id)}/process`, { method: "POST" }),
  retry: (id: string) => request<ProcessingStatus>(`${path(id)}/retry`, { method: "POST" }),
  get: (id: string, signal?: AbortSignal) => request<Extraction>(`${path(id)}/extraction`, { signal }),
  evidence: (id: string, candidate: string) => request<Source>(`${path(id)}/extraction/evidence/${encodeURIComponent(candidate)}`),
};
