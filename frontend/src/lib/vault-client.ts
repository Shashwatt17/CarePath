import { authClient, ApiError } from "./auth-client.ts";
export const categories = ["LAB_REPORT", "PRESCRIPTION", "DIAGNOSTIC_REPORT", "DISCHARGE_SUMMARY", "VACCINATION_RECORD", "DOCTOR_NOTE", "REFERRAL", "MEDICAL_BILL", "OTHER"] as const;
export type Category = typeof categories[number];
export type Metadata = { documentType: Category; documentDate: string | null; providerName: string | null; tags: string[] };
export type DocumentRecord = Metadata & { id: string; originalFilename: string; mimeType: string; byteSize: number; sha256: string; status: string; uploadedAt: string; createdAt: string; updatedAt: string; version: number; pageCount: number };
export type DocumentPage = { items: DocumentRecord[]; total: number; page: number; size: number };
export const label = (value: string) => value.toLowerCase().replaceAll("_", " ").replace(/^./, (c) => c.toUpperCase());
export const fileSize = (bytes: number) => bytes < 1048576 ? `${Math.ceil(bytes / 1024)} KB` : `${(bytes / 1048576).toFixed(1)} MB`;
export function validateFile(file: Pick<File, "size" | "type" | "name">, maximum: number): string | null {
  if (!file.size) return "Choose a file that is not empty.";
  if (file.size > maximum) return `Choose a file smaller than ${fileSize(maximum)}.`;
  if (!/\.(pdf|jpe?g|png)$/i.test(file.name) || !["application/pdf", "image/jpeg", "image/png"].includes(file.type)) return "Choose a PDF, JPG, JPEG or PNG file.";
  return null;
}
export async function checked(response: Response): Promise<Response> {
  if (response.ok) return response;
  let code = "REQUEST_FAILED";
  try { const body = await response.json() as { code?: string }; code = body.code ?? code; } catch { /* Never display proxy HTML/errors. */ }
  const messages: Record<string, string> = {
    COMPARISON_LIMIT: "Choose reports with no more than 500 verified readings each.",
    REVIEW_VALIDATION: "Check the value, concept, unit and supplied reference range.",
    CORRECTION_REQUIRED: "This candidate needs explicit source review. Use Correct / verify.",
    REVIEW_STATE_CONFLICT: "This candidate was already reviewed or changed. Reload before continuing.",
    PROVENANCE_INVALID: "The source evidence could not be validated. Verification is blocked.",
    PROCESSING_LIMIT: "Processing limit reached. Wait for current jobs before trying again.",
    RETRY_NOT_ALLOWED: "This document cannot be retried. Check the original before uploading a corrected copy.",
    EXTRACTION_NOT_READY: "Extraction results are not available yet.",
    INVALID_FILE: "This file could not be accepted. Use a valid PDF, JPEG or PNG. PDFs must be unencrypted and contain no active content; filenames must not contain paths.",
    FILE_TOO_LARGE: "The file exceeds the upload limit.",
    NOT_FOUND: "This document is no longer available.",
    VERSION_CONFLICT: "This document changed in another window. Reload before saving.",
    INTEGRITY_FAILURE: "CarePath could not verify the original file. Preview and download are blocked.",
    VALIDATION_FAILED: "Check the dates, category and metadata lengths.",
    VAULT_BUSY: "The vault is busy. Please retry shortly.",
  };
  throw new ApiError(response.status, code, messages[code] ?? "CarePath could not complete this request. Please retry.");
}
const path = (id: string) => `/api/v1/documents/${encodeURIComponent(id)}`;
export const vault = {
  async config(): Promise<{ maxBytes: number }> { return (await checked(await authClient.api("/api/v1/documents/config"))).json(); },
  async list(query: URLSearchParams): Promise<DocumentPage> { return (await checked(await authClient.api(`/api/v1/documents?${query}`))).json(); },
  async get(id: string): Promise<DocumentRecord> { return (await checked(await authClient.api(path(id)))).json(); },
  async upload(file: File, metadata: Metadata, progress: (value: number) => void): Promise<DocumentRecord> {
    const form = new FormData(); form.append("file", file); form.append("metadata", new Blob([JSON.stringify(metadata)], { type: "application/json" }));
    return (await checked(await authClient.upload(form, progress))).json();
  },
  async update(id: string, version: number, metadata: Metadata): Promise<DocumentRecord> {
    return (await checked(await authClient.api(path(id), { method: "PUT", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ version, metadata }) }))).json();
  },
  async remove(id: string): Promise<boolean> { return (await checked(await authClient.api(path(id), { method: "DELETE" }))).status === 204; },
  async preview(id: string, page: number, signal: AbortSignal): Promise<Blob> { return (await checked(await authClient.api(`${path(id)}/preview?page=${page}`, { signal }))).blob(); },
  async download(document: DocumentRecord) {
    const blob = await (await checked(await authClient.api(`${path(document.id)}/download`))).blob();
    const url = URL.createObjectURL(blob); const anchor = window.document.createElement("a"); anchor.href = url; anchor.download = document.originalFilename;
    window.document.body.append(anchor); anchor.click(); anchor.remove(); setTimeout(() => URL.revokeObjectURL(url), 1000);
  },
};
