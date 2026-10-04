import { authClient, ApiError } from "./auth-client.ts";
import type { Page } from "./history-client.ts";
export type Symptom = { id: string; name: string; startedAt: string; resolvedAt: string | null; severity: number; frequency: string; notes: string | null; status: string; version: number };
export type SymptomInput = Pick<Symptom, "name" | "startedAt" | "resolvedAt" | "severity" | "frequency" | "notes" | "version">;
export type Appointment = { id: string; providerName: string; specialty: string | null; startsAt: string; timeZone: string; location: string | null; phone: string | null; externalUrl: string | null; notes: string | null; followUpDate: string | null; status: string; version: number; documentIds: string[]; symptomIds: string[]; questionIds: string[]; offsets: number[] };
export type AppointmentInput = Omit<Appointment, "id" | "status">;
export type FollowUp = { id: string; documentId: string; page: number; filename: string; instruction: string; suggestedDate: string | null; confirmedDate: string | null; confirmedAt: string | null; timeZone: string | null; confidence: string; status: string; version: number };
export type Notice = { id: string; title: string; message: string; createdAt: string; readAt: string | null; sourceType: string; sourceId: string };
export type Reminder = { id: string; sourceType: string; sourceId: string; scheduledAt: string; offsetMinutes: number; status: string; deliveredAt: string | null };
export const reminderOffsets = [{ value: 0, label: "At time" }, { value: 60, label: "1 hour before" }, { value: 1440, label: "1 day before" }, { value: 2880, label: "2 days before" }, { value: 10080, label: "1 week before" }];
async function request<T>(path: string, method = "GET", body?: unknown): Promise<T> {
 const response = await authClient.api(`/api/v1/care/${path}`, { method, ...(body === undefined ? {} : { headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) }) });
 if (!response.ok) throw new ApiError(response.status, "CARE_REQUEST_FAILED", response.status === 409 ? "This item changed. Reload before saving." : response.status === 404 ? "This item is no longer available." : response.status === 400 ? "Check dates, timezone, selected records and field lengths." : "CarePath could not complete the request. Please retry.");
 return response.status === 204 ? undefined as T : response.json() as Promise<T>;
}
const id = encodeURIComponent;
export const care = {
 symptoms: (page = 0, status = "ALL", q = "") => request<Page<Symptom>>(`symptoms?${new URLSearchParams({ page: String(page), status, q })}`),
 symptom: (key: string) => request<Symptom>(`symptoms/${id(key)}`),
 saveSymptom: (key: string | null, x: SymptomInput) => request<Symptom>(key ? `symptoms/${id(key)}` : "symptoms", key ? "PUT" : "POST", { name: x.name, startedAt: x.startedAt, resolvedAt: x.resolvedAt, severity: x.severity, frequency: x.frequency, notes: x.notes, version: x.version }),
 deleteSymptom: (key: string) => request<void>(`symptoms/${id(key)}`, "DELETE"),
 appointments: (page = 0, scope = "ALL") => request<Page<Appointment>>(`appointments?${new URLSearchParams({ page: String(page), scope })}`),
 appointment: (key: string) => request<Appointment>(`appointments/${id(key)}`),
 saveAppointment: (key: string | null, x: AppointmentInput) => request<Appointment>(key ? `appointments/${id(key)}` : "appointments", key ? "PUT" : "POST", { providerName: x.providerName, specialty: x.specialty, startsAt: x.startsAt, timeZone: x.timeZone, location: x.location, phone: x.phone, externalUrl: x.externalUrl, notes: x.notes, followUpDate: x.followUpDate, documentIds: x.documentIds, symptomIds: x.symptomIds, questionIds: x.questionIds, offsets: x.offsets, version: x.version }),
 appointmentStatus: (key: string, status: "COMPLETED" | "CANCELLED", version: number) => request<Appointment>(`appointments/${id(key)}/status`, "POST", { status, version }),
 deleteAppointment: (key: string) => request<void>(`appointments/${id(key)}`, "DELETE"),
 followUps: (page = 0, scope = "ALL") => request<Page<FollowUp>>(`follow-ups?${new URLSearchParams({ page: String(page), scope })}`),
 followUp: (key: string) => request<FollowUp>(`follow-ups/${id(key)}`),
 detect: (key: string) => request<void>(`documents/${id(key)}/detect-follow-ups`, "POST"),
 decide: (key: string, action: "CONFIRM" | "IGNORE", version: number, confirmedAt: string | null, timeZone: string | null, offsets: number[]) => request<FollowUp>(`follow-ups/${id(key)}/decision`, "POST", { action, version, confirmedAt, timeZone, offsets }),
 reminders: (page = 0, status = "ALL") => request<Page<Reminder>>(`reminders?${new URLSearchParams({ page: String(page), status })}`),
 notices: (page = 0) => request<Page<Notice>>(`notifications?page=${page}`),
 unread: () => request<{ count: number }>("notifications/unread"),
 read: (key: string) => request<void>(`notifications/${id(key)}/read`, "POST"),
 readAll: () => request<void>("notifications/read-all", "POST"),
};
export function localInput(value: string): string { const d = new Date(value); const pad = (n: number) => String(n).padStart(2, "0"); return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`; }
export function offsetDate(value: string): string {
 const d = new Date(value); if (!Number.isFinite(d.getTime()) || localInput(d.toISOString()) !== value) throw new Error("This local time does not exist. Choose another time.");
 const offset = -d.getTimezoneOffset(); return `${value}:00${offset >= 0 ? "+" : "-"}${String(Math.floor(Math.abs(offset) / 60)).padStart(2, "0")}:${String(Math.abs(offset) % 60).padStart(2, "0")}`;
}
export const sourceLink = (kind: string, key: string) => `${kind === "APPOINTMENT" ? "/appointments" : "/follow-ups"}?id=${id(key)}`;
